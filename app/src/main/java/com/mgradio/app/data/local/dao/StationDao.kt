package com.mgradio.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mgradio.app.data.local.entity.StationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {

    @Query("SELECT * FROM stations ORDER BY orden ASC, nombre ASC")
    fun getAllStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE isFavorite = 1 ORDER BY nombre ASC")
    fun getFavoriteStations(): Flow<List<StationEntity>>

    @Query("SELECT id FROM stations WHERE isFavorite = 1")
    suspend fun getFavoriteStationIds(): List<String>

    @Query("SELECT * FROM stations WHERE id = :id")
    suspend fun getStationById(id: String): StationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<StationEntity>)

    @Query("UPDATE stations SET isFavorite = :isFavorite WHERE id = :stationId")
    suspend fun updateFavoriteStatus(stationId: String, isFavorite: Boolean)

    @Query("UPDATE stations SET activo = :activo WHERE id = :stationId")
    suspend fun updateActiveStatus(stationId: String, activo: Boolean)
}
