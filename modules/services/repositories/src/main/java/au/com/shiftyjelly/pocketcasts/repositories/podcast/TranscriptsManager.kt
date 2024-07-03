package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import kotlinx.coroutines.flow.Flow

interface TranscriptsManager {
    suspend fun updateTranscripts(
        episodeUuid: String,
        transcripts: List<Transcript>,
    )

    fun observerTranscriptForEpisode(episodeUuid: String): Flow<Transcript?>
}
