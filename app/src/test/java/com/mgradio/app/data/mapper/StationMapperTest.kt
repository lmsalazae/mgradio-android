package com.mgradio.app.data.mapper

import com.mgradio.app.data.local.entity.StationEntity
import com.mgradio.app.data.mapper.toDomain
import com.mgradio.app.data.mapper.toEntity
import com.mgradio.app.data.remote.model.StationDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationMapperTest {

    @Test
    fun stationEntity_toDomain_mapsCorrectly() {
        val entity = StationEntity(
            id = "cdmx-1",
            nombre = "Universal 88.1 FM",
            frecuencia = "88.1 FM",
            banda = "FM",
            grupo = "Grupo Radio Centro",
            streamUrl = "https://stream.example.com/live",
            streamType = "ICECAST",
            logoUrl = "https://example.com/logo.png",
            categoria = "Clásicos en Inglés",
            pais = "México",
            ciudad = "Ciudad de México",
            activo = true,
            orden = 1,
            isFavorite = true,
            lastChecked = "2026-08-12T20:00:00Z"
        )

        val domain = entity.toDomain()

        assertEquals("cdmx-1", domain.id)
        assertEquals("Universal 88.1 FM", domain.nombre)
        assertEquals("88.1 FM", domain.frecuencia)
        assertEquals("Ciudad de México", domain.ciudad)
        assertTrue(domain.isFavorite)
    }

    @Test
    fun stationDto_toEntity_preservesFavoriteStatus() {
        val dto = StationDto(
            id = "cdmx-2",
            nombre = "Reactor 105.7",
            frecuencia = "105.7 FM",
            banda = "FM",
            grupo = "IMER",
            streamUrl = "https://stream.imer.link/reactor",
            streamType = "ICECAST",
            logoUrl = "https://example.com/reactor.png",
            categoria = "Rock",
            pais = "México",
            ciudad = "Ciudad de México",
            activo = true,
            orden = 2,
            lastChecked = "2026-08-12T20:00:00Z"
        )

        val entity = dto.toEntity(isFavorite = true)

        assertEquals("cdmx-2", entity.id)
        assertTrue(entity.isFavorite)
        assertEquals("IMER", entity.grupo)
    }
}
