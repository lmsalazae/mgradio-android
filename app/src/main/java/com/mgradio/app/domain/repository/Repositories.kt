package com.mgradio.app.domain.repository

import com.mgradio.app.domain.model.Station
import com.mgradio.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface StationRepository {
    fun getAllStations(): Flow<List<Station>>
    fun getFavoriteStations(): Flow<List<Station>>
    fun getRecentStations(): Flow<List<Station>>
    suspend fun getStationById(id: String): Station?
    suspend fun refreshStations(): Result<Int>
    suspend fun toggleFavorite(stationId: String, isFavorite: Boolean)
    suspend fun markStationStatus(stationId: String, active: Boolean)
    suspend fun recordRecentPlay(stationId: String)
}

interface PreferencesRepository {
    val preferencesFlow: Flow<UserPreferences>
    suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences)
}
