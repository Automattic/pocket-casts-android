package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import kotlinx.coroutines.flow.Flow

interface TranscriptManager {
    fun observeIsTranscriptAvailable(episodeUuid: String): Flow<Boolean>

    suspend fun loadTranscript(
        episodeUuid: String,
    ): Transcript?

    fun resetInvalidTranscripts(
        episodeUuid: String,
    )
}
