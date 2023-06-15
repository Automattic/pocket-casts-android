package au.com.shiftyjelly.pocketcasts.featureflag

import android.content.Context
import android.content.SharedPreferences
import au.com.shiftyjelly.pocketcasts.featureflag.providers.PreferencesFeatureFlagProvider
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
class FeatureFlagManagerTest {
    @Mock
    private lateinit var featureFlag: FeatureFlag

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    @Mock
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    @Mock
    private lateinit var storeFeatureFlagProvider: FeatureFlagProvider

    @Mock
    private lateinit var remoteFeatureFlagProvider: RemoteFeatureFlagProvider

    private lateinit var preferencesFeatureFlagProvider: ModifiableFeatureFlagProvider

    @Before
    fun setup() {
        initPreferenceFeatureFlagProvider()
    }

    @Test
    fun `given feature flag not set in preferences, then return default value`() {
        FeatureFlagManager.initialize(listOf(preferencesFeatureFlagProvider))

        assertTrue(FeatureFlagManager.isFeatureEnabled(featureFlag) == featureFlag.defaultValue)
    }

    @Test
    fun `given modifiable provider added, when feature flag value changed, then value is saved in preferences`() {
        FeatureFlagManager.initialize(listOf(preferencesFeatureFlagProvider))

        val result = FeatureFlagManager.setFeatureEnabled(featureFlag, true)

        assertTrue(result)
    }

    @Test
    fun `given modifiable provider added, when feature flag set to true, then return true for feature flag`() {
        initPreferenceFeatureFlagProvider(defaultFeatureFlagValue = false)
        whenever(sharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(true)
        FeatureFlagManager.initialize(listOf(preferencesFeatureFlagProvider))

        FeatureFlagManager.setFeatureEnabled(featureFlag, true)

        assertTrue(FeatureFlagManager.isFeatureEnabled(featureFlag))
    }

    @Test
    fun `given modifiable provider added, when feature flag set to false, then return false for feature flag`() {
        initPreferenceFeatureFlagProvider(defaultFeatureFlagValue = true)
        whenever(sharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(false)
        FeatureFlagManager.initialize(listOf(preferencesFeatureFlagProvider))

        FeatureFlagManager.setFeatureEnabled(featureFlag, false)

        assertTrue(!FeatureFlagManager.isFeatureEnabled(featureFlag))
    }

    @Test
    fun `given non modifiable provider added, when feature flag value changed, then value is not saved in preferences`() {
        FeatureFlagManager.initialize(listOf(storeFeatureFlagProvider))

        val result = FeatureFlagManager.setFeatureEnabled(featureFlag, true)

        assertFalse(result)
    }

    @Test
    fun `given remote provider added, when feature flags refresh invoked, then feature flags are refreshed`() {
        FeatureFlagManager.initialize(listOf(remoteFeatureFlagProvider))

        FeatureFlagManager.refreshFeatureFlags()

        verify(remoteFeatureFlagProvider).refreshFeatureFlags()
    }

    private fun initPreferenceFeatureFlagProvider(
        defaultFeatureFlagValue: Boolean = false,
    ) {
        whenever(featureFlag.key).thenReturn(FEATURE_FLAG_KEY)
        whenever(featureFlag.defaultValue).thenReturn(defaultFeatureFlagValue)
        whenever(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
        whenever(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.putBoolean(anyString(), anyBoolean()))
            .thenReturn(sharedPreferencesEditor)

        preferencesFeatureFlagProvider = PreferencesFeatureFlagProvider(context)
    }

    @After
    fun tearDown() {
        FeatureFlagManager.clearFeatureFlagProviders()
    }
}
