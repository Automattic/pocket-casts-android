package au.com.shiftyjelly.pocketcasts.repositories.file;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode;
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode;
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum;
import au.com.shiftyjelly.pocketcasts.preferences.Settings;
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager;
import au.com.shiftyjelly.pocketcasts.utils.FileUtil;
import au.com.shiftyjelly.pocketcasts.utils.StringUtil;
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer;
import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

@Singleton
public class FileStorage {
	public static final String DIR_CUSTOM_FILES = "custom_episodes";
	public static final String DIR_NETWORK_IMAGES = "network_images";
	public static final String DIR_EPISODES = "podcasts";

	private Settings settings;
	private Context context;
	private final FileStorageKtDelegate delegate;

	@Inject
    public FileStorage(Settings settings, @ApplicationContext  Context context) {
        this.settings = settings;
		this.context = context;
		delegate = new FileStorageKtDelegate(settings, context);
    }

	@NonNull
	public File getPodcastEpisodeFile(BaseEpisode episode) throws StorageException {
		String fileName = episode.getUuid() + episode.getFileExtension();
		File directory;
		if (episode instanceof PodcastEpisode) {
			directory = getPodcastDirectory();
		} else {
			directory = getCloudFilesFolder();
		}

		return new File(directory, fileName);
	}

	@NonNull
	public File getTempPodcastEpisodeFile(BaseEpisode episode) throws StorageException {
		String fileName = episode.getUuid() + episode.getFileExtension();
		return new File(getTempPodcastDirectory(), fileName);
	}

	@Nullable
	public File getCloudFileImage(String uuid) {
    	return delegate.getCloudFileImage(uuid);
	}

    public File getCloudFilesFolder() throws StorageException {
    	return delegate.getCloudFilesFolder();
	}

	public File getOpmlFileFolder() throws StorageException {
		return delegate.getOpmlFileFolder();
	}

	public  File getNetworkImageDirectory() throws StorageException {
		return delegate.getNetworkImageDirectory();
	}
	
	public  File getPodcastDirectory() throws StorageException {
		return delegate.getPodcastDirectory();
	}
	
	public  File getTempPodcastDirectory() throws StorageException {
		return delegate.getTempPodcastDirectory();
	}

	public File getOldTempPodcastDirectory() throws StorageException {
    	return delegate.getOldTempPodcastDirectory();
	}

	public  File getPodcastGroupImageDirectory() throws StorageException {
		return delegate.getPodcastGroupImageDirectory();
	}

	public File getOrCreateCacheDirectory(String name) throws StorageException {
    	return delegate.getOrCreateCacheDirectory(name);
	}

    public File getOrCreateDirectory(String name) throws StorageException {
		return delegate.getOrCreateDirectory(name);
	}
	
	private File getOrCreateDirectory(File parentDir, String name) throws StorageException {
		return delegate.getOrCreateDirectory(parentDir, name);
	}
	
	public File getStorageDirectory() throws StorageException {
		return delegate.getStorageDirectory();
	}
	
	public File getBaseStorageDirectory() throws StorageException {
		return delegate.getBaseStorageDirectory();
	}
	
	public File getBaseStorageDirectory(String choice) throws StorageException {
		return delegate.getBaseStorageDirectory(choice);
	}

	private File createDirectory(File dir) {
		return delegate.createDirectory(dir);
	}

	private void addNoMediaFile(File folder) {
		delegate.addNoMediaFile(folder);
	}
	
	// confirms that all the folders we want to hide from the user have .nomedia files in them
	public void checkNoMediaDirs(){
		// we can do this by getting all the folders
		try {
            addNoMediaFile(getStorageDirectory());
			getNetworkImageDirectory();
			getPodcastGroupImageDirectory();
			getTempPodcastDirectory();
		}
		catch (Exception e) {
			Timber.e(e);
		}
	}

