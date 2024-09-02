package au.com.shiftyjelly.pocketcasts.repositories.podcast

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptCuesInfo
import kotlinx.coroutines.flow.Flow

interface TranscriptsManager {
    suspend fun updateTranscripts(
        podcastUuid: String,
        episodeUuid: String,
        transcripts: List<Transcript>,
        loadTranscriptSource: LoadTranscriptSource,
        fromUpdateAlternativeTranscript: Boolean = false,
    )

    fun observerTranscriptForEpisode(episodeUuid: String): Flow<Transcript?>

    @OptIn(UnstableApi::class)
    suspend fun loadTranscriptCuesInfo(
        podcastUuid: String,
        transcript: Transcript,
        source: LoadTranscriptSource = LoadTranscriptSource.DEFAULT,
        forceRefresh: Boolean = false,
    ): List<TranscriptCuesInfo>
}

enum class LoadTranscriptSource {
    DOWNLOAD_EPISODE, // When transcript is downloaded as part of episode download
    DEFAULT,
}
