package com.mgradio.app.domain.util

import com.mgradio.app.domain.model.Station
import java.text.Normalizer

object StationUtils {

    private val combiningMarksRegex = Regex("\\p{InCombiningDiacriticalMarks}+")

    fun normalizeText(input: String): String {
        val decomposed = Normalizer.normalize(input.trim().lowercase(), Normalizer.Form.NFD)
        return combiningMarksRegex.replace(decomposed, "")
    }

    /**
     * Búsqueda normalizada sin acentos por nombre, frecuencia, categoría y tags.
     */
    fun matchesSearchQuery(station: Station, rawQuery: String): Boolean {
        val query = normalizeText(rawQuery)
        if (query.isEmpty()) return true
        if (normalizeText(station.nombre).contains(query)) return true
        if (normalizeText(station.frecuencia).contains(query)) return true
        if (normalizeText(station.categoria).contains(query)) return true
        if (station.tags.any { normalizeText(it).contains(query) }) return true
        return false
    }

    /**
     * Filtra y ordena priorizando coincidencias cuyo nombre comienza con el prefijo buscado.
     */
    fun filterAndSortStations(
        stations: List<Station>,
        query: String,
        country: String,
        city: String,
        category: String,
        allCountriesLabel: String,
        allCitiesLabel: String,
        allCategoriesLabel: String
    ): List<Station> {
        val normQuery = normalizeText(query)
        return stations
            .filter { station ->
                (country == allCountriesLabel || station.pais.equals(country, ignoreCase = true)) &&
                    (city == allCitiesLabel || station.ciudad.equals(city, ignoreCase = true)) &&
                    (category == allCategoriesLabel || station.categoria.equals(category, ignoreCase = true)) &&
                    matchesSearchQuery(station, query)
            }
            .sortedWith(
                compareByDescending<Station> {
                    normQuery.isNotEmpty() && normalizeText(it.nombre).startsWith(normQuery)
                }.thenBy { it.orden }
                    .thenBy { normalizeText(it.nombre) }
            )
    }

    /**
     * Detección unificada de MIME type para Media3 / ExoPlayer.
     */
    fun detectMimeType(streamUrl: String, declaredType: String): String? {
        val url = streamUrl.lowercase()
        val type = declaredType.lowercase()
        return when {
            url.contains(".m3u8") || type.contains("hls") -> "application/x-mpegURL"
            url.contains(".pls") || type.contains("pls") -> "audio/x-scpls"
            url.contains(".m3u") || type.contains("m3u") -> "audio/x-mpegurl"
            url.contains(".aac") || type.contains("aac") -> "audio/aac"
            url.contains(".ogg") || type.contains("ogg") -> "audio/ogg"
            url.contains(".mp3") || type.contains("mp3") || type.contains("mpeg") -> "audio/mpeg"
            else -> null
        }
    }
}
