package au.com.shiftyjelly.pocketcasts.models.di

import android.app.Application
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ModelModule {

    @Provides
    fun providesAppDatabase(application: Application): AppDatabase {
        return AppDatabase.getInstance(application)
    }
}
