package au.com.shiftyjelly.pocketcasts.voicecontrol.playback

import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.feedback.EarconId
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class PlaybackManagerChapterSink @Inject constructor(
    private val playbackManager: PlaybackManager,
) : VoiceChapterSink {
    override fun next(): VoiceResponse {
        playbackManager.skipToNextSelectedOrLastChapter()
        return VoiceResponse.Silent
    }

    override fun previous(): VoiceResponse {
        playbackManager.skipToPreviousSelectedOrLastChapter()
        return VoiceResponse.Silent
    }

    override fun byIndex(index: Int): VoiceResponse {
        playbackManager.skipToChapter(index)
        return VoiceResponse.Silent
    }

    override fun openLink(index: Int): VoiceResponse {
        playbackManager.skipToChapter(index)
        return VoiceResponse.Earcon(EarconId.SUCCESS)
    }

    override fun queryList(): VoiceResponse.Spoken {
        val state = playbackManager.playbackStateRelay.blockingFirst()
        val chapters = state.chapters
        return if (chapters.isEmpty()) {
            VoiceResponse.Spoken("No chapters")
        } else {
            val titles = chapters.take(5).mapIndexed { i, ch -> "${i + 1}. ${ch.title}" }
            val more = if (chapters.size > 5) ", and ${chapters.size - 5} more" else ""
            VoiceResponse.Spoken(titles.joinToString(", ") + more)
        }
    }

    override fun queryCurrent(): VoiceResponse.Spoken {
        val state = playbackManager.playbackStateRelay.blockingFirst()
        val chapter = state.chapters.getChapter(state.positionMs.milliseconds)
        return if (chapter != null) {
            VoiceResponse.Spoken(chapter.title)
        } else {
            VoiceResponse.Spoken("No chapter playing")
        }
    }

    override fun queryCount(): VoiceResponse.Spoken {
        val state = playbackManager.playbackStateRelay.blockingFirst()
        return VoiceResponse.Spoken("${state.chapters.size} chapters")
    }

    override fun queryNext(): VoiceResponse.Spoken {
        val state = playbackManager.playbackStateRelay.blockingFirst()
        val chapters = state.chapters
        val current = chapters.getChapter(state.positionMs.milliseconds)
        if (current == null) return VoiceResponse.Spoken("No next chapter")
        val idx = chapters.indexOf(current)
        return if (idx < chapters.size - 1) {
            VoiceResponse.Spoken(chapters[idx + 1].title)
        } else {
            VoiceResponse.Spoken("Last chapter")
        }
    }
}
