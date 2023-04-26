package au.com.shiftyjelly.pocketcasts.utils

import au.com.shiftyjelly.pocketcasts.utils.extensions.md5Hex

object Gravatar {

    /**
     * d=404: display no image if there is not one associated with the requested email hash
     * https://en.gravatar.com/site/implement/images/
     */
    fun getUrl(email: String): String? =
        email.md5Hex()?.let { md5Email ->
            "https://www.gravatar.com/avatar/$md5Email?d=404"
        }
}
