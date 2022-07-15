package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralEpisodes
import au.com.shiftyjelly.pocketcasts.settings.databinding.FragmentManualcleanupBinding
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.ManualCleanupViewModel
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.extensions.findToolbar
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val ARG_TOOLBAR = "showtoolbar"

@AndroidEntryPoint
class ManualCleanupFragment : BaseFragment() {
    companion object {
        fun newInstance(showToolbar: Boolean = false): ManualCleanupFragment {
            val fragment = ManualCleanupFragment()
            fragment.arguments = bundleOf(
                ARG_TOOLBAR to showToolbar
            )
            return fragment
        }
    }

    private var binding: FragmentManualcleanupBinding? = null
    private val viewModel: ManualCleanupViewModel by viewModels()
    private val showToolbar: Boolean
        get() = arguments?.getBoolean(ARG_TOOLBAR) ?: false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        binding = FragmentManualcleanupBinding.inflate(inflater, container, false)
//        return binding?.root
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme(theme.activeTheme) {
                    ManualCleanupPage(
                        viewModel = viewModel,
                        showToolbar = showToolbar,
                        onBackClick = { activity?.onBackPressed() },
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!showToolbar) {
            parentFragment?.view?.findToolbar()?.title =
                getString(LR.string.settings_title_manage_downloads)
        }
//        val fragmentBinding = this.binding ?: return
//        with(fragmentBinding) {
//            setupUi()
//            setupObservers()
//        }
    }

    private fun FragmentManualcleanupBinding.setupUi() {
        unplayed.setup(LR.string.unplayed, "")
        inProgress.setup(LR.string.in_progress, "")
        played.setup(LR.string.played, "")
        switchStarred.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onStarredSwitchClicked(isChecked)
        }
    }

    private fun FragmentManualcleanupBinding.setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.state.collect {
                it.unplayedDiskSpaceView.update(unplayed)
                it.inProgressDiskSpaceView.update(inProgress)
                it.playedDiskSpaceView.update(played)
                lblTotal.text = Util.formattedBytes(
                    bytes = it.totalDownloadSize,
                    context = lblTotal.context
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.snackbarMessage.collect { showSnackbar(it) }
        }
    }

    private fun ManualCleanupViewModel.State.DiskSpaceView.update(
        view: DiskSpaceSizeView
    ) {
        val context = context ?: return
        val byteString = Util.formattedBytes(bytes = episodesBytesSize, context = context)
        val subtitle = "${resources.getStringPluralEpisodes(episodesSize)} · $byteString"
        view.update(subtitle)
        viewModel.updateDeleteList(view.isChecked, episodes)
        view.onCheckedChanged =
            { isChecked -> viewModel.onDiskSpaceCheckedChanged(isChecked, episodes) }
    }

    private fun FragmentManualcleanupBinding.showSnackbar(@StringRes stringResId: Int) {
        Toast.makeText(btnDelete.context, stringResId, Toast.LENGTH_SHORT).show()
    }
}
