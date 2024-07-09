package au.com.shiftyjelly.pocketcasts.utils

import au.com.shiftyjelly.pocketcasts.utils.extensions.sha256
import java.net.URLEncoder

object Gravatar {

    fun getGravatarChangeAvatarUrl(email: String): String =
        "https://gravatar.com/profile?is_quick_editor=true&email=${URLEncoder.encode(email, "UTF-8")}&scope=avatars"

    private var lastTimeStamp = System.currentTimeMillis()

    /**
     * d=404: display no image if there is not one associated with the requested email hash
     * s=400: size of the image
     * https://en.gravatar.com/site/implement/images/
     */
    fun getUrl(email: String): String? =
        email.sha256()?.let { sha256Email ->
            "https://www.gravatar.com/avatar/$sha256Email?d=404&s=400&_=$lastTimeStamp"
        }

    fun refreshGravatarTimestamp() {
        lastTimeStamp = System.currentTimeMillis()
    }
}
