package com.textvision.alistclient.di

import android.content.Context
import androidx.room.Room
import com.textvision.alistclient.data.local.AppDatabase
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.data.secure.EncryptedCredentialStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CredentialModule {
    @Binds
    @Singleton
    abstract fun bindCredentialStore(impl: EncryptedCredentialStore): CredentialStore
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "transfer_tasks.db")
            .fallbackToDestructiveMigration()
            .build()
}
