package au.com.shiftyjelly.pocketcasts.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Variation
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.servers.di.Downloads
import com.automattic.android.experimentation.Experiment
import com.automattic.android.experimentation.ExperimentLogger
import com.automattic.android.experimentation.VariationsRepository
import com.google.firebase.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    companion object {
        @Provides
        fun connectivityManager(@ApplicationContext application: Context): ConnectivityManager {
            return application.getSystemService<ConnectivityManager>()!!
        }

        @Provides
        @Downloads
        fun downloadRequestBuilder(): Request.Builder = Request.Builder()

        @Provides
        fun variationsRepository(
            @ApplicationContext application: Context,
            @ApplicationScope coroutineScope: CoroutineScope,
        ): VariationsRepository = VariationsRepository.create(
            platform = "pcandroid",
            experiments = setOf(
                Experiment("my first experiment"),
                Experiment("or my second experiment")
            ),
            logger = object : ExperimentLogger {
                override fun d(message: String) = Timber.d(message)
                override fun e(message: String, throwable: Throwable?) =
                    Timber.e(message, throwable)
            },
            failFast = BuildConfig.DEBUG,
            cacheDir = application.cacheDir,
            coroutineScope = coroutineScope
        )
    }

    @Binds
    @Downloads
    abstract fun downloadsCallFactory(@Downloads client: OkHttpClient): Call.Factory
}
