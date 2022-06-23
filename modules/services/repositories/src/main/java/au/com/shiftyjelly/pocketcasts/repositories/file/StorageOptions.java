package au.com.shiftyjelly.pocketcasts.repositories.file;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import au.com.shiftyjelly.pocketcasts.localization.R;

public class StorageOptions {
	
	private ArrayList<FolderLocation> folderLocations = null;

	public List<FolderLocation> getFolderLocations(Context context){
		if (folderLocations == null){
			folderLocations = new ArrayList<>();

            ArrayList<String> confirmedMountPoints = null;
			File[] externalDirs = context.getExternalFilesDirs(null);

			confirmedMountPoints = new ArrayList<>();
			if (externalDirs != null && externalDirs.length > 0){
				for (File directory : externalDirs){
					if (directory != null) {
						confirmedMountPoints.add(directory.getAbsolutePath());
					}
				}
			}

            testAndCleanMountsList(confirmedMountPoints);
            determineFolderLocations(confirmedMountPoints, context);
		}
		
		return folderLocations;
	}

	private void testAndCleanMountsList(ArrayList<String> confirmedMountPoints) {
		/*
		 * Now that we have a cleaned list of mount paths Test each one to make
		 * sure it's a valid and available path. If it is not, remove it from
		 * the list.
		 */

		for (Iterator<String> it = confirmedMountPoints.iterator(); it.hasNext();){
			String mount = it.next();
			File folder = new File(mount);
			if (!folder.exists() || !folder.isDirectory() || !folder.canWrite()){
				it.remove();
			}
		}
	}

	@SuppressLint("NewApi")
	private void determineFolderLocations(ArrayList<String> confirmedMountPoints, Context context) {
		if (confirmedMountPoints.size() == 0) return;
		
		int externalSDCardCount = 1;
		String firstMountPoint = confirmedMountPoints.get(0);
		confirmedMountPoints.remove(0);

		Resources resources = context.getResources();
		//the first mount point is different to the rest
		if (!Environment.isExternalStorageRemovable() || Environment.isExternalStorageEmulated()){
			folderLocations.add(new FolderLocation(firstMountPoint, resources.getString(R.string.settings_storage_phone)));
		}
		else {
			folderLocations.add(new FolderLocation(firstMountPoint, resources.getString(R.string.settings_storage_sd_card)));
			externalSDCardCount++;
		}
		
		//label all the rest as external storage
		for (String mountPoint : confirmedMountPoints){
			String label = externalSDCardCount == 1 ? resources.getString(R.string.settings_storage_sd_card) : resources.getString(R.string.settings_storage_sd_card_number, String.valueOf(externalSDCardCount));
			folderLocations.add(new FolderLocation(mountPoint, label));
			externalSDCardCount++;
		}
	}
}
