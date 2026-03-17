package au.com.shiftyjelly.pocketcasts.views.swipe

import au.com.shiftyjelly.pocketcasts.models.to.EpisodeUuidPair
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.automattic.eventhorizon.AddToPlaylistFlowSourceType
import com.automattic.eventhorizon.PlaylistAddEpisodeSourceType
import com.automattic.eventhorizon.PlaylistRemoveEpisodeSourceType

interface AddToPlaylistFragmentFactory {
    fun create(
        source: Source,
        uuids: List<EpisodeUuidPair>,
        customTheme: Theme.ThemeType? = null,
    ): BaseDialogFragment

    fun create(
        source: Source,
        episodeUuid: String,
        podcastUuid: String,
        customTheme: Theme.ThemeType? = null,
    ): BaseDialogFragment = create(
        source = source,
        uuids = listOf(
            EpisodeUuidPair(
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
            ),
        ),
        customTheme = customTheme,
    )

    enum class Source(
        val analyticsValue: AddToPlaylistFlowSourceType,
        val episodeAddAnalyticsValue: PlaylistAddEpisodeSourceType,
        val episodeRemoveAnalyticsValue: PlaylistRemoveEpisodeSourceType,
    ) {
        Swipe(
            analyticsValue = AddToPlaylistFlowSourceType.Swipe,
            episodeAddAnalyticsValue = PlaylistAddEpisodeSourceType.SwipeEdit,
            episodeRemoveAnalyticsValue = PlaylistRemoveEpisodeSourceType.SwipeEdit,
        ),
        Shelf(
            analyticsValue = AddToPlaylistFlowSourceType.Shelf,
            episodeAddAnalyticsValue = PlaylistAddEpisodeSourceType.Shelf,
            episodeRemoveAnalyticsValue = PlaylistRemoveEpisodeSourceType.Shelf,
        ),
        EpisodeDetails(
            analyticsValue = AddToPlaylistFlowSourceType.EpisodeDetails,
            episodeAddAnalyticsValue = PlaylistAddEpisodeSourceType.EpisodeDetails,
            episodeRemoveAnalyticsValue = PlaylistRemoveEpisodeSourceType.EpisodeDetails,
        ),
        MultiSelect(
            analyticsValue = AddToPlaylistFlowSourceType.MultiSelect,
            episodeAddAnalyticsValue = PlaylistAddEpisodeSourceType.MultiSelect,
            episodeRemoveAnalyticsValue = PlaylistRemoveEpisodeSourceType.MultiSelect,
        ),
    }

    companion object {
        // We support adding episodes only from phones but we need a stub to satisfy Dagger.
        val Stub = object : AddToPlaylistFragmentFactory {
            override fun create(
                source: Source,
                uuids: List<EpisodeUuidPair>,
                customTheme: Theme.ThemeType?,
            ): BaseDialogFragment {
                error("Adding episodes to playlist is not supported")
            }
        }
    }
}
