package au.com.shiftyjelly.pocketcasts.settings.notifications.data

import android.os.Build
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import javax.inject.Inject

internal class NotificationFeaturesProvider(
    val hasNotificationChannels: Boolean,
    val isRevampFeatureEnabled: Boolean,
) {
    @Inject
    constructor() : this(
        hasNotificationChannels = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O,
        isRevampFeatureEnabled = FeatureFlag.isEnabled(
            Feature.NOTIFICATIONS_REVAMP,
        ),
    )
}
