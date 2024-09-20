package au.com.shiftyjelly.pocketcasts.utils

import au.com.shiftyjelly.pocketcasts.utils.extensions.sha256
import java.net.URLEncoder

object Gravatar {

    fun getGravatarChangeAvatarUrl(email: String): String =
        "https://gravatar.com/profile?is_quick_editor=true&email=${URLEncoder.encode(email, "UTF-8")}&scope=avatars&is_app_origin=true"

    /**
     * The timestamp is used to force a refresh of the gravatar image. We use it as a cache buster.
     * When a user updates their avatar, it takes a few seconds to be updated everywhere, so if we reload the same previous URL,
     * we'll get the old avatar. We'll get the recently uploaded avatar using the cache buster (which can be any random string).
     */
    private var lastTimeStamp = System.currentTimeMillis()

    /**
     * d=404: display no image if there is not one associated with the requested email hash
     * s=400: size of the image
     * _=lastTimeStamp: cache buster
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
