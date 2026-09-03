package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolCallMapper @Inject constructor() {

    fun map(call: ToolCall): VoiceIntent? {
        if (call.name == "no_match") return null
        return when (call.name) {
            "playback" -> mapPlayback(call.action, call)
            "effects" -> mapEffects(call.action, call)
            "volume" -> mapVolume(call.action, call)
            "sleep" -> mapSleep(call.action, call)
            "chapter" -> mapChapter(call.action, call)
            "bookmark" -> mapBookmark(call.action, call)
            "queue" -> mapQueue(call.action, call)
            "playback_query" -> mapPlaybackQuery(call.action)
            "stats_query" -> mapStatsQuery(call.action, call)
            "cloud_route" -> mapCloudRoute(call.action, call)
            else -> null
        }
    }

    private fun mapPlayback(action: String, call: ToolCall): VoiceIntent.Playback? = when (action) {
        "pause" -> VoiceIntent.Playback.Pause

        "resume" -> VoiceIntent.Playback.Resume

        "seek_relative" -> {
            // Last-resort magnitude when SlotRepair did not fill a signed default.
            // Prefer SlotRepair's utterance-aware ±30s; this is not the user skip setting.
            val seconds = call.intParam("delta_seconds") ?: 30
            VoiceIntent.Playback.SeekRelative(seconds * 1000)
        }

        "seek_to" -> {
            val seconds = call.intParam("position_seconds") ?: return null
            VoiceIntent.Playback.SeekAbsolute(seconds * 1000)
        }

        "next_episode" -> VoiceIntent.Playback.NextEpisode

        else -> null
    }

    private fun mapEffects(action: String, call: ToolCall): VoiceIntent.Effects? = when (action) {
        "set_speed" -> {
            val speed = call.doubleParam("speed") ?: return null
            VoiceIntent.Effects.SetSpeed(speed)
        }

        "adjust_speed" -> {
            val delta = call.doubleParam("delta") ?: return null
            VoiceIntent.Effects.AdjustSpeed(delta)
        }

        "set_trim_mode" -> {
            val mode = call.stringParam("mode") ?: return null
            VoiceIntent.Effects.SetTrimMode(mode)
        }

        "set_volume_boost" -> {
            val enabled = call.boolParam("enabled") ?: return null
            VoiceIntent.Effects.SetVolumeBoost(enabled)
        }

        "query_effects" -> VoiceIntent.Effects.QueryEffects

        else -> null
    }

    private fun mapVolume(action: String, call: ToolCall): VoiceIntent.Volume? = when (action) {
        "set_volume" -> {
            val volume = call.intParam("volume") ?: return null
            VoiceIntent.Volume.SetVolume(volume)
        }

        "adjust_volume" -> {
            val delta = call.intParam("delta") ?: return null
            VoiceIntent.Volume.AdjustVolume(delta)
        }

        "query" -> VoiceIntent.Volume.Query

        else -> null
    }

    private fun mapSleep(action: String, call: ToolCall): VoiceIntent.Sleep? = when (action) {
        "set" -> {
            val minutes = call.intParam("minutes") ?: return null
            VoiceIntent.Sleep.Set(minutes)
        }

        "end_of_episode" -> VoiceIntent.Sleep.EndOfEpisode

        "end_of_chapter" -> VoiceIntent.Sleep.EndOfChapter

        "add_time" -> {
            val minutes = call.intParam("minutes") ?: return null
            VoiceIntent.Sleep.AddTime(minutes)
        }

        "cancel" -> VoiceIntent.Sleep.Cancel

        "query" -> VoiceIntent.Sleep.QuerySleep

        else -> null
    }

    private fun mapChapter(action: String, call: ToolCall): VoiceIntent.Chapter? = when (action) {
        "next" -> VoiceIntent.Chapter.NextChapter

        "previous" -> VoiceIntent.Chapter.PreviousChapter

        "by_index" -> {
            val index = call.intParam("index") ?: return null
            VoiceIntent.Chapter.ByIndex(index)
        }

        "by_title" -> {
            val query = call.stringParam("query") ?: return null
            VoiceIntent.Chapter.ByTitle(query)
        }

        "open_link" -> {
            call.intParam("index")?.let { return VoiceIntent.Chapter.OpenLink(it) }
            // Schema allows query when the speaker names the chapter; route as by_title.
            val query = call.stringParam("query") ?: return null
            VoiceIntent.Chapter.ByTitle(query)
        }

        "query_list" -> VoiceIntent.Chapter.QueryList

        "query_current" -> VoiceIntent.Chapter.QueryCurrent

        "query_count" -> VoiceIntent.Chapter.QueryCount

        "query_next" -> VoiceIntent.Chapter.QueryNext

        else -> null
    }

    private fun mapBookmark(action: String, call: ToolCall): VoiceIntent.Bookmark? = when (action) {
        "add" -> VoiceIntent.Bookmark.Add(call.stringParam("title"))

        "rename" -> {
            val ref = call.stringParam("ref") ?: return null
            val title = call.stringParam("title") ?: return null
            VoiceIntent.Bookmark.Rename(ref, title)
        }

        "play" -> {
            val ref = call.stringParam("ref") ?: return null
            VoiceIntent.Bookmark.Play(ref)
        }

        "delete" -> {
            val ref = call.stringParam("ref") ?: return null
            VoiceIntent.Bookmark.Delete(ref)
        }

        "delete_all" -> VoiceIntent.Bookmark.DeleteAll

        "query_list" -> VoiceIntent.Bookmark.QueryBookmarkList

        "query_count" -> VoiceIntent.Bookmark.QueryBookmarkCount

        "query_nearby" -> VoiceIntent.Bookmark.QueryNearby

        else -> null
    }

    private fun mapQueue(action: String, call: ToolCall): VoiceIntent.Queue? = when (action) {
        "add_top" -> VoiceIntent.Queue.AddTop(call.stringParam("episode"))

        "add_bottom" -> VoiceIntent.Queue.AddBottom(call.stringParam("episode"))

        "remove" -> {
            val episode = call.stringParam("episode") ?: return null
            VoiceIntent.Queue.Remove(episode)
        }

        "move_to_top" -> {
            val episode = call.stringParam("episode") ?: return null
            VoiceIntent.Queue.MoveToTop(episode)
        }

        "move_to_bottom" -> {
            val episode = call.stringParam("episode") ?: return null
            VoiceIntent.Queue.MoveToBottom(episode)
        }

        "clear" -> VoiceIntent.Queue.Clear

        "remove_by_podcast" -> {
            val podcast = call.stringParam("podcast") ?: return null
            VoiceIntent.Queue.RemoveByPodcast(podcast)
        }

        "sort" -> {
            val order = call.stringParam("sort_order") ?: return null
            VoiceIntent.Queue.Sort(order)
        }

        "query_contents" -> VoiceIntent.Queue.QueryContents

        "query_next" -> VoiceIntent.Queue.QueryQueueNext

        "query_length" -> VoiceIntent.Queue.QueryLength

        "query_is_queued" -> {
            val episode = call.stringParam("episode") ?: return null
            VoiceIntent.Queue.QueryIsQueued(episode)
        }

        else -> null
    }

    private fun mapPlaybackQuery(action: String): VoiceIntent.PlaybackQuery? = when (action) {
        "whats_playing" -> VoiceIntent.PlaybackQuery.WhatsPlaying
        "position" -> VoiceIntent.PlaybackQuery.Position
        "time_remaining" -> VoiceIntent.PlaybackQuery.TimeRemaining
        "current_podcast" -> VoiceIntent.PlaybackQuery.CurrentPodcast
        "episode_duration" -> VoiceIntent.PlaybackQuery.EpisodeDuration
        "publish_date" -> VoiceIntent.PlaybackQuery.PublishDate
        "episode_description" -> VoiceIntent.PlaybackQuery.EpisodeDescription
        "download_status" -> VoiceIntent.PlaybackQuery.DownloadStatus
        "episode_title" -> VoiceIntent.PlaybackQuery.EpisodeTitle
        else -> null
    }

    private fun mapStatsQuery(action: String, call: ToolCall): VoiceIntent.StatsQuery? = when (action) {
        "listening_time" -> VoiceIntent.StatsQuery.ListeningTime(call.stringParam("period"))
        "top_podcasts" -> VoiceIntent.StatsQuery.TopPodcasts(call.stringParam("period"))
        "episodes_finished" -> VoiceIntent.StatsQuery.EpisodesFinished(call.stringParam("period"))
        "listening_streak" -> VoiceIntent.StatsQuery.ListeningStreak
        "subscription_count" -> VoiceIntent.StatsQuery.SubscriptionCount
        "unplayed_total" -> VoiceIntent.StatsQuery.UnplayedTotal
        "download_stats" -> VoiceIntent.StatsQuery.DownloadStats
        "queue_total" -> VoiceIntent.StatsQuery.QueueTotal
        "new_episodes" -> VoiceIntent.StatsQuery.NewEpisodes(call.stringParam("timeframe"))
        "time_since_last_listen" -> VoiceIntent.StatsQuery.TimeSinceLastListen
        else -> null
    }

    private fun mapCloudRoute(action: String, call: ToolCall): VoiceIntent.CloudRoute? {
        if (action != "route") return null
        val request = call.stringParam("request") ?: return null
        val tier = when (call.stringParam("tier")?.lowercase()) {
            "free" -> VoiceIntent.CloudTier.Free
            "premium" -> VoiceIntent.CloudTier.Premium
            else -> VoiceIntent.CloudTier.Unknown
        }
        return VoiceIntent.CloudRoute(request, tier)
    }
}
