package au.com.shiftyjelly.pocketcasts.views.swipe

import au.com.shiftyjelly.pocketcasts.models.to.EpisodeUuids
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment

interface AddToPlaylistFragmentFactory {
    fun create(
        source: Source,
        uuids: List<EpisodeUuids>,
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
            EpisodeUuids(
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
            ),
        ),
        customTheme = customTheme,
    )

    enum class Source(
        val analyticsValue: String,
        val episodeEditAnalyticsValue: String,
    ) {
        Swipe(
            analyticsValue = "swipe",
            episodeEditAnalyticsValue = "swipe_edit",
        ),
        Shelf(
            analyticsValue = "shelf",
            episodeEditAnalyticsValue = "shelf",
        ),
        EpisodeDetails(
            analyticsValue = "episode_details",
            episodeEditAnalyticsValue = "episode_details",
        ),
        MultiSelect(
            analyticsValue = "multi_select",
            episodeEditAnalyticsValue = "multi_select",
        ),
    }

    companion object {
        // We support adding episodes only from phones but we need a stub to satisfy Dagger.
        val Stub = object : AddToPlaylistFragmentFactory {
            override fun create(
                source: Source,
                uuids: List<EpisodeUuids>,
                customTheme: Theme.ThemeType?,
            ): BaseDialogFragment {
                error("Adding episodes to playlist is not supported")
            }
        }
    }
}