	private String moveFileToDirectory(String filePath, File directory) {
		// validate the path, check PocketCasts is in the path so we don't delete something important
		if (StringUtil.isBlank(filePath) || filePath.indexOf("/PocketCasts") == -1) {
			LogBuffer.INSTANCE.e(LogBuffer.TAG_BACKGROUND_TASKS, "Not moving because it's blank or not PocketCasts");
			return filePath;
		}

		File file = new File(filePath);
        // check we aren't copying to the same directory
        if (file.getParentFile().equals(directory)) {
			LogBuffer.INSTANCE.e(LogBuffer.TAG_BACKGROUND_TASKS, "Not moving because it's the same directory");
            return filePath;
        }
		File newFile = new File(directory, file.getName());
		if (file.exists() && file.isFile()) {
			try {
				FileUtil.copyFile(file, newFile);
				boolean wasDeleted = file.delete();
				LogBuffer.INSTANCE.i(LogBuffer.TAG_BACKGROUND_TASKS, "Moved " + file.getAbsolutePath() + " to " + newFile.getAbsolutePath() + " wasDeleted: " + wasDeleted);
			} catch (IOException e) {
				LogBuffer.INSTANCE.e(LogBuffer.TAG_BACKGROUND_TASKS, e,"Problems moving a file to a new location. from: "+file.getAbsolutePath()+" to: "+newFile.getAbsolutePath());
			}
		}
		return newFile.getAbsolutePath();
	}
	
	private void moveDirectory(File fromDirectory, File toDirectory) {
		if (fromDirectory == null || !fromDirectory.exists() || !fromDirectory.isDirectory() || toDirectory == null) {
			return;
		}

		try {
			FileUtil.copyDirectory(fromDirectory, toDirectory);
			fromDirectory.delete();
		} catch (IOException e) {
			Timber.e(e,"Problems moving a directory to a new location. from: "+fromDirectory.getAbsolutePath()+" to: "+toDirectory.getAbsolutePath());
		}
	}

	public void moveStorage(File oldDirectory, File newDirectory, EpisodeManager episodeManager) {
		try {
			final File pocketCastsDir = new File(oldDirectory, "PocketCasts");
	 	   	if (pocketCastsDir.exists() && pocketCastsDir.isDirectory()) {
				LogBuffer.INSTANCE.i(LogBuffer.TAG_BACKGROUND_TASKS, "Pocket casts directory exists");
				
	 	   		newDirectory.mkdirs();
	 	   		final File newPocketCastsDir = new File(newDirectory, "PocketCasts");

				boolean folderAlreadyExisted = newPocketCastsDir.exists();

				// check existing media and mark those episodes as downloaded
				final File episodeDirectory = getOrCreateDirectory(newPocketCastsDir, DIR_EPISODES);
				if (folderAlreadyExisted) {
					boolean foundMedia = false;
					File[] files = episodeDirectory.listFiles();
					if (files != null) {
						for (File file : files) {
							String fileName = FileUtil.getFileNameWithoutExtension(file);
							if (fileName.length() < 36) {
								continue;
							}
							//noinspection deprecation
							PodcastEpisode episode = episodeManager.findByUuidSync(fileName);
							if (episode != null) {
								// Delete the original file if it is already there
								if (!StringUtil.isBlank(episode.getDownloadedFilePath())) {
									File originalFile = new File(episode.getDownloadedFilePath());
									if (originalFile.exists()) {
										originalFile.delete();
									}
								}
								episodeManager.updateDownloadFilePath(episode, file.getAbsolutePath(), true);

								foundMedia = true;
							}
						}
					}
				}

				// move episodes
				List<PodcastEpisode> episodes = episodeManager.observeDownloadedEpisodes().blockingFirst();
				for (PodcastEpisode episode : episodes) {
					LogBuffer.INSTANCE.i(LogBuffer.TAG_BACKGROUND_TASKS, "Found downloaded episode " + episode.getTitle());
					String episodeFilePath = episode.getDownloadedFilePath();
					if (StringUtil.isBlank(episodeFilePath)) {
						LogBuffer.INSTANCE.e(LogBuffer.TAG_BACKGROUND_TASKS, "Episode had no file path");
						continue;
					}
					File file = new File(episodeFilePath);
					if (file.exists() && file.isFile()) {
						episode.setDownloadedFilePath(moveFileToDirectory(episodeFilePath, episodeDirectory));
						episodeManager.updateDownloadFilePath(episode, episode.getDownloadedFilePath(), false);
					}
				}

                // move custom folders
                File customFilesDirectory = getOrCreateDirectory(pocketCastsDir, DIR_CUSTOM_FILES);
                File newCustomFilesDirectory = getOrCreateDirectory(newPocketCastsDir, DIR_CUSTOM_FILES);
                if (customFilesDirectory.exists()) {
                    moveDirectory(customFilesDirectory, newCustomFilesDirectory);
                }
	 	   	    
	 	   	    // move network and group images
		 	   	File networkImageDirectory = getOrCreateDirectory(pocketCastsDir, DIR_NETWORK_IMAGES);
	 	   	    File newNetworkImageDirectory = getOrCreateDirectory(newPocketCastsDir, DIR_NETWORK_IMAGES);
	 	   	    if (networkImageDirectory.exists()) {
	 	   	    	moveDirectory(networkImageDirectory, newNetworkImageDirectory);
	 	   	    }
	 	   	} else {
	 	   		LogBuffer.INSTANCE.e(LogBuffer.TAG_BACKGROUND_TASKS, "Old directory did not exist");
			}
		}
		catch(StorageException e) {
			LogBuffer.INSTANCE.e(LogBuffer.TAG_BACKGROUND_TASKS, e,"Unable to move storage to new location.");
		}
	}

