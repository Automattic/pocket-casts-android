package au.com.shiftyjelly.pocketcasts.endofyear.di

import au.com.shiftyjelly.pocketcasts.endofyear.StorySharingClient
import au.com.shiftyjelly.pocketcasts.endofyear.asStoryClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object EoyModule {
    @Provides
    fun storySharingClient(client: SharingClient): StorySharingClient = client.asStoryClient()
}
