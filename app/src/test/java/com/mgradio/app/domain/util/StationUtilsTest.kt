package com.mgradio.app.domain.util

import com.mgradio.app.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StationUtilsTest {

    private val sampleStations = listOf(
        Station(
            id = "1", nombre = "Radio Fórmula", frecuencia = "103.3", banda = "FM",
            grupo = "Grupo Fórmula", streamUrl = "https://example.com/live.mp3",
            streamType = "mp3", logoUrl = "", categoria = "Noticias",
            pais = "México", ciudad = "CDMX", tags = listOf("politica", "debate"), orden = 2
        ),
        Station(
            id = "2", nombre = "Alfa 91.3", frecuencia = "91.3", banda = "FM",
            grupo = "GRC", streamUrl = "https://example.com/stream.m3u8",
            streamType = "hls", logoUrl = "", categoria = "Pop",
            pais = "México", ciudad = "CDMX", tags = listOf("rock", "pop", "hits"), orden = 1
        ),
        Station(
            id = "3", nombre = "Fórmula Melódica", frecuencia = "97.9", banda = "FM",
            grupo = "GDL", streamUrl = "https://example.com/audio.aac",
            streamType = "aac", logoUrl = "", categoria = "Balada",
            pais = "México", ciudad = "Guadalajara", tags = listOf("romantica"), orden = 3
        )
    )

    @Test
    fun `matchesSearchQuery searches accents-insensitive across name frequency category and tags`() {
        assertTrue(StationUtils.matchesSearchQuery(sampleStations[0], "formula"))
        assertTrue(StationUtils.matchesSearchQuery(sampleStations[0], "103.3"))
        assertTrue(StationUtils.matchesSearchQuery(sampleStations[0], "noticias"))
        assertTrue(StationUtils.matchesSearchQuery(sampleStations[1], "hits"))
    }

    @Test
    fun `filterAndSortStations prioritizes prefix matches`() {
        val result = StationUtils.filterAndSortStations(
            stations = sampleStations,
            query = "formula",
            country = "Todos",
            city = "Todas",
            category = "Todas",
            allCountriesLabel = "Todos",
            allCitiesLabel = "Todas",
            allCategoriesLabel = "Todas"
        )
        assertEquals(2, result.size)
        assertEquals("3", result.first().id) // "Fórmula Melódica" starts with "formula"
    }

    @Test
    fun `detectMimeType identifies hls pls m3u aac mp3 and unknown`() {
        assertEquals("application/x-mpegURL", StationUtils.detectMimeType("http://a.com/x.m3u8", ""))
        assertEquals("audio/x-scpls", StationUtils.detectMimeType("http://a.com/listen.pls", ""))
        assertEquals("audio/aac", StationUtils.detectMimeType("http://a.com/stream", "aac"))
        assertEquals("audio/mpeg", StationUtils.detectMimeType("http://a.com/live.mp3", ""))
        assertNull(StationUtils.detectMimeType("http://a.com/stream", "unknown"))
    }
}
