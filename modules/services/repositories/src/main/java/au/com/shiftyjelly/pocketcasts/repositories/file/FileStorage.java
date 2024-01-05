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
	private final FileStorageKtDelegate delegate;

	@Inject
    public FileStorage(Settings settings, @ApplicationContext  Context context) {
		delegate = new FileStorageKtDelegate(settings, context);
    }

	@NonNull
	public File getPodcastEpisodeFile(BaseEpisode episode) throws StorageException {
		return delegate.getPodcastEpisodeFile(episode);
	}

	@NonNull
	public File getTempPodcastEpisodeFile(BaseEpisode episode) throws StorageException {
		return delegate.getTempPodcastEpisodeFile(episode);
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
		delegate.checkNoMediaDirs();
	}

	private String moveFileToDirectory(String filePath, File directory) {
		return delegate.moveFileToDirectory(filePath,directory);
	}
	
	private void moveDirectory(File fromDirectory, File toDirectory) {
		delegate.moveDirectory(fromDirectory, toDirectory);
	}

	public void moveStorage(File oldDirectory, File newDirectory, EpisodeManager episodeManager) {
		delegate.moveStorage(oldDirectory, newDirectory, episodeManager);
	}

	public void fixBrokenFiles(EpisodeManager episodeManager) {
		delegate.fixBrokenFiles(episodeManager);
	}
}
