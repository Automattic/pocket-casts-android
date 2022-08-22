package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import au.com.shiftyjelly.pocketcasts.preferences.BuildConfig
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.Arrays
import java.util.regex.Matcher

object IntentUtil {

    fun isPocketCastsWebsite(intent: Intent): Boolean {
        return intent.data?.host == BuildConfig.WEB_BASE_HOST
    }

    fun isPodloveUrl(intent: Intent): Boolean {
        val scheme = intent.scheme
        if (scheme == null || scheme != "pktc" || intent.data == null || intent.data?.host == null) {
            return false
        }

        val host = intent.data?.host
        return host == "subscribe" || host == "subscribehttps"
    }

    fun isSonosAppLinkUrl(intent: Intent): Boolean {
        val scheme = intent.scheme
        if (scheme == null || scheme != "pktc" || intent.data == null || intent.data?.host == null) {
            return false
        }

        val host = intent.data?.host
        return host == "applink"
    }

    fun getPodloveUrl(intent: Intent): String? {
        val uri = intent.data ?: return null
        var path = uri.path ?: return null
        if (path.startsWith("/")) {
            path = path.replaceFirst(Matcher.quoteReplacement("/").toRegex(), "")
        }
        if (path.length < 3) {
            return null
        }
        val host = uri.host
        val params = uri.encodedQuery
        return (if (host == "subscribehttps") "https" else "http") + "://" + path + if (!params.isNullOrEmpty()) "?${uri.encodedQuery}" else ""
    }

    fun isShareLink(intent: Intent): Boolean {
        val scheme = intent.scheme
        return scheme != null && scheme == "pktc" && intent.data != null && intent.data?.path != null
    }

    // http://subscribeonandroid.com/geeknewscentral.com/podcast.xml
    fun isSubscribeOnAndroidUrl(intent: Intent): Boolean {
        val scheme = intent.scheme
        if (scheme == null || !(scheme == "http" || scheme == "https")) {
            return false
        }

        if (intent.data == null || intent.data?.host == null) {
            return false
        }
        val host = intent.data?.host
        return host == "subscribeonandroid.com" || host == "www.subscribeonandroid.com"
    }

    fun getSubscribeOnAndroidUrl(intent: Intent): String? {
        val uri = intent.data ?: return null
        var path = uri.path ?: return null
        if (path.startsWith("/")) {
            path = path.replaceFirst(Matcher.quoteReplacement("/").toRegex(), "")
        }
        if (path.length < 3) {
            return null
        }
        val scheme = intent.scheme
        return "$scheme://$path"
    }

    fun webViewShouldOverrideUrl(url: String?, context: Context): Boolean {
        var urlFound: String = url ?: return true
        val urlLower = urlFound.lowercase()
        if (urlLower.startsWith("http://") || urlLower.startsWith("https://") || urlLower.startsWith("ftp://") || urlLower.startsWith("market://")) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlFound)))
            return true
        }
        if (urlFound.startsWith("mailto:")) {
            try {
                val to = ArrayList<String>()
                val cc = ArrayList<String>()
                val bcc = ArrayList<String>()
                var subject: String? = null
                var body: String? = null

                urlFound = urlFound.replaceFirst("mailto:".toRegex(), "")

                val urlSections = urlFound.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (urlSections.size >= 2) {

                    to.addAll(Arrays.asList(*urlSections[0].split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))

                    for (i in 1 until urlSections.size) {
                        val urlSection = urlSections[i]
                        val keyValue = urlSection.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                        if (keyValue.size == 2) {
                            val key = keyValue[0]
                            var value = keyValue[1]

                            value = URLDecoder.decode(value, "UTF-8")

                            if (key == "cc") {
                                cc.addAll(Arrays.asList(*urlFound.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
                            } else if (key == "bcc") {
                                bcc.addAll(Arrays.asList(*urlFound.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
                            } else if (key == "subject") {
                                subject = value
                            } else if (key == "body") {
                                body = value
                            }
                        }
                    }
                } else {
                    to.addAll(Arrays.asList(*urlFound.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
                }

                val emailIntent = Intent(android.content.Intent.ACTION_SEND)
                emailIntent.type = "message/rfc822"
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, to.toTypedArray())
                if (cc.size > 0) {
                    emailIntent.putExtra(android.content.Intent.EXTRA_CC, cc.toTypedArray())
                }
                if (bcc.size > 0) {
                    emailIntent.putExtra(android.content.Intent.EXTRA_BCC, bcc.toTypedArray())
                }
                if (subject != null) {
                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
                }
                if (body != null) {
                    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, body)
                }
                try {
                    context.startActivity(emailIntent)
                } catch (ae: ActivityNotFoundException) {
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("No mail app found")
                    builder.setMessage("Sorry failed to compose an email to " + to.toString().replace("[", "").replace("]", ""))
                    builder.setPositiveButton("Close", null)
                    builder.show()
                }

                return true
            } catch (e: UnsupportedEncodingException) {
                /* Won't happen*/
            }
        }

        // bug to fix href="patreon.com/dearhankandjohn"
        if (urlFound.startsWith("file:///android_asset/")) {
            val relativeUrl = urlFound.replace("file:///android_asset/", "")
            if (!relativeUrl.startsWith("http://")) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://$relativeUrl")))
                return true
            }
        }

        return true
    }

    fun isPodcastListShare(intent: Intent): Boolean {
        val scheme = intent.scheme
        if (scheme == null || !(scheme == "http" || scheme == "https")) {
            return false
        }

        val host = intent.data?.host
        return host == "lists.pocketcasts.com"
    }

    fun isPodcastListShareMobile(intent: Intent): Boolean {
        val scheme = intent.scheme
        if (scheme == null || scheme != "pktc") {
            return false
        }

        val host = intent.data?.host
        return host == "sharelist"
    }

    fun getUrl(intent: Intent): String? {
        return if (intent.data != null && intent.data.toString().isNotBlank()) intent.data.toString() else null
    }

    fun isItunesLink(intent: Intent): Boolean {
        return intent.data?.host == "itunes.apple.com" || intent.data?.host == "podcasts.apple.com"
    }

    fun isCloudFilesIntent(intent: Intent): Boolean {
        val scheme = intent.scheme
        if (scheme == null || scheme != "pktc") {
            return false
        }

        return Uri.parse(Settings.INTENT_LINK_CLOUD_FILES).host == intent.data?.host
    }

    fun isUpgradeIntent(intent: Intent): Boolean {
        val scheme = intent.scheme
        if (scheme == null || scheme != "pktc") {
            return false
        }

        return Uri.parse(Settings.INTENT_LINK_UPGRADE).host == intent.data?.host
    }

    fun isPromoCodeIntent(intent: Intent): Boolean {
        val scheme = intent.scheme
        if (scheme == null || scheme != "pktc") {
            return false
        }

        val uri = Uri.parse(Settings.INTENT_LINK_PROMO_CODE)
        return uri.host == intent.data?.host && intent.data?.pathSegments?.firstOrNull() == uri.pathSegments.first()
    }

    fun openWebPage(url: String): Intent {
        var uri = Uri.parse(url)
        // fix for podcast web pages that don't start with http://
        if (uri.scheme.isNullOrBlank() && !url.contains("://")) {
            uri = Uri.parse("http://$url")
        }
        return Intent(Intent.ACTION_VIEW, uri)
    }
}
