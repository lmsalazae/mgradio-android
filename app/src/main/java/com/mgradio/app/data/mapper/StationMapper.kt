package com.mgradio.app.data.mapper

import com.mgradio.app.data.local.entity.StationEntity
import com.mgradio.app.data.remote.model.StationDto
import com.mgradio.app.domain.model.Station

fun StationEntity.toDomain(): Station = Station(
    id = id,
    nombre = nombre,
    frecuencia = frecuencia,
    banda = banda,
    grupo = grupo,
    streamUrl = streamUrl,
    streamType = streamType,
    logoUrl = logoUrl,
    categoria = categoria,
    pais = pais,
    ciudad = ciudad,
    activo = activo,
    orden = orden,
    isFavorite = isFavorite,
    lastChecked = lastChecked
)

fun StationDto.toEntity(isFavorite: Boolean = false): StationEntity = StationEntity(
    id = id,
    nombre = nombre,
    frecuencia = frecuencia,
    banda = banda,
    grupo = grupo,
    streamUrl = streamUrl,
    streamType = streamType,
    logoUrl = logoUrl,
    categoria = categoria,
    pais = pais,
    ciudad = ciudad,
    activo = activo,
    orden = orden,
    isFavorite = isFavorite,
    lastChecked = lastChecked
)
