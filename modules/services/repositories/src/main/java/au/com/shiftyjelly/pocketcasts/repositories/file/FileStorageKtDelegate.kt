package au.com.shiftyjelly.pocketcasts.repositories.file

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageKtDelegate @Inject constructor(
    val settings: Settings,
    @ApplicationContext val context: Context,
) {
    // TODO: Make private after migration
    fun createDirectory(dir: File): File = dir.also(File::mkdirs)

    // TODO: Make private after migration
    fun addNoMediaFile(folder: File) {
        if (!folder.exists()) {
            return
        }
        val noMediaFile = File(folder, ".nomedia")
        if (!noMediaFile.exists()) {
            try {
                noMediaFile.createNewFile()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}
