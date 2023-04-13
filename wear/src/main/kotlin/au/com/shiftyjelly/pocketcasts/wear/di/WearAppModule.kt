package au.com.shiftyjelly.pocketcasts.wear.di

import android.content.Context
import android.net.ConnectivityManager
import au.com.shiftyjelly.pocketcasts.wear.data.service.log.Logging
import au.com.shiftyjelly.pocketcasts.wear.ui.AppConfig
import com.google.android.horologist.media3.config.WearMedia3Factory
import com.google.android.horologist.media3.logging.ErrorReporter
import com.google.android.horologist.media3.navigation.IntentBuilder
import com.google.android.horologist.media3.navigation.NavDeepLinkIntentBuilder
import com.google.android.horologist.media3.rules.PlaybackRules
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WearAppModule {

    @Singleton
    @Provides
    fun intentBuilder(
        @ApplicationContext application: Context,
        appConfig: AppConfig,
    ): IntentBuilder =
        NavDeepLinkIntentBuilder(
            application,
            "${appConfig.deeplinkUriPrefix}/player?page=1",
            "${appConfig.deeplinkUriPrefix}/player?page=0"
        )

    @Singleton
    @Provides
    fun playbackRules(
        /*appConfig: AppConfig,*/
        @IsEmulator isEmulator: Boolean,
    ): PlaybackRules =
        /*if (appConfig.playbackRules != null) {
            appConfig.playbackRules
        } else */if (isEmulator) {
            PlaybackRules.SpeakerAllowed
        } else {
            PlaybackRules.Normal
        }

    @Singleton
    @Provides
    @ForApplicationScope
    fun coroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Singleton
    @Provides
    fun wearMedia3Factory(
        @ApplicationContext application: Context
    ): WearMedia3Factory =
        WearMedia3Factory(application)

    @Singleton
    @Provides
    fun logger(
        @ApplicationContext application: Context
    ): Logging = Logging(res = application.resources)

    @Singleton
    @Provides
    fun errorReporter(
        logging: Logging,
    ): ErrorReporter = logging

    @Provides
    fun connectivityManager(
        @ApplicationContext application: Context
    ): ConnectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
}
