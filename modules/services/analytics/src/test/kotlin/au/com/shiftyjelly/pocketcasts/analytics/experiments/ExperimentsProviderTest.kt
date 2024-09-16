package au.com.shiftyjelly.pocketcasts.analytics.experiments

import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Variation.Control
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Variation.Treatment
import com.automattic.android.experimentation.Experiment
import com.automattic.android.experimentation.VariationsRepository
import com.automattic.android.experimentation.domain.Variation
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Experiment as ExperimentModel

class ExperimentsProviderTest {

    private lateinit var accountStatusInfo: AccountStatusInfo
    private lateinit var repository: VariationsRepository
    private lateinit var experimentsProvider: ExperimentsProvider

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        accountStatusInfo = mock(AccountStatusInfo::class.java)
        repository = mock(VariationsRepository::class.java)
        experimentsProvider = ExperimentsProvider(accountStatusInfo, repository)
    }

    @Test
    fun `initialize should call repository initialize with correct uuid`() {
        val uuid = "test-uuid"
        `when`(accountStatusInfo.getUuid()).thenReturn(uuid)

        experimentsProvider.initialize()

        verify(repository).initialize(uuid)
        verify(accountStatusInfo).getUuid()
    }

    @Test
    fun `initialize should generate uuid if accountStatusInfo getUuid is null`() {
        `when`(accountStatusInfo.getUuid()).thenReturn(null)

        experimentsProvider.initialize()

        verify(repository).initialize(anyString(), eq(null))
        verify(accountStatusInfo).getUuid()
    }

    @Test
    fun `clear should call repository clear`() {
        experimentsProvider.clear()

        verify(repository).clear()
    }

    @Test
    fun `getVariation should return Control when repository returns Control`() {
        val experiment = ExperimentModel.PaywallAATest
        `when`(repository.getVariation(Experiment(experiment.identifier))).thenReturn(Variation.Control)

        val variation = experimentsProvider.getVariation(experiment)

        assertEquals(Control, variation)
    }

    @Test
    fun `getVariation should return Treatment when repository returns Treatment`() {
        val experiment = ExperimentModel.PaywallAATest
        `when`(repository.getVariation(Experiment(experiment.identifier))).thenReturn(Variation.Treatment(experiment.identifier))

        val variation = experimentsProvider.getVariation(experiment)

        assertTrue(variation is Treatment)
        assertEquals(ExperimentModel.PaywallAATest.identifier, (variation as Treatment).name)
    }

    @Test
    fun `getVariation should return null when repository returns null`() {
        val experiment = ExperimentModel.PaywallAATest
        `when`(repository.getVariation(Experiment(experiment.identifier))).thenReturn(null)

        val variation = experimentsProvider.getVariation(experiment)

        assertNull(variation)
    }
}
