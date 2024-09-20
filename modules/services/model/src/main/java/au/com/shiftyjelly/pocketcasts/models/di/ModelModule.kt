package au.com.shiftyjelly.pocketcasts.models.di

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import au.com.shiftyjelly.pocketcasts.model.BuildConfig
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.ChapterDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.ExternalDataDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextDao
import au.com.shiftyjelly.pocketcasts.models.entity.AnonymousBumpStat
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ModelModule {
    @Provides
    @RoomConverters
    fun provideRoomConverters(moshi: Moshi): List<Any> {
        return listOf(
            AnonymousBumpStat.CustomEventPropsTypeConverter(moshi),
        )
    }

    @Provides
    @Singleton
    fun providesAppDatabase(
        application: Application,
        @RoomConverters converters: List<@JvmSuppressWildcards Any>,
    ): AppDatabase {
        val databaseBuilder = Room.databaseBuilder(application, AppDatabase::class.java, "pocketcasts")
        AppDatabase.addMigrations(databaseBuilder, application)
        if (BuildConfig.DEBUG) {
            databaseBuilder.fallbackToDestructiveMigration()
        }
        return databaseBuilder
            .addTypeConverters(converters)
            .build()
    }

    @Provides
    fun provideEpisodeDao(database: AppDatabase): EpisodeDao = database.episodeDao()

    @Provides
    fun provideChapterDao(database: AppDatabase): ChapterDao = database.chapterDao()

    @Provides
    fun provideUpNextDao(database: AppDatabase): UpNextDao = database.upNextDao()

    @Provides
    fun providePodcastDao(database: AppDatabase): PodcastDao = database.podcastDao()

    @Provides
    fun provideTranscriptDao(database: AppDatabase): TranscriptDao = database.transcriptDao()

    @Provides
    fun provideExternalDataDao(database: AppDatabase): ExternalDataDao = database.externalDataDao()
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RoomConverters

fun <T : RoomDatabase> RoomDatabase.Builder<T>.addTypeConverters(converters: List<Any>) = converters.fold(this) { builder, converter ->
    builder.addTypeConverter(converter)
}
