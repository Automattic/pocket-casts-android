package au.com.shiftyjelly.pocketcasts.servers.di

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.search.AutoCompleteResult
import au.com.shiftyjelly.pocketcasts.servers.search.SearchService
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.protobuf.ProtoConverterFactory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SearchMoshi


@Module
@InstallIn(SingletonComponent::class)
class SearchModule {
    @Provides
    @Singleton
    @SearchMoshi
    internal fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(
                PolymorphicJsonAdapterFactory.of(AutoCompleteResult::class.java, "type")
                    .withSubtype(AutoCompleteResult.TermResult::class.java, "term")
                    .withSubtype(AutoCompleteResult.PodcastResult::class.java, "podcast")
            )
            .build()
    }

    @Provides
    @Singleton
    internal fun provideApiRetrofit(@Cached okHttpClient: OkHttpClient, @SearchMoshi moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(ProtoConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(Settings.SEARCH_API_URL)
            .client(okHttpClient)
            .build()
    }

    @Provides
    internal fun provideSearchService(retrofit: Retrofit): SearchService = retrofit.create(SearchService::class.java)
}