@file:Suppress("NAME_SHADOWING", "DEPRECATION")

package au.com.shiftyjelly.pocketcasts.views.helper

import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Xml
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceFragmentCompat
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.ServiceManager
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import java.io.File
import java.io.FileWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class OpmlExporter(
    private val fragment: PreferenceFragmentCompat,
    private val serviceManager: ServiceManager,
    private val podcastManager: PodcastManager,
    private val syncManager: SyncManager,
    private val context: Context,
    private val analyticsTracker: AnalyticsTracker,
    private val applicationScope: CoroutineScope,
) {

    private var exportJob: Job? = null
    private var progressDialog: ProgressDialog? = null

    fun sendEmail() {
        exportPodcasts(sendAsEmail = true)
    }

    fun saveFile() {
        exportPodcasts(sendAsEmail = false)
    }

    private fun exportPodcasts(sendAsEmail: Boolean) {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_IMPORT_EXPORT_STARTED)
        showProgressDialog()

        exportJob?.cancel()
        exportJob = applicationScope.launch {
            val uuidToTitle = podcastManager.findSubscribedNoOrder().associateBy({ it.uuid }, { it.title })
            val uuids = uuidToTitle.keys.toList()

            val opmlFile = serviceManager.exportFeedUrls(uuids)
                .onFailure { trackFailure(reason = "server_call_failure") }
                .getOrNull()
                ?.let { result ->
                    runCatching { exportOpml(uuidToTitle, result, context) }
                        .onFailure { error ->
                            trackFailure(reason = "unknown")
                            Timber.e(error)
                        }
                }
                ?.getOrNull()
                ?.takeIf(File::exists)

            withContext(Dispatchers.Main) {
                UiUtil.hideProgressDialog(progressDialog)
                if (opmlFile != null) {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_IMPORT_EXPORT_FINISHED)
                    if (sendAsEmail) {
                        sendIntentEmail(opmlFile)
                    } else {
                        sendIntent(opmlFile)
                    }
                }
            }
        }
    }

    private fun trackFailure(reason: String) {
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_IMPORT_EXPORT_FAILED,
            mapOf("reason" to reason),
        )
    }

    private fun sendIntentEmail(file: File) {
        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/html"

            val email = syncManager.getEmail()
            if (!email.isNullOrBlank()) {
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            }

            intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(LR.string.settings_opml_email_subject))
            intent.putExtra(Intent.EXTRA_TEXT, HtmlCompat.fromHtml(context.getString(LR.string.settings_opml_email_body), HtmlCompat.FROM_HTML_MODE_COMPACT))
            val uri = FileUtil.createUriWithReadPermissions(fragment.requireActivity(), file, intent)
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Timber.e(e)
                UiUtil.displayAlertError(context, context.getString(LR.string.settings_no_email_app_title), context.getString(LR.string.settings_no_email_app), null)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun sendIntent(file: File) {
        IntentUtil.sendIntent(
            context = context,
            file = file,
            intentType = "text/xml",
            errorMessage = context.getString(LR.string.settings_opml_export_failed),
            errorTitle = context.getString(LR.string.settings_no_file_browser_title),
        )
    }

    @Suppress("deprecation")
    fun showProgressDialog() {
        UiUtil.hideProgressDialog(progressDialog)
        progressDialog = ProgressDialog.show(context, "", context.getString(LR.string.settings_opml_exporting), true, true) {
            exportJob?.cancel()
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
