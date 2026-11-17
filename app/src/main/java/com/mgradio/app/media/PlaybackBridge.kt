package com.mgradio.app.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Puente en memoria entre RadioMediaService y RadioPlayerManager para
 * audioSessionId (ecualizador), metadatos ICY en vivo y estado de reconexión.
 */
@Singleton
class PlaybackBridge @Inject constructor() {

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    private val _icyNowPlaying = MutableStateFlow<String?>(null)
    val icyNowPlaying: StateFlow<String?> = _icyNowPlaying.asStateFlow()

    private val _reconnectAttempt = MutableStateFlow(0)
    val reconnectAttempt: StateFlow<Int> = _reconnectAttempt.asStateFlow()

    fun updateAudioSessionId(sessionId: Int) {
        if (sessionId > 0) _audioSessionId.value = sessionId
    }

    fun updateIcyTitle(title: String?) {
        _icyNowPlaying.value = title?.takeIf { it.isNotBlank() }
    }

    fun updateReconnectAttempt(attempt: Int) {
        _reconnectAttempt.value = attempt
    }
}
