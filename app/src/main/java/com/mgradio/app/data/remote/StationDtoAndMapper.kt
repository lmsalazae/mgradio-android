package com.mgradio.app.data.remote

import com.mgradio.app.data.local.StationEntity
import com.mgradio.app.domain.model.Station

data class StationDto(
    val id: String = "",
    val nombre: String = "",
    val frecuencia: String = "",
    val banda: String = "",
    val grupo: String = "",
    val streamUrl: String = "",
    val streamType: String = "",
    val logoUrl: String = "",
    val categoria: String = "",
    val pais: String = "",
    val ciudad: String = "",
    val tags: List<String> = emptyList(),
    val activo: Boolean = true,
    val orden: Int = 0,
    val lastChecked: Long = 0L
)

object StationMapper {

    fun entityToDomain(entity: StationEntity): Station = Station(
        id = entity.id,
        nombre = entity.nombre,
        frecuencia = entity.frecuencia,
        banda = entity.banda,
        grupo = entity.grupo,
        streamUrl = entity.streamUrl,
        streamType = entity.streamType,
        logoUrl = entity.logoUrl,
        categoria = entity.categoria,
        pais = entity.pais,
        ciudad = entity.ciudad,
        tags = entity.tags.split(',').map { it.trim() }.filter { it.isNotEmpty() },
        activo = entity.activo,
        orden = entity.orden,
        isFavorite = entity.isFavorite,
        lastChecked = entity.lastChecked
    )

    fun dtoToEntity(dto: StationDto, isFavorite: Boolean): StationEntity = StationEntity(
        id = dto.id,
        nombre = dto.nombre,
        frecuencia = dto.frecuencia,
        banda = dto.banda,
        grupo = dto.grupo,
        streamUrl = dto.streamUrl,
        streamType = dto.streamType,
        logoUrl = dto.logoUrl,
        categoria = dto.categoria,
        pais = dto.pais,
        ciudad = dto.ciudad,
        tags = dto.tags.joinToString(",") { it.trim() },
        activo = dto.activo,
        orden = dto.orden,
        isFavorite = isFavorite,
        lastChecked = dto.lastChecked
    )
}
