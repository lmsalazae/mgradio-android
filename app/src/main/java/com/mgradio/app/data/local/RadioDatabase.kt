package com.mgradio.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mgradio.app.data.local.dao.StationDao
import com.mgradio.app.data.local.entity.StationEntity

@Database(
    entities = [StationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RadioDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
}
