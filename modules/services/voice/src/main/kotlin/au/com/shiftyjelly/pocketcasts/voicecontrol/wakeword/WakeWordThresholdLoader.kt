package au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword

import android.content.res.AssetManager
import org.json.JSONObject

/** Loads the validation-selected threshold; missing or legacy fields fail closed. */
internal object WakeWordThresholdLoader {
    private const val EVAL_ASSET = "oww/auris_eval.json"

    fun load(assets: AssetManager): Result<Float> = runCatching {
        val manifest = assets.open(EVAL_ASSET).use { JSONObject(String(it.readBytes())) }
        require(manifest.has("deployment_threshold")) {
            "auris_eval.json has no deployment_threshold"
        }
        val threshold = manifest.getDouble("deployment_threshold").toFloat()
        require(threshold.isFinite() && threshold > 0f && threshold <= 1f) {
            "deployment_threshold must be finite and in (0, 1]"
        }
        threshold
    }
}
