package com.textvision.alistclient.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.AuthRepositoryContract
import com.textvision.alistclient.data.local.AppDatabase
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.data.secure.EncryptedCredentialStore
import com.textvision.alistclient.file.FileRepository
import com.textvision.alistclient.file.FileRepositoryContract
import com.textvision.alistclient.home.HomeRepository
import com.textvision.alistclient.home.HomeRepositoryContract
import com.textvision.alistclient.network.AuthInterceptor
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.common.network.NetworkMonitor
import com.textvision.alistclient.common.network.NetworkMonitorContract
import com.textvision.alistclient.transfer.data.TransferDao
import dagger.Binds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "transfer_tasks.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    @Provides
    fun provideTransferDao(database: AppDatabase): TransferDao = database.transferDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
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
