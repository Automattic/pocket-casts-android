package au.com.shiftyjelly.pocketcasts.utils.featureflag

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkFeatureControl @Inject constructor() {
    fun isAvailable(userTier: UserTier): Boolean {
        return when (userTier) {
            UserTier.Plus, UserTier.Patron -> true
            UserTier.Free -> false
        }
    }
}
