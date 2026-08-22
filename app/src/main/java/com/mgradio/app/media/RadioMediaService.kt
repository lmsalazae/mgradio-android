package com.mgradio.app.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mgradio.app.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RadioMediaService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    companion object {
        const val CHANNEL_ID = "radio_playback_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY_STATION = "com.mgradio.app.action.PLAY_STATION"
        const val EXTRA_STATION_ID = "extra_station_id"
        const val EXTRA_STATION_NAME = "extra_station_name"
        const val EXTRA_STATION_FREQ = "extra_station_freq"
        const val EXTRA_STREAM_URL = "extra_stream_url"
        const val EXTRA_LOGO_URL = "extra_logo_url"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(20000)

        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        error.printStackTrace()
                        try {
                            stop()
                            clearMediaItems()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                })
            }

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val callback = object : MediaSession.Callback {}

        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(callback)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PLAY_STATION) {
            val stationId = intent.getStringExtra(EXTRA_STATION_ID) ?: ""
            val stationName = intent.getStringExtra(EXTRA_STATION_NAME) ?: "Emisora de Radio"
            val stationFreq = intent.getStringExtra(EXTRA_STATION_FREQ) ?: ""
            val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL) ?: ""
            val logoUrl = intent.getStringExtra(EXTRA_LOGO_URL) ?: ""

            if (streamUrl.isNotBlank()) {
                playStream(
                    id = stationId,
                    title = stationName,
                    subtitle = stationFreq,
                    streamUrl = streamUrl,
                    logoUrl = logoUrl
                )
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun playStream(
        id: String,
        title: String,
        subtitle: String,
        streamUrl: String,
        logoUrl: String
    ) {
        val trimmed = streamUrl.trim()
        if (trimmed.isBlank()) return

        try {
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setArtworkUri(if (logoUrl.isNotBlank()) Uri.parse(logoUrl.trim()) else null)
                .build()

            val builder = MediaItem.Builder()
                .setMediaId(id)
                .setUri(Uri.parse(trimmed))
                .setMediaMetadata(metadata)

            val urlLower = trimmed.lowercase()
            if (urlLower.contains(".m3u8") || urlLower.contains("/hls") || urlLower.contains("playlist")) {
                builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            } else if (urlLower.contains(".aac")) {
                builder.setMimeType(MimeTypes.AUDIO_AAC)
            } else if (urlLower.contains(".mp3")) {
                builder.setMimeType(MimeTypes.AUDIO_MPEG)
            }

            exoPlayer?.apply {
                stop()
                clearMediaItems()
                setMediaItem(builder.build())
                prepare()
                playWhenReady = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        try {
            mediaSession?.run {
                player.release()
                release()
                mediaSession = null
            }
            exoPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Reproducción de Radio"
            val descriptionText = "Controles de reproducción en vivo"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