	public void fixBrokenFiles(EpisodeManager episodeManager) {
		try {
			// get all possible locations
			List<String> folderPaths = new ArrayList<>();
			for (FolderLocation location : new StorageOptions().getFolderLocations(context)) {
				folderPaths.add(location.getFilePath());
			}
			folderPaths.add(context.getFilesDir().getAbsolutePath());
			String customFolder = settings.getStorageCustomFolder();
			if (StringUtil.isPresent(customFolder) && !folderPaths.contains(customFolder) && new File(customFolder).exists()) {
				folderPaths.add(customFolder);
			}

			// search each folder for missing files
			for (String folderPath : folderPaths) {
				File folder = new File(folderPath);
				if (!folder.exists() || !folder.canRead()) {
					continue;
				}
				File pocketcastsFolder = new File(folder, "PocketCasts");
				if (!pocketcastsFolder.exists() || !pocketcastsFolder.canRead()) {
					continue;
				}
				File episodesFolder = new File(pocketcastsFolder, DIR_EPISODES);
				if (!episodesFolder.exists() || !episodesFolder.canRead()) {
					continue;
				}
				File[] files = episodesFolder.listFiles();
				for (int i=0; i<files.length; i++) {
					File file = files[i];
					String filename = file.getName();
					int dotPosition = filename.lastIndexOf(".");
					if (dotPosition < 1) {
						continue;
					}
					String uuid = filename.substring(0, dotPosition);
					if (uuid.length() != 36) {
						continue;
					}
					//noinspection deprecation
					PodcastEpisode episode = episodeManager.findByUuidSync(uuid);
					if (episode != null) {
						if (episode.getDownloadedFilePath() != null && new File(episode.getDownloadedFilePath()).exists() && episode.isDownloaded()) {
							// skip as the episode download already exists
							continue;
						}

						LogBuffer.INSTANCE.i(LogBuffer.TAG_BACKGROUND_TASKS, "Restoring downloaded file for " + episode.getTitle() + " from " + file.getAbsolutePath());
						// link to the found episode
						episode.setEpisodeStatus(EpisodeStatusEnum.DOWNLOADED);
						episode.setDownloadedFilePath(file.getAbsolutePath());
						episodeManager.update(episode);
					}
				}
			}
		}
		catch (Exception e) {
			Timber.e(e);
		}
	}
}
