package com.mgradio.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface RemoteDataSource {
    suspend fun fetchAllStations(): List<StationDto>
}

@Singleton
class FirestoreRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : RemoteDataSource {

    override suspend fun fetchAllStations(): List<StationDto> {
        val snapshot = firestore.collection("stations").get().await()
        return snapshot.documents.mapNotNull { doc ->
            runCatching {
                val rawTags = (doc.get("tags") as? List<*>)
                    ?.mapNotNull { it?.toString() }
                    ?: emptyList()

                val streamUrl = (doc.getString("stream_url") ?: doc.getString("streamUrl"))
                    ?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null

                val nombre = doc.getString("nombre")
                    ?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null

                val lastChecked = when (val raw = doc.get("last_checked") ?: doc.get("lastChecked")) {
                    is Number -> raw.toLong()
                    is String -> raw.toLongOrNull() ?: 0L
                    else -> 0L
                }

                StationDto(
                    id = doc.getString("id")?.ifBlank { doc.id } ?: doc.id,
                    nombre = nombre,
                    frecuencia = doc.getString("frecuencia") ?: "",
                    banda = doc.getString("banda") ?: "",
                    grupo = doc.getString("grupo") ?: "",
                    streamUrl = streamUrl,
                    streamType = doc.getString("stream_type") ?: doc.getString("streamType") ?: "ICECAST",
                    logoUrl = doc.getString("logo_url") ?: doc.getString("logoUrl") ?: "",
                    categoria = doc.getString("categoria") ?: "General",
                    pais = doc.getString("pais") ?: "México",
                    ciudad = doc.getString("ciudad") ?: "",
                    tags = rawTags,
                    activo = doc.getBoolean("activo") ?: true,
                    orden = (doc.getLong("orden") ?: 999L).toInt(),
                    lastChecked = lastChecked
                )
            }.getOrNull()
        }
    }
}
