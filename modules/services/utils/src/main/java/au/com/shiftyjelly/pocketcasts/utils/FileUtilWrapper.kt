package au.com.shiftyjelly.pocketcasts.utils

import javax.inject.Inject

class FileUtilWrapper @Inject constructor() {
    fun deleteDirectoryContents(path: String) {
        FileUtil.deleteDirectoryContents(path)
    }
}
