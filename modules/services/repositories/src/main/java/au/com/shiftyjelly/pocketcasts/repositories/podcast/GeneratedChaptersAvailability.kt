package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class GeneratedChaptersAvailability @Inject constructor(
    private val settings: Settings,
) {
    val isAvailableFlow: Flow<Boolean> = combine(
        FeatureFlag.isEnabledFlow(Feature.GENERATED_CHAPTERS),
        settings.cachedSubscription.flow,
    ) { isFeatureEnabled, subscription ->
        isFeatureEnabled && subscription != null
    }.distinctUntilChanged()

    val isAllowedFlow: Flow<Boolean> = combine(
        isAvailableFlow,
        settings.showGeneratedChapters.flow,
    ) { isAvailable, showGeneratedChapters ->
        isAvailable && showGeneratedChapters
    }.distinctUntilChanged()

    fun isAllowed(): Boolean = FeatureFlag.isEnabled(Feature.GENERATED_CHAPTERS) &&
        settings.cachedSubscription.value != null &&
        settings.showGeneratedChapters.value
}
