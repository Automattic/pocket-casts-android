package au.com.shiftyjelly.pocketcasts.settings.plus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.UpgradeAccountViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.activity.WebViewActivity
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class PlusUpgradeFragment : BaseDialogFragment() {
    companion object {
        private const val EXTRA_FEATURE_BLOCKED = "extra_feature_blocked"

        fun newInstance(featureBlocked: Boolean): PlusUpgradeFragment {
            return PlusUpgradeFragment().apply {
                // if the upgrade dialog is shown after clicking a Plus feature the title will be "This feature requires Pocket Casts Plus" otherwise "Help support Pocket Casts"
                arguments = bundleOf(EXTRA_FEATURE_BLOCKED to featureBlocked)
            }
        }
    }

    @Inject lateinit var settings: Settings

    private val viewModel: UpgradeAccountViewModel by viewModels()

    private val featureBlocked: Boolean
        get() = arguments?.getBoolean(EXTRA_FEATURE_BLOCKED) ?: true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme(theme.activeTheme) {
                    PlusUpgradePage(
                        onCloseClick = { dismiss() },
                        onUpgradeClick = { upgrade() },
                        onLearnMoreClick = { openLearnMore() },
                        storageLimitGb = settings.getCustomStorageLimitGb(),
                        featureBlocked = featureBlocked,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    private fun openLearnMore() {
        WebViewActivity.show(context, getString(LR.string.learn_more), Settings.INFO_LEARN_MORE_URL)
    }

    private fun upgrade() {
        // Check we are in the right module, if not we will have to deeplink in to the upgrade process
        val fragmentHost = activity as? FragmentHostListener
        if (fragmentHost != null) {
            fragmentHost.showAccountUpgradeNow(autoSelectPlus = true)
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(Settings.INTENT_LINK_UPGRADE)
            startActivity(intent)
        }
        dialog?.dismiss()
    }
}
