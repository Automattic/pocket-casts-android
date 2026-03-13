package au.com.shiftyjelly.pocketcasts.views.swipe

import au.com.shiftyjelly.pocketcasts.models.to.EpisodeUuidPair
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.automattic.eventhorizon.AddToPlaylistFlowSource
import com.automattic.eventhorizon.PlaylistAddEpisodeSource
import com.automattic.eventhorizon.PlaylistRemoveEpisodeSource

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
        val analyticsValue: AddToPlaylistFlowSource,
        val episodeAddAnalyticsValue: PlaylistAddEpisodeSource,
        val episodeRemoveAnalyticsValue: PlaylistRemoveEpisodeSource,
    ) {
        Swipe(
            analyticsValue = AddToPlaylistFlowSource.Swipe,
            episodeAddAnalyticsValue = PlaylistAddEpisodeSource.SwipeEdit,
            episodeRemoveAnalyticsValue = PlaylistRemoveEpisodeSource.SwipeEdit,
        ),
        Shelf(
            analyticsValue = AddToPlaylistFlowSource.Shelf,
            episodeAddAnalyticsValue = PlaylistAddEpisodeSource.Shelf,
            episodeRemoveAnalyticsValue = PlaylistRemoveEpisodeSource.Shelf,
        ),
        EpisodeDetails(
            analyticsValue = AddToPlaylistFlowSource.EpisodeDetails,
            episodeAddAnalyticsValue = PlaylistAddEpisodeSource.EpisodeDetails,
            episodeRemoveAnalyticsValue = PlaylistRemoveEpisodeSource.EpisodeDetails,
        ),
        MultiSelect(
            analyticsValue = AddToPlaylistFlowSource.MultiSelect,
            episodeAddAnalyticsValue = PlaylistAddEpisodeSource.MultiSelect,
            episodeRemoveAnalyticsValue = PlaylistRemoveEpisodeSource.MultiSelect,
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
