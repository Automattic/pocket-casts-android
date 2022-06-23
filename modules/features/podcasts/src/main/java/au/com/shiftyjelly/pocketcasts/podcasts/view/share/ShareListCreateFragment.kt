package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.FragmentSharePodcastsBinding
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.ShareServerManager
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.utils.StringUtil
import au.com.shiftyjelly.pocketcasts.views.extensions.tintIcons
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class ShareListCreateFragment : BaseFragment(), SharePodcastAdapter.ClickListener, View.OnClickListener, Toolbar.OnMenuItemClickListener {

    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var shareServerManager: ShareServerManager
    @Inject lateinit var settings: Settings

    private lateinit var adapter: SharePodcastAdapter
    private val viewModel: ShareListCreateViewModel by viewModels()
    private var layoutManager: GridLayoutManager? = null
    private var shareSent: Boolean = false
    private var binding: FragmentSharePodcastsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSharePodcastsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return
        val titleText = binding.titleText
        val descriptionText = binding.descriptionText
        val selectAllButton = binding.selectAllButton
        val recyclerView = binding.recyclerView

        // use opacity to blend component with background
        titleText.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(0x1F000000, BlendModeCompat.SRC_IN)
        descriptionText.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(0x1F000000, BlendModeCompat.SRC_IN)

        selectAllButton.setOnClickListener(this)

        adapter = SharePodcastAdapter(view.context, this)
        layoutManager = GridLayoutManager(activity, 4)
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
        recyclerView.isNestedScrollingEnabled = false
        titleText.requestFocus()

        binding.toolbar.let {
            it.inflateMenu(R.menu.share_podcasts_menu)
            it.setOnMenuItemClickListener(this)
            it.setNavigationOnClickListener { activity?.finish() }
            val iconTint = it.context.getThemeColor(UR.attr.secondary_icon_01)
            it.menu.tintIcons(iconTint)
            it.navigationIcon?.setTint(iconTint)
        }

        viewModel.podcastsLive.observe(viewLifecycleOwner) { podcasts ->
            adapter.submitList(podcasts)
            adapter.notifyDataSetChanged()
        }

        viewModel.selectedUuidsLive.observe(viewLifecycleOwner) { selectedUuids ->
            if (selectedUuids != null) {
                adapter.selectedPodcastUuids = selectedUuids
                adapter.notifyDataSetChanged()
                updateSelectedCount(selectedUuids)
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.share_podcasts -> sharePodcasts()
        }
        return true
    }
    override fun onResume() {
        super.onResume()

        // user returning from external sharing app
        if (shareSent) {
            activity?.finish()
        }
    }

    @Suppress("DEPRECATION")
    private fun sharePodcasts() {
        val binding = binding ?: return
        val titleText = binding.titleText
        val descriptionText = binding.descriptionText

        UiUtil.hideKeyboard(titleText)

        val title = titleText.text.toString()
        val description = descriptionText.text.toString()

        val selectedPodcasts = viewModel.getSelectedPodcasts()

        if (StringUtil.isBlank(title)) {
            UiUtil.displayAlert(
                context = titleText.context,
                title = getString(LR.string.podcasts_share_create_error),
                message = getString(LR.string.podcasts_share_missing_title),
                onComplete = null
            )
            return
        }

        if (selectedPodcasts.isEmpty()) {
            UiUtil.displayAlert(
                context = titleText.context,
                title = getString(LR.string.podcasts_share_create_error),
                message = getString(LR.string.podcasts_share_missing_podcast),
                onComplete = null
            )
            return
        }

        val progressDialog = android.app.ProgressDialog(titleText.context)
        progressDialog.setMessage(getString(LR.string.podcasts_share_generating_list))
        progressDialog.show()

        shareServerManager.sharePodcastList(
            title, description, selectedPodcasts,
            object : ShareServerManager.SendPodcastListCallback {
                override fun onSuccess(url: String) {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, url)
                    try {
                        startActivity(Intent.createChooser(intent, getString(LR.string.podcasts_share_via)))
                    } catch (e: IllegalStateException) {
                        // Not attached to activity anymore
                    }

                    UiUtil.hideProgressDialog(progressDialog)
                    shareSent = true
                }

                override fun onFailed() {
                    UiUtil.hideProgressDialog(progressDialog)
                    UiUtil.displayAlert(titleText.context, getString(LR.string.podcasts_share_failed_title), getString(LR.string.podcasts_share_failed), null)
                }
            }
        )
    }

    override fun onPodcastSelected(podcast: Podcast) {
        val binding = binding ?: return

        viewModel.selectPodcast(podcast)
        binding.titleText.clearFocus()
        binding.descriptionText.clearFocus()
        UiUtil.hideKeyboard(binding.titleText)
    }

    override fun onPodcastUnselected(podcast: Podcast) {
        val binding = binding ?: return

        viewModel.unselectPodcast(podcast)
        binding.titleText.clearFocus()
        binding.descriptionText.clearFocus()
        UiUtil.hideKeyboard(binding.titleText)
    }

    private fun updateSelectedCount(selectedUuids: Set<String>) {
        val binding = binding ?: return
        val selectAllTick = binding.selectAllTick
        val selectAllButton = binding.selectAllButton

        binding.selectedCountText.text = getString(LR.string.podcasts_share_selected, selectedUuids.size)
        if (viewModel.allPodcastSelected) {
            if (!selectAllTick.isSelected) {
                selectAllTick.isSelected = true
            }
            selectAllButton.contentDescription = getString(LR.string.podcasts_share_all_podcasts_selected)
        } else {
            selectAllTick.isSelected = false
            selectAllButton.contentDescription = getString(LR.string.podcasts_share_action_select_all)
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.selectAllButton) {
            selectAll()
        }
    }

    private fun selectAll() {
        val binding = binding ?: return
        val selectAllTick = binding.selectAllTick
        if (selectAllTick.isSelected) {
            selectAllTick.isSelected = false
            viewModel.unselectAll()
        } else {
            selectAllTick.isSelected = true
            viewModel.selectAll()
        }
    }
}
