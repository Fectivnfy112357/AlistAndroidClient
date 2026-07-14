package com.textvision.alistclient.di

import android.content.Context
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.music.MusicLibraryRootStore
import com.textvision.alistclient.music.data.MusicDao
import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.data.MusicScanner
import com.textvision.alistclient.music.data.SignProvider
import com.textvision.alistclient.music.playback.MusicCache
import com.textvision.alistclient.music.playback.PlaybackController
import com.textvision.alistclient.network.api.AlistApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MusicModule {

    @Provides
    @Singleton
    fun provideLibraryRootStore(@ApplicationContext ctx: Context): MusicLibraryRootStore =
        MusicLibraryRootStore(ctx)

    @Provides
    @Singleton
    fun provideScanner(
        api: AlistApi,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): MusicScanner = MusicScanner(api, dispatcher)

    @Provides
    @Singleton
    fun provideSignProvider(
        api: AlistApi,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): SignProvider = SignProvider(api, dispatcher)

    @Provides
    @Singleton
    fun provideMusicIndexRepository(
        dao: MusicDao,
        scanner: MusicScanner,
        rootStore: MusicLibraryRootStore,
        sessionManager: SessionManager,
        okHttp: OkHttpClient,
        signProvider: SignProvider,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): MusicIndexRepository = MusicIndexRepository(
        dao = dao,
        scanner = scanner,
        rootStore = rootStore,
        sessionManager = sessionManager,
        okHttp = okHttp,
        signProvider = signProvider,
        dispatcher = dispatcher,
    )

    @Provides
    @Singleton
    fun provideMusicCache(
        @ApplicationContext context: Context,
        okHttp: OkHttpClient,
    ): MusicCache = MusicCache(context, okHttp)

    @Provides
    @Singleton
    fun providePlaybackController(
        @ApplicationContext context: Context,
        indexRepo: MusicIndexRepository,
    ): PlaybackController = PlaybackController(context, indexRepo)
}
