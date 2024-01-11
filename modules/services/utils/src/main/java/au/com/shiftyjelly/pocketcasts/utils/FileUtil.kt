package au.com.shiftyjelly.pocketcasts.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.OutputStream
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber

object FileUtil {
    fun deleteFileByPath(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                if (!file.delete()) {
                    Timber.e("Could not delete file $file")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Could not delete file $path")
        }
    }

    fun deleteDirectoryContents(path: String) {
        try {
            val dir = File(path)
            if (!dir.isDirectory) {
                return
            }

            dir.listFiles()?.forEach { file ->
                if (!file.name.equals(".nomedia", ignoreCase = true)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Could not delete directory $path contents")
        }
    }

    fun readFileTo(file: File, output: OutputStream) {
        file.source().buffer().use { source ->
            source.readAll(output.sink())
        }
    }

    fun copy(src: File, dst: File) {
        if (src.isDirectory) {
            copyDirectory(src, dst)
        } else {
            copyFile(src, dst)
        }
    }

    fun copyFile(src: File, dst: File) {
        dst.parentFile?.mkdirs()
        src.source().use { source ->
            dst.sink().buffer().use { sink ->
                sink.writeAll(source)
            }
        }
    }

    fun copyDirectory(src: File, dst: File) {
        if (src.isDirectory) {
            if (!dst.exists()) {
                dst.mkdirs()
            }

            src.list()?.forEach { name ->
                copyDirectory(File(src, name), File(dst, name))
            }
        } else {
            copyFile(src, dst)
        }
    }

    fun getFileNameWithoutExtension(file: File): String {
        val dotIndex = file.name.lastIndexOf('.')
        return if (dotIndex != -1) {
            file.name.substring(0, dotIndex)
        } else {
            file.name
        }
    }

    fun folderSize(directory: File): Long = directory.listFiles().orEmpty()
        .fold(0L) { size, file ->
            size + if (file.isFile) file.length() else folderSize(file)
        }

    fun createUriWithReadPermissions(file: File, context: Context, intent: Intent): Uri {
        val uri = getUriForFile(context, file)
        context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).forEach { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return uri
    }

    fun getUriForFile(context: Context, file: File): Uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
}
