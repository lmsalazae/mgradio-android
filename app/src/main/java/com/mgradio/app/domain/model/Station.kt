package com.mgradio.app.domain.model

data class Station(
    val id: String,
    val nombre: String,
    val frecuencia: String,
    val banda: String,
    val grupo: String,
    val streamUrl: String,
    val streamType: String,
    val logoUrl: String,
    val categoria: String,
    val pais: String,
    val ciudad: String,
    val activo: Boolean,
    val orden: Int,
    val isFavorite: Boolean = false,
    val lastChecked: String = ""
)
