package com.mgradio.app.domain.util

import com.mgradio.app.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationFilterTest {

    private val sampleStations = listOf(
        Station(
            id = "s1",
            nombre = "Universal",
            frecuencia = "88.1 FM",
            banda = "FM",
            grupo = "GRC",
            streamUrl = "https://stream1",
            streamType = "ICECAST",
            logoUrl = "",
            categoria = "Clásicos",
            pais = "México",
            ciudad = "Ciudad de México",
            activo = true,
            orden = 1
        ),
        Station(
            id = "s2",
            nombre = "Radio Formula",
            frecuencia = "104.1 FM",
            banda = "FM",
            grupo = "Formula",
            streamUrl = "https://stream2",
            streamType = "ICECAST",
            logoUrl = "",
            categoria = "Noticias",
            pais = "Mexico",
            ciudad = "Guadalajara",
            activo = true,
            orden = 2
        ),
        Station(
            id = "s3",
            nombre = "Stereo Joya",
            frecuencia = "93.7 FM",
            banda = "FM",
            grupo = "GRC",
            streamUrl = "https://stream3",
            streamType = "ICECAST",
            logoUrl = "",
            categoria = "Pop",
            pais = "Colombia",
            ciudad = "Bogotá",
            activo = true,
            orden = 3
        )
    )

    @Test
    fun matchesCountry_handlesAccentVariations() {
        assertTrue(StationUtils.matchesCountry("México", "Mexico"))
        assertTrue(StationUtils.matchesCountry("Mexico", "México"))
        assertTrue(StationUtils.matchesCountry("México", "Todos los países"))
        assertFalse(StationUtils.matchesCountry("Colombia", "México"))
    }

    @Test
    fun matchesCity_filtersCorrectly() {
        assertTrue(StationUtils.matchesCity("Ciudad de México", "Ciudad de México"))
        assertTrue(StationUtils.matchesCity("Ciudad de México", "Todas las ciudades"))
        assertFalse(StationUtils.matchesCity("Guadalajara", "Bogotá"))
    }

    @Test
    fun filterStations_appliesAllFilters() {
        val filtered = StationUtils.filterStations(
            stations = sampleStations,
            country = "México",
            city = "Guadalajara",
            category = "Noticias"
        )

        assertEquals(1, filtered.size)
        assertEquals("s2", filtered[0].id)
    }

    @Test
    fun extractCountries_includesDefaultOptionAndSortedList() {
        val countries = StationUtils.extractCountries(sampleStations)

        assertEquals(listOf("Todos los países", "Colombia", "Mexico", "México"), countries)
    }

    @Test
    fun extractCities_filtersByCountry() {
        val cities = StationUtils.extractCities(sampleStations, "Colombia")

        assertEquals(listOf("Todas las ciudades", "Bogotá"), cities)
    }

    @Test
    fun normalizeText_removesAccentsAndLowercases() {
        assertEquals("exito", StationUtils.normalizeText("éxito"))
        assertEquals("cancion", StationUtils.normalizeText("Canción"))
        assertEquals("bogota", StationUtils.normalizeText("BOGOTÁ"))
        assertEquals("mexico", StationUtils.normalizeText("México"))
    }

    @Test
    fun matchesSearchQuery_handlesAccentsAndCase() {
        assertTrue(StationUtils.matchesSearchQuery("Radio Éxitos", "exito"))
        assertTrue(StationUtils.matchesSearchQuery("Radio Exitos", "éxito"))
        assertTrue(StationUtils.matchesSearchQuery("Universal Stereo", "universal"))
        assertTrue(StationUtils.matchesSearchQuery("Universal Stereo", "STEREO"))
        assertFalse(StationUtils.matchesSearchQuery("Universal Stereo", "Caracol"))
    }

    @Test
    fun filterStations_withSearchQuery_filtersAccurately() {
        val results = StationUtils.filterStations(
            stations = sampleStations,
            searchQuery = "formula"
        )
        assertEquals(1, results.size)
        assertEquals("s2", results[0].id)
    }

    @Test
    fun filterStations_prioritizesPrefixMatches() {
        val stations = listOf(
            Station(
                id = "1",
                nombre = "Super Radio",
                frecuencia = "100.1 FM",
                banda = "FM",
                grupo = "",
                streamUrl = "",
                streamType = "ICECAST",
                logoUrl = "",
                categoria = "",
                pais = "México",
                ciudad = "CDMX",
                activo = true,
                orden = 1
            ),
            Station(
                id = "2",
                nombre = "Radio Uno",
                frecuencia = "101.1 FM",
                banda = "FM",
                grupo = "",
                streamUrl = "",
                streamType = "ICECAST",
                logoUrl = "",
                categoria = "",
                pais = "México",
                ciudad = "CDMX",
                activo = true,
                orden = 2
            )
        )

        val results = StationUtils.filterStations(stations = stations, searchQuery = "rad")
        assertEquals(2, results.size)
        // "Radio Uno" empieza con "rad", por lo que debe ir primero (coincidencia más próxima)
        assertEquals("2", results[0].id)
        assertEquals("1", results[1].id)
    }

    @Test
    fun filterStations_dynamicTabCounts_adjustsToCountryAndCity() {
        val activeMexico = StationUtils.filterStations(
            stations = sampleStations,
            country = "México"
        )
        val activeColombia = StationUtils.filterStations(
            stations = sampleStations,
            country = "Colombia"
        )
        val activeGuadalajara = StationUtils.filterStations(
            stations = sampleStations,
            country = "México",
            city = "Guadalajara"
        )

        assertEquals(2, activeMexico.size)
        assertEquals(1, activeColombia.size)
        assertEquals(1, activeGuadalajara.size)
        assertEquals("s2", activeGuadalajara[0].id)
    }
}
