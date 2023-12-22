package au.com.shiftyjelly.pocketcasts.wear.di

import android.content.Context
import android.net.ConnectivityManager
import com.google.android.horologist.media3.rules.PlaybackRules
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WearAppModule {

    @Provides
    fun connectivityManager(
        @ApplicationContext application: Context,
    ): ConnectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Singleton
    @Provides
    fun playbackRules(
        @IsEmulator isEmulator: Boolean,
    ): PlaybackRules = if (isEmulator) {
        PlaybackRules.SpeakerAllowed
    } else {
        PlaybackRules.Normal
    }
}
