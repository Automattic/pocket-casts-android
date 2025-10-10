package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.FragmentShareIncomingBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.hide
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.extensions.show
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class ShareListIncomingFragment :
    BaseFragment(),
    ShareListIncomingAdapter.ClickListener {

    @Inject lateinit var podcastManager: PodcastManager

    @Inject lateinit var settings: Settings

    private lateinit var adapter: ShareListIncomingAdapter
    private val viewModel: ShareListIncomingViewModel by viewModels()
    private var binding: FragmentShareIncomingBinding? = null

    companion object {
        const val EXTRA_URL = "EXTRA_URL"
        const val EXTRA_SOURCE = "EXTRA_SOURCE"

        fun newInstance(
            listPath: String,
            sourceView: SourceView = SourceView.UNKNOWN,
        ): ShareListIncomingFragment {
            return ShareListIncomingFragment().apply {
                arguments = bundleOf(
                    EXTRA_URL to listPath,
                    EXTRA_SOURCE to sourceView.analyticsValue,
                )
            }
        }
    }

    val source get() = SourceView.fromString(arguments?.getString(EXTRA_SOURCE))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getString(EXTRA_URL)?.let { url ->
            viewModel.loadShareUrl(url)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentShareIncomingBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        binding?.recyclerView?.adapter = null
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = view.context

        adapter = ShareListIncomingAdapter(this, context)

        val binding = binding ?: return

        val recyclerView = binding.recyclerView
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        binding.toolbar.setup(navigationIcon = NavigationIcon.Close, activity = activity, theme = theme, includeStatusBarPadding = false)

        viewModel.share.observe(viewLifecycleOwner) { share ->
            when (share) {
                is ShareState.Loaded -> {
                    this.binding?.progressCircle?.hide()
                    adapter.load(share.title, share.description, share.podcasts)
                    adapter.notifyDataSetChanged()
                }
                is ShareState.Loading -> {
                    this.binding?.progressCircle?.show()
                }
                else -> {
                    this.binding?.progressCircle?.hide()
                }
            }
        }

        viewModel.subscribedUuids.observe(viewLifecycleOwner) { uuids ->
            adapter.subscribedUuids = uuids.toHashSet()
        }

        if (!viewModel.isFragmentChangingConfigurations) {
            viewModel.trackShareEvent(AnalyticsEvent.INCOMING_SHARE_LIST_SHOWN, mapOf("source" to source.analyticsValue))
        }

        // add bottom padding to make sure the content isn't hidden by the mini player
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.bottomInset.collect {
                    binding.recyclerView.updatePadding(bottom = it)
                }
            }
        }
    }

    override fun onPodcastClick(podcast: Podcast) {
        val fragment = PodcastFragment.newInstance(podcast.uuid, sourceView = SourceView.SHARE_LIST)
        (activity as FragmentHostListener).addFragment(fragment)
    }

    override fun onSubscribeClick(podcast: Podcast) {
        viewModel.subscribeToPodcast(podcast.uuid)
        viewModel.trackShareEvent(
            AnalyticsEvent.PODCAST_SUBSCRIBED,
            AnalyticsProp.subscribeToggledMap(uuid = podcast.uuid),
        )
    }

    override fun onUnsubscribeClick(podcast: Podcast) {
        val uuid = podcast.uuid
        val dialog = OptionsDialog()
            .setTitle(getString(LR.string.podcasts_unsubscribe_are_you_sure))
            .addTextOption(
                titleId = LR.string.unsubscribe,
                click = {
                    viewModel.unsubscribeFromPodcast(uuid)
                    viewModel.trackShareEvent(
                        AnalyticsEvent.PODCAST_UNSUBSCRIBED,
                        AnalyticsProp.subscribeToggledMap(uuid = uuid),
                    )
                },
            )
        activity?.supportFragmentManager?.let {
            dialog.show(it, "unsubscribe")
        }
    }

    override fun onSubscribeToAllClick(podcasts: List<Podcast>) {
        viewModel.trackShareEvent(
            AnalyticsEvent.INCOMING_SHARE_LIST_SUBSCRIBED_ALL,
            AnalyticsProp.countMap(podcasts.size),
        )
        for (podcastHeader in podcasts) {
            val uuid = podcastHeader.uuid
            viewModel.subscribeToPodcast(uuid)
            viewModel.trackShareEvent(
                AnalyticsEvent.PODCAST_SUBSCRIBED,
                AnalyticsProp.subscribeToggledMap(uuid = uuid),
            )
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }

    private object AnalyticsProp {
        private const val COUNT = "count"
        private const val SOURCE = "source"
        private const val UUID = "uuid"
        fun countMap(count: Int) = mapOf(this.COUNT to count)
        fun subscribeToggledMap(uuid: String) = mapOf(this.SOURCE to "incoming_share_list", this.UUID to uuid)
    }
}
