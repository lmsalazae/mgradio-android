package com.mgradio.app.domain.usecase

import com.mgradio.app.domain.model.Station
import com.mgradio.app.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllStationsUseCase @Inject constructor(
    private val repository: StationRepository
) {
    operator fun invoke(): Flow<List<Station>> = repository.getAllStations()
}

class GetFavoriteStationsUseCase @Inject constructor(
    private val repository: StationRepository
) {
    operator fun invoke(): Flow<List<Station>> = repository.getFavoriteStations()
}

class GetRecentStationsUseCase @Inject constructor(
    private val repository: StationRepository
) {
    operator fun invoke(): Flow<List<Station>> = repository.getRecentStations()
}

class RefreshStationsUseCase @Inject constructor(
    private val repository: StationRepository
) {
    suspend operator fun invoke(): Result<Int> = repository.refreshStations()
}

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: StationRepository
) {
    suspend operator fun invoke(stationId: String, isFavorite: Boolean) {
        repository.toggleFavorite(stationId, isFavorite)
    }
}

class MarkStationStatusUseCase @Inject constructor(
    private val repository: StationRepository
) {
    suspend operator fun invoke(stationId: String, active: Boolean) {
        repository.markStationStatus(stationId, active)
    }
}

class RecordRecentPlayUseCase @Inject constructor(
    private val repository: StationRepository
) {
    suspend operator fun invoke(stationId: String) {
        repository.recordRecentPlay(stationId)
    }
}
