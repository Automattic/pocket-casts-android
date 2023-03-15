package au.com.shiftyjelly.pocketcasts.repositories.di

import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncAccountManager
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryProviderModule {

    @Provides
    @Singleton
    fun provideTokenHandler(syncAccountManager: SyncAccountManager): TokenHandler = syncAccountManager
}
