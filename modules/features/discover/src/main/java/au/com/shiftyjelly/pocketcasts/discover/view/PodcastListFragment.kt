package au.com.shiftyjelly.pocketcasts.discover.view

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.databinding.PodcastListFragmentBinding
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.PodcastListViewState
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastFragment
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPromotion
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.images.ThemedImageTintTransformation
import au.com.shiftyjelly.pocketcasts.utils.AnalyticsHelper
import au.com.shiftyjelly.pocketcasts.views.activity.WebViewActivity
import au.com.shiftyjelly.pocketcasts.views.extensions.hide
import au.com.shiftyjelly.pocketcasts.views.extensions.showIf
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import coil.load
import coil.transform.CircleCropTransformation
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class PodcastListFragment : PodcastGridListFragment() {

    companion object {
        fun newInstance(listUuid: String?, title: String, sourceUrl: String, listType: ListType, displayStyle: DisplayStyle, expandedStyle: ExpandedStyle, tagline: String? = null, curated: Boolean = false): PodcastListFragment {
            return PodcastListFragment().apply {
                arguments = newInstanceBundle(
                    listUuid = listUuid,
                    title = title,
                    sourceUrl = sourceUrl,
                    listType = listType,
                    displayStyle = displayStyle,
                    expandedStyle = expandedStyle,
                    tagline = tagline,
                    curated = curated
                )
            }
        }
    }

    private val onPromotionClick: (DiscoverPromotion) -> Unit = { promotion ->
        AnalyticsHelper.podcastTappedFromList(promotion.promotionUuid, promotion.podcastUuid)

        val fragment = PodcastFragment.newInstance(podcastUuid = promotion.podcastUuid, fromListUuid = promotion.promotionUuid)
        (activity as FragmentHostListener).addFragment(fragment)
    }

    lateinit var adapter: ListAdapter<Any, RecyclerView.ViewHolder>

    private var analyticsImpressionSent = false
    private var binding: PodcastListFragmentBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = PodcastListFragmentBinding.inflate(inflater, container, false)

        this.binding = binding

        viewModel.load(arguments?.getString(ARG_SOURCE_URL), expandedStyle)
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PodcastListViewState.Loading -> {}
                is PodcastListViewState.ListLoaded -> createLoadedView(state)
                is PodcastListViewState.Error -> Timber.e("Could not load feed ${state.error.message}")
            }
        }

        return binding.root
    }

    private fun createLoadedView(state: PodcastListViewState.ListLoaded) {
        val binding = binding ?: return

        trackImpression(impressionId = state.feed.promotion?.promotionUuid ?: listUuid)

        binding.toolbar.menu.findItem(R.id.share_list)?.isVisible = curated && listType is ListType.PodcastList

        if (displayStyle is DisplayStyle.CollectionList && (expandedStyle is ExpandedStyle.PlainList || expandedStyle is ExpandedStyle.DescriptiveList)) {
            createCollectionView(feed = state.feed)
        } else {
            binding.toolbar.title = arguments?.getString(ARG_TITLE)?.tryToLocalise(resources)
        }

        when {
            listType is ListType.EpisodeList -> adapter.submitList(state.feed.episodes)
            expandedStyle is ExpandedStyle.PlainList -> adapter.submitList(state.feed.displayList)
            else -> adapter.submitList(state.feed.podcasts)
        }

        state.feed.tintColors?.tintColorInt(theme.isDarkTheme)?.let { tintColor ->
            (adapter as? PlainListAdapter)?.listTintColor = tintColor
        }
    }

    private fun trackImpression(impressionId: String?) {
        if (analyticsImpressionSent || impressionId == null) {
            return
        }
        AnalyticsHelper.listImpression(impressionId)
        analyticsImpressionSent = true
    }

    private fun createCollectionView(feed: ListFeed) {
        val binding = binding ?: return

        binding.headerLayout.visibility = View.VISIBLE

        binding.toolbar.title = feed.subtitle?.tryToLocalise(resources)
        binding.lblSubtitle.text = feed.subtitle?.uppercase()
        binding.lblTitle.text = feed.title
        binding.lblBody.text = feed.description

        val linkTitle = feed.webLinkTitle
        val linkUrl = feed.webLinkUrl
        if (linkTitle != null && linkUrl != null) {
            binding.linkLayout.visibility = View.VISIBLE
            binding.lblLinkTitle.text = linkTitle
            binding.linkLayout.setOnClickListener {
                WebViewActivity.show(context, linkTitle, linkUrl)
            }
        }

        // circular headshot image
        val headshotImage = feed.collectionImageUrl
        binding.highlightImage.apply {
            showIf(headshotImage != null)
            load(headshotImage) {
                transformations(ThemedImageTintTransformation(context), CircleCropTransformation())
            }
        }

        // tint the header background image if there is also a headshot
        binding.imagePodcast.colorFilter = if (headshotImage == null) {
            null
        } else {
            val colorMatrix = ColorMatrix().apply { setSaturation(0.0f) }
            ColorMatrixColorFilter(colorMatrix)
        }

        // header background image
        val headerImage = feed.headerImageUrl
        if (headerImage == null) {
            // use the background collage image if background hasn't been manually added
            val backgroundImageUrl = feed.collageImages?.find { collage -> collage.key == "mobile" }?.imageUrl
            if (backgroundImageUrl != null) {
                binding.imagePodcast.load(backgroundImageUrl) {
                    transformations(ThemedImageTintTransformation(binding.imagePodcast.context))
                }
            }
        } else {
            binding.imagePodcast.load(headerImage)
            binding.imagePodcast.alpha = 1f
            binding.imageTint.hide()
        }

        feed.tintColors?.let { tintColors ->
            try {
                val tintColor = tintColors.tintColorInt(theme.isDarkTheme) ?: return@let
                binding.lblSubtitle.setTextColor(tintColor)
                binding.imageTint.setBackgroundColor(tintColor)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    @Suppress("UNCHECKED_CAST")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        val toolbar = binding.toolbar
        setupToolbarAndStatusBar(
            toolbar = toolbar,
            title = "",
            menu = R.menu.discover_share,
            navigationIcon = BackArrow
        )
        toolbar.setOnMenuItemClickListener(this)

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        if (expandedStyle !is ExpandedStyle.DescriptiveList) {
            val dividerDrawable = ContextCompat.getDrawable(binding.recyclerView.context, UR.drawable.divider_indented)
            if (dividerDrawable != null) {
                val divider = DividerItemDecoration(context, RecyclerView.VERTICAL).apply {
                    setDrawable(dividerDrawable)
                }
                recyclerView.addItemDecoration(divider)
            }
        }
        adapter = when (expandedStyle) {
            is ExpandedStyle.PlainList -> PlainListAdapter(onPodcastClicked, onPodcastSubscribe, onPromotionClick, onEpisodeClick, onEpisodePlayClick, onEpisodeStopClick)
            is ExpandedStyle.RankedList -> RankedListAdapter(onPodcastClicked, onPodcastSubscribe, tagline, theme)
            is ExpandedStyle.DescriptiveList -> DescriptiveListAdapter(onPodcastClicked, onPodcastSubscribe) as ListAdapter<Any, RecyclerView.ViewHolder>
            is ExpandedStyle.GridList -> GridListAdapter(GridListAdapter.defaultImageSize, onPodcastClicked, onPodcastSubscribe) as ListAdapter<Any, RecyclerView.ViewHolder>
        }
        recyclerView.adapter = adapter
        (recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        (recyclerView.itemAnimator as? SimpleItemAnimator)?.changeDuration = 0
    }
}
