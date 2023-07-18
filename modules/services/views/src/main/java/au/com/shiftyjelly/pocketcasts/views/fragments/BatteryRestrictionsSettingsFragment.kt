package au.com.shiftyjelly.pocketcasts.views.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.GradientIcon
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.SystemBatteryRestrictions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

private const val ARG_CLOSE_BUTTON = "close_button"

@AndroidEntryPoint
class BatteryRestrictionsSettingsFragment : BaseFragment() {
    companion object {
        fun newInstance(closeButton: Boolean) =
            BatteryRestrictionsSettingsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_CLOSE_BUTTON, closeButton)
                }
            }
    }

    @Inject
    lateinit var batteryRestrictions: SystemBatteryRestrictions

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                AppThemeWithBackground(theme.activeTheme) {

                    var isUnrestricted by remember { mutableStateOf(batteryRestrictions.isUnrestricted()) }
                    DisposableEffect(this) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                isUnrestricted = batteryRestrictions.isUnrestricted()
                            }
                        }

                        lifecycle.addObserver(observer)
                        onDispose {
                            lifecycle.removeObserver(observer)
                        }
                    }

                    val navigationButton = if (arguments?.getBoolean(ARG_CLOSE_BUTTON) == true) {
                        NavigationButton.Close
                    } else {
                        NavigationButton.Back
                    }
                    Page(
                        isUnrestricted = isUnrestricted,
                        navigationButton = navigationButton,
                        onBackPressed = {
                            @Suppress("DEPRECATION")
                            activity?.onBackPressed()
                        },
                        onClick = { batteryRestrictions.promptToUpdateBatteryRestriction(context) },
                        openUrl = { url ->
                            startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            )
                        }
                    )
                }
            }
        }
}

@Composable
private fun Page(
    isUnrestricted: Boolean,
    navigationButton: NavigationButton,
    onBackPressed: () -> Unit,
    onClick: () -> Unit,
    openUrl: (String) -> Unit
) {
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_battery),
            bottomShadow = true,
            navigationButton = navigationButton,
            onNavigationClick = onBackPressed
        )

        val startPadding = 72.dp

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.theme.colors.primaryInteractive01) //
                    .toggleable(
                        value = isUnrestricted,
                        onValueChange = { onClick() },
                        role = Role.Switch
                    )
                    .padding(start = startPadding, end = 16.dp)
            ) {
                TextH30(
                    text = stringResource(LR.string.settings_battery_unrestricted),
                    color = MaterialTheme.theme.colors.primaryInteractive02,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Spacer(
                    modifier = Modifier
                        .width(12.dp)
                        .weight(1f)
                )

                Switch(
                    checked = isUnrestricted,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.theme.colors.primaryInteractive02,
                        checkedTrackColor = MaterialTheme.theme.colors.primaryInteractive02,
                        uncheckedThumbColor = MaterialTheme.theme.colors.primaryInteractive02.copy(
                            alpha = 0.7f
                        ),
                        uncheckedTrackColor = MaterialTheme.theme.colors.primaryInteractive02,
                    )
                )
            }

            val learnMoreUrl = stringResource(LR.string.settings_battery_learn_more_url)
            Column(
                Modifier.clickable(
                    onClick = { openUrl(learnMoreUrl) },
                    onClickLabel = stringResource(LR.string.settings_battery_learn_more)
                )
            ) {
                Row(Modifier.padding(top = 16.dp, end = 16.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(startPadding)
                            .align(Alignment.CenterVertically)
                    ) {
                        GradientIcon(
                            painter = painterResource(VR.drawable.ic_outline_info_24),
                            colors = if (isUnrestricted) {
                                listOf(MaterialTheme.theme.colors.primaryText02)
                            } else {
                                listOf(
                                    MaterialTheme.theme.colors.gradient03A,
                                    MaterialTheme.theme.colors.gradient03E
                                )
                            }
                        )
                    }

                    val learnMoreString = stringResource(LR.string.learn_more)
                    TextP50(
                        text = buildAnnotatedString {
                            append(stringResource(LR.string.settings_battery_usage_message))
                            append(" ")
                            withStyle(SpanStyle(color = MaterialTheme.theme.colors.primaryInteractive01)) {
                                append(learnMoreString)
                            }
                        },
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.theme.colors.primaryText02,
                    )
                }

                if (!isUnrestricted) {
                    TextP50(
                        text = stringResource(
                            LR.string.settings_battery_update_message,
                            stringResource(LR.string.settings_battery_unrestricted)
                        ),
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.theme.colors.primaryText02,
                        modifier = Modifier.padding(
                            start = startPadding,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 16.dp
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PagePreview_restricted(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        Page(
            isUnrestricted = false,
            navigationButton = NavigationButton.Close,
            onBackPressed = {},
            onClick = {},
            openUrl = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PagePreview_unrestricted(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        Page(
            isUnrestricted = true,
            navigationButton = NavigationButton.Back,
            onBackPressed = {},
            onClick = {},
            openUrl = {}
        )
    }
}
