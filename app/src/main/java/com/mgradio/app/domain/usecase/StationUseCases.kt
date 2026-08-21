package com.mgradio.app.domain.usecase

import com.mgradio.app.domain.model.Station
import com.mgradio.app.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllStationsUseCase @Inject constructor(
    private val repository: StationRepository
) {
    operator fun invoke(): Flow<List<Station>> {
        return repository.getAllStations()
    }
}

class RefreshStationsUseCase @Inject constructor(
    private val repository: StationRepository
) {
    suspend operator fun invoke() {
        repository.refreshStations()
    }
}

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: StationRepository
) {
    suspend operator fun invoke(stationId: String, isFavorite: Boolean) {
        repository.toggleFavorite(stationId, isFavorite)
    }
}

class GetFavoriteStationsUseCase @Inject constructor(
    private val repository: StationRepository
) {
    operator fun invoke(): Flow<List<Station>> {
        return repository.getFavoriteStations()
    }
}

class MarkStationOfflineUseCase @Inject constructor(
    private val repository: StationRepository
) {
    suspend operator fun invoke(stationId: String) {
        repository.updateStationActiveStatus(stationId, false)
    }
}
