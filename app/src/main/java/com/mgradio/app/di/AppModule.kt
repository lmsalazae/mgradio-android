package com.mgradio.app.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import com.mgradio.app.data.local.RadioDatabase
import com.mgradio.app.data.local.dao.StationDao
import com.mgradio.app.data.remote.FirestoreRemoteDataSource
import com.mgradio.app.data.remote.RemoteDataSource
import com.mgradio.app.data.repository.StationRepositoryImpl
import com.mgradio.app.domain.repository.StationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRadioDatabase(@ApplicationContext context: Context): RadioDatabase {
        return Room.databaseBuilder(
            context,
            RadioDatabase::class.java,
            "mgradio.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    @Provides
    @Singleton
    fun provideStationDao(database: RadioDatabase): StationDao {
        return database.stationDao()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideRemoteDataSource(firestore: FirebaseFirestore): RemoteDataSource {
        return FirestoreRemoteDataSource(firestore)
    }

    @Provides
    @Singleton
    fun provideStationRepository(
        stationDao: StationDao,
        remoteDataSource: RemoteDataSource
    ): StationRepository {
        return StationRepositoryImpl(stationDao, remoteDataSource)
    }
}
