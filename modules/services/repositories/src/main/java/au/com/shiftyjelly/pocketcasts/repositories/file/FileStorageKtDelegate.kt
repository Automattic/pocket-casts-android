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
    fun getStorageDirectory(): File? = getBaseStorageDirectory()?.let { dir ->
        File(dir, "PocketCasts" + File.separator).also(::createDirectory)
    }

    fun getBaseStorageDirectory(): File? = settings.getStorageChoice()?.let(::getBaseStorageDirectory)

    // TODO: Make private after migration
    fun getBaseStorageDirectory(choice: String): File = if (choice == Settings.STORAGE_ON_CUSTOM_FOLDER) {
        val path = settings.getStorageCustomFolder()
        if (path.isBlank()) {
            throw StorageException("Ooops, please set the Custom Folder Location in the settings.")
        }
        val folder = File(path)
        if (!folder.exists() && !folder.mkdirs()) {
            throw StorageException("Storage custom folder unavailable.")
        }
        folder
    } else {
        File(choice)
    }

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
