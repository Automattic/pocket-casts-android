package au.com.shiftyjelly.pocketcasts.preferences.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @PublicSharedPreferences
    @Provides
    fun providesSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @PrivateSharedPreferences
    @Provides
    fun providesPrivatePreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("POCKETCASTS_SECURE", Context.MODE_PRIVATE)
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicSharedPreferences

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PrivateSharedPreferences
