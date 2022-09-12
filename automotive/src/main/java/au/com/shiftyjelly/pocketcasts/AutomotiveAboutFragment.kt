package au.com.shiftyjelly.pocketcasts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.compose.AutomotiveTheme
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.BuildConfig
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeDrawable
import au.com.shiftyjelly.pocketcasts.ui.extensions.startActivityViewUrl
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class AutomotiveAboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AutomotiveTheme {
                    AboutPage(onOpenLicenses = { openLicenses() })
                }
            }
        }
    }

    private fun openLicenses() {
        (activity as? FragmentHostListener)?.addFragment(AutomotiveLicensesFragment())
    }
}

@Composable
private fun AboutPage(onOpenLicenses: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.verticalScroll(scrollState)
    ) {
        Image(
            painter = painterResource(context.getThemeDrawable(UR.attr.logo_title_vertical)),
            contentDescription = stringResource(R.string.settings_app_icon),
            modifier = Modifier
                .padding(top = 56.dp)
                .size(width = 220.dp, height = 132.dp)
        )
        Text(
            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString()),
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.theme.colors.primaryText02
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 56.dp, bottom = 16.dp)
        )
        Text(
            text = stringResource(R.string.settings_about_legal),
            fontSize = 24.sp,
            fontWeight = FontWeight(500),
            letterSpacing = 0.25.sp,
            color = MaterialTheme.theme.colors.primaryInteractive01,
            modifier = Modifier.align(Alignment.Start).padding(horizontal = 48.dp, vertical = 16.dp)
        )
        TextLinkButton(
            text = stringResource(R.string.settings_about_terms_of_serivce),
            onClick = { context.startActivityViewUrl(Settings.INFO_TOS_URL) }
        )
        TextLinkButton(
            text = stringResource(R.string.settings_about_privacy_policy),
            onClick = { context.startActivityViewUrl(Settings.INFO_PRIVACY_URL) }
        )
        TextLinkButton(
            text = stringResource(R.string.settings_about_acknowledgements),
            onClick = { onOpenLicenses() }
        )
        Spacer(Modifier.height(15.dp))
    }
}

@Composable
private fun TextLinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 48.dp, vertical = 24.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            fontSize = 32.sp,
            color = MaterialTheme.theme.colors.primaryText01
        )
    }
}

@Composable
@Preview
private fun AboutPageRow() {
    AboutPage(onOpenLicenses = {})
}
