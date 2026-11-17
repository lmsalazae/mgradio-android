package com.mgradio.app.media

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.mgradio.app.domain.model.PlaybackStatus
import com.mgradio.app.domain.model.PlayerUiState
import com.mgradio.app.domain.model.Station
import com.mgradio.app.domain.util.StationUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackBridge: PlaybackBridge,
    private val streamRecorder: StreamRecorder
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val equalizerController = AudioEqualizerController()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var sleepTimerJob: Job? = null

    private val _playerState = MutableStateFlow(
        PlayerUiState(volumeFraction = readSystemVolumeFraction())
    )
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    init {
        connectController()
        observeBridgeAndChildren()
    }

    private fun observeBridgeAndChildren() {
        scope.launch {
            playbackBridge.audioSessionId.collect { sessionId ->
                if (sessionId > 0) {
                    equalizerController.attachToSession(sessionId)
                }
            }
        }
        scope.launch {
            playbackBridge.icyNowPlaying.collect { title ->
                _playerState.update { it.copy(nowPlayingTitle = title) }
            }
        }
        scope.launch {
            playbackBridge.reconnectAttempt.collect { attempt ->
                _playerState.update { state ->
                    state.copy(
                        reconnectAttempt = attempt,
                        status = if (attempt > 0) PlaybackStatus.RECONNECTING else state.status
                    )
                }
            }
        }
        scope.launch {
            equalizerController.state.collect { eqState ->
                _playerState.update { it.copy(equalizerState = eqState) }
            }
        }
        scope.launch {
            streamRecorder.state.collect { recState ->
                _playerState.update { it.copy(recordingState = recState) }
            }
        }
    }

    private fun connectController() {
        val sessionToken = SessionToken(context, ComponentName(context, RadioMediaService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val controller = future.get()
                    mediaController = controller
                    controller.addListener(playerListener)
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackStatus()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackStatus()
        }

        override fun onPlayerError(error: PlaybackException) {
            val attempt = playbackBridge.reconnectAttempt.value
            if (attempt > 0 && attempt <= RadioMediaService.MAX_RECONNECT_ATTEMPTS) {
                _playerState.update {
                    it.copy(
                        status = PlaybackStatus.RECONNECTING,
                        reconnectAttempt = attempt,
                        errorMessage = null
                    )
                }
                return
            }
            val station = _playerState.value.currentStation
            val msg = translateError(error)
            _playerState.update {
                it.copy(
                    status = PlaybackStatus.ERROR,
                    errorMessage = msg,
                    failedStation = station
                )
            }
        }
    }

    private fun updatePlaybackStatus() {
        val ctrl = mediaController ?: return
        val status = when {
            playbackBridge.reconnectAttempt.value > 0 -> PlaybackStatus.RECONNECTING
            ctrl.playbackState == Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
            ctrl.isPlaying -> PlaybackStatus.PLAYING
            ctrl.playbackState == Player.STATE_READY && !ctrl.playWhenReady -> PlaybackStatus.PAUSED
            ctrl.playbackState == Player.STATE_IDLE -> PlaybackStatus.IDLE
            else -> _playerState.value.status
        }
        _playerState.update {
            it.copy(
                status = status,
                errorMessage = if (status == PlaybackStatus.PLAYING) null else it.errorMessage
            )
        }
    }

    fun restoreLastStationPreview(station: Station) {
        if (_playerState.value.currentStation == null) {
            _playerState.update { it.copy(currentStation = station, status = PlaybackStatus.IDLE) }
        }
    }

    fun playStation(station: Station) {
        if (streamRecorder.state.value.isRecording) {
            streamRecorder.stopRecording()
        }
        _playerState.update {
            it.copy(
                currentStation = station,
                status = PlaybackStatus.BUFFERING,
                nowPlayingTitle = null,
                errorMessage = null,
                failedStation = null,
                reconnectAttempt = 0
            )
        }
        val ctrl = mediaController ?: return
        val mediaItem = buildMediaItem(station)
        ctrl.setMediaItem(mediaItem)
        ctrl.prepare()
        ctrl.play()
    }

    fun togglePlayPause() {
        val ctrl = mediaController ?: return
        val current = _playerState.value.currentStation ?: return
        if (ctrl.isPlaying) {
            ctrl.pause()
        } else if (ctrl.playbackState == Player.STATE_IDLE || ctrl.playbackState == Player.STATE_ENDED) {
            playStation(current)
        } else {
            ctrl.play()
        }
    }

    fun stop() {
        streamRecorder.stopRecording()
        cancelSleepTimer()
        mediaController?.stop()
        mediaController?.clearMediaItems()
        _playerState.update {
            it.copy(
                status = PlaybackStatus.IDLE,
                nowPlayingTitle = null,
                reconnectAttempt = 0
            )
        }
    }

    fun setVolumeFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (clamped * maxVol).toInt().coerceIn(0, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        _playerState.update { it.copy(volumeFraction = clamped) }
    }

    fun syncSystemVolume() {
        _playerState.update { it.copy(volumeFraction = readSystemVolumeFraction()) }
    }

    fun startSleepTimer(minutes: Int, onFinished: () -> Unit) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _playerState.update { it.copy(sleepTimerRemainingSeconds = null) }
            return
        }
        var remaining = minutes * 60L
        _playerState.update { it.copy(sleepTimerRemainingSeconds = remaining) }
        sleepTimerJob = scope.launch {
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _playerState.update { it.copy(sleepTimerRemainingSeconds = remaining) }
            }
            stop()
            _playerState.update { it.copy(sleepTimerRemainingSeconds = null) }
            onFinished()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _playerState.update { it.copy(sleepTimerRemainingSeconds = null) }
    }

    fun toggleRecording(customTreeUri: String?, onError: (String) -> Unit) {
        if (streamRecorder.state.value.isRecording) {
            streamRecorder.stopRecording()
        } else {
            val station = _playerState.value.currentStation ?: return
            streamRecorder.startRecording(station, customTreeUri, onError)
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) = equalizerController.setEnabled(enabled)
    fun selectEqualizerPreset(index: Int) = equalizerController.selectPreset(index)
    fun setEqualizerBand(band: Short, levelMb: Short) = equalizerController.setBandLevel(band, levelMb)

    fun clearError() {
        _playerState.update { it.copy(errorMessage = null, failedStation = null) }
    }

    private fun readSystemVolumeFraction(): Float {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (cur.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    }

    private fun buildMediaItem(station: Station): MediaItem {
        val subtitle = listOf(station.frecuencia, station.banda, station.ciudad)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        val metadata = MediaMetadata.Builder()
            .setTitle(station.nombre)
            .setSubtitle(subtitle)
            .setArtist(station.ciudad.ifBlank { station.pais })
            .setStation(station.nombre)
            .setArtworkUri(station.logoUrl.takeIf { it.isNotBlank() }?.let(Uri::parse))
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .build()

        val builder = MediaItem.Builder()
            .setMediaId(station.id)
            .setUri(station.streamUrl)
            .setMediaMetadata(metadata)

        StationUtils.detectMimeType(station.streamUrl, station.streamType)?.let {
            builder.setMimeType(it)
        }
        return builder.build()
    }

    private fun translateError(error: PlaybackException): String {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "Sin conexión con la emisora tras varios intentos."
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                "La transmisión de esta emisora no está disponible."
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                "Formato de audio no compatible."
            else -> "No se pudo reproducir la emisora (${error.errorCodeName})."
        }
    }
}
