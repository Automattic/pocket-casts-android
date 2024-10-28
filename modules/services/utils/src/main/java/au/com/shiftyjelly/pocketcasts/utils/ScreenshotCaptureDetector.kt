package au.com.shiftyjelly.pocketcasts.utils

import android.Manifest.permission.DETECT_SCREEN_CAPTURE
import android.app.Activity
import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.MediaStore.Images.Media
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

class ScreenshotCaptureDetector private constructor(
    private val registerCallback: () -> Unit,
    private val unregisterCallback: () -> Unit,
) {
    fun register() = registerCallback()

    fun unregister() = unregisterCallback()

    companion object {
        @RequiresPermission(DETECT_SCREEN_CAPTURE)
        fun create(
            activity: Activity,
            onScreenshot: () -> Unit,
        ): ScreenshotCaptureDetector {
            return when {
                Build.VERSION.SDK_INT >= 34 -> createNativeScreenshotProcess(activity, onScreenshot)
                else -> createContentResolverScreenshotProcess(activity, onScreenshot)
            }
        }

        @RequiresPermission(DETECT_SCREEN_CAPTURE)
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        private fun createNativeScreenshotProcess(
            activity: Activity,
            onScreenshot: () -> Unit,
        ): ScreenshotCaptureDetector {
            val callback = Activity.ScreenCaptureCallback { onScreenshot() }
            return ScreenshotCaptureDetector(
                registerCallback = {
                    activity.registerScreenCaptureCallback(activity.mainExecutor, callback)
                },
                unregisterCallback = {
                    activity.unregisterScreenCaptureCallback(callback)
                },
            )
        }

        private fun createContentResolverScreenshotProcess(
            activity: Activity,
            onScreenshot: () -> Unit,
        ): ScreenshotCaptureDetector {
            val contentObserver = createContentObserver(activity, onScreenshot)
            return ScreenshotCaptureDetector(
                registerCallback = {
                    activity.contentResolver.registerContentObserver(
                        Media.EXTERNAL_CONTENT_URI,
                        true, // notifyForDescendants
                        contentObserver,
                    )
                },
                unregisterCallback = {
                    activity.contentResolver.unregisterContentObserver(contentObserver)
                },
            )
        }

        private fun createContentObserver(
            activity: Activity,
            onScreenshot: () -> Unit,
        ): ContentObserver {
            return object : ContentObserver(Handler(activity.mainLooper)) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    if (uri == null) {
                        return
                    }
                    when {
                        Build.VERSION.SDK_INT >= 29 -> activity.contentResolver.detectScreenshotBasedOnPath(uri, onScreenshot)
                        else -> activity.contentResolver.detectScreenshotBasedOnData(uri, onScreenshot)
                    }
                }
            }
        }

        private fun ContentResolver.detectScreenshotBasedOnPath(
            uri: Uri,
            onScreenshot: () -> Unit,
        ) {
            query(
                uri,
                arrayOf(Media.DISPLAY_NAME, Media.RELATIVE_PATH),
                null, // selection
                null, // selctionArgs
                null, // sortOrder
            )?.use { cursor ->
                runCatching {
                    val nameColumn = cursor.getColumnIndex(Media.DISPLAY_NAME)
                    val pathColum = cursor.getColumnIndex(Media.RELATIVE_PATH)
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameColumn)
                        val path = cursor.getString(pathColum)
                        if (name.contains("screenshot", ignoreCase = true) || path.contains("screenshot", ignoreCase = true)) {
                            onScreenshot()
                            break
                        }
                    }
                }
            }
        }

        private fun ContentResolver.detectScreenshotBasedOnData(
            uri: Uri,
            onScreenshot: () -> Unit,
        ) {
            query(
                uri,
                arrayOf(Media.DATA),
                null, // selection
                null, // selctionArgs
                null, // sortOrder
            )?.use { cursor ->
                runCatching {
                    val dataColumn = cursor.getColumnIndex(Media.DATA)
                    while (cursor.moveToNext()) {
                        val data = cursor.getString(dataColumn)
                        if (data.contains("screenshot", ignoreCase = true)) {
                            onScreenshot()
                            break
                        }
                    }
                }
            }
        }
    }
}
