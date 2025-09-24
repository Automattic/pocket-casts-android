package au.com.shiftyjelly.pocketcasts.repositories.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.repositories.playlist.DefaultPlaylistsStartupInitializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface InitializerEntryPoint {
    fun inject(initializer: DefaultPlaylistsStartupInitializer)
}

internal fun Context.initializerEntryPoint() = EntryPointAccessors.fromApplication<InitializerEntryPoint>(this)
