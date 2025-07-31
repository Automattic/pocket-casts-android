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
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.ad.AdReportContent
import au.com.shiftyjelly.pocketcasts.compose.ad.BlazeAd
import au.com.shiftyjelly.pocketcasts.compose.ad.rememberAdColors
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import com.google.android.material.R as MR

@AndroidEntryPoint
class AdReportFragment : BaseDialogFragment() {
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
                    onReportAd = { reason -> Timber.i("Report ad: $reason, ${args.ad.id}") },
                )

                val surfaceColor = sheetColors.surface
                LaunchedEffect(surfaceColor) {
                    refreshSystemColors(surfaceColor)
                }
            }
        }
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
