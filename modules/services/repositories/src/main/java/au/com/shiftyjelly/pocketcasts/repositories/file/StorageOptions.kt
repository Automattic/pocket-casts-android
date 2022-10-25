package au.com.shiftyjelly.pocketcasts.repositories.file

import android.content.Context
import android.os.Environment
import au.com.shiftyjelly.pocketcasts.localization.R
import java.io.File

class StorageOptions {

    private var folderLocations: ArrayList<FolderLocation>? = null

    fun getFolderLocations(context: Context): List<FolderLocation> {
        if (folderLocations == null) {
            folderLocations = arrayListOf()
            val confirmedMountPoints = arrayListOf<String>()

            val externalDirs = context.getExternalFilesDirs(null)
            if (externalDirs != null && externalDirs.isNotEmpty()) {
                for (directory in externalDirs) {
                    if (directory != null) {
                        confirmedMountPoints.add(directory.absolutePath)
                    }
                }
            }

            testAndCleanMountsList(confirmedMountPoints)
            determineFolderLocations(confirmedMountPoints, context)
        }

        return folderLocations as List<FolderLocation>
    }

    private fun testAndCleanMountsList(confirmedMountPoints: ArrayList<String>) {
        /*
		 * Now that we have a cleaned list of mount paths Test each one to make
		 * sure it's a valid and available path. If it is not, remove it from
		 * the list.
		 */
        val iterator = confirmedMountPoints.iterator()
        while (iterator.hasNext()) {
            val mount = iterator.next()
            val folder = File(mount)
            if (!folder.exists() || !folder.isDirectory || !folder.canWrite()) {
                iterator.remove()
            }
        }
    }

    private fun determineFolderLocations(
        confirmedMountPoints: ArrayList<String>,
        context: Context
    ) {
        if (confirmedMountPoints.isEmpty()) return

        var externalSDCardCount = 1
        val firstMountPoint = confirmedMountPoints[0]
        confirmedMountPoints.removeAt(0)

        val resources = context.resources
        // the first mount point is different to the rest
        if (!Environment.isExternalStorageRemovable() ||
            Environment.isExternalStorageEmulated()
        ) {
            requireNotNull(folderLocations) {
                "folderLocations can not be null"
            }.add(
                FolderLocation(
                    firstMountPoint,
                    resources.getString(R.string.settings_storage_phone)
                )
            )
        } else {
            requireNotNull(folderLocations) {
                "folderLocations can not be null"
            }.add(
                FolderLocation(
                    firstMountPoint,
                    resources.getString(R.string.settings_storage_sd_card)
                )
            )
            externalSDCardCount++
        }

        // label all the rest as external storage
        for (mountPoint in confirmedMountPoints) {
            val label = if (externalSDCardCount == 1) {
                resources.getString(R.string.settings_storage_sd_card)
            } else {
                resources.getString(
                    R.string.settings_storage_sd_card_number,
                    externalSDCardCount.toString()
                )
            }
            requireNotNull(folderLocations) {
                "folderLocations can not be null"
            }.add(FolderLocation(mountPoint, label))
            externalSDCardCount++
        }
    }
}
