package au.com.shiftyjelly.pocketcasts.endofyear.di

import au.com.shiftyjelly.pocketcasts.endofyear.StoriesDataSource
import au.com.shiftyjelly.pocketcasts.endofyear.stories.EndOfYearStoriesDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class EndOfYearModule {
    @Binds
    abstract fun providesEndOfYearStoriesDataSource(dataSource: EndOfYearStoriesDataSource): StoriesDataSource
}
