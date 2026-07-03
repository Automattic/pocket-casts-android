package au.com.shiftyjelly.pocketcasts.voicecontrol.feedback

import android.content.Context

class SpokenTemplateResolver(
    private val templates: Map<String, String>,
) {
    constructor(context: Context) : this(loadTemplatesFromResources(context))

    fun resolve(intentKey: String, params: Map<String, String>): String {
        val template = templates[intentKey] ?: return ""
        return params.entries.fold(template) { acc, (key, value) ->
            acc.replace("{$key}", value)
        }
    }

    private companion object {
        fun loadTemplatesFromResources(context: Context): Map<String, String> {
            val resources = context.resources
            val arrayId = resources.getIdentifier(
                "voice_spoken_templates",
                "array",
                context.packageName,
            )
            val array = if (arrayId != 0) resources.getStringArray(arrayId) else emptyArray()
            return array.associate { entry ->
                val parts = entry.split("|", limit = 2)
                parts[0] to parts[1]
            }
        }
    }
}
