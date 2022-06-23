package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.FragmentShareIncomingBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeTintedDrawable
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.hide
import au.com.shiftyjelly.pocketcasts.views.extensions.show
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class ShareListIncomingFragment : BaseFragment(), ShareListIncomingAdapter.ClickListener {

    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var serverManager: ServerManager
    @Inject lateinit var settings: Settings

    private lateinit var adapter: ShareListIncomingAdapter
    private val viewModel: ShareListIncomingViewModel by viewModels()
    private var binding: FragmentShareIncomingBinding? = null

    companion object {
        const val EXTRA_URL = "EXTRA_URL"

        fun newInstance(url: String): ShareListIncomingFragment {
            return ShareListIncomingFragment().apply {
                arguments = bundleOf(
                    EXTRA_URL to url
                )
            }
        }
    }

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

        val toolbar = binding.toolbar
        toolbar.setNavigationOnClickListener { activity?.onBackPressed() }
        toolbar.navigationIcon = context.getThemeTintedDrawable(IR.drawable.ic_cancel, UR.attr.secondary_icon_01)

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
    }

    override fun onPodcastClick(podcast: Podcast) {
        val fragment = PodcastFragment.newInstance(podcast.uuid)
        (activity as FragmentHostListener).addFragment(fragment)
    }

    override fun onSubscribeClick(podcast: Podcast) {
        viewModel.subscribeToPodcast(podcast.uuid)
    }

    override fun onUnsubscribeClick(podcast: Podcast) {
        val uuid = podcast.uuid
        val dialog = OptionsDialog()
            .setTitle(getString(LR.string.podcasts_unsubscribe_are_you_sure))
            .addTextOption(
                titleId = LR.string.unsubscribe,
                click = {
                    viewModel.unsubscribeFromPodcast(uuid)
                }
            )
        activity?.supportFragmentManager?.let {
            dialog.show(it, "unsubscribe")
        }
    }

    override fun onSubscribeToAllClick(podcasts: List<Podcast>) {
        for (podcastHeader in podcasts) {
            val uuid = podcastHeader.uuid
            viewModel.subscribeToPodcast(uuid)
        }
    }
}
