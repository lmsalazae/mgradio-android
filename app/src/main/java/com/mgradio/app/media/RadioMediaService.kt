package com.mgradio.app.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.mgradio.app.MainActivity
import com.mgradio.app.data.local.StationDao
import com.mgradio.app.data.local.StationEntity
import com.mgradio.app.domain.util.StationUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class RadioMediaService : MediaLibraryService() {

    @Inject lateinit var stationDao: StationDao
    @Inject lateinit var playbackBridge: PlaybackBridge

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer

    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("VLC/3.0.20 LibVLC/3.0.20")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(20_000)
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 50_000, 1500, 3000)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                playbackBridge.updateAudioSessionId(audioSessionId)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    reconnectAttempt = 0
                    playbackBridge.updateReconnectAttempt(0)
                    playbackBridge.updateAudioSessionId(player.audioSessionId)
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                val icy = mediaMetadata.title?.toString()
                    ?: mediaMetadata.displayTitle?.toString()
                    ?: mediaMetadata.artist?.toString()
                val stationName = player.currentMediaItem?.mediaMetadata?.station?.toString()
                if (!icy.isNullOrBlank() && icy != stationName) {
                    playbackBridge.updateIcyTitle(icy)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                playbackBridge.updateIcyTitle(null)
                reconnectJob?.cancel()
                reconnectAttempt = 0
                playbackBridge.updateReconnectAttempt(0)
            }

            override fun onPlayerError(error: PlaybackException) {
                val isNetworkError = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED

                val currentItem = player.currentMediaItem
                if (isNetworkError && currentItem != null && reconnectAttempt < MAX_RECONNECT_ATTEMPTS) {
                    reconnectAttempt++
                    playbackBridge.updateReconnectAttempt(reconnectAttempt)
                    val backoffMs = 1000L * (1 shl (reconnectAttempt - 1)) // 1s, 2s, 4s
                    reconnectJob?.cancel()
                    reconnectJob = serviceScope.launch {
                        delay(backoffMs)
                        player.setMediaItem(currentItem)
                        player.prepare()
                        player.playWhenReady = true
                    }
                } else {
                    reconnectAttempt = 0
                    playbackBridge.updateReconnectAttempt(0)
                }
            }
        })

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        reconnectJob?.cancel()
        serviceScope.cancel()
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproducción de Radio",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles multimedia de mgRadio"
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Callback para Android Auto (MediaLibraryService): expone Favoritas, Recientes y Todas.
     */
    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootExtras = Bundle().apply {
                putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
            }
            val root = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("mgRadio")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setExtras(rootExtras)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch(Dispatchers.IO) {
                val items = when (parentId) {
                    ROOT_ID -> listOf(
                        browsableFolder(NODE_FAVORITES, "Favoritas"),
                        browsableFolder(NODE_RECENTS, "Recientes"),
                        browsableFolder(NODE_ALL, "Todas las emisoras")
                    )
                    NODE_FAVORITES -> stationDao.getFavoriteStationsOnce().map(::stationToMediaItem)
                    NODE_RECENTS -> stationDao.getRecentStationsOnce().map(::stationToMediaItem)
                    NODE_ALL -> stationDao.getAllStationsOnce().take(100).map(::stationToMediaItem)
                    else -> emptyList()
                }
                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
            }
            return future
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val future = SettableFuture.create<MutableList<MediaItem>>()
            serviceScope.launch(Dispatchers.IO) {
                val resolved = mediaItems.map { item ->
                    if (item.localConfiguration != null) {
                        item
                    } else {
                        val entity = stationDao.getStationById(item.mediaId)
                        if (entity != null) stationToMediaItem(entity) else item
                    }
                }.toMutableList()
                future.set(resolved)
            }
            return future
        }
    }

    private fun browsableFolder(id: String, title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
                    .build()
            )
            .build()
    }

    private fun stationToMediaItem(entity: StationEntity): MediaItem {
        val subtitle = listOf(entity.frecuencia, entity.banda, entity.ciudad)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        val metadata = MediaMetadata.Builder()
            .setTitle(entity.nombre)
            .setSubtitle(subtitle)
            .setArtist(entity.ciudad.ifBlank { entity.pais })
            .setStation(entity.nombre)
            .setArtworkUri(entity.logoUrl.takeIf { it.isNotBlank() }?.let(Uri::parse))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .build()

        val builder = MediaItem.Builder()
            .setMediaId(entity.id)
            .setUri(entity.streamUrl)
            .setMediaMetadata(metadata)

        StationUtils.detectMimeType(entity.streamUrl, entity.streamType)?.let {
            builder.setMimeType(it)
        }
        return builder.build()
    }

    companion object {
        private const val CHANNEL_ID = "radio_playback_channel"
        private const val ROOT_ID = "mgradio_root"
        private const val NODE_FAVORITES = "node_favorites"
        private const val NODE_RECENTS = "node_recents"
        private const val NODE_ALL = "node_all"
        const val MAX_RECONNECT_ATTEMPTS = 3
    }
}
