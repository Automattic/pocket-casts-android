package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import kotlinx.coroutines.flow.Flow

interface TranscriptManager {
    fun observeIsTranscriptAvailable(episodeUuid: String): Flow<Boolean>

    suspend fun loadTranscript(
        episodeUuid: String,
    ): Transcript?

    suspend fun loadGeneratedTranscript(
        episodeUuid: String,
    ): Transcript.Text?

    suspend fun loadGeneratedChapters(
        episodeUuid: String,
    )

    fun resetInvalidTranscripts(
        episodeUuid: String,
    )
}
