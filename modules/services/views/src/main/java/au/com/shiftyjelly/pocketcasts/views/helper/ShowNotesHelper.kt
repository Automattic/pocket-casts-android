package au.com.shiftyjelly.pocketcasts.views.helper

object ShowNotesHelper {

    fun convertTimesToLinks(html: String): String {
        return html.replace("(\\A|\\s|>|[^a-zsA-Z_0-9/])(\\d{0,2}:?\\d{1,2}:\\d{2})(<|\\s|\\z|[^a-zsA-Z_0-9\"])".toRegex()) { match ->
            return@replace if (matchIsInUrl(match, html)) {
                match.value
            } else {
                val (prefix, timeskip, suffix) = match.destructured
                "$prefix<a href=\"http://localhost/#playerJumpTo=$timeskip\">$timeskip</a>$suffix"
            }
        }
    }

    // searches for the first closing tag that comes after a potential timeskip and compares it to url closing tag (</a>)
    private fun matchIsInUrl(match: MatchResult, html: String): Boolean {
        // try to find by the <a> tag first (either <a href=...> or <a>)
        val tagRegex = "<a(?:\\s|>)|</a>".toRegex()
        return tagRegex.find(html.substring(match.range.first))?.value == "</a>"
    }
}
