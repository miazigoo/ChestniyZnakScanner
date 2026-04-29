package ru.devandprod.chestniyznak.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import ru.devandprod.chestniyznak.data.local.dao.MarkingCodeDao
import ru.devandprod.chestniyznak.data.local.dao.ScanLogDao
import ru.devandprod.chestniyznak.data.local.database.AppDatabase
import ru.devandprod.chestniyznak.data.repository.LocalChestniyZnakRepository
import ru.devandprod.chestniyznak.domain.repository.ChestniyZnakRepository

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "chestniy_znak.db",
    ).build()

    @Provides
    fun provideMarkingCodeDao(database: AppDatabase): MarkingCodeDao = database.markingCodeDao()

    @Provides
    fun provideScanLogDao(database: AppDatabase): ScanLogDao = database.scanLogDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindChestniyZnakRepository(
        implementation: LocalChestniyZnakRepository,
    ): ChestniyZnakRepository
}
