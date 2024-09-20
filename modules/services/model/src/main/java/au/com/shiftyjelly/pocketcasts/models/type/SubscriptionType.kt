package au.com.shiftyjelly.pocketcasts.models.type

import com.squareup.moshi.JsonClass

/* pdeCcb-2fL-p2#comment-2074
The SubscriptionType field will always be Plus.
It is being replaced by SubscriptionTier with Plus and Patron tiers but currently not supported in production.
This can be removed once SubscriptionTier is supported.
*/
@JsonClass(generateAdapter = false)
enum class SubscriptionType(val label: String) {
    NONE("none"),
    PLUS("plus"),
    ;
    override fun toString() = label
}
