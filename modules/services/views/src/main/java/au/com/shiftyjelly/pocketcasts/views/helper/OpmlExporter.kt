@file:Suppress("NAME_SHADOWING", "DEPRECATION")

package au.com.shiftyjelly.pocketcasts.views.helper

import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Xml
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceFragmentCompat
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.ServerCallback
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class OpmlExporter(
    private val fragment: PreferenceFragmentCompat,
    private val serverManager: ServerManager,
    private val podcastManager: PodcastManager,
    private val syncManager: SyncManager,
    private val context: Context,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) {

    companion object {
        const val EXPORT_PICKER_REQUEST_CODE = 43
    }

    private var serverTask: Call? = null
    private var progressDialog: ProgressDialog? = null
    private var sendAsEmail: Boolean = false
    private var opmlFile: File? = null

    fun sendEmail() {
        sendAsEmail = true
        exportPodcasts()
    }

    fun saveFile() {
        sendAsEmail = false
        exportPodcasts()
    }

    fun exportToUri(uri: Uri) {
        val opmlFile = opmlFile
        if (opmlFile == null || !opmlFile.exists()) {
            return
        }

        try {
            context.contentResolver.openFileDescriptor(uri, "w").use { fileDescriptor ->
                val fileDescriptor = fileDescriptor?.fileDescriptor ?: return@use
                FileOutputStream(fileDescriptor).use { fileOutputStream ->
                    FileInputStream(opmlFile).use { fileInputStream ->
                        val buffer = ByteArray(1024)
                        var length = fileInputStream.read(buffer)

                        // Transferring data
                        while (length != -1) {
                            fileOutputStream.write(buffer, 0, length)
                            length = fileInputStream.read(buffer)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "OPML export failed.")
            UiUtil.hideProgressDialog(progressDialog)
            UiUtil.displayAlertError(
                context = context, title = context.getString(LR.string.settings_opml_export_failed_title),
                message = context.getString(
                    LR.string.settings_opml_export_failed
                ),
                onComplete = null
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun exportPodcasts() {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_IMPORT_EXPORT_STARTED)
        showProgressDialog()

        GlobalScope.launch(Dispatchers.IO) {
            val uuidToTitle = podcastManager.findSubscribed().associateBy({ it.uuid }, { it.title })
            val uuids = uuidToTitle.keys.toList()

            serverTask = serverManager.exportFeedUrls(
                uuids,
                object : ServerCallback<Map<String, String>> {
                    override fun onFailed(
                        errorCode: Int,
                        userMessage: String?,
                        serverMessageId: String?,
                        serverMessage: String?,
                        throwable: Throwable?
                    ) {
                        trackFailure(reason = "server_call_failure")
                        UiUtil.hideProgressDialog(progressDialog)
                    }

                    override fun dataReturned(result: Map<String, String>?) {
                        try {
                            opmlFile = if (result == null) null else exportOpml(uuidToTitle, result, context)
                            analyticsTracker.track(AnalyticsEvent.SETTINGS_IMPORT_EXPORT_FINISHED)
                            UiUtil.hideProgressDialog(progressDialog)
                            val opmlFile = opmlFile
                            if (opmlFile != null && opmlFile.exists()) {
                                if (sendAsEmail) {
                                    sendIntentEmail(opmlFile)
                                } else {
                                    sendIntentFile(opmlFile)
                                }
                            }
                        } catch (e: Exception) {
                            trackFailure(reason = "unknown")
                            UiUtil.hideProgressDialog(progressDialog)
                            Timber.e(e)
                        }
                    }
                }
            )
        }
    }

    private fun trackFailure(reason: String) {
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_IMPORT_EXPORT_FAILED,
            mapOf("reason" to reason),
        )
    }

    private fun sendIntentFile(file: File) {
        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/xml"
            val uri = FileUtil.createUriWithReadPermissions(file, intent, fragment.requireActivity())
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Timber.e(e)
                UiUtil.displayAlertError(context, context.getString(LR.string.settings_no_file_browser_title), context.getString(LR.string.settings_no_file_browser), null)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun sendIntentEmail(file: File) {
        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/html"

            val email = syncManager.getEmail()
            if (email != null && email.isNotBlank()) {
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            }

            intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(LR.string.settings_opml_email_subject))
            intent.putExtra(Intent.EXTRA_TEXT, HtmlCompat.fromHtml(context.getString(LR.string.settings_opml_email_body), HtmlCompat.FROM_HTML_MODE_COMPACT))
            val uri = FileUtil.createUriWithReadPermissions(file, intent, fragment.requireActivity())
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            try {
                context.startActivity(intent)
                // fragment.startActivityForResult(intent, EXPORT_PICKER_REQUEST_CODE);
            } catch (e: ActivityNotFoundException) {
                Timber.e(e)
                UiUtil.displayAlertError(context, context.getString(LR.string.settings_no_email_app_title), context.getString(LR.string.settings_no_email_app), null)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    @Suppress("deprecation")
    fun showProgressDialog() {
        UiUtil.hideProgressDialog(progressDialog)
        progressDialog = ProgressDialog.show(context, "", context.getString(LR.string.settings_opml_exporting), true, true) {
            serverTask?.cancel()
        }.apply {
            show()
        }
    }

    private fun exportOpml(uuidToTitle: Map<String, String>, uuidToFeedUrls: Map<String, String>, context: Context): File {
        val emailFolder = File(context.filesDir, "email")
        emailFolder.mkdirs()
        val file = File(emailFolder, "podcasts_opml.xml")
        generateOpml(file, uuidToTitle, uuidToFeedUrls)
        return file
    }

    private fun generateOpml(file: File, uuidToTitle: Map<String, String>, uuidToFeedUrls: Map<String, String>) {
        FileWriter(file.absoluteFile).use { writer ->
            val xml = Xml.newSerializer()
            with(xml) {
                setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
                setOutput(writer)
                startDocument("UTF-8", true)
                startTag("", "opml")
                attribute("", "version", "1.0")
                startTag("", "head")
                startTag("", "title")
                text("Pocket Casts Feeds")
                endTag("", "title")
                endTag("", "head")
                startTag("", "body")
                startTag("", "outline")
                attribute("", "text", "feeds")

                val uuids = uuidToFeedUrls.keys.iterator()
                while (uuids.hasNext()) {
                    val uuid = uuids.next()
                    val feedUrl = uuidToFeedUrls[uuid]
                    val title = uuidToTitle[uuid]

                    startTag("", "outline")
                    attribute("", "type", "rss")
                    attribute("", "text", title ?: "")
                    attribute("", "xmlUrl", feedUrl ?: "")
                    endTag("", "outline")
                }

                endTag("", "outline")
                endTag("", "body")
                endTag("", "opml")
                endDocument()
            }
        }
    }
}
