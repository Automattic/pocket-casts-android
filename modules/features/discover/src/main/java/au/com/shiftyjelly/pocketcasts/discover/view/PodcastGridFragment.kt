package au.com.shiftyjelly.pocketcasts.discover.view

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.databinding.PodcastGridFragmentBinding
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.LIST_ID_KEY
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.PodcastListViewState
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.ui.images.ThemedImageTintTransformation
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.activity.WebViewActivity
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import coil.load
import coil.transform.CircleCropTransformation
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class PodcastGridFragment : PodcastGridListFragment() {
    companion object {
        private const val NONE = "none"
        fun newInstance(listUuid: String?, title: String, sourceUrl: String, listType: ListType, displayStyle: DisplayStyle, expandedStyle: ExpandedStyle, tagline: String? = null, curated: Boolean = false): PodcastGridFragment {
            return PodcastGridFragment().apply {
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

    private var binding: PodcastGridFragmentBinding? = null

    lateinit var adapter: GridListAdapter
    var feed: ListFeed? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = PodcastGridFragmentBinding.inflate(inflater, container, false)

        viewModel.load(arguments?.getString(ARG_SOURCE_URL), expandedStyle)
        viewModel.state.observe(
            viewLifecycleOwner,
            Observer { state ->
                val binding = binding ?: return@Observer
                when (state) {
                    is PodcastListViewState.Loading -> {}
                    is PodcastListViewState.ListLoaded -> {

                        feed = state.feed
                        feed?.let {

                            if (displayStyle.toString() != DisplayStyle.CollectionList().toString()) {
                                binding.headerLayout.visibility = View.GONE
                            } else {
                                binding.headerLayout.visibility = View.VISIBLE

                                binding.toolbar.title = it.subtitle
                                binding.toolbar.menu.findItem(R.id.share_list)?.isVisible = curated

                                binding.lblSubtitle.text = it.subtitle?.uppercase()
                                binding.lblTitle.text = it.title
                                binding.lblBody.text = it.description

                                it.webLinkTitle?.let { linkTitle ->
                                    it.webLinkUrl?.let { linkUrl ->
                                        binding.linkLayout.visibility = View.VISIBLE
                                        binding.lblLinkTitle.text = linkTitle
                                        binding.linkLayout.setOnClickListener {
                                            analyticsTracker.track(AnalyticsEvent.DISCOVER_COLLECTION_LINK_TAPPED, mapOf(LIST_ID_KEY to (listUuid ?: NONE)))
                                            WebViewActivity.show(context, linkTitle, linkUrl)
                                        }
                                    }
                                }

                                val colorMatrix = ColorMatrix().apply { setSaturation(0.0f) }
                                binding.imgPodcast.colorFilter = ColorMatrixColorFilter(colorMatrix)

                                context?.let { context ->
                                    it.collageImages?.let { images ->
                                        if (images.isNotEmpty()) {
                                            val backgroundUrl = images[0].imageUrl
                                            binding.imgPodcast.load(backgroundUrl) {
                                                transformations(ThemedImageTintTransformation(context))
                                            }
                                        }
                                    }

                                    it.tintColors?.let { tintColors ->
                                        val tintColor: Int
                                        try {
                                            tintColor = if (theme.isDarkTheme) Color.parseColor(tintColors.darkTintColor) else Color.parseColor(tintColors.lightTintColor)
                                            binding.lblSubtitle.setTextColor(tintColor)
                                            binding.imgTint.setBackgroundColor(tintColor)
                                        } catch (e: Exception) {
                                        }
                                    }

                                    it.collectionImageUrl?.let { url ->
                                        binding.highlightImage.load(url) {
                                            transformations(ThemedImageTintTransformation(context), CircleCropTransformation())
                                        }
                                    }
                                }
                            }
                        }

                        adapter.submitList(state.feed.podcasts)
                    }
                    is PodcastListViewState.Error -> {
                        Timber.e("Could not load feed ${state.error.message}")
                    }
                }
            }
        )

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        val title = if (displayStyle.toString() != DisplayStyle.CollectionList().toString()) arguments?.getString(ARG_TITLE) else ""

        val toolbar = binding.toolbar
        setupToolbarAndStatusBar(
            toolbar = toolbar,
            title = title,
            menu = R.menu.discover_share,
            navigationIcon = BackArrow
        )
        toolbar.setOnMenuItemClickListener(this)

        val recyclerView = binding.recyclerView
        val columnCount = UiUtil.getDiscoverGridColumnCount(context = recyclerView.context)
        recyclerView.layoutManager = GridLayoutManager(context, columnCount)

        recyclerView.addItemDecoration(SpaceItemDecoration())
        val imageSize = UiUtil.getDiscoverGridImageWidthPx(context = recyclerView.context)
        adapter = GridListAdapter(imageSize, onPodcastClicked, onPodcastSubscribe)
        recyclerView.adapter = adapter
    }

    class SpaceItemDecoration : ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val lp = view.layoutParams as GridLayoutManager.LayoutParams
            val spanIndex = lp.spanIndex
            if (spanIndex == 0) {
                outRect.left = 16.dpToPx(parent.context)
                outRect.right = 8.dpToPx(parent.context)
            } else {
                outRect.left = 8.dpToPx(parent.context)
                outRect.right = 16.dpToPx(parent.context)
            }
        }
    }
}
