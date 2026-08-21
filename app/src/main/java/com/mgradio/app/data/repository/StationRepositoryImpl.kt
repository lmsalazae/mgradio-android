package com.mgradio.app.data.repository

import com.mgradio.app.data.local.dao.StationDao
import com.mgradio.app.data.mapper.toDomain
import com.mgradio.app.data.mapper.toEntity
import com.mgradio.app.data.remote.RemoteDataSource
import com.mgradio.app.domain.model.Station
import com.mgradio.app.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StationRepositoryImpl @Inject constructor(
    private val stationDao: StationDao,
    private val remoteDataSource: RemoteDataSource
) : StationRepository {

    override fun getAllStations(): Flow<List<Station>> {
        return stationDao.getAllStations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteStations(): Flow<List<Station>> {
        return stationDao.getFavoriteStations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun toggleFavorite(stationId: String, isFavorite: Boolean) {
        stationDao.updateFavoriteStatus(stationId, isFavorite)
    }

    override suspend fun updateStationActiveStatus(stationId: String, activo: Boolean) {
        stationDao.updateActiveStatus(stationId, activo)
    }

    override suspend fun refreshStations() {
        try {
            val remoteDtos = remoteDataSource.getAllStations()
            if (remoteDtos.isNotEmpty()) {
                val favoriteIds = stationDao.getFavoriteStationIds().toSet()
                val newEntities = remoteDtos.map { dto ->
                    val isFav = favoriteIds.contains(dto.id)
                    dto.toEntity(isFavorite = isFav)
                }
                stationDao.insertStations(newEntities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
