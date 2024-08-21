package au.com.shiftyjelly.pocketcasts.analytics.experiments

import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.get
import javax.inject.Inject

class ExperimentProvider @Inject constructor(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
) {

    fun getVariation(experiment: Experiment): Variation {
        val name = firebaseRemoteConfig.getString(experiment.identifier)
        val variation = Variation.fromName(name)
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Experiment $experiment, variation ${if (name.isBlank()) "not set" else variation.toString()}")
        return variation
    }
}
