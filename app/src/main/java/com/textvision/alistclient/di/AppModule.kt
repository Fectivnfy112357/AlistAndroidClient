package com.textvision.alistclient.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.textvision.alistclient.admin.settings.SettingsRepository
import com.textvision.alistclient.admin.settings.SettingsRepositoryContract
import com.textvision.alistclient.admin.storage.StorageRepository
import com.textvision.alistclient.admin.storage.StorageRepositoryContract
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.AuthRepositoryContract
import com.textvision.alistclient.data.local.AppDatabase
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.data.secure.EncryptedCredentialStore
import com.textvision.alistclient.file.FileRepository
import com.textvision.alistclient.file.FileRepositoryContract
import com.textvision.alistclient.ui.feature.home.HomeRepository
import com.textvision.alistclient.ui.feature.home.HomeRepositoryContract
import com.textvision.alistclient.network.AuthInterceptor
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.common.network.NetworkMonitor
import com.textvision.alistclient.common.network.NetworkMonitorContract
import com.textvision.alistclient.transfer.RealTransferExecutor
import com.textvision.alistclient.transfer.TransferExecutor
import com.textvision.alistclient.transfer.data.TransferDao
import com.textvision.alistclient.music.data.MusicDao
import dagger.Binds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
abstract class CredentialModule {
    @Binds
    @Singleton
    abstract fun bindCredentialStore(impl: EncryptedCredentialStore): CredentialStore

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepository): AuthRepositoryContract

    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepository): FileRepositoryContract

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: NetworkMonitor): NetworkMonitorContract

    @Binds
    @Singleton
    abstract fun bindHomeRepository(impl: HomeRepository): HomeRepositoryContract

    @Binds
    @Singleton
    abstract fun bindStorageRepository(impl: StorageRepository): StorageRepositoryContract

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepository): SettingsRepositoryContract
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `transfer_tasks` (
                    `id` TEXT NOT NULL,
                    `fileName` TEXT NOT NULL,
                    `remotePath` TEXT NOT NULL,
                    `localPath` TEXT,
                    `sourceUri` TEXT,
                    `bytesDone` INTEGER NOT NULL,
                    `totalBytes` INTEGER NOT NULL,
                    `type` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `failureReason` TEXT,
                    `createdAtMillis` INTEGER NOT NULL,
                    `updatedAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `smoke` (
                    `id` TEXT NOT NULL,
                    `value` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `music_artist` (
                    `name` TEXT NOT NULL,
                    `path` TEXT NOT NULL,
                    `albumCount` INTEGER NOT NULL,
                    `songCount` INTEGER NOT NULL,
                    PRIMARY KEY(`name`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `music_album` (
                    `artist` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `path` TEXT NOT NULL,
                    `coverPath` TEXT,
                    `songCount` INTEGER NOT NULL,
                    `id` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `music_song` (
                    `path` TEXT NOT NULL,
                    `trackNo` TEXT NOT NULL,
                    `trackNoInt` INTEGER,
                    `artist` TEXT NOT NULL,
                    `album` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `lrcPath` TEXT,
                    `coverPath` TEXT,
                    `sizeBytes` INTEGER NOT NULL,
                    PRIMARY KEY(`path`)
                )
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "transfer_tasks.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    @Provides
    fun provideTransferDao(database: AppDatabase): TransferDao = database.transferDao()

    @Provides
    fun provideMusicDao(database: AppDatabase): MusicDao = database.musicDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(@IoDispatcher dispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcher)

    @Provides
    @Singleton
    fun provideTransferExecutor(impl: RealTransferExecutor): TransferExecutor = impl

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(0, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl("http://127.0.0.1:5244/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideAlistApi(retrofit: Retrofit): AlistApi =
        retrofit.create(AlistApi::class.java)
}
