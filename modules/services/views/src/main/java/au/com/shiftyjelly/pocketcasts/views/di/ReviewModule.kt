package au.com.shiftyjelly.pocketcasts.views.di

import android.content.Context
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ReviewModule {
    @Provides
    @Singleton
    internal fun provideReviewManager(@ApplicationContext context: Context): ReviewManager {
        return ReviewManagerFactory.create(context)
    }
}
