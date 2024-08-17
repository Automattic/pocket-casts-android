package au.com.shiftyjelly.pocketcasts.analytics.experiments

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ExperimentProviderTest {

    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var experimentProvider: ExperimentProvider

    @Before
    fun setUp() {
        firebaseRemoteConfig = mock<FirebaseRemoteConfig> {}
        experimentProvider = ExperimentProvider(firebaseRemoteConfig)
    }

    @Test
    fun getVariationControl() {
        val experiment = Experiment.PaywallAATest
        val variationName = "control"

        whenever(firebaseRemoteConfig.getString(experiment.identifier)).thenReturn(variationName)

        val variation = experimentProvider.getVariation(experiment)
        assertEquals(Variation.Control, variation)
    }

    @Test
    fun getVariationTreatment() {
        val experiment = Experiment.PaywallAATest
        val variationName = "treatment"

        whenever(firebaseRemoteConfig.getString(experiment.identifier)).thenReturn(variationName)

        val variation = experimentProvider.getVariation(experiment)
        assertEquals(Variation.Treatment, variation)
    }
}
