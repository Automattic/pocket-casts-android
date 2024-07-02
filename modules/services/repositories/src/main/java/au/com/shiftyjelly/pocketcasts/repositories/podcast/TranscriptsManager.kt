package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.to.Transcript

interface TranscriptsManager {
    suspend fun updateTranscripts(
        episodeUuid: String,
        transcripts: List<Transcript>,
    )
}
