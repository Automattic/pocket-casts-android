package au.com.shiftyjelly.pocketcasts.voicecontrol.playback

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.feedback.EarconId
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceResponse
import com.automattic.eventhorizon.BookmarkSourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackManagerBookmarkSink @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val bookmarkManager: BookmarkManager,
) : VoiceBookmarkSink {
    override suspend fun add(title: String?): VoiceResponse {
        val episode = playbackManager.getCurrentEpisode() ?: return VoiceResponse.Silent
        val positionMs = playbackManager.getCurrentTimeMs(episode)
        val timeSecs = positionMs / 1000
        bookmarkManager.sourceView = SourceView.VOICE_COMMANDS
        bookmarkManager.add(
            episode = episode,
            timeSecs = timeSecs,
            title = title ?: "Bookmark at ${timeSecs / 60}:${"%02d".format(timeSecs % 60)}",
            creationSource = BookmarkSourceType.Headphones,
        )
        return VoiceResponse.Earcon(EarconId.SUCCESS)
    }

    override fun rename(ref: String, title: String): VoiceResponse {
        // TODO: resolve ref to bookmark UUID, then rename
        return VoiceResponse.Earcon(EarconId.SUCCESS)
    }

    override fun play(ref: String): VoiceResponse {
        // TODO: resolve ref to bookmark, then seek to position
        return VoiceResponse.Silent
    }

    override fun delete(ref: String): VoiceResponse {
        // TODO: resolve ref to bookmark UUID, then delete
        return VoiceResponse.Earcon(EarconId.SUCCESS)
    }

    override fun deleteAll(): VoiceResponse {
        // TODO: delete all bookmarks for current episode
        return VoiceResponse.Earcon(EarconId.CONFIRM_REQUIRED)
    }

    override fun queryList(): VoiceResponse.Spoken {
        // TODO: list bookmarks for current episode
        return VoiceResponse.Spoken("No bookmarks")
    }

    override fun queryCount(): VoiceResponse.Spoken {
        // TODO: count bookmarks for current episode
        return VoiceResponse.Spoken("0 bookmarks")
    }

    override fun queryNearby(): VoiceResponse.Spoken {
        // TODO: find nearest bookmark to current position
        return VoiceResponse.Spoken("No nearby bookmarks")
    }
}
