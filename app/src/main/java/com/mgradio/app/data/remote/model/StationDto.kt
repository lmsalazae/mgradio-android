package com.mgradio.app.data.remote.model

import com.google.firebase.firestore.PropertyName

data class StationDto(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("nombre") @set:PropertyName("nombre") var nombre: String = "",
    @get:PropertyName("frecuencia") @set:PropertyName("frecuencia") var frecuencia: String = "",
    @get:PropertyName("banda") @set:PropertyName("banda") var banda: String = "",
    @get:PropertyName("grupo") @set:PropertyName("grupo") var grupo: String = "",
    @get:PropertyName("stream_url") @set:PropertyName("stream_url") var streamUrl: String = "",
    @get:PropertyName("stream_type") @set:PropertyName("stream_type") var streamType: String = "ICECAST",
    @get:PropertyName("logo_url") @set:PropertyName("logo_url") var logoUrl: String = "",
    @get:PropertyName("categoria") @set:PropertyName("categoria") var categoria: String = "",
    @get:PropertyName("pais") @set:PropertyName("pais") var pais: String = "",
    @get:PropertyName("ciudad") @set:PropertyName("ciudad") var ciudad: String = "",
    @get:PropertyName("activo") @set:PropertyName("activo") var activo: Boolean = true,
    @get:PropertyName("orden") @set:PropertyName("orden") var orden: Int = 999,
    @get:PropertyName("tags") @set:PropertyName("tags") var tags: List<String> = emptyList(),
    @get:PropertyName("last_checked") @set:PropertyName("last_checked") var lastChecked: String = ""
)
