package au.com.shiftyjelly.pocketcasts.models.entity

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.text.format.DateUtils
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.models.to.Bundle
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import java.io.Serializable
import java.net.MalformedURLException
import java.net.URL
import java.util.Date
import java.util.UUID
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Entity(tableName = "podcasts")
data class Podcast(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "uuid") var uuid: String,
    @ColumnInfo(name = "added_date") var addedDate: Date? = null,
    @ColumnInfo(name = "thumbnail_url") var thumbnailUrl: String? = null,
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "podcast_url") var podcastUrl: String? = null,
    @ColumnInfo(name = "podcast_description") var podcastDescription: String = "",
    @ColumnInfo(name = "podcast_category") var podcastCategory: String = "",
    @ColumnInfo(name = "podcast_language") var podcastLanguage: String = "",
    @ColumnInfo(name = "media_type") var mediaType: String? = null,
    @ColumnInfo(name = "latest_episode_uuid") var latestEpisodeUuid: String? = null,
    @ColumnInfo(name = "author") var author: String = "",
    @ColumnInfo(name = "sort_order") var sortPosition: Int = 0,
    @ColumnInfo(name = "episodes_sort_order") var episodesSortType: EpisodesSortType = EpisodesSortType.EPISODES_SORT_BY_DATE_DESC,
    @ColumnInfo(name = "episodes_sort_order_modified") var episodesSortTypeModified: Date? = null,
    @ColumnInfo(name = "latest_episode_date") var latestEpisodeDate: Date? = null,
    // TODO remove this later in a separate PR as it is no longer used
    @ColumnInfo(name = "episodes_to_keep") var episodesToKeep: Int = 0,
    @ColumnInfo(name = "override_global_settings") var overrideGlobalSettings: Boolean = false,
    @ColumnInfo(name = "override_global_effects") var overrideGlobalEffects: Boolean = false,
    @ColumnInfo(name = "override_global_effects_modified") var overrideGlobalEffectsModified: Date? = null,
    @ColumnInfo(name = "start_from") var startFromSecs: Int = 0,
    @ColumnInfo(name = "start_from_modified") var startFromModified: Date? = null,
    @ColumnInfo(name = "playback_speed") var playbackSpeed: Double = 1.0,
    @ColumnInfo(name = "playback_speed_modified") var playbackSpeedModified: Date? = null,
    @ColumnInfo(name = "volume_boosted") var isVolumeBoosted: Boolean = false,
    @ColumnInfo(name = "volume_boosted_modified") var volumeBoostedModified: Date? = null,
    @ColumnInfo(name = "is_folder") var isFolder: Boolean = false,
    @ColumnInfo(name = "subscribed") var isSubscribed: Boolean = false,
    @ColumnInfo(name = "show_notifications") var isShowNotifications: Boolean = false,
    @ColumnInfo(name = "show_notifications_modified") var showNotificationsModified: Date? = null,
    @ColumnInfo(name = "auto_download_status") var autoDownloadStatus: Int = 0,
    @ColumnInfo(name = "auto_add_to_up_next") var autoAddToUpNext: AutoAddUpNext = AutoAddUpNext.OFF,
    @ColumnInfo(name = "auto_add_to_up_next_modified") var autoAddToUpNextModified: Date? = null,
    @ColumnInfo(name = "most_popular_color") var backgroundColor: Int = 0,
    @ColumnInfo(name = "primary_color") var tintColorForLightBg: Int = 0,
    @ColumnInfo(name = "secondary_color") var tintColorForDarkBg: Int = 0,
    @ColumnInfo(name = "light_overlay_color") var fabColorForDarkBg: Int = 0,
    @ColumnInfo(name = "fab_for_light_bg") var fabColorForLightBg: Int = 0,
    @ColumnInfo(name = "link_for_dark_bg") var linkColorForLightBg: Int = 0,
    @ColumnInfo(name = "link_for_light_bg") var linkColorForDarkBg: Int = 0,
    @ColumnInfo(name = "color_version") var colorVersion: Int = 0,
    @ColumnInfo(name = "color_last_downloaded") var colorLastDownloaded: Long = 0,
    @ColumnInfo(name = "sync_status") var syncStatus: Int = SYNC_STATUS_NOT_SYNCED,
    @ColumnInfo(name = "exclude_from_auto_archive") var excludeFromAutoArchive: Boolean = false, // Not used anymore
    @ColumnInfo(name = "override_global_archive") var overrideGlobalArchive: Boolean = false,
    @ColumnInfo(name = "override_global_archive_modified") var overrideGlobalArchiveModified: Date? = null,
    @Deprecated(
        message = "This property doesn't account for global override. Use 'autoArchiveAfterPlaying' instead.",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith(expression = "autoArchiveAfterPlaying"),
    )
    @ColumnInfo(name = "auto_archive_played_after") internal var rawAutoArchiveAfterPlaying: AutoArchiveAfterPlaying = AutoArchiveAfterPlaying.Never,
    @ColumnInfo(name = "auto_archive_played_after_modified") var autoArchiveAfterPlayingModified: Date? = null,
    @Deprecated(
        message = "This property doesn't account for global override. Use 'autoArchiveInactive' instead.",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith(expression = "autoArchiveInactive"),
    )
    @ColumnInfo(name = "auto_archive_inactive_after") internal var rawAutoArchiveInactive: AutoArchiveInactive = AutoArchiveInactive.Default,
    @ColumnInfo(name = "auto_archive_inactive_after_modified") var autoArchiveInactiveModified: Date? = null,
    @Deprecated(
        message = "This property doesn't account for global override. Use 'autoArchiveEpisodeLimit' instead.",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith(expression = "autoArchiveEpisodeLimit"),
    )
    @ColumnInfo(name = "auto_archive_episode_limit") internal var rawAutoArchiveEpisodeLimit: Int? = null,
    @ColumnInfo(name = "auto_archive_episode_limit_modified") var autoArchiveEpisodeLimitModified: Date? = null,
    @ColumnInfo(name = "estimated_next_episode") var estimatedNextEpisode: Date? = null,
    @ColumnInfo(name = "episode_frequency") var episodeFrequency: String? = null,
    @ColumnInfo(name = "grouping") var grouping: PodcastGrouping = PodcastGrouping.None,
    @ColumnInfo(name = "grouping_modified") var groupingModified: Date? = null,
    @ColumnInfo(name = "skip_last") var skipLastSecs: Int = 0,
    @ColumnInfo(name = "skip_last_modified") var skipLastModified: Date? = null,
    @ColumnInfo(name = "show_archived") var showArchived: Boolean = false,
    @ColumnInfo(name = "show_archived_modified") var showArchivedModified: Date? = null,
    @ColumnInfo(name = "trim_silence_level") var trimMode: TrimMode = TrimMode.OFF,
    @ColumnInfo(name = "trim_silence_level_modified") var trimModeModified: Date? = null,
    @ColumnInfo(name = "refresh_available") var refreshAvailable: Boolean = false,
    @Deprecated(
        message = "This property doesn't account for home folder. Use 'folderUuid' instead.",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith(expression = "folderUuid"),
    )
    @ColumnInfo(name = "folder_uuid") internal var rawFolderUuid: String? = null,
    @ColumnInfo(name = "licensing") var licensing: Licensing = Licensing.KEEP_EPISODES,
    @ColumnInfo(name = "isPaid") var isPaid: Boolean = false,
    @Embedded(prefix = "bundle") var singleBundle: Bundle? = null,
    @Ignore val episodes: MutableList<PodcastEpisode> = mutableListOf(),
) : Serializable {

    constructor() : this(uuid = "")

    enum class AutoAddUpNext(val databaseInt: Int, val analyticsValue: String) {
        OFF(0, "off"),
        PLAY_LAST(1, "add_last"),
        PLAY_NEXT(2, "add_first"),
        ;

        companion object {
            fun fromDatabaseInt(int: Int?) = values().firstOrNull { it.databaseInt == int }
        }
    }

    companion object {
        // A holder of podcast substitutes when needed for UserEpisodes
        val userPodcast = Podcast(
            uuid = "da7aba5e-f11e-f11e-f11e-da7aba5ef11e",
            title = "Custom Episode",
        )

        const val SYNC_STATUS_NOT_SYNCED = 0
        const val SYNC_STATUS_SYNCED = 1

        const val AUTO_DOWNLOAD_OFF = 0
        const val AUTO_DOWNLOAD_NEW_EPISODES = 1
    }

    @Transient
    @Ignore
    var unplayedEpisodeCount: Int = 0

    val isAutoDownloadNewEpisodes: Boolean
        get() = autoDownloadStatus == AUTO_DOWNLOAD_NEW_EPISODES

    val isAutoAddToUpNextOff: Boolean
        get() = autoAddToUpNext == AutoAddUpNext.OFF

    val isAutoAddToUpNextPlayLast: Boolean
        get() = autoAddToUpNext == AutoAddUpNext.PLAY_LAST

    val isAutoAddToUpNextPlayNext: Boolean
        get() = autoAddToUpNext == AutoAddUpNext.PLAY_NEXT

    val adapterId: Long
        get() = UUID.nameUUIDFromBytes(uuid.toByteArray()).mostSignificantBits

    val isNotSynced: Boolean
        get() = syncStatus == SYNC_STATUS_NOT_SYNCED

    val isSilenceRemoved: Boolean
        get() = trimMode != TrimMode.OFF

    val isUsingEffects: Boolean
        get() = overrideGlobalEffects && (isSilenceRemoved || isVolumeBoosted || playbackSpeed != 1.0)

    val playbackEffects: PlaybackEffects
        get() {
            val effects = PlaybackEffects()
            effects.playbackSpeed = playbackSpeed
            effects.trimMode = trimMode
            effects.isVolumeBoosted = isVolumeBoosted
            return effects
        }

    @Suppress("DEPRECATION_ERROR")
    var folderUuid: String?
        get() = rawFolderUuid?.takeIf { it != Folder.homeFolderUuid }
        set(value) {
            rawFolderUuid = value?.takeIf { it != Folder.homeFolderUuid }
        }

    @Suppress("DEPRECATION_ERROR")
    var autoArchiveAfterPlaying: AutoArchiveAfterPlaying?
        get() = rawAutoArchiveAfterPlaying.takeIf { overrideGlobalArchive }
        set(value) {
            if (value != null) {
                rawAutoArchiveAfterPlaying = value
            }
        }

    @Suppress("DEPRECATION_ERROR")
    var autoArchiveInactive: AutoArchiveInactive?
        get() = rawAutoArchiveInactive.takeIf { overrideGlobalArchive }
        set(value) {
            if (value != null) {
                rawAutoArchiveInactive = value
            }
        }

    @Suppress("DEPRECATION_ERROR")
    var autoArchiveEpisodeLimit: Int?
        get() = rawAutoArchiveEpisodeLimit.takeIf { overrideGlobalArchive }
        set(value) {
            if (value != null) {
                rawAutoArchiveEpisodeLimit = value
            }
        }

    enum class Licensing {
        KEEP_EPISODES, DELETE_EPISODES
    }

    fun getFirstCategory(resources: Resources): String {
        return podcastCategory.split(delimiters = arrayOf("\n")).first().tryToLocalise(resources)
    }

    fun addEpisode(episode: PodcastEpisode) {
        this.episodes.add(episode)
    }

    fun getShortUrl(): String {
        val podcastUrl = podcastUrl
        if (podcastUrl == null || podcastUrl.isBlank()) {
            return ""
        }
        // remove invalid urls such as http://null
        if (podcastUrl.indexOf('.') == -1) {
            return ""
        }
        return try {
            val host = URL(podcastUrl).host
            if (host.startsWith("www.", ignoreCase = true)) {
                host.substring(4)
            } else {
                host
            }
        } catch (e: MalformedURLException) {
            ""
        }
    }

    fun getTintColor(isDarkTheme: Boolean): Int {
        val lightThemeColor = if (tintColorForLightBg != 0 && tintColorForLightBg != DEFAULT_SERVER_LIGHT_TINT_COLOR) tintColorForLightBg else DEFAULT_LIGHT_TINT
        val darkThemeColor = if (tintColorForDarkBg != 0 && tintColorForDarkBg != DEFAULT_SERVER_DARK_TINT_COLOR) tintColorForDarkBg else DEFAULT_DARK_TINT
        return if (isDarkTheme) darkThemeColor else lightThemeColor
    }

    fun getPlayerTintColor(isDarkTheme: Boolean): Int {
        return if (isDarkTheme) tintColorForDarkBg else tintColorForLightBg
    }

    fun displayableFrequency(resources: Resources): String? {
        val frequency = episodeFrequency ?: return null
        val stringId = when (frequency.lowercase()) {
            "hourly" -> LR.string.podcast_released_hourly
            "daily" -> LR.string.podcast_released_daily
            "weekly" -> LR.string.podcast_released_weekly
            "fortnightly" -> LR.string.podcast_released_fortnightly
            "monthly" -> LR.string.podcast_released_monthly
            else -> return null
        }
        return resources.getString(stringId)
    }

    @Suppress("DEPRECATION")
    fun displayableNextEpisodeDate(context: Context): String? {
        val expectedDate = estimatedNextEpisode ?: return null
        val expectedTime = expectedDate.time
        if (expectedTime <= 0) {
            return null
        }
        val sevenDaysInMs = 7 * DateUtils.DAY_IN_MILLIS
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - sevenDaysInMs

        val resources = context.resources
        return when {
            expectedTime < sevenDaysAgo -> null
            DateUtils.isToday(expectedTime) -> resources.getString(LR.string.podcast_next_episode_today)
            DateUtils.isToday(expectedTime - DateUtils.DAY_IN_MILLIS) -> resources.getString(LR.string.podcast_next_episode_tomorrow)
            expectedTime in sevenDaysAgo..now -> resources.getString(LR.string.podcast_next_episode_any_day_now)
            else -> {
                val formattedDate = RelativeDateFormatter(context).format(expectedDate)
                resources.getString(LR.string.podcast_next_episode_value, formattedDate)
            }
        }
    }
}

private val DEFAULT_SERVER_LIGHT_TINT_COLOR = Color.parseColor("#F44336")
private val DEFAULT_SERVER_DARK_TINT_COLOR = Color.parseColor("#C62828")
private val DEFAULT_LIGHT_TINT = Color.parseColor("#1E1F1E")
private val DEFAULT_DARK_TINT = Color.parseColor("#FFFFFF")
