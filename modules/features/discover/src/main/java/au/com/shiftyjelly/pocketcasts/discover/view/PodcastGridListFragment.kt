package au.com.shiftyjelly.pocketcasts.discover.view

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.PodcastListViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.AnalyticsHelper
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

open class PodcastGridListFragment : BaseFragment(), Toolbar.OnMenuItemClickListener {

    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var settings: Settings
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    companion object {
        internal const val ARG_LIST_UUID = "listUuid"
        internal const val ARG_TITLE = "title"
        internal const val ARG_SOURCE_URL = "url"
        internal const val ARG_LIST_TYPE = "listType"
        internal const val ARG_DISPLAY_STYLE = "displayStyle"
        internal const val ARG_EXPANDED_STYLE = "expandedStyle"
        internal const val ARG_BACKGROUND_COLOR = "backgroundColor"
        internal const val ARG_TAGLINE = "tagline"
        internal const val ARG_CURATED = "curated"

        fun newInstanceBundle(listUuid: String?, title: String, sourceUrl: String, listType: ListType, displayStyle: DisplayStyle, expandedStyle: ExpandedStyle, tagline: String? = null, curated: Boolean = false): Bundle {
            return Bundle().apply {
                putString(ARG_LIST_UUID, listUuid)
                putString(ARG_TITLE, title)
                putString(ARG_SOURCE_URL, sourceUrl)
                putString(ARG_LIST_TYPE, listType.stringValue)
                putString(ARG_DISPLAY_STYLE, displayStyle.stringValue)
                putString(ARG_EXPANDED_STYLE, expandedStyle.stringValue)
                putString(ARG_TAGLINE, tagline)
                putBoolean(ARG_CURATED, curated)
            }
        }
    }

    val listType: ListType
        get() = arguments?.getString(ARG_LIST_TYPE)?.let { ListType.fromString(it) } ?: ListType.PodcastList()

    val displayStyle: DisplayStyle
        get() = DisplayStyle.fromString(arguments?.getString(ARG_DISPLAY_STYLE)!!) ?: DisplayStyle.SmallList()

    val expandedStyle: ExpandedStyle
        get() = ExpandedStyle.fromString(arguments?.getString(ARG_EXPANDED_STYLE)) ?: ExpandedStyle.PlainList()

    val backgroundColor: String?
        get() = arguments?.getString(ARG_BACKGROUND_COLOR)

    val tagline: String?
        get() = arguments?.getString(ARG_TAGLINE)

    val listUuid: String?
        get() = arguments?.getString(ARG_LIST_UUID)

    val sourceUrl: String?
        get() = arguments?.getString(ARG_SOURCE_URL)

    val shareUrl: String?
        get() = sourceUrl?.replace("(\\.json)?".toRegex(), "")

    val curated: Boolean
        get() = arguments?.getBoolean(ARG_CURATED) ?: false

    protected val viewModel: PodcastListViewModel by viewModels()

    val onPodcastClicked: (DiscoverPodcast) -> Unit = { podcast ->
        listUuid?.let {
            AnalyticsHelper.podcastTappedFromList(it, podcast.uuid)
            analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_PODCAST_TAPPED, mapOf(LIST_ID_KEY to it, PODCAST_UUID_KEY to podcast.uuid))
        }
        val fragment = PodcastFragment.newInstance(podcastUuid = podcast.uuid, fromListUuid = listUuid)
        (activity as FragmentHostListener).addFragment(fragment)
    }

    val onPodcastSubscribe: (String) -> Unit = { podcastUuid ->
        listUuid?.let {
            AnalyticsHelper.podcastSubscribedFromList(it, podcastUuid)
            analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_PODCAST_SUBSCRIBED, mapOf(LIST_ID_KEY to it, PODCAST_UUID_KEY to podcastUuid))
        }
        podcastManager.subscribeToPodcast(podcastUuid, sync = true)
    }

    val onEpisodeClick: (DiscoverEpisode) -> Unit = { episode ->
        listUuid?.let { listUuid ->
            AnalyticsHelper.podcastEpisodeTappedFromList(listId = listUuid, podcastUuid = episode.podcast_uuid, episodeUuid = episode.uuid)
            analyticsTracker.track(
                AnalyticsEvent.DISCOVER_LIST_EPISODE_TAPPED,
                mapOf(LIST_ID_KEY to listUuid, PODCAST_UUID_KEY to episode.podcast_uuid, EPISODE_UUID_KEY to episode.uuid)
            )
        }
        val fragment = EpisodeFragment.newInstance(episodeUuid = episode.uuid, podcastUuid = episode.podcast_uuid, fromListUuid = listUuid)
        fragment.show(parentFragmentManager, "episode_card")
    }

    val onEpisodePlayClick: (DiscoverEpisode) -> Unit = { episode ->
        viewModel.findOrDownloadEpisode(episode) { databaseEpisode ->
            viewModel.playEpisode(databaseEpisode)
        }
    }

    val onEpisodeStopClick: () -> Unit = {
        viewModel.stopPlayback()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (item.itemId == R.id.share_list) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareUrl ?: "")
            }
            listUuid?.let { AnalyticsHelper.listShared(it) }
            startActivity(Intent.createChooser(intent, getString(LR.string.podcasts_share_via)))
            return true
        }
        return false
    }
}
