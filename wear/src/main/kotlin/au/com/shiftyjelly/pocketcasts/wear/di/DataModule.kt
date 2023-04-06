package au.com.shiftyjelly.pocketcasts.wear.di

import com.google.android.horologist.media.data.mapper.MediaExtrasMapper
import com.google.android.horologist.media.data.mapper.MediaExtrasMapperNoopImpl
import com.google.android.horologist.media.data.mapper.MediaMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class DataModule {
    @Provides
    fun mediaExtrasMapper(): MediaExtrasMapper = MediaExtrasMapperNoopImpl

    @Provides
    fun mediaMapper(mediaExtrasMapper: MediaExtrasMapper): MediaMapper =
        MediaMapper(mediaExtrasMapper)
}
