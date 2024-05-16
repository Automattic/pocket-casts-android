package au.com.shiftyjelly.pocketcasts.utils

import au.com.shiftyjelly.pocketcasts.utils.extensions.sha256

object Gravatar {

    /**
     * d=404: display no image if there is not one associated with the requested email hash
     * s=400: size of the image
     * https://en.gravatar.com/site/implement/images/
     */
    fun getUrl(email: String): String? =
        email.sha256()?.let { sha256Email ->
            "https://www.gravatar.com/avatar/$sha256Email?d=404&s=400"
        }
}
