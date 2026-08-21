package com.mgradio.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stations",
    indices = [
        Index(value = ["pais", "ciudad"]),
        Index(value = ["isFavorite"])
    ]
)
data class StationEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val frecuencia: String = "",
    val banda: String = "",
    val grupo: String = "",
    val streamUrl: String,
    val streamType: String = "ICECAST",
    val logoUrl: String = "",
    val categoria: String = "",
    val pais: String,
    val ciudad: String,
    val activo: Boolean = true,
    val orden: Int = 999,
    val isFavorite: Boolean = false,
    val lastChecked: String = ""
)
