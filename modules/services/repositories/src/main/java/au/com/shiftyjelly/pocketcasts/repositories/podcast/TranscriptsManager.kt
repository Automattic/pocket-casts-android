package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody

interface TranscriptsManager {
    suspend fun updateTranscripts(
        episodeUuid: String,
        transcripts: List<Transcript>,
    )

    fun observerTranscriptForEpisode(episodeUuid: String): Flow<Transcript?>

    suspend fun loadTranscript(url: String, source: LoadTranscriptSource = LoadTranscriptSource.DEFAULT): ResponseBody?
}
