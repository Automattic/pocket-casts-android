package au.com.shiftyjelly.pocketcasts.discover.view

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.databinding.PodcastGridFragmentBinding
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.PodcastListViewState
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class PodcastGridFragment : PodcastGridListFragment() {
    companion object {
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

                                updateCollectionHeaderView(
                                    listFeed = it,
                                    headshotImageView = binding.highlightImage,
                                    headerImageView = binding.imgPodcast,
                                    tintImageView = binding.imgTint,
                                    titleTextView = binding.lblTitle,
                                    subTitleTextView = binding.lblSubtitle,
                                    bodyTextView = binding.lblBody,
                                    linkView = binding.linkLayout,
                                    linkTextView = binding.lblLinkTitle,
                                    toolbar = binding.toolbar
                                )
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
