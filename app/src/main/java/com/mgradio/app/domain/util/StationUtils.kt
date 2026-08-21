package com.mgradio.app.domain.util

import com.mgradio.app.domain.model.Station
import java.text.Normalizer

object StationUtils {
    const val ALL_COUNTRIES = "Todos los países"
    const val ALL_CITIES = "Todas las ciudades"

    private val DIACRITICS_REGEX = "\\p{InCombiningDiacriticalMarks}+".toRegex()

    fun normalizeText(text: String): String {
        val normalized = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
        return DIACRITICS_REGEX.replace(normalized, "").lowercase()
    }

    fun matchesCountry(stationCountry: String, selectedCountry: String): Boolean {
        if (selectedCountry == ALL_COUNTRIES) return true
        val normStation = normalizeText(stationCountry)
        val normSelected = normalizeText(selectedCountry)
        if (normStation == normSelected) return true
        if (normStation.contains(normSelected)) return true
        return false
    }

    fun matchesCity(stationCity: String, selectedCity: String): Boolean {
        if (selectedCity == ALL_CITIES) return true
        val normStation = normalizeText(stationCity)
        val normSelected = normalizeText(selectedCity)
        if (normStation == normSelected) return true
        return normStation.contains(normSelected)
    }

    fun matchesCategory(stationCategory: String, selectedCategory: String?): Boolean {
        if (selectedCategory == null) return true
        return stationCategory.equals(selectedCategory, ignoreCase = true)
    }

    fun matchesSearchQuery(stationName: String, query: String): Boolean {
        if (query.isBlank()) return true
        val normName = normalizeText(stationName)
        val normQuery = normalizeText(query)
        return normName.contains(normQuery)
    }

    fun filterStations(
        stations: List<Station>,
        country: String = ALL_COUNTRIES,
        city: String = ALL_CITIES,
        category: String? = null,
        searchQuery: String = ""
    ): List<Station> {
        val filtered = stations.filter { station ->
            matchesCountry(station.pais, country) &&
                    matchesCity(station.ciudad, city) &&
                    matchesCategory(station.categoria, category) &&
                    matchesSearchQuery(station.nombre, searchQuery)
        }

        if (searchQuery.isBlank()) {
            return filtered
        }

        val normQuery = normalizeText(searchQuery)
        // Posicionar las coincidencias más próximas: las que inician con el texto buscado primero
        return filtered.sortedWith(
            compareBy(
                { !normalizeText(it.nombre).startsWith(normQuery) },
                { it.orden },
                { it.nombre }
            )
        )
    }

    fun extractCountries(stations: List<Station>): List<String> {
        val countries = mutableListOf(ALL_COUNTRIES)
        countries.addAll(
            stations.map { it.pais.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        )
        return countries.distinct()
    }

    fun extractCities(stations: List<Station>, selectedCountry: String): List<String> {
        val filtered = stations.filter { matchesCountry(it.pais, selectedCountry) }
        val cities = mutableListOf(ALL_CITIES)
        cities.addAll(
            filtered.map { it.ciudad.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        )
        return cities.distinct()
    }

    fun extractCategories(stations: List<Station>): List<String> {
        return stations.map { it.categoria.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun detectMimeType(streamUrl: String, streamType: String): String? {
        val urlLower = streamUrl.trim().lowercase()
        val typeLower = streamType.trim().lowercase()

        return when {
            typeLower == "hls" || urlLower.contains(".m3u8") || urlLower.contains("/hls") || urlLower.contains("playlist") -> {
                "application/x-mpegURL"
            }
            typeLower == "aac" || urlLower.contains(".aac") -> {
                "audio/mp4a-latm"
            }
            typeLower == "mp3" || urlLower.contains(".mp3") -> {
                "audio/mpeg"
            }
            else -> null
        }
    }
}
