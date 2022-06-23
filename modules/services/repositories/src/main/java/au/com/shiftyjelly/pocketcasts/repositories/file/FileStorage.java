package au.com.shiftyjelly.pocketcasts.repositories.file;

import android.content.Context;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager;
import au.com.shiftyjelly.pocketcasts.preferences.Settings;
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum;
import au.com.shiftyjelly.pocketcasts.models.entity.Episode;
import au.com.shiftyjelly.pocketcasts.models.entity.Playable;
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager;
import au.com.shiftyjelly.pocketcasts.utils.FileUtil;
import au.com.shiftyjelly.pocketcasts.utils.StringUtil;
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer;
import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

@Singleton
public class FileStorage {

	private static final String FOLDER_POCKETCASTS = "PocketCasts";
	private static final String FOLDER_TEMP_EPISODES = "downloadTmp";

	public static final String DIR_OPML_FOLDER = "opml_import";
	public static final String DIR_CUSTOM_FILES = "custom_episodes";
	public static final String DIR_PODCAST_THUMBNAILS = "thumbnails";
	public static final String DIR_NETWORK_IMAGES = "network_images";
	public static final String DIR_WEB_CACHE = "web_cache";
	public static final String DIR_PODCAST_IMAGES = "images";
	public static final String DIR_EPISODES = "podcasts";
	public static final String DIR_PODCAST_GROUP_IMAGES = "network_images" + File.separator + "groups" + File.separator;
	public static final String DIR_CLOUD_FILES = "cloud_files";

	private Settings settings;
	private Context context;

	@Inject
    public FileStorage(Settings settings, @ApplicationContext  Context context) {
        this.settings = settings;
		this.context = context;
    }

	@NonNull
	public File getPodcastEpisodeFile(Playable episode) throws StorageException {
		String fileName = episode.getUuid() + episode.getFileExtension();
		File directory;
		if (episode instanceof Episode) {
			directory = getPodcastDirectory();
		} else {
			directory = getCloudFilesFolder();
		}

		return new File(directory, fileName);
	}

	@NonNull
	public File getTempPodcastEpsisodeFile(Playable episode) throws StorageException {
		String fileName = episode.getUuid() + episode.getFileExtension();		
		return new File(getTempPodcastDirectory(), fileName);
	}

	@Nullable
	public File getCloudFileImage(String uuid) {
    	String fileName = uuid + "_imagefile";
		try {
			return new File(getCloudFilesFolder(), fileName);
		}
		catch(StorageException e) {
			Timber.e(e);
			return null;
		}
	}

    public File getCloudFilesFolder() throws StorageException {
    	return getOrCreateDirectory(DIR_CLOUD_FILES);
	}

	public File getOpmlFileFolder() throws StorageException {
		return getOrCreateDirectory(DIR_OPML_FOLDER);
	}

	public  File getNetworkImageDirectory() throws StorageException {
		return getOrCreateDirectory(DIR_NETWORK_IMAGES);
	}
	
	public  File getPodcastDirectory() throws StorageException {
		return getOrCreateDirectory(DIR_EPISODES);
	}
	
	public  File getTempPodcastDirectory() throws StorageException {
		return getOrCreateCacheDirectory(FOLDER_TEMP_EPISODES);
	}

	public File getOldTempPodcastDirectory() throws StorageException {
    	return getOrCreateDirectory(FOLDER_TEMP_EPISODES);
	}

	public  File getPodcastGroupImageDirectory() throws StorageException {
		File dir = new File(getStorageDirectory().getAbsolutePath() + File.separator + DIR_PODCAST_GROUP_IMAGES);
		createDirectory(dir);
        addNoMediaFile(dir, settings);
		return dir;
	}

	public File getOrCreateCacheDirectory(String name) throws StorageException {
    	return getOrCreateDirectory(context.getCacheDir(), name);
	}

    public File getOrCreateDirectory(String name) throws StorageException {
		return getOrCreateDirectory(getStorageDirectory(), name);
	}
	
	private File getOrCreateDirectory(File parentDir, String name) throws StorageException {
		File dir = new File(parentDir, name + File.separator);
		createDirectory(dir);
        addNoMediaFile(dir, settings);
		return dir;
	}
	
	public File getStorageDirectory() throws StorageException {
		File dir = new File(getBaseStorageDirectory(), "PocketCasts" + File.separator);
		createDirectory(dir);
		return dir; 
	}
	
