package com.mgradio.app.media

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.mgradio.app.domain.model.Station
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class PlaybackState {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR
}

data class PlayerUiState(
    val currentStation: Station? = null,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val errorMessage: String? = null,
    val failedStation: Station? = null
)

@Singleton
class RadioPlayerManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var pendingStation: Station? = null

    init {
        initController()
    }

    private fun initController() {
        try {
            val sessionToken = SessionToken(
                context,
                ComponentName(context, RadioMediaService::class.java)
            )

            controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture?.let { future ->
                Futures.addCallback(
                    future,
                    object : FutureCallback<MediaController> {
                        override fun onSuccess(result: MediaController?) {
                            mediaController = result
                            setupControllerListener()
                            pendingStation?.let {
                                val st = it
                                pendingStation = null
                                playStation(st)
                            }
                        }

                        override fun onFailure(t: Throwable) {
                            t.printStackTrace()
                            _uiState.value = _uiState.value.copy(
                                currentStation = null,
                                playbackState = PlaybackState.ERROR,
                                errorMessage = "No se pudo iniciar el servicio de audio en segundo plano."
                            )
                        }
                    },
                    MoreExecutors.directExecutor()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.value = _uiState.value.copy(
                currentStation = null,
                playbackState = PlaybackState.ERROR,
                errorMessage = "Error al inicializar el reproductor de radio."
            )
        }
    }

    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (_uiState.value.playbackState != PlaybackState.ERROR) {
                    _uiState.value = _uiState.value.copy(
                        playbackState = if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED
                    )
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        if (_uiState.value.playbackState != PlaybackState.ERROR) {
                            _uiState.value = _uiState.value.copy(
                                playbackState = PlaybackState.BUFFERING,
                                errorMessage = null
                            )
                        }
                    }
                    Player.STATE_READY -> {
                        if (_uiState.value.playbackState != PlaybackState.ERROR) {
                            val isPlaying = mediaController?.isPlaying == true
                            _uiState.value = _uiState.value.copy(
                                playbackState = if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED,
                                errorMessage = null
                            )
                        }
                    }
                    Player.STATE_ENDED -> {
                        _uiState.value = _uiState.value.copy(playbackState = PlaybackState.IDLE)
                    }
                    Player.STATE_IDLE -> {
                        if (_uiState.value.playbackState != PlaybackState.ERROR) {
                            _uiState.value = _uiState.value.copy(playbackState = PlaybackState.IDLE)
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()
                val station = _uiState.value.currentStation

                // Detener y limpiar el controlador de forma segura para evitar estados corruptos
                try {
                    mediaController?.stop()
                    mediaController?.clearMediaItems()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val explanation = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "No se pudo conectar al servidor de transmisión de '${station?.nombre}' (tiempo de espera o red)."

                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                    PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
                        "El enlace de streaming de '${station?.nombre}' no está disponible o rechazó la conexión (HTTP ${error.errorCodeName})."

                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                        "El formato de audio o lista de reproducción de '${station?.nombre}' no es compatible con el reproductor."

                    else ->
                        "La emisora '${station?.nombre ?: "seleccionada"}' no se encuentra transmitiendo en este momento."
                }

                _uiState.value = _uiState.value.copy(
                    currentStation = null,
                    playbackState = PlaybackState.ERROR,
                    errorMessage = explanation,
                    failedStation = station
                )
            }
        })
    }

    fun playStation(station: Station) {
        val streamUrl = station.streamUrl.trim()

        if (streamUrl.isBlank() || (!streamUrl.startsWith("http://", ignoreCase = true) && !streamUrl.startsWith("https://", ignoreCase = true))) {
            _uiState.value = _uiState.value.copy(
                currentStation = null,
                playbackState = PlaybackState.ERROR,
                errorMessage = "La emisora '${station.nombre}' no tiene una URL de transmisión web válida.",
                failedStation = station
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            currentStation = station,
            playbackState = PlaybackState.BUFFERING,
            errorMessage = null,
            failedStation = null
        )

        val controller = mediaController
        if (controller == null) {
            pendingStation = station
            return
        }

        try {
            val mediaItem = buildMediaItem(station)

            controller.stop()
            controller.clearMediaItems()
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.value = _uiState.value.copy(
                currentStation = null,
                playbackState = PlaybackState.ERROR,
                errorMessage = "No se pudo iniciar la reproducción de '${station.nombre}': ${e.localizedMessage ?: "Error desconocido"}",
                failedStation = station
            )
        }
    }

    fun togglePlayPause() {
        try {
            mediaController?.let { controller ->
                if (controller.isPlaying) {
                    controller.pause()
                } else {
                    controller.play()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            mediaController?.stop()
            mediaController?.clearMediaItems()
            _uiState.value = PlayerUiState()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            playbackState = PlaybackState.IDLE,
            errorMessage = null,
            failedStation = null,
            currentStation = null
        )
    }

    companion object {
        fun buildMediaItem(station: Station): MediaItem {
            val uri = Uri.parse(station.streamUrl.trim())
            val metadata = MediaMetadata.Builder()
                .setTitle(station.nombre)
                .setSubtitle(station.frecuencia)
                .setArtworkUri(if (station.logoUrl.isNotBlank()) Uri.parse(station.logoUrl.trim()) else null)
                .build()

            val builder = MediaItem.Builder()
                .setMediaId(station.id)
                .setUri(uri)
                .setMediaMetadata(metadata)

            val urlLower = station.streamUrl.lowercase()
            val typeLower = station.streamType.lowercase()

            when {
                typeLower == "hls" || urlLower.contains(".m3u8") || urlLower.contains("/hls") || urlLower.contains("playlist") -> {
                    builder.setMimeType(MimeTypes.APPLICATION_M3U8)
                }
                typeLower == "aac" || urlLower.contains(".aac") -> {
                    builder.setMimeType(MimeTypes.AUDIO_AAC)
                }
                typeLower == "mp3" || urlLower.contains(".mp3") -> {
                    builder.setMimeType(MimeTypes.AUDIO_MPEG)
                }
            }

            return builder.build()
        }
    }
}
