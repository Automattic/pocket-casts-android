package au.com.shiftyjelly.pocketcasts.ads

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.ad.AdReportContent
import au.com.shiftyjelly.pocketcasts.compose.ad.rememberAdColors
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import au.com.shiftyjelly.pocketcasts.models.type.AdReportReason
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import com.google.android.material.R as MR

@AndroidEntryPoint
class AdReportFragment : BaseDialogFragment() {

    @Inject
    internal lateinit var analyticsTracker: AnalyticsTracker

    companion object {
        const val NEW_INSTANCE_KEY = "new_instance_key"

        fun newInstance(
            ad: BlazeAd,
            podcastColors: PodcastColors?,
        ) = AdReportFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_KEY to Args(ad, podcastColors))
        }
    }

    private val args
        get() = requireNotNull(BundleCompat.getParcelable(requireArguments(), NEW_INSTANCE_KEY, Args::class.java)) {
            "Missing input parameters"
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        DialogBox(
            fillMaxHeight = false,
            useThemeBackground = false,
        ) {
            CompositionLocalProvider(LocalPodcastColors provides args.podcastColors) {
                val sheetColors = rememberAdColors().reportSheet
                AdReportContent(
                    colors = sheetColors,
                    onClickRemoveAds = {
                        OnboardingLauncher.openOnboardingFlow(requireActivity(), OnboardingFlow.Upsell(OnboardingUpgradeSource.BANNER_AD))
                        dismiss()
                    },
                    onReportAd = ::reportAd,
                )

                val surfaceColor = sheetColors.surface
                LaunchedEffect(surfaceColor) {
                    refreshSystemColors(surfaceColor)
                }
            }
        }
    }

    private fun reportAd(reason: AdReportReason) {
        analyticsTracker.trackBannerAdReport(id = args.ad.id, reason = reason.analyticsName, location = args.ad.location.value)

        val snackbarView = (requireActivity() as FragmentHostListener).snackBarView()
        Snackbar.make(snackbarView, getString(LR.string.ad_report_confirmation), Snackbar.LENGTH_LONG).show()
        dismiss()
    }

    private fun refreshSystemColors(color: Color) {
        val argbColor = color.toArgb()
        val bottomSheet = requireDialog().findViewById<View>(MR.id.design_bottom_sheet)
        bottomSheet.backgroundTintList = ColorStateList.valueOf(argbColor)
    }

    @Parcelize
    private class Args(
        val ad: BlazeAd,
        val podcastColors: PodcastColors?,
    ) : Parcelable
}
