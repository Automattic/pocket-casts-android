package au.com.shiftyjelly.pocketcasts.featureflag.providers

import android.content.Context
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.featureflag.ModifiableFeatureFlagProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Used to override values for feature flags at runtime in debug builds.
 * See StoreFeatureFlagProvider to set feature flag values for release builds.
 */
@Singleton
class PreferencesFeatureFlagProvider @Inject constructor(
    @ApplicationContext context: Context,
) : ModifiableFeatureFlagProvider {
    private val preferences = context.featureFlagsSharedPrefs()

    override fun isEnabled(featureFlag: FeatureFlag) =
        preferences.getBoolean(featureFlag.key, featureFlag.defaultValue)

    override fun setEnabled(featureFlag: FeatureFlag, enabled: Boolean) =
        preferences.edit().putBoolean(featureFlag.key, enabled).apply()

    private fun Context.featureFlagsSharedPrefs() =
        this.getSharedPreferences("POCKETCASTS_FEATURE_FLAGS", Context.MODE_PRIVATE)
}
