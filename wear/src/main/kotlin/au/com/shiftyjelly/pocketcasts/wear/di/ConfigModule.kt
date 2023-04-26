package au.com.shiftyjelly.pocketcasts.wear.di

import android.content.Context
import android.os.Build
import android.os.Vibrator
import au.com.shiftyjelly.pocketcasts.wear.ui.AppConfig
import com.google.android.horologist.audio.SystemAudioRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {
    @Singleton
    @Provides
    @IsEmulator
    fun isEmulator() = listOf(Build.PRODUCT, Build.MODEL).any { it.startsWith("sdk_gwear") }

    @Singleton
    @Provides
    fun appConfig(): AppConfig = AppConfig()

    @Singleton
    @Provides
    fun systemAudioRepository(
        @ApplicationContext application: Context
    ): SystemAudioRepository =
        SystemAudioRepository.fromContext(application)

    @Singleton
    @Provides
    fun vibrator(
        @ApplicationContext application: Context
    ): Vibrator =
        application.getSystemService(Vibrator::class.java)
}
