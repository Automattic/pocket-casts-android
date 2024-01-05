package au.com.shiftyjelly.pocketcasts.repositories.file

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageKtDelegate @Inject constructor(
    val settings: Settings,
    @ApplicationContext val context: Context,
)
