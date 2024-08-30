package au.com.shiftyjelly.pocketcasts.crashlogging

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.android.tracks.crashlogging.CrashLogging

class FilteringCrashLogging(private val crashLogging: CrashLogging) : CrashLogging by crashLogging {
    override fun sendReport(exception: Throwable?, tags: Map<String, String>, message: String?) {
        if (exception != null && ExceptionsFilter.shouldIgnoreExceptions(exception)) {
            return
        }
        val tagsWithFeatures = buildMap {
            putAll(tags)
            runCatching {
                val featuresMap = Feature.entries.associate { feature ->
                    "feature_${feature.key}" to FeatureFlag.isEnabled(feature).toString()
                }
                putAll(featuresMap)
            }
        }
        crashLogging.sendReport(exception, tagsWithFeatures, message)
    }

    override fun recordException(exception: Throwable, category: String?) {
        if (ExceptionsFilter.shouldIgnoreExceptions(exception)) {
            return
        }
        crashLogging.recordException(exception, category)
    }
}
