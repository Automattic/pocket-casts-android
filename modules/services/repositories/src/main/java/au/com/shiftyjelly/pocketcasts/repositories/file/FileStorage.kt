package au.com.shiftyjelly.pocketcasts.repositories.file

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorage @Inject constructor(
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

    private fun getOrCreateDirectory(parentDir: File, name: String): File = File(parentDir, name + File.separator).also { dir ->
        createDirectory(dir)
        addNoMediaFile(dir)
    }

    fun getStorageDirectory(): File? = getBaseStorageDirectory()?.let { dir ->
        File(dir, "PocketCasts" + File.separator).also(::createDirectory)
    }

    fun getBaseStorageDirectory(): File? = settings.getStorageChoice()?.let(::getBaseStorageDirectory)

    private fun getBaseStorageDirectory(choice: String): File = if (choice == Settings.STORAGE_ON_CUSTOM_FOLDER) {
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

    private fun createDirectory(dir: File): File = dir.also(File::mkdirs)

    private fun addNoMediaFile(folder: File) {
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

    private fun moveFileToDirectory(filePath: String?, directory: File): String? {
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

    private fun moveDirectory(fromDirectory: File, toDirectory: File) {
        if (fromDirectory.exists() && fromDirectory.isDirectory) {
            try {
                FileUtil.copyDirectory(fromDirectory, toDirectory)
                fromDirectory.delete()
            } catch (e: IOException) {
                Timber.e(e, "Problems moving a  directory to a new location. from: ${fromDirectory.absolutePath} to: ${toDirectory.absolutePath}")
            }
        }
    }

    fun moveStorage(oldDir: File, newDir: File, episodesManager: EpisodeManager) {
        try {
            val oldPocketCastsDir = File(oldDir, "PocketCasts")
            if (oldPocketCastsDir.exists() && oldPocketCastsDir.isDirectory) {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Pocket casts directory exists")

                newDir.mkdirs()
                val newPocketCastsDir = File(newDir, "PocketCasts")
                val episodesDir = getOrCreateDirectory(newPocketCastsDir, DIR_EPISODES)

                // Check existing media and mark those episodes as downloaded
                if (episodesDir.exists()) {
                    episodesDir.listFiles()?.forEach { file ->
                        val fileName = FileUtil.getFileNameWithoutExtension(file)
                        if (fileName.length < 36) {
                            return@forEach
                        }

                        @Suppress("DEPRECATION")
                        episodesManager.findByUuidSync(fileName)?.let { episode ->
                            // Delete original file if it is already there
                            episode.downloadedFilePath?.takeIf(String::isNotBlank)?.let { downloadedFilePath ->
                                val originalFile = File(downloadedFilePath)
                                if (originalFile.exists()) {
                                    originalFile.delete()
                                }
                            }

                            episodesManager.updateDownloadFilePath(episode, file.absolutePath, markAsDownloaded = true)
                        }
                    }
                }

                // Move episodes
                episodesManager.observeDownloadedEpisodes().blockingFirst().forEach { episode ->
                    LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Found downloaded episode ${episode.title}")
                    val downloadedFilePath = episode.downloadedFilePath?.takeIf(String::isNotBlank)
                    if (downloadedFilePath == null) {
                        LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Episode had not file path")
                        return@forEach
                    }
                    val file = File(downloadedFilePath)
                    if (file.exists() && file.isFile) {
                        moveFileToDirectory(downloadedFilePath, episodesDir)?.let { updatedPath ->
                            episodesManager.updateDownloadFilePath(episode, updatedPath, markAsDownloaded = false)
                        }
                    }
                }

                val oldCustomFilesDir = getOrCreateDirectory(oldPocketCastsDir, DIR_CUSTOM_FILES)
                val newCustomFilesDir = getOrCreateDirectory(newPocketCastsDir, DIR_CUSTOM_FILES)
                if (oldCustomFilesDir.exists()) {
                    moveDirectory(oldCustomFilesDir, newCustomFilesDir)
                }

                val oldNetworkImageDir = getOrCreateDirectory(oldPocketCastsDir, DIR_NETWORK_IMAGES)
                val newNetworkImageDir = getOrCreateDirectory(newPocketCastsDir, DIR_NETWORK_IMAGES)
                if (newNetworkImageDir.exists()) {
                    moveDirectory(oldNetworkImageDir, newNetworkImageDir)
                }
            } else {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Old directory did not exist")
            }
        } catch (e: StorageException) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Unable to move storage to new location")
        }
    }

    fun fixBrokenFiles(episodeManager: EpisodeManager) {
        try {
            // Get all possible locations
            val folderPaths = buildSet {
                addAll(StorageOptions().getFolderLocations(context).map(FolderLocation::filePath))
                add(context.filesDir.absolutePath)
                val customFolder = settings.getStorageCustomFolder()
                if (customFolder.isNotBlank() && File(customFolder).exists()) {
                    add(customFolder)
                }
            }

            // Search each folder for missing files
            folderPaths.forEach folderIteration@{ folderPath ->
                val folder = File(folderPath)
                if (!folder.exists() || !folder.canRead()) {
                    return@folderIteration
                }
                val pocketCastsFolder = File(folder, "PocketCasts")
                if (!pocketCastsFolder.exists() || !pocketCastsFolder.canRead()) {
                    return@folderIteration
                }
                val episodesFolder = File(pocketCastsFolder, DIR_EPISODES)
                if (!episodesFolder.exists() || !episodesFolder.canRead()) {
                    return@folderIteration
                }
                episodesFolder.listFiles()?.forEach fileIteration@{ file ->
                    val fileName = file.name
                    val dotPosition = fileName.lastIndexOf('.')
                    if (dotPosition < 1) {
                        return@fileIteration
                    }
                    val uuid = fileName.substring(0, dotPosition)
                    if (uuid.length != 36) {
                        return@fileIteration
                    }

                    @Suppress("DEPRECATION")
                    episodeManager.findByUuidSync(uuid)?.let { episode ->
                        val downloadedFilePath = episode.downloadedFilePath
                        if (downloadedFilePath != null && File(downloadedFilePath).exists() && episode.isDownloaded) {
                            return@fileIteration
                        }

                        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Restoring downloaded file for ${episode.title} from ${file.absolutePath}")
                        // Link to the found episode
                        episode.episodeStatus = EpisodeStatusEnum.DOWNLOADED
                        episode.downloadedFilePath = file.absolutePath
                        episodeManager.update(episode)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private companion object {
        const val DIR_CUSTOM_FILES = "custom_episodes"
        const val DIR_CLOUD_FILES = "cloud_files"
        const val DIR_OPML_FOLDER = "opml_import"
        const val DIR_NETWORK_IMAGES = "network_images"
        const val DIR_EPISODES = "podcasts"
        const val FOLDER_TEMP_EPISODES = "downloadTmp"
        val DIR_PODCAST_GROUP_IMAGES = "network_images" + File.separator + "groups" + File.separator
    }
}
