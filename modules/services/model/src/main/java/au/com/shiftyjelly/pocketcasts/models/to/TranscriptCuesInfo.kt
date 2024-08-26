@file:UnstableApi

package au.com.shiftyjelly.pocketcasts.models.to

import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming

data class TranscriptCuesInfo(
    val cuesWithTiming: CuesWithTiming,
    val cuesAdditionalInfo: CuesAdditionalInfo? = null,
)

data class CuesAdditionalInfo(
    val speaker: String?,
)