	public File getBaseStorageDirectory() throws StorageException {
		if (context == null) throw new StorageException("Context is null");
		
		return getBaseStorageDirectory(settings.getStorageChoice(), context);
	}
	
	public File getBaseStorageDirectory(String choice, Context context) throws StorageException {
		if (choice.equals(Settings.STORAGE_ON_CUSTOM_FOLDER)) {
			String path = settings.getStorageCustomFolder();
			if (StringUtil.isBlank(path)) {
				throw new StorageException("Ooops, please set the Custom Folder Location in the settings.");
			}
			File folder = new File(path);
			if (!folder.exists()) {
				boolean success = folder.mkdirs();
				if (!success && !folder.exists()) {
					throw new StorageException("Storage custom folder unavailable.");
				}
			}
			return folder;
		}
		else {
			return new File(choice);
		}
	}
	
	private final static File createDirectory(File dir) {
		dir.mkdirs();
		return dir;
	}
	
	private final static void addNoMediaFile(File folder, Settings settings) {
		if (folder == null || !folder.exists() || settings.allowOtherAppsAccessToEpisodes()) {
			return;
		}
		File file = new File(folder, ".nomedia");
		if (!file.exists()){
			try {
				file.createNewFile();
			} 
			catch (Exception e) {
				Timber.e(e);
			}
		}
	}
	
	public final static boolean isExternalStorageAvailable() {
		String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}
	
	// confirms that all the folders we want to hide from the user have .nomedia files in them
	public void checkNoMediaDirs(){
		// we can do this by getting all the folders
		try {
            addNoMediaFile(getStorageDirectory(), settings);
			getNetworkImageDirectory();
			getPodcastGroupImageDirectory();
			getTempPodcastDirectory();
		} 
		catch (Exception e) {
			Timber.e(e);
		}
	}

    public void clearNoMediaFiles() {
        try {
            File storageDir = getStorageDirectory();
            enableNoMediaFile(storageDir, false);
        }
        catch (Exception e) {
			Timber.e(e);
        }
    }

    public void addNoMediaFiles() {
        try {
            File storageDir = getStorageDirectory();
            enableNoMediaFile(storageDir, true);
        }
        catch (Exception e) {
			Timber.e(e);
        }
    }

    private void enableNoMediaFile(File dir, boolean enable) {
        try {
            File noMediaFile = new File(dir, ".nomedia");
            if (enable) {
                if (!noMediaFile.exists()) {
                    noMediaFile.createNewFile();
                }
            }
            else {
                if (noMediaFile.exists()) {
                    noMediaFile.delete();
                }
            }

            File[] files = dir.listFiles();
            if (files == null || files.length == 0) return;
            for (int i=0; i<files.length; i++) {
                File file = files[i];
                if (file.isDirectory()) {
                    enableNoMediaFile(file, enable);
                }
            }
        }
        catch (Exception e) {
			Timber.e(e);
        }
    }

	public void removeDirectoryFiles(File directory) {
		if (directory == null || !directory.exists() || !directory.isDirectory()) return;
		
		File[] files = directory.listFiles();
		if (files == null || files.length == 0) return;
		
		for (File file : files) {
			if (file.getName() == null || file.getName().startsWith(".")) {
				continue;
			}
			file.delete();
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

	public void moveStorage(File oldDirectory, File newDirectory, PodcastManager podcastManager, EpisodeManager episodeManager) {
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
							Episode episode = episodeManager.findByUuid(fileName);
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
				List<Episode> episodes = episodeManager.observeDownloadedEpisodes().blockingFirst();
				for (Episode episode : episodes) {
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

    public static boolean moveFile(String oldPath, String newPath) {
		if (StringUtil.isBlank(oldPath) || StringUtil.isBlank(newPath))  return false;
		
		File oldFile = new File(oldPath);
		File newFile = new File(newPath);
		if (!oldFile.exists()) return false;
		
		try {
			FileUtil.copyFile(oldFile, newFile);
			oldFile.delete();
			
			return true;
		} 
		catch (IOException e) {
			Timber.e(e,"Problems moving a file to a new location. from: "+oldFile.getAbsolutePath()+" to: "+newFile.getAbsolutePath());
		}
		
		return false;
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

			boolean foundEpisodes = false;

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
					Episode episode = episodeManager.findByUuid(uuid);
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
						foundEpisodes = true;
					}
				}
			}
		}
		catch (Exception e) {
			Timber.e(e);
		}
	}
}
