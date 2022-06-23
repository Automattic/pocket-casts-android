package au.com.shiftyjelly.pocketcasts.views.helper

object ShowNotesHelper {

    fun convertTimesToLinks(html: String): String {
        return html.replace("(\\A|\\s|>|[^a-zsA-Z_0-9/])(\\d{0,2}:?\\d{1,2}:\\d{2})(<|\\s|\\z|[^a-zsA-Z_0-9\"])".toRegex(), "$1<a href=\"http://localhost/#playerJumpTo=$2\">$2</a>$3")
    }
}
