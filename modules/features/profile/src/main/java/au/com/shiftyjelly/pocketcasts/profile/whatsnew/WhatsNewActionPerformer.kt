package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.settings.SettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.activity.WebViewActivity
import au.com.shiftyjelly.pocketcasts.views.R as VR

internal fun Fragment.performWhatsNewAction(event: WhatsNewActionEvent, url: String?) {
    val host = activity as? FragmentHostListener ?: return
    when (event) {
        WhatsNewActionEvent.OpenPodcasts -> host.openTab(VR.id.navigation_podcasts)

        WhatsNewActionEvent.OpenDiscover -> host.openTab(VR.id.navigation_discover)

        WhatsNewActionEvent.OpenUpNext -> host.openTab(VR.id.navigation_upnext)

        WhatsNewActionEvent.OpenPlaylists -> host.openTab(VR.id.navigation_filters)

        WhatsNewActionEvent.OpenProfile -> host.closeProfileToRoot()

        WhatsNewActionEvent.OpenSettings -> host.addFragment(SettingsFragment())

        WhatsNewActionEvent.OpenUpsell -> OnboardingLauncher.openOnboardingFlow(
            requireActivity(),
            OnboardingFlow.Upsell(OnboardingUpgradeSource.WHATS_NEW),
        )

        WhatsNewActionEvent.CreatePlaylist -> host.openCreatePlaylist()

        WhatsNewActionEvent.OpenLink -> url?.let { openWhatsNewLink(host, it) }

        WhatsNewActionEvent.OpenNetworks -> host.openNetworks()

        WhatsNewActionEvent.OpenFiles -> host.openCloudFiles()
    }
}

private fun Fragment.openWhatsNewLink(host: FragmentHostListener, url: String) {
    if (!host.openInAppDeepLink(url)) {
        WebViewActivity.show(context, url.toUri().host.orEmpty(), url)
    }
}
