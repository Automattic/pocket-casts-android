package au.com.shiftyjelly.pocketcasts.analytics.experiments

import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
import au.com.shiftyjelly.pocketcasts.analytics.UserIds
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Variation.Control
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Variation.Treatment
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.android.experimentation.Experiment
import com.automattic.android.experimentation.VariationsRepository
import com.automattic.android.experimentation.domain.Variation
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ExperimentProviderTest {
    private lateinit var repository: VariationsRepository
    private lateinit var experimentProvider: ExperimentProvider
    private lateinit var accountStatusInfo: AccountStatusInfo

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Before
    fun setUp() {
        repository = mock(VariationsRepository::class.java)
        accountStatusInfo = mock(AccountStatusInfo::class.java)
        experimentProvider = ExperimentProvider(repository, accountStatusInfo, coroutineRule.testDispatcher)
    }

    @Test
    fun `should initialize with uuid`() {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, true)

        val uuid = "test-uuid"
        whenever(accountStatusInfo.getUserIds()).thenReturn(UserIds(accountId = uuid, anonId = "invalid"))

        experimentProvider.initialize()

        verify(repository).initialize(uuid, null)
        verify(accountStatusInfo).getUserIds()
    }

    @Test
    fun `should initialize with anonID`() {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, true)

        val uuid = "test-anonID"

        whenever(accountStatusInfo.getUserIds()).thenReturn(UserIds(accountId = null, anonId = uuid))

        experimentProvider.initialize()

        verify(repository).initialize(uuid, null)
        verify(accountStatusInfo).getUserIds()
    }

    @Test
    fun `getVariation should return Control when repository returns Control`() {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, true)

        val experiment = DummyExperiment.DUMMY_EXPERIMENT
        whenever(repository.getVariation(Experiment(experiment.identifier))).thenReturn(Variation.Control)

        val variation = experimentProvider.getVariation(experiment)

        assertEquals(Control, variation)
    }

    @Test
    fun `getVariation should return Treatment when repository returns Treatment`() {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, true)

        val experiment = DummyExperiment.DUMMY_EXPERIMENT
        whenever(repository.getVariation(Experiment(experiment.identifier))).thenReturn(Variation.Treatment(experiment.identifier))

        val variation = experimentProvider.getVariation(experiment)

        assertTrue(variation is Treatment)
        assertEquals(DummyExperiment.DUMMY_EXPERIMENT.identifier, (variation as Treatment).name)
    }

    @Test
    fun `should not initialize when feature flag is disabled`() {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, false)

        experimentProvider.initialize()

        verify(repository, never()).initialize(anyString(), eq(null))
    }

    @Test
    fun `should return null variation when feature flag is disabled`() {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, false)

        val experiment = DummyExperiment.DUMMY_EXPERIMENT
        whenever(repository.getVariation(Experiment(experiment.identifier))).thenReturn(Variation.Control)

        val variation = experimentProvider.getVariation(experiment)

        assertNull(variation)
    }

    @Test
    fun `refreshExperiments should refresh experiments`() = runTest {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, true)

        val uuid = "test-uuid"
        whenever(accountStatusInfo.getUserIds()).thenReturn(UserIds(accountId = uuid, anonId = "invalid"))

        experimentProvider.refreshExperiments()

        verify(repository).clear()
        verify(repository).initialize(uuid, null)
    }

    @Test
    fun `refreshExperiments should not refresh experiments when feature flag is disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.EXPLAT_EXPERIMENT, false)

        experimentProvider.refreshExperiments()

        verify(repository, never()).clear()
        verify(repository, never()).initialize(anyString(), eq(null))
    }
}

enum class DummyExperiment(override val identifier: String) : ExperimentType {
    DUMMY_EXPERIMENT("dummy_experiment"),
}
