package ru.devandprod.chestniyznak.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import ru.devandprod.chestniyznak.BuildConfig
import ru.devandprod.chestniyznak.data.local.dao.MarkingCodeDao
import ru.devandprod.chestniyznak.data.local.dao.ScanLogDao
import ru.devandprod.chestniyznak.data.local.database.AppDatabase
import ru.devandprod.chestniyznak.data.remote.api.AccountApi
import ru.devandprod.chestniyznak.data.remote.api.ChestniyZnakApi
import ru.devandprod.chestniyznak.data.remote.api.OrdersApi
import ru.devandprod.chestniyznak.data.remote.api.PackingApi
import ru.devandprod.chestniyznak.data.remote.auth.BearerAuthInterceptor
import ru.devandprod.chestniyznak.data.remote.auth.BearerTokenAuthenticator
import ru.devandprod.chestniyznak.data.remote.auth.CsrfInterceptor
import ru.devandprod.chestniyznak.data.remote.auth.LanguageHeaderInterceptor
import ru.devandprod.chestniyznak.data.remote.auth.PersistentCookieJar
import ru.devandprod.chestniyznak.data.remote.auth.RemoteAuthRepository
import ru.devandprod.chestniyznak.data.remote.auth.TsdSurfaceInterceptor
import ru.devandprod.chestniyznak.data.settings.ThemePreferencesRepository
import ru.devandprod.chestniyznak.data.repository.HybridChestniyZnakRepository
import ru.devandprod.chestniyznak.data.repository.RemoteOrdersRepository
import ru.devandprod.chestniyznak.data.repository.RemotePackingRepository
import ru.devandprod.chestniyznak.domain.repository.ChestniyZnakRepository
import ru.devandprod.chestniyznak.domain.repository.AuthRepository
import ru.devandprod.chestniyznak.domain.repository.OrdersRepository
import ru.devandprod.chestniyznak.domain.repository.PackingRepository
import ru.devandprod.chestniyznak.domain.repository.ThemeRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

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
    fun provideSharedPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences = context.getSharedPreferences("chz_prefs", Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "chestniy_znak.db",
    )
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()

    @Provides
    fun provideMarkingCodeDao(database: AppDatabase): MarkingCodeDao = database.markingCodeDao()

    @Provides
    fun provideScanLogDao(database: AppDatabase): ScanLogDao = database.scanLogDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: PersistentCookieJar,
        csrfInterceptor: CsrfInterceptor,
        tsdSurfaceInterceptor: TsdSurfaceInterceptor,
        languageHeaderInterceptor: LanguageHeaderInterceptor,
        bearerAuthInterceptor: BearerAuthInterceptor,
        bearerTokenAuthenticator: BearerTokenAuthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .authenticator(bearerTokenAuthenticator)
        .addInterceptor(languageHeaderInterceptor)
        .addInterceptor(tsdSurfaceInterceptor)
        .addInterceptor(bearerAuthInterceptor)
        .addInterceptor(csrfInterceptor)
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.ENABLE_HTTP_LOGGING) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            },
        )
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideAccountApi(retrofit: Retrofit): AccountApi = retrofit.create(AccountApi::class.java)

    @Provides
    @Singleton
    fun provideChestniyZnakApi(retrofit: Retrofit): ChestniyZnakApi = retrofit.create(ChestniyZnakApi::class.java)

    @Provides
    @Singleton
    fun providePackingApi(retrofit: Retrofit): PackingApi = retrofit.create(PackingApi::class.java)

    @Provides
    @Singleton
    fun provideOrdersApi(retrofit: Retrofit): OrdersApi = retrofit.create(OrdersApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindChestniyZnakRepository(
        implementation: HybridChestniyZnakRepository,
    ): ChestniyZnakRepository

    @Binds
    @Singleton
    abstract fun bindPackingRepository(
        implementation: RemotePackingRepository,
    ): PackingRepository

    @Binds
    @Singleton
    abstract fun bindOrdersRepository(
        implementation: RemoteOrdersRepository,
    ): OrdersRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        implementation: RemoteAuthRepository,
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        implementation: ThemePreferencesRepository,
    ): ThemeRepository
}
