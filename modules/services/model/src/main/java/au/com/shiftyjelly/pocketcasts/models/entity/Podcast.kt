package au.com.shiftyjelly.pocketcasts.models.entity

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.text.format.DateUtils
import androidx.core.graphics.toColorInt
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveLimit
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
    @ColumnInfo(name = "podcast_html_description") var podcastHtmlDescription: String = "",
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
    @ColumnInfo(name = "auto_archive_played_after") internal var rawAutoArchiveAfterPlaying: AutoArchiveAfterPlaying = AutoArchiveAfterPlaying.Never,
    @ColumnInfo(name = "auto_archive_played_after_modified") var autoArchiveAfterPlayingModified: Date? = null,
    @ColumnInfo(name = "auto_archive_inactive_after") internal var rawAutoArchiveInactive: AutoArchiveInactive = AutoArchiveInactive.Default,
    @ColumnInfo(name = "auto_archive_inactive_after_modified") var autoArchiveInactiveModified: Date? = null,
    @ColumnInfo(name = "auto_archive_episode_limit") internal var rawAutoArchiveEpisodeLimit: AutoArchiveLimit = AutoArchiveLimit.None,
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
    @ColumnInfo(name = "folder_uuid") internal var rawFolderUuid: String? = null,
    @ColumnInfo(name = "licensing") var licensing: Licensing = Licensing.KEEP_EPISODES,
    @ColumnInfo(name = "isPaid") var isPaid: Boolean = false,
    @ColumnInfo(name = "is_private") var isPrivate: Boolean = false,
    @ColumnInfo(name = "is_header_expanded", defaultValue = "1") var isHeaderExpanded: Boolean = true,
    @ColumnInfo(name = "funding_url") var fundingUrl: String? = null,
    @ColumnInfo(name = "slug") var slug: String = "",
    @Embedded(prefix = "bundle") var singleBundle: Bundle? = null,
    @Ignore val episodes: MutableList<PodcastEpisode> = mutableListOf(),
) : Serializable {

    constructor() : this(uuid = "")

    enum class AutoAddUpNext(
        val databaseInt: Int,
        val analyticsValue: String,
        val labelId: Int,
    ) {
        OFF(
            databaseInt = 0,
            analyticsValue = "off",
            labelId = LR.string.off,
        ),
        PLAY_LAST(
            databaseInt = 1,
            analyticsValue = "add_last",
            labelId = LR.string.play_last,
        ),
        PLAY_NEXT(
            databaseInt = 2,
            analyticsValue = "add_first",
            labelId = LR.string.play_next,
        ),
        ;

        companion object {
            fun fromDatabaseInt(int: Int?) = entries.firstOrNull { it.databaseInt == int }
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

    val adapterId: Long
        get() = UUID.nameUUIDFromBytes(uuid.toByteArray()).mostSignificantBits

    val isNotSynced: Boolean
        get() = syncStatus == SYNC_STATUS_NOT_SYNCED

    val isSilenceRemoved: Boolean
        get() = trimMode != TrimMode.OFF

    val canShare: Boolean
        get() = !isPrivate

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
        get() = rawFolderUuid?.takeIf { it != Folder.HOME_FOLDER_UUID }
        set(value) {
            rawFolderUuid = value?.takeIf { it != Folder.HOME_FOLDER_UUID }
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
    var autoArchiveEpisodeLimit: AutoArchiveLimit?
        get() = rawAutoArchiveEpisodeLimit.takeIf { overrideGlobalArchive }
        set(value) {
            if (value != null) {
                rawAutoArchiveEpisodeLimit = value
            }
        }

    enum class Licensing {
        KEEP_EPISODES,
        DELETE_EPISODES,
    }

    fun getFirstCategoryUnlocalised() = podcastCategory.split("\n").first().trim()

    fun getFirstCategory(resources: Resources): String {
        return getFirstCategoryUnlocalised().tryToLocalise(resources)
    }

    fun getFirstCategoryId() = getCategoryIdForName(getFirstCategoryUnlocalised().trim().lowercase())

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
        } catch (_: MalformedURLException) {
            ""
        }
    }

    fun lightThemeTint() = if (tintColorForLightBg != 0 && tintColorForLightBg != DEFAULT_SERVER_LIGHT_TINT_COLOR) tintColorForLightBg else DEFAULT_LIGHT_TINT

    fun darkThemeTint() = if (tintColorForDarkBg != 0 && tintColorForDarkBg != DEFAULT_SERVER_DARK_TINT_COLOR) tintColorForDarkBg else DEFAULT_DARK_TINT

    fun getTintColor(isDarkTheme: Boolean): Int {
        return if (isDarkTheme) darkThemeTint() else lightThemeTint()
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

    /**
     * Copies playback effect settings from a source podcast to this podcast
     */
    fun copyPlaybackEffects(
        sourcePodcast: Podcast,
    ) {
        sourcePodcast.overrideGlobalEffectsModified?.takeIf { overrideGlobalEffectsModified?.before(it) ?: true }?.let {
            overrideGlobalEffects = sourcePodcast.overrideGlobalEffects
            overrideGlobalEffectsModified = sourcePodcast.overrideGlobalEffectsModified
        }
        sourcePodcast.playbackSpeedModified?.takeIf { playbackSpeedModified?.before(it) ?: true }?.let {
            playbackSpeed = sourcePodcast.playbackSpeed
            playbackSpeedModified = sourcePodcast.playbackSpeedModified
        }
        sourcePodcast.volumeBoostedModified?.takeIf { volumeBoostedModified?.before(it) ?: true }?.let {
            isVolumeBoosted = sourcePodcast.isVolumeBoosted
            volumeBoostedModified = sourcePodcast.volumeBoostedModified
        }
        sourcePodcast.trimModeModified?.takeIf { trimModeModified?.before(it) ?: true }?.let {
            trimMode = sourcePodcast.trimMode
            trimModeModified = sourcePodcast.trimModeModified
        }
    }
}

private val DEFAULT_SERVER_LIGHT_TINT_COLOR = "#F44336".toColorInt()
private val DEFAULT_SERVER_DARK_TINT_COLOR = "#C62828".toColorInt()
private val DEFAULT_LIGHT_TINT = "#1E1F1E".toColorInt()
private val DEFAULT_DARK_TINT = "#FFFFFF".toColorInt()

private val KnownCategoryIds = mapOf(
    "arts" to 1,
    "business" to 2,
    "comedy" to 3,
    "education" to 4,
    "leisure" to 5,
    "government" to 6,
    "health & fitness" to 7,
    "kids & family" to 8,
    "music" to 9,
    "news" to 10,
    "spirituality" to 11,
    "science" to 12,
    "society & culture" to 13,
    "sports" to 14,
    "tech" to 15,
    "technology" to 15,
    "tv & film" to 16,
    "fiction" to 17,
    "history" to 18,
    "true crime" to 19,
)

private fun getCategoryIdForName(name: String) = KnownCategoryIds[name.trim().lowercase()]
