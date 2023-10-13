package au.com.shiftyjelly.pocketcasts.utils.featureflag

import android.content.Context
import android.content.SharedPreferences
import au.com.shiftyjelly.pocketcasts.utils.featureflag.providers.PreferencesFeatureProvider
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private const val FEATURE_FLAG_KEY = "feature_flag_key"

@RunWith(MockitoJUnitRunner::class)
class FeatureFlagTest {
    @Mock
    private lateinit var feature: Feature

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    @Mock
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    @Mock
    private lateinit var defaultReleaseFeatureProvider: FeatureProvider

    @Mock
    private lateinit var remoteFeatureProvider: RemoteFeatureProvider

    private lateinit var preferencesFeatureProvider: ModifiableFeatureProvider

    @Before
    fun setup() {
        initPreferenceFeatureFlagProvider()
    }

    @Test
    fun `given feature flag not set in preferences, then return default value`() {
        FeatureFlag.initialize(listOf(preferencesFeatureProvider))

        assertTrue(FeatureFlag.isEnabled(feature) == feature.defaultValue)
    }

    @Test
    fun `given modifiable provider added, when feature flag value changed, then value is saved in preferences`() {
        FeatureFlag.initialize(listOf(preferencesFeatureProvider))

        val result = FeatureFlag.setEnabled(feature, true)

        assertTrue(result)
    }

    @Test
    fun `given modifiable provider added, when feature flag set to true, then return true for feature flag`() {
        initPreferenceFeatureFlagProvider(defaultFeatureFlagValue = false)
        whenever(sharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(true)
        FeatureFlag.initialize(listOf(preferencesFeatureProvider))

        FeatureFlag.setEnabled(feature, true)

        assertTrue(FeatureFlag.isEnabled(feature))
    }

    @Test
    fun `given modifiable provider added, when feature flag set to false, then return false for feature flag`() {
        initPreferenceFeatureFlagProvider(defaultFeatureFlagValue = true)
        whenever(sharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(false)
        FeatureFlag.initialize(listOf(preferencesFeatureProvider))

        FeatureFlag.setEnabled(feature, false)

        assertTrue(!FeatureFlag.isEnabled(feature))
    }

    @Test
    fun `given non modifiable provider added, when feature flag value changed, then value is not saved in preferences`() {
        FeatureFlag.initialize(listOf(defaultReleaseFeatureProvider))

        val result = FeatureFlag.setEnabled(feature, true)

        assertFalse(result)
    }

    @Test
    fun `given remote provider added, when feature flags refresh invoked, then feature flags are refreshed`() {
        FeatureFlag.initialize(listOf(remoteFeatureProvider))

        FeatureFlag.refresh()

        verify(remoteFeatureProvider).refresh()
    }

    private fun initPreferenceFeatureFlagProvider(
        defaultFeatureFlagValue: Boolean = false,
    ) {
        whenever(feature.key).thenReturn(FEATURE_FLAG_KEY)
        whenever(feature.defaultValue).thenReturn(defaultFeatureFlagValue)
        whenever(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
        whenever(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.putBoolean(anyString(), anyBoolean()))
            .thenReturn(sharedPreferencesEditor)

        preferencesFeatureProvider = PreferencesFeatureProvider(context)
    }

    @After
    fun tearDown() {
        FeatureFlag.clearProviders()
    }
}
