package com.mgradio.app.domain.repository

import com.mgradio.app.domain.model.Station
import kotlinx.coroutines.flow.Flow

interface StationRepository {
    fun getAllStations(): Flow<List<Station>>
    fun getFavoriteStations(): Flow<List<Station>>
    suspend fun toggleFavorite(stationId: String, isFavorite: Boolean)
    suspend fun updateStationActiveStatus(stationId: String, activo: Boolean)
    suspend fun refreshStations()
}
