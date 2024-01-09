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
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber

open class FileStorage @Inject constructor(
    val settings: Settings,
    @ApplicationContext val context: Context,
) {
    fun getOrCreatePodcastEpisodeFile(episode: BaseEpisode): File? {
        val fileName = episode.uuid + episode.getFileExtension()
        val episodeDir = when (episode) {
            is PodcastEpisode -> getOrCreateEpisodesDir()
            is UserEpisode -> getOrCreateCloudDir()
        }
        return episodeDir?.let { dir -> File(dir, fileName) }
    }

    fun getOrCreatePodcastEpisodeTempFile(episode: BaseEpisode): File {
        val fileName = episode.uuid + episode.getFileExtension()
        return File(getOrCreateEpisodesTempDir(), fileName)
    }

    fun getOrCreateCloudFileImage(uuid: String): File? = try {
        val fileName = uuid + "_imagefile"
        getOrCreateCloudDir()?.let { dir -> File(dir, fileName) }
    } catch (e: StorageException) {
        Timber.e(e)
        null
    }

    fun getOrCreateCloudDir(): File? = getOrCreateDir(DIR_CLOUD_FILES)

    fun getOrCreateOpmlDir(): File? = getOrCreateDir(DIR_OPML_FILES)

    fun getOrCreateNetworkImagesDir(): File? = getOrCreateDir(DIR_NETWORK_IMAGES)

    fun getOrCreateEpisodesDir(): File? = getOrCreateDir(DIR_EPISODES)

    fun getOrCreateEpisodesTempDir(): File = getOrCreateCacheDir(DIR_TEMP_EPISODES)

    fun getOrCreateEpisodesOldTempDir(): File? = getOrCreateDir(DIR_TEMP_EPISODES)

    fun getOrCreatePodcastGroupImagesDir(): File? = getOrCreateDir(DIR_PODCAST_GROUP_IMAGES)

    fun getOrCreateCacheDir(name: String): File = getOrCreateDir(context.cacheDir, name)

    fun getOrCreateDir(name: String): File? = getOrCreateStorageDir()?.let { dir ->
        getOrCreateDir(dir, name)
    }

    private fun getOrCreateDir(parentDir: File, name: String): File = File(parentDir, name + File.separator).also { dir ->
        createDir(dir)
        addNoMediaFile(dir)
    }

    fun getOrCreateStorageDir(): File? = getOrCreateBaseStorageDir()?.let { dir ->
        File(dir, "PocketCasts" + File.separator).also(::createDir)
    }

    fun getOrCreateBaseStorageDir(): File? = settings.getStorageChoice()?.let(::getOrCreateBaseStorageDir)

    private fun getOrCreateBaseStorageDir(choice: String): File = if (choice == Settings.STORAGE_ON_CUSTOM_FOLDER) {
        val path = settings.getStorageCustomFolder()
        if (path.isBlank()) {
            throw StorageException("Ooops, please set the Custom Folder Location in the settings.")
        }
        val storageCustomDir = File(path)
        if (!storageCustomDir.exists() && !storageCustomDir.mkdirs()) {
            throw StorageException("Storage custom folder unavailable.")
        }
        storageCustomDir
    } else {
        File(choice)
    }

    private fun createDir(dir: File): File = dir.also(File::mkdirs)

    private fun addNoMediaFile(dir: File) {
        if (!dir.exists()) {
            return
        }
        val noMediaFile = File(dir, ".nomedia")
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
            getOrCreateStorageDir()?.let(::addNoMediaFile)
            getOrCreateNetworkImagesDir()
            getOrCreatePodcastGroupImagesDir()
            getOrCreateEpisodesTempDir()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun moveFileToDir(filePath: String, dir: File): String? {
        // Validate the path, check PocketCasts is in the path so we don't delete something important
        if (filePath.isBlank() || "/PocketCasts" !in filePath) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Not moving because it's blank or not PocketCasts")
            return filePath
        }

        val file = File(filePath)
        // Check we aren't copying to the same directory
        if (file.parentFile == dir) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Not moving because it's the same directory")
            return filePath
        }

        val newFile = File(dir, file.name)
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

    private fun moveDir(fromDir: File, toDir: File) {
        if (fromDir.exists() && fromDir.isDirectory) {
            try {
                FileUtil.copyDirectory(fromDir, toDir)
                fromDir.delete()
            } catch (e: IOException) {
                Timber.e(e, "Problems moving a  directory to a new location. from: ${fromDir.absolutePath} to: ${toDir.absolutePath}")
            }
        }
    }

    suspend fun moveStorage(oldDir: File, newDir: File, episodesManager: EpisodeManager) {
        try {
            val oldPocketCastsDir = File(oldDir, "PocketCasts")
            if (oldPocketCastsDir.exists() && oldPocketCastsDir.isDirectory) {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Pocket casts directory exists")

                newDir.mkdirs()
                val newPocketCastsDir = File(newDir, "PocketCasts")
                val episodesDir = getOrCreateDir(newPocketCastsDir, DIR_EPISODES)

                // Check existing media and mark those episodes as downloaded
                episodesDir.takeIf(File::exists)?.listFiles().orEmpty()
                    .matchWithFileNames()
                    .filterInvalidFileNames()
                    .findMatchingEpisodes(episodesManager)
                    .deleteExistingFiles()
                    .updateEpisodesWithNewFilePaths(episodesManager)

                // Move episodes
                episodesManager.observeDownloadedEpisodes().asFlow().first()
                    .onEach { episode -> LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Found downloaded episode ${episode.title}") }
                    .matchWithDownloadedFilePaths()
                    .filterNotExistingFiles()
                    .moveFilesToEpisodesDirAndUpdatePaths(episodesManager, episodesDir)

                val oldCustomFilesDir = getOrCreateDir(oldPocketCastsDir, DIR_CUSTOM_EPISODES)
                val newCustomFilesDir = getOrCreateDir(newPocketCastsDir, DIR_CUSTOM_EPISODES)
                moveDir(oldCustomFilesDir, newCustomFilesDir)

                val oldNetworkImageDir = getOrCreateDir(oldPocketCastsDir, DIR_NETWORK_IMAGES)
                val newNetworkImageDir = getOrCreateDir(newPocketCastsDir, DIR_NETWORK_IMAGES)
                moveDir(oldNetworkImageDir, newNetworkImageDir)
            } else {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Old directory did not exist")
            }
        } catch (e: StorageException) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Unable to move storage to new location")
        }
    }

    private fun Array<out File>.matchWithFileNames() = map { file -> file to FileUtil.getFileNameWithoutExtension(file) }

    private fun List<Pair<File, String>>.filterInvalidFileNames() = filter { (_, fileName) -> fileName.length >= UUID_LENGTH }

    private suspend fun List<Pair<File, String>>.findMatchingEpisodes(episodeManager: EpisodeManager) = mapNotNull { (file, fileName) ->
        episodeManager.findByUuid(fileName)?.let { episode -> file to episode }
    }

    private fun List<Pair<File, PodcastEpisode>>.deleteExistingFiles() = onEach { (_, episode) ->
        episode.downloadedFilePath?.takeIf(String::isNotBlank)?.let { downloadedFilePath ->
            val originalFile = File(downloadedFilePath)
            if (originalFile.exists()) {
                originalFile.delete()
            }
        }
    }

    private fun List<Pair<File, PodcastEpisode>>.updateEpisodesWithNewFilePaths(episodeManager: EpisodeManager) = forEach { (file, episode) ->
        episodeManager.updateDownloadFilePath(episode, file.absolutePath, markAsDownloaded = true)
    }

    private fun List<PodcastEpisode>.matchWithDownloadedFilePaths() = mapNotNull { episode ->
        val downloadedFilePath = episode.downloadedFilePath?.takeIf(String::isNotBlank)
        if (downloadedFilePath == null) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Episode had not file path")
        }
        downloadedFilePath?.let { path -> episode to path }
    }

    private fun List<Pair<PodcastEpisode, String>>.filterNotExistingFiles() = filter { (_, path) -> File(path).takeIf { it.exists() && it.isFile } != null }

    private fun List<Pair<PodcastEpisode, String>>.moveFilesToEpisodesDirAndUpdatePaths(episodeManager: EpisodeManager, episodesDir: File) = forEach { (episode, path) ->
        moveFileToDir(path, episodesDir)?.let { updatedPath ->
            episodeManager.updateDownloadFilePath(episode, updatedPath, markAsDownloaded = false)
        }
    }

    suspend fun fixBrokenFiles(episodeManager: EpisodeManager) {
        try {
            // Get all possible locations
            val dirPaths = buildSet {
                addAll(StorageOptions().getFolderLocations(context).map(FolderLocation::filePath))
                add(context.filesDir.absolutePath)
                val customDir = settings.getStorageCustomFolder()
                if (customDir.isNotBlank() && File(customDir).exists()) {
                    add(customDir)
                }
            }

            dirPaths.asSequence()
                .toExistingDirs()
                .toExistingPocketCastsDirs()
                .toExistingEpisodesDirs()
                .listFiles()
                .matchWithFileNameDotPosition()
                .toFileNameUuids()
                .asFlow()
                .toMatchingEpisodes(episodeManager)
                .filterDownloadedEpisodes()
                .restoreDownloadedFilePaths(episodeManager)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun Sequence<String>.toExistingDirs() = mapNotNull { dirPath -> File(dirPath).takeIf(::isFileReadable) }

    private fun Sequence<File>.toExistingPocketCastsDirs() = mapNotNull { parentDir -> File(parentDir, "PocketCasts").takeIf(::isFileReadable) }

    private fun Sequence<File>.toExistingEpisodesDirs() = mapNotNull { parentDir -> File(parentDir, DIR_EPISODES).takeIf(::isFileReadable) }

    private fun isFileReadable(file: File) = file.exists() && file.canRead()

    private fun Sequence<File>.listFiles() = flatMap { it.listFiles().orEmpty().asSequence() }

    private fun Sequence<File>.matchWithFileNameDotPosition() = mapNotNull { file ->
        file.name.lastIndexOf('.').takeIf { it >= 1 }?.let { dotPosition -> file to dotPosition }
    }

    private fun Sequence<Pair<File, Int>>.toFileNameUuids() = mapNotNull { (file, dotPosition) ->
        file.name.substring(0, dotPosition).takeIf { it.length != UUID_LENGTH }?.let { uuid -> file to uuid }
    }

    private fun Flow<Pair<File, String>>.toMatchingEpisodes(episodeManager: EpisodeManager) = mapNotNull { (file, uuid) ->
        episodeManager.findByUuid(uuid)?.let { episode -> file to episode }
    }

    private fun Flow<Pair<File, PodcastEpisode>>.filterDownloadedEpisodes() = filter { (_, episode) ->
        val downloadedFilePath = episode.downloadedFilePath
        downloadedFilePath == null || !File(downloadedFilePath).exists() || !episode.isDownloaded
    }

    private suspend fun Flow<Pair<File, PodcastEpisode>>.restoreDownloadedFilePaths(episodeManager: EpisodeManager) = collect { (file, episode) ->
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Restoring downloaded file for ${episode.title} from ${file.absolutePath}")
        // Link to the found episode
        episode.episodeStatus = EpisodeStatusEnum.DOWNLOADED
        episode.downloadedFilePath = file.absolutePath
        episodeManager.update(episode)
    }

    private companion object {
        const val DIR_EPISODES = "podcasts"
        const val DIR_CUSTOM_EPISODES = "custom_episodes"
        const val DIR_TEMP_EPISODES = "downloadTmp"
        const val DIR_NETWORK_IMAGES = "network_images"
        val DIR_PODCAST_GROUP_IMAGES = "network_images" + File.separator + "groups" + File.separator
        const val DIR_CLOUD_FILES = "cloud_files"
        const val DIR_OPML_FILES = "opml_import"

        const val UUID_LENGTH = 36
    }
}
