package com.mgradio.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [StationEntity::class, RecentPlayEntity::class],
    version = 2,
    exportSchema = false
)
abstract class RadioDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao

    companion object {
        /**
         * Migración explícita v1 -> v2: añade columna 'tags' a 'stations' y crea 'recent_plays'
         * preservando el 100% de favoritos del usuario.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stations ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recent_plays (
                        stationId TEXT NOT NULL PRIMARY KEY,
                        playedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
