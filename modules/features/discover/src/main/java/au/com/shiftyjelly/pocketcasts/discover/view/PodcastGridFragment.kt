package au.com.shiftyjelly.pocketcasts.discover.view

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.databinding.PodcastGridFragmentBinding
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.PodcastListViewState
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class PodcastGridFragment : PodcastGridListFragment() {
    companion object {
        fun newInstance(networkLoadableList: NetworkLoadableList): PodcastGridFragment {
            return PodcastGridFragment().apply {
                arguments = newInstanceBundle(networkLoadableList)
            }
        }

        fun newInstance(listId: String, title: String? = null, sourceView: SourceView? = null): PodcastGridFragment {
            return PodcastGridFragment().apply {
                arguments = newInstanceBundle(listId, title, sourceView)
            }
        }
    }

    private var binding: PodcastGridFragmentBinding? = null

    lateinit var adapter: GridListAdapter
    var feed: ListFeed? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = PodcastGridFragmentBinding.inflate(inflater, container, false)

        viewModel.load(
            sourceUrl = arguments?.getString(ARG_SOURCE_URL),
            listStyle = expandedStyle,
            authenticated = arguments?.getBoolean(ARG_AUTHENTICATED),
        )
        viewModel.state.observe(
            viewLifecycleOwner,
            Observer { state ->
                val binding = binding ?: return@Observer
                when (state) {
                    is PodcastListViewState.Loading -> {
                        binding.loading.isVisible = true
                        binding.errorLayout.isVisible = false
                        binding.mainNestedScrollView.isVisible = false
                    }

                    is PodcastListViewState.ListLoaded -> {
                        binding.loading.isVisible = false
                        binding.errorLayout.isVisible = false
                        binding.mainNestedScrollView.isVisible = true

                        feed = state.feed
                        feed?.let {
                            // a network page always gets the header, whatever summary_style its Discover row carried
                            val showHeader = isNetworkPage || displayStyle.toString() == DisplayStyle.CollectionList().toString()
                            if (!showHeader) {
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
                                    toolbar = binding.toolbar,
                                )
                            }
                        }

                        adapter.submitList(state.feed.podcasts)
                    }

                    is PodcastListViewState.Error -> {
                        Timber.e("Could not load feed ${state.error.message}")
                        // a refresh that fails leaves the already rendered list alone rather than replacing it
                        val hasContent = feed != null
                        binding.loading.isVisible = false
                        binding.errorLayout.isVisible = !hasContent
                        binding.mainNestedScrollView.isVisible = hasContent
                        binding.btnRetry.setOnClickListener { viewModel.retry() }
                    }
                }
            },
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.bottomInset.collect {
                    binding?.mainNestedScrollView?.updatePadding(bottom = it)
                }
            }
        }

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        // the fragment outlives its view on the back stack, so a stale feed must not pass for rendered content
        feed = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        // a network's header title only arrives with the feed, so the caller's title fills the nav bar until then
        val title = when {
            isNetworkPage -> arguments?.getString(ARG_TITLE).orEmpty()
            displayStyle.toString() != DisplayStyle.CollectionList().toString() -> arguments?.getString(ARG_TITLE)
            else -> ""
        }

        val toolbar = binding.toolbar
        setupToolbarAndStatusBar(
            toolbar = toolbar,
            title = title,
            menu = R.menu.discover_share,
            navigationIcon = BackArrow,
        )
        toolbar.setOnMenuItemClickListener(this)

        val recyclerView = binding.recyclerView
        val columnCount = UiUtil.getDiscoverGridColumnCount(context = recyclerView.context)
        recyclerView.layoutManager = GridLayoutManager(context, columnCount)

        recyclerView.addItemDecoration(SpaceItemDecoration())
        val imageSize = UiUtil.getDiscoverGridImageWidthPx(context = recyclerView.context)
        adapter = GridListAdapter(onPodcastClicked, onPodcastSubscribe, imageSize)
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
