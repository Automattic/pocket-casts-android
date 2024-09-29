package au.com.shiftyjelly.pocketcasts.analytics.experiments

import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Variation.Control
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Variation.Treatment
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.android.experimentation.Experiment
import com.automattic.android.experimentation.VariationsRepository
import com.automattic.android.experimentation.domain.Variation
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.`when`
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Experiment as ExperimentModel

class ExperimentProviderTest {

    private lateinit var accountStatusInfo: AccountStatusInfo
    private lateinit var repository: VariationsRepository
    private lateinit var experimentProvider: ExperimentProvider

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Before
    fun setUp() {
        accountStatusInfo = mock(AccountStatusInfo::class.java)
        repository = mock(VariationsRepository::class.java)
        experimentProvider = ExperimentProvider(accountStatusInfo, repository)
    }

    @Test
    fun `initialize should call repository initialize with correct uuid`() {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, true)

        val uuid = "test-uuid"
        `when`(accountStatusInfo.getUuid()).thenReturn(uuid)

        experimentProvider.initialize()

        verify(repository).initialize(uuid)
        verify(accountStatusInfo).getUuid()
    }

    @Test
    fun `initialize should generate uuid if accountStatusInfo getUuid is null`() {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, true)

        `when`(accountStatusInfo.getUuid()).thenReturn(null)

        experimentProvider.initialize()

        verify(repository).initialize(anyString(), eq(null))
        verify(accountStatusInfo).getUuid()
    }

    @Test
    fun `clear should call repository clear`() {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, true)

        experimentProvider.clear()

        verify(repository).clear()
    }

    @Test
    fun `getVariation should return Control when repository returns Control`() {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, true)

        val experiment = ExperimentModel.PaywallAATest
        `when`(repository.getVariation(Experiment(experiment.identifier))).thenReturn(Variation.Control)

        val variation = experimentProvider.getVariation(experiment)

        assertEquals(Control, variation)
    }

    @Test
    fun `getVariation should return Treatment when repository returns Treatment`() {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, true)

        val experiment = ExperimentModel.PaywallAATest
        `when`(repository.getVariation(Experiment(experiment.identifier))).thenReturn(Variation.Treatment(experiment.identifier))

        val variation = experimentProvider.getVariation(experiment)

        assertTrue(variation is Treatment)
        assertEquals(ExperimentModel.PaywallAATest.identifier, (variation as Treatment).name)
    }

    @Test
    fun `getVariation should return null when repository returns null`() {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, true)

        val experiment = ExperimentModel.PaywallAATest
        `when`(repository.getVariation(Experiment(experiment.identifier))).thenReturn(null)

        val variation = experimentProvider.getVariation(experiment)

        assertNull(variation)
    }

    @Test
    fun `should not initialize when feature flag is disabled`() {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, false)

        `when`(accountStatusInfo.getUuid()).thenReturn(null)

        experimentProvider.initialize()

        verify(repository, never()).initialize(anyString(), eq(null))
    }

    @Test
    fun `should not clear when feature flag is disabled`() {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, false)

        experimentProvider.clear()

        verify(repository, never()).clear()
    }

    @Test
    fun `should return null variation when feature flag is disabled`() {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, false)

        val experiment = ExperimentModel.PaywallAATest
        `when`(repository.getVariation(Experiment(experiment.identifier))).thenReturn(Variation.Control)

        val variation = experimentProvider.getVariation(experiment)

        assertNull(variation)
    }
}
