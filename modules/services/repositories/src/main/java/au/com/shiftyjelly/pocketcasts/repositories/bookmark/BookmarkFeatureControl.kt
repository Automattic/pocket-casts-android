package au.com.shiftyjelly.pocketcasts.repositories.bookmark

import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkFeatureControl @Inject constructor() {
    fun isAvailable(userTier: UserTier): Boolean =
        userTier == UserTier.Patron || userTier == UserTier.Plus
}
