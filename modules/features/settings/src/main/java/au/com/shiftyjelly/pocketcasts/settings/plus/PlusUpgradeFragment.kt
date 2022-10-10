package au.com.shiftyjelly.pocketcasts.settings.plus

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsPropValue
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.UpgradeAccountViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.activity.WebViewActivity
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class PlusUpgradeFragment : BaseDialogFragment() {

    sealed class UpgradePage(val promotionId: String, val promotionName: String, val featureBlocked: Boolean) {
        object Profile : UpgradePage(promotionId = "PROFILE", promotionName = "Upgrade to Plus from profile", featureBlocked = false)
        object Files : UpgradePage(promotionId = "FILES", promotionName = "Upgrade to Plus for files", featureBlocked = true)
        object Folders : UpgradePage(promotionId = "FOLDERS", promotionName = "Upgrade to Plus for folders", featureBlocked = true)
        object Themes : UpgradePage(promotionId = "THEMES", promotionName = "Upgrade to Plus for themes", featureBlocked = true)
        object Icons : UpgradePage(promotionId = "ICONS", promotionName = "Upgrade to Plus for icons", featureBlocked = true)
        object Unknown : UpgradePage(promotionId = "UNKNOWN", promotionName = "Unknown", featureBlocked = false)

        companion object {
            fun fromString(value: String) = when (value) {
                Profile.promotionId -> Profile
                Files.promotionId -> Files
                Folders.promotionId -> Folders
                Themes.promotionId -> Themes
                Icons.promotionId -> Icons
                else -> Unknown
            }
        }
    }

    companion object {
        private const val EXTRA_START_PAGE = "extra_start_page"
        private const val SOURCE_KEY = "source"

        fun newInstance(upgradePage: UpgradePage): PlusUpgradeFragment {
            return PlusUpgradeFragment().apply {
                arguments = bundleOf(EXTRA_START_PAGE to upgradePage.promotionId)
            }
        }
    }

    @Inject lateinit var settings: Settings
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private val viewModel: UpgradeAccountViewModel by viewModels()

    private val upgradePage = UpgradePage.fromString(arguments?.getString(EXTRA_START_PAGE) ?: "")
    private val promotionSource = AnalyticsPropValue(upgradePage.promotionId.lowercase(Locale.ENGLISH))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppThemeWithBackground(theme.activeTheme) {
                    PlusUpgradePage(
                        onCloseClick = { closeUpgrade() },
                        onUpgradeClick = { acceptUpgrade() },
                        onLearnMoreClick = { openLearnMore() },
                        storageLimitGb = settings.getCustomStorageLimitGb(),
                        featureBlocked = upgradePage.featureBlocked,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        analyticsTracker.track(
            AnalyticsEvent.PLUS_PROMOTION_SHOWN,
            mapOf(SOURCE_KEY to promotionSource)
        )
        FirebaseAnalyticsTracker.plusUpgradeViewed(promotionId = upgradePage.promotionId, promotionName = upgradePage.promotionName)
    }

    private fun openLearnMore() {
        WebViewActivity.show(context, getString(LR.string.learn_more), Settings.INFO_LEARN_MORE_URL)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        analyticsTracker.track(
            AnalyticsEvent.PLUS_PROMOTION_DISMISSED,
            mapOf(SOURCE_KEY to promotionSource)
        )
        FirebaseAnalyticsTracker.plusUpgradeClosed(promotionId = upgradePage.promotionId, promotionName = upgradePage.promotionName)
    }

    private fun acceptUpgrade() {
        // Check we are in the right module, if not we will have to deeplink in to the upgrade process
        val fragmentHost = activity as? FragmentHostListener
        if (fragmentHost != null) {
            fragmentHost.showAccountUpgradeNow(autoSelectPlus = true)
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(Settings.INTENT_LINK_UPGRADE)
            startActivity(intent)
        }
        analyticsTracker.track(
            AnalyticsEvent.PLUS_PROMOTION_UPGRADE_BUTTON_TAPPED,
            mapOf(SOURCE_KEY to promotionSource)
        )
        FirebaseAnalyticsTracker.plusUpgradeConfirmed(
            promotionId = upgradePage.promotionId,
            promotionName = upgradePage.promotionName
        )
        dismiss()
    }

    private fun closeUpgrade() {
        analyticsTracker.track(
            AnalyticsEvent.PLUS_PROMOTION_DISMISSED,
            mapOf(SOURCE_KEY to promotionSource)
        )
        FirebaseAnalyticsTracker.plusUpgradeClosed(
            promotionId = upgradePage.promotionId,
            promotionName = upgradePage.promotionName
        )
        dismiss()
    }
}
