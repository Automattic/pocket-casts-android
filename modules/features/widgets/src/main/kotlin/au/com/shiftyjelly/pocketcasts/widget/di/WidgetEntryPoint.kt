package au.com.shiftyjelly.pocketcasts.widget.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextDao
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import com.squareup.moshi.Moshi
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun moshi(): Moshi
    fun upNextDao(): UpNextDao
    fun playbackManager(): PlaybackManager
    fun settings(): Settings
}

internal fun Context.widgetEntryPoint() = EntryPointAccessors.fromApplication<WidgetEntryPoint>(this)
