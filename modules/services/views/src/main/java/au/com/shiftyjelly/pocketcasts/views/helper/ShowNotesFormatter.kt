package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import timber.log.Timber
import java.text.Bidi
import java.util.*
import java.util.regex.Pattern

class ShowNotesFormatter(settings: Settings?, private val context: Context) {

    private val showImages: Boolean = settings != null && settings.isShowNotesImagesOn()
    private var padding = "0px"
    private var convertTimesToLinks: Boolean = false

    var backgroundColor = "#fafafa"
    var textColor = "#202020"
    var linkColor = "#ff4444"

    fun setBackgroundThemeColor(backgroundColorAttrId: Int): ShowNotesFormatter {
        backgroundColor = UiUtil.colorIntToHexString(context.getThemeColor(backgroundColorAttrId))
        return this
    }

    fun setTextThemeColor(textColorAttrId: Int): ShowNotesFormatter {
        textColor = UiUtil.colorIntToHexString(context.getThemeColor(textColorAttrId))
        return this
    }

    fun setLinkThemeColor(linkColorAttrId: Int): ShowNotesFormatter {
        linkColor = UiUtil.colorIntToHexString(context.getThemeColor(linkColorAttrId))
        return this
    }

    fun setConvertTimesToLinks(convertTimesToLinks: Boolean): ShowNotesFormatter {
        this.convertTimesToLinks = convertTimesToLinks
        return this
    }

    fun format(showNotes: String?): String? {
        if (showNotes == null) {
            return null
        }

        val html = StringBuilder()

        try {
            // remove the show notes body
            val bodyIndexStart = showNotes.indexOf("<body>")
            val bodyIndexEnd = showNotes.indexOf("</body>")
            var body: String
            if (bodyIndexStart == -1 || bodyIndexEnd == -1) {
                body = showNotes.replace(Pattern.quote("<body>").toRegex(), "").replace(Pattern.quote("</body>").toRegex(), "").replace(Pattern.quote("<html>").toRegex(), "")
                    .replace(Pattern.quote("</html>").toRegex(), "")
            } else {
                body = showNotes.substring(bodyIndexStart + 6, bodyIndexEnd)
            }

            // link episode times to player
            if (convertTimesToLinks) {
                body = ShowNotesHelper.convertTimesToLinks(body)
            }

            addShowNotesHead(html)

            val isRtl = !Bidi(showNotes, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT).baseIsLeftToRight()
            html.append("<body ${if (isRtl) "dir=\"rtl\"" else ""}>")

            val bodyTrimmed = body.trim { it <= ' ' }
            val addParagraph = !bodyTrimmed.startsWith("<p")
            if (addParagraph) {
                html.append("<p>")
            }
            html.append(bodyTrimmed)
            if (addParagraph) {
                html.append("</p>")
            }
            html.append("</body>\n</html>")
        } catch (e: Exception) {
            Timber.e(e, "Unable to format show notes. ")
        }

        // Timber.i("SHOW_NOTES \n"+html.toString());
        // Timber.i("SHOW_NOTES BODY ["+body.toString()+"]");
        return html.toString()
    }

    private fun addShowNotesHead(html: StringBuilder) {
        val fontScale = context.resources.configuration.fontScale
        val fontSize = fontScale * 15
        val lineHeight = 1.5f

        html.append("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-16le\">\n")
        html.append("<style type=\"text/css\">\n")
        html.append("body { margin: 0; padding: 0 ").append(padding).append(" ").append(padding).append(" ").append(padding).append("; }\n")
        html.append("@font-face { font-family: 'sans-serif-medium'; src: url('fonts/sans-serif-medium.ttf'); } \n")
        html.append("@font-face { font-family: 'sans-serif-regular'; src: url('fonts/sans-serif-regular.ttf'); } \n")
        html.append("body, h1, h2, h3, h4, h5, h6 { font-family: 'sans-serif-regular'; font-weight: 400; word-wrap:break-word; font-size:").append(fontSize).append("px; line-height: ").append(lineHeight)
            .append("em; color:").append(textColor).append("; background-color: ").append(backgroundColor).append("; ").append(" } \n")
        html.append(".pageHeader { font-family: 'sans-serif-medium'; margin: 16px 0 4px 0; padding: 0 0 0 0  } \n")
        html.append(".separator { padding: 0 2px; color: #D8D8D8; } \n")
        html.append("p { margin: 4px 0 8px 0; } \n")
        html.append("a, .pageHeader { color:").append(linkColor).append("; font-weight: 400; } \n")
        if (showImages) {
            html.append("img { width: auto !important; height: auto !important; max-width:100%; max-height: auto; padding-bottom: 10px; padding-top: 10px; display: block; }\nimg[src*='coverart'], img[src*='CoverArt'], img[src*='COVERART'], img[src*='feeds.feedburner.com'] { display: none; } \n")
        } else {
            html.append("img { display: none; } \n")
        }

        html.append("</style></head>\n")
    }
}
