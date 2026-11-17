package com.mgradio.app.data.repository

import com.mgradio.app.data.local.RecentPlayEntity
import com.mgradio.app.data.local.StationDao
import com.mgradio.app.data.local.StationEntity
import com.mgradio.app.data.remote.RemoteDataSource
import com.mgradio.app.data.remote.StationDto
import com.mgradio.app.data.remote.StationMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationMapperAndRepoTest {

    @Test
    fun `mapper preserves tags and favorite flag`() {
        val dto = StationDto(
            id = "s1",
            nombre = "Estación 1",
            streamUrl = "https://stream.test/1",
            tags = listOf("rock", "indie")
        )
        val entity = StationMapper.dtoToEntity(dto, isFavorite = true)
        assertTrue(entity.isFavorite)
        assertEquals("rock,indie", entity.tags)

        val domain = StationMapper.entityToDomain(entity)
        assertEquals(listOf("rock", "indie"), domain.tags)
        assertTrue(domain.isFavorite)
    }

    @Test
    fun `refreshStations preserves favorites and removes obsolete non-favorites`() = runTest {
        val dao = FakeStationDao(
            initial = listOf(
                StationEntity("fav_old", "Fav Antigua", "", "", "", "http://1", "", "", "Pop", "MX", "CDMX", "", true, 1, isFavorite = true, 0L),
                StationEntity("nonfav_old", "Vieja", "", "", "", "http://2", "", "", "Pop", "MX", "CDMX", "", true, 2, isFavorite = false, 0L)
            )
        )
        val remote = object : RemoteDataSource {
            override suspend fun fetchAllStations(): List<StationDto> = listOf(
                StationDto(id = "new_1", nombre = "Nueva", streamUrl = "http://3")
            )
        }
        val repo = StationRepositoryImpl(dao, remote)
        val result = repo.refreshStations()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())

        val stored = dao.getAllStationsOnce()
        assertTrue(stored.any { it.id == "fav_old" && it.isFavorite })
        assertTrue(stored.any { it.id == "new_1" && !it.isFavorite })
        assertFalse(stored.any { it.id == "nonfav_old" })
    }

    private class FakeStationDao(initial: List<StationEntity>) : StationDao {
        private val state = MutableStateFlow(initial)

        override fun observeAllStations(): Flow<List<StationEntity>> = state
        override fun observeFavoriteStations(): Flow<List<StationEntity>> =
            state.map { list -> list.filter { it.isFavorite } }
        override fun observeRecentStations(): Flow<List<StationEntity>> = state
        override suspend fun getStationById(id: String): StationEntity? = state.value.firstOrNull { it.id == id }
        override suspend fun getAllStationsOnce(): List<StationEntity> = state.value
        override suspend fun getFavoriteStationsOnce(): List<StationEntity> = state.value.filter { it.isFavorite }
        override suspend fun getRecentStationsOnce(): List<StationEntity> = emptyList()
        override suspend fun getFavoriteStationIds(): List<String> = state.value.filter { it.isFavorite }.map { it.id }

        override suspend fun upsertStations(stations: List<StationEntity>) {
            val map = state.value.associateBy { it.id }.toMutableMap()
            stations.forEach { map[it.id] = it }
            state.value = map.values.toList()
        }

        override suspend fun deleteNonFavoriteStationsNotIn(keepIds: List<String>) {
            val keep = keepIds.toSet()
            state.value = state.value.filter { it.id in keep || it.isFavorite }
        }

        override suspend fun updateFavoriteStatus(stationId: String, isFavorite: Boolean) {
            state.value = state.value.map { if (it.id == stationId) it.copy(isFavorite = isFavorite) else it }
        }

        override suspend fun updateStationStatus(stationId: String, active: Boolean, checkedAt: Long) {
            state.value = state.value.map { if (it.id == stationId) it.copy(activo = active, lastChecked = checkedAt) else it }
        }

        override suspend fun insertRecentPlay(recent: RecentPlayEntity) {}
    }
}
