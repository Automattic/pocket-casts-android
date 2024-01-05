package au.com.shiftyjelly.pocketcasts.repositories.file

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageKtDelegate @Inject constructor(
    val settings: Settings,
    @ApplicationContext val context: Context,
) {
    fun getPodcastEpisodeFile(episode: BaseEpisode): File? {
        val fileName = episode.uuid + episode.getFileExtension()
        val episodeDir = when (episode) {
            is PodcastEpisode -> getPodcastDirectory()
            is UserEpisode -> getCloudFilesFolder()
        }
        return episodeDir?.let { dir -> File(dir, fileName) }
    }

    fun getTempPodcastEpisodeFile(episode: BaseEpisode): File {
        val fileName = episode.uuid + episode.getFileExtension()
        return File(getTempPodcastDirectory(), fileName)
    }

    fun getCloudFileImage(uuid: String): File? = try {
        val fileName = uuid + "_imagefile"
        getCloudFilesFolder()?.let { dir -> File(dir, fileName) }
    } catch (e: StorageException) {
        Timber.e(e)
        null
    }

    fun getCloudFilesFolder(): File? = getOrCreateDirectory(DIR_CLOUD_FILES)

    fun getOpmlFileFolder(): File? = getOrCreateDirectory(DIR_OPML_FOLDER)

    fun getNetworkImageDirectory(): File? = getOrCreateDirectory(DIR_NETWORK_IMAGES)

    fun getPodcastDirectory(): File? = getOrCreateDirectory(DIR_EPISODES)

    fun getTempPodcastDirectory(): File = getOrCreateCacheDirectory(FOLDER_TEMP_EPISODES)

    fun getOldTempPodcastDirectory(): File? = getOrCreateDirectory(FOLDER_TEMP_EPISODES)

    fun getPodcastGroupImageDirectory(): File? = getOrCreateDirectory(DIR_PODCAST_GROUP_IMAGES)

    fun getOrCreateCacheDirectory(name: String): File = getOrCreateDirectory(context.cacheDir, name)

    fun getOrCreateDirectory(name: String): File? = getStorageDirectory()?.let { dir ->
        getOrCreateDirectory(dir, name)
    }

    // TODO: Make private after migration
    fun getOrCreateDirectory(parentDir: File, name: String): File = File(parentDir, name + File.separator).also { dir ->
        createDirectory(dir)
        addNoMediaFile(dir)
    }

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

    /**
     * Confirms that all the directories we want to hide from the user have .nomedia files in them
     */
    fun checkNoMediaDirs() {
        // Getting a directory also adds .nomedia file to that dir
        try {
            getStorageDirectory()?.let(::addNoMediaFile)
            getNetworkImageDirectory()
            getPodcastGroupImageDirectory()
            getTempPodcastDirectory()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    // TODO: Make private after migration
    fun moveFileToDirectory(filePath: String?, directory: File): String? {
        // Validate the path, check PocketCasts is in the path so we don't delete something important
        if (filePath.isNullOrBlank() || "/PocketCasts" !in filePath) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Not moving because it's blank or not PocketCasts")
            return filePath
        }

        val file = File(filePath)
        // Check we aren't copying to the same directory
        if (file.parentFile == directory) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Not moving because it's the same directory")
            return filePath
        }

        val newFile = File(directory, file.name)
        if (file.exists() && file.isFile) {
            try {
                FileUtil.copyFile(file, newFile)
                val wasDeleted = file.delete()
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Moved ${file.absolutePath} to ${newFile.absolutePath} wasDeleted: $wasDeleted")
            } catch (e: IOException) {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Problems moving a file to a new location. from: ${file.absolutePath} to: ${newFile.absolutePath}")
            }
        }

        return newFile.absolutePath
    }

    private companion object {
        const val DIR_CLOUD_FILES = "cloud_files"
        const val DIR_OPML_FOLDER = "opml_import"
        const val DIR_NETWORK_IMAGES = "network_images"
        const val DIR_EPISODES = "podcasts"
        const val FOLDER_TEMP_EPISODES = "downloadTmp"
        val DIR_PODCAST_GROUP_IMAGES = "network_images" + File.separator + "groups" + File.separator
    }
}
