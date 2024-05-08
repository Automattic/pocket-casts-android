package au.com.shiftyjelly.pocketcasts.servers.di

import com.automattic.android.tracks.crashlogging.CrashLoggingOkHttpInterceptorProvider
import com.automattic.android.tracks.crashlogging.FormattedUrl
import com.automattic.android.tracks.crashlogging.RequestFormatter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import okhttp3.Request

@InstallIn(SingletonComponent::class)
@Module
class InterceptorModule {
    @Provides
    @IntoSet
    fun provideMonitoringInterceptor(): Interceptor = CrashLoggingOkHttpInterceptorProvider
        .createInstance(object : RequestFormatter {
            override fun formatRequestUrl(request: Request): FormattedUrl {
                return request.url.host.takeIf { it.contains("pocketcasts") } ?: "filtered"
            }
        })
}
