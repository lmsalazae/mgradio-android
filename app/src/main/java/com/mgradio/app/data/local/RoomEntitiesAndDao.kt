package com.mgradio.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "stations",
    indices = [
        Index(value = ["pais", "ciudad"]),
        Index(value = ["isFavorite"])
    ]
)
data class StationEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val frecuencia: String,
    val banda: String,
    val grupo: String,
    val streamUrl: String,
    val streamType: String,
    val logoUrl: String,
    val categoria: String,
    val pais: String,
    val ciudad: String,
    val tags: String = "",
    val activo: Boolean,
    val orden: Int,
    val isFavorite: Boolean,
    val lastChecked: Long
)

@Entity(tableName = "recent_plays")
data class RecentPlayEntity(
    @PrimaryKey val stationId: String,
    val playedAt: Long
)

@Dao
interface StationDao {

    @Query("SELECT * FROM stations ORDER BY orden ASC, nombre ASC")
    fun observeAllStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE isFavorite = 1 ORDER BY orden ASC, nombre ASC")
    fun observeFavoriteStations(): Flow<List<StationEntity>>

    @Query(
        """
        SELECT s.* FROM stations s
        INNER JOIN recent_plays r ON s.id = r.stationId
        ORDER BY r.playedAt DESC
        LIMIT 50
        """
    )
    fun observeRecentStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE id = :id LIMIT 1")
    suspend fun getStationById(id: String): StationEntity?

    @Query("SELECT * FROM stations ORDER BY orden ASC, nombre ASC")
    suspend fun getAllStationsOnce(): List<StationEntity>

    @Query("SELECT * FROM stations WHERE isFavorite = 1 ORDER BY orden ASC, nombre ASC")
    suspend fun getFavoriteStationsOnce(): List<StationEntity>

    @Query(
        """
        SELECT s.* FROM stations s
        INNER JOIN recent_plays r ON s.id = r.stationId
        ORDER BY r.playedAt DESC
        LIMIT 30
        """
    )
    suspend fun getRecentStationsOnce(): List<StationEntity>

    @Query("SELECT id FROM stations WHERE isFavorite = 1")
    suspend fun getFavoriteStationIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStations(stations: List<StationEntity>)

    @Query("DELETE FROM stations WHERE id NOT IN (:keepIds) AND isFavorite = 0")
    suspend fun deleteNonFavoriteStationsNotIn(keepIds: List<String>)

    @Query("UPDATE stations SET isFavorite = :isFavorite WHERE id = :stationId")
    suspend fun updateFavoriteStatus(stationId: String, isFavorite: Boolean)

    @Query("UPDATE stations SET activo = :active, lastChecked = :checkedAt WHERE id = :stationId")
    suspend fun updateStationStatus(stationId: String, active: Boolean, checkedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentPlay(recent: RecentPlayEntity)

    @Transaction
    suspend fun syncFromRemote(remoteEntities: List<StationEntity>) {
        if (remoteEntities.isEmpty()) return
        upsertStations(remoteEntities)
        deleteNonFavoriteStationsNotIn(remoteEntities.map { it.id })
    }
}
