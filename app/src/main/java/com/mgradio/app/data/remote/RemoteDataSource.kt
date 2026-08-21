package com.mgradio.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.mgradio.app.data.remote.model.StationDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface RemoteDataSource {
    suspend fun getAllStations(): List<StationDto>
}

class FirestoreRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : RemoteDataSource {

    override suspend fun getAllStations(): List<StationDto> {
        return try {
            val snapshot = firestore.collection("stations")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(StationDto::class.java)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
