package com.mgradio.app.media

import com.mgradio.app.domain.util.StationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RadioPlayerManagerTest {

    @Test
    fun detectMimeType_detectsHlsFromStreamType() {
        val mime = StationUtils.detectMimeType(
            streamUrl = "https://stream.radioformula.com.mx/live",
            streamType = "HLS"
        )
        assertEquals("application/x-mpegURL", mime)
    }

    @Test
    fun detectMimeType_detectsHlsFromM3u8Url() {
        val mime = StationUtils.detectMimeType(
            streamUrl = "https://server.example.com/live/playlist.m3u8",
            streamType = "ICECAST"
        )
        assertEquals("application/x-mpegURL", mime)
    }

    @Test
    fun detectMimeType_detectsAac() {
        val mime = StationUtils.detectMimeType(
            streamUrl = "https://server.example.com/audio.aac",
            streamType = "AAC"
        )
        assertEquals("audio/mp4a-latm", mime)
    }

    @Test
    fun detectMimeType_detectsMp3() {
        val mime = StationUtils.detectMimeType(
            streamUrl = "https://server.example.com/audio.mp3",
            streamType = "MP3"
        )
        assertEquals("audio/mpeg", mime)
    }

    @Test
    fun detectMimeType_returnsNullForGenericIcecast() {
        val mime = StationUtils.detectMimeType(
            streamUrl = "https://server.example.com/stream",
            streamType = "ICECAST"
        )
        assertNull(mime)
    }
}
