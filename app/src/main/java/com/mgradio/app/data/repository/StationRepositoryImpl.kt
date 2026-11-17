package com.mgradio.app.data.repository

import com.mgradio.app.data.local.RecentPlayEntity
import com.mgradio.app.data.local.StationDao
import com.mgradio.app.data.remote.RemoteDataSource
import com.mgradio.app.data.remote.StationMapper
import com.mgradio.app.domain.model.Station
import com.mgradio.app.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StationRepositoryImpl @Inject constructor(
    private val stationDao: StationDao,
    private val remoteDataSource: RemoteDataSource
) : StationRepository {

    override fun getAllStations(): Flow<List<Station>> =
        stationDao.observeAllStations().map { list -> list.map(StationMapper::entityToDomain) }

    override fun getFavoriteStations(): Flow<List<Station>> =
        stationDao.observeFavoriteStations().map { list -> list.map(StationMapper::entityToDomain) }

    override fun getRecentStations(): Flow<List<Station>> =
        stationDao.observeRecentStations().map { list -> list.map(StationMapper::entityToDomain) }

    override suspend fun getStationById(id: String): Station? =
        stationDao.getStationById(id)?.let(StationMapper::entityToDomain)

    override suspend fun refreshStations(): Result<Int> {
        return runCatching {
            val remoteDtos = remoteDataSource.fetchAllStations()
            if (remoteDtos.isNotEmpty()) {
                val favoriteIds = stationDao.getFavoriteStationIds().toSet()
                val entities = remoteDtos.map { dto ->
                    StationMapper.dtoToEntity(dto, isFavorite = dto.id in favoriteIds)
                }
                stationDao.syncFromRemote(entities)
            }
            remoteDtos.size
        }
    }

    override suspend fun toggleFavorite(stationId: String, isFavorite: Boolean) {
        stationDao.updateFavoriteStatus(stationId, isFavorite)
    }

    override suspend fun markStationStatus(stationId: String, active: Boolean) {
        stationDao.updateStationStatus(stationId, active, System.currentTimeMillis())
    }

    override suspend fun recordRecentPlay(stationId: String) {
        stationDao.insertRecentPlay(RecentPlayEntity(stationId, System.currentTimeMillis()))
    }
}
