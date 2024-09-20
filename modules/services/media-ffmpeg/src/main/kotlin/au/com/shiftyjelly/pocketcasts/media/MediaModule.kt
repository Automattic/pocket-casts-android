package au.com.shiftyjelly.pocketcasts.media

import android.content.Context
import au.com.shiftyjelly.pocketcasts.sharing.MediaService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {
    @Provides
    fun provideMediaService(
        @ApplicationContext context: Context,
    ): MediaService = FFmpegMediaService(context)
}
