package au.com.shiftyjelly.pocketcasts.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.settings.about.AboutFragment
import au.com.shiftyjelly.pocketcasts.settings.developer.DeveloperFragment
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.settings.privacy.PrivacyFragment
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.views.fragments.BatteryRestrictionsSettingsFragment
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.settings.R as SR

@Composable
fun SettingsFragmentPage(
    signInState: SignInState,
    isDebug: Boolean,
    isUnrestrictedBattery: Boolean,
    onBackPressed: () -> Unit,
    openFragment: (Fragment) -> Unit,
    bottomInset: Dp,
) {
    val context = LocalContext.current
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings),
            bottomShadow = true,
            onNavigationClick = onBackPressed,
        )
        LazyColumn(
            contentPadding = PaddingValues(bottom = bottomInset),
            modifier = Modifier
                .background(MaterialTheme.theme.colors.primaryUi02)
                .padding(vertical = 8.dp),
        ) {
            if (isDebug) {
                item {
                    DeveloperRow(onClick = { openFragment(DeveloperFragment()) })
                }
            }

            if (isDebug) {
                item {
                    BetaFeatures(onClick = { openFragment(BetaFeaturesFragment()) })
                }
            }

            if (!isUnrestrictedBattery && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    BatteryOptimizationRow(onClick = { openFragment(BatteryRestrictionsSettingsFragment.newInstance(closeButton = false)) })
                }
            }

            if (!signInState.isSignedIn || signInState.isSignedInAsFree) {
                item {
                    PlusRow(onClick = {
                        OnboardingLauncher.openOnboardingFlow(
                            context.getActivity(),
                            OnboardingFlow.Upsell(
                                OnboardingUpgradeSource.SETTINGS,
                            ),
                        )
                    })
                }
            }
            item {
                GeneralRow(onClick = { openFragment(PlaybackSettingsFragment()) })
            }
            item {
                NotificationRow(onClick = { openFragment(NotificationsSettingsFragment()) })
            }
            item {
                AppearanceRow(
                    isSignedInAsPlusOrPatron = signInState.isSignedInAsPlusOrPatron,
                    onClick = { openFragment(AppearanceSettingsFragment.newInstance()) },
                )
            }
            item {
                StorageAndDataUseRow(onClick = { openFragment(StorageSettingsFragment()) })
            }
            item {
                AutoArchiveRow(onClick = { openFragment(AutoArchiveFragment()) })
            }
            item {
                AutoDownloadRow(onClick = { openFragment(AutoDownloadSettingsFragment.newInstance()) })
            }
            item {
                AutoAddToUpNextRow(onClick = { openFragment(AutoAddSettingsFragment()) })
            }
            item {
                HeadphoneControlsRow(onClick = { openFragment(HeadphoneControlsSettingsFragment()) })
            }
            item {
                ImportAndExportOpmlRow(onClick = { openFragment(ExportSettingsFragment()) })
            }
            item {
                AdvancedRow(onClick = { openFragment(AdvancedSettingsFragment()) })
            }
            item {
                PrivacyRow(onClick = { openFragment(PrivacyFragment()) })
            }
            item {
                AboutRow(onClick = { openFragment(AboutFragment()) })
            }
        }
    }
}

@Composable
private fun DeveloperRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_developer),
        icon = painterResource(SR.drawable.ic_developer_mode),
        iconGradientColors = listOf(
            MaterialTheme.theme.colors.gradient03A,
            MaterialTheme.theme.colors.gradient03E,
        ),
        modifier = Modifier.rowModifier(onClick),
    )
}

@Composable
private fun BetaFeatures(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_beta_features),
        icon = painterResource(IR.drawable.ic_science),
        iconGradientColors = listOf(
            MaterialTheme.theme.colors.gradient03A,
            MaterialTheme.theme.colors.gradient03E,
        ),
        modifier = Modifier.rowModifier(onClick),
    )
}

@Composable
private fun BatteryOptimizationRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_battery_settings),
        icon = painterResource(SR.drawable.ic_baseline_warning_amber_24),
        iconGradientColors = listOf(
            MaterialTheme.theme.colors.gradient03A,
            MaterialTheme.theme.colors.gradient03E,
        ),
        modifier = Modifier.rowModifier(onClick),
    )
}

@Composable
private fun PlusRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.pocket_casts_plus),
        icon = painterResource(IR.drawable.ic_plus),
        iconGradientColors = listOf(
            MaterialTheme.theme.colors.gradient01A,
            MaterialTheme.theme.colors.gradient01E,
        ),
        modifier = Modifier.rowModifier(onClick),
    )
}

@Composable
private fun GeneralRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_playback),
        icon = painterResource(IR.drawable.ic_profile_settings),
        modifier = Modifier.rowModifier(onClick),
    )
}

@Composable
private fun NotificationRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_notifications),
        icon = painterResource(SR.drawable.settings_notifications),
        modifier = Modifier.rowModifier(onClick),
    )
}

@Composable
private fun AppearanceRow(isSignedInAsPlusOrPatron: Boolean, onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_appearance),
        icon = painterResource(SR.drawable.settings_appearance),
        primaryTextEndDrawable = if (isSignedInAsPlusOrPatron) null else IR.drawable.ic_plus,
        modifier = Modifier.rowModifier(onClick),
    )
}

@Composable
private fun StorageAndDataUseRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_storage),
        icon = painterResource(SR.drawable.settings_storage),
        modifier = Modifier.rowModifier(onClick),
    )
}

@Composable
private fun AutoArchiveRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_auto_archive),
        icon = painterResource(SR.drawable.settings_auto_archive),
        modifier = Modifier.rowModifier(onClick),
    )
}

@Composable
private fun AutoDownloadRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_auto_download),
        icon = painterResource(SR.drawable.settings_auto_download),
        modifier = Modifier.rowModifier(onClick),
    )
}

@Composable
private fun AutoAddToUpNextRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_auto_add_to_up_next),
        icon = painterResource(IR.drawable.ic_upnext_playlast),
        modifier = Modifier.rowModifier(onClick),
    )
}

@Composable
private fun HeadphoneControlsRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_headphone_controls),
        icon = painterResource(IR.drawable.ic_headphone),
        modifier = Modifier.rowModifier(onClick),
    )
}

@Composable
private fun ImportAndExportOpmlRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_import_export),
        icon = painterResource(SR.drawable.settings_import_export),
        modifier = Modifier.rowModifier(onClick),
    )
}

@Composable
private fun PrivacyRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_privacy),
        icon = painterResource(SR.drawable.whatsnew_privacy),
        modifier = Modifier.rowModifier(onClick),
    )
}

@Composable
private fun AboutRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_about),
        icon = painterResource(SR.drawable.settings_about),
        modifier = Modifier.rowModifier(onClick),
    )
}

@Composable
private fun AdvancedRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_advanced),
        icon = painterResource(SR.drawable.settings_advanced),
        modifier = Modifier.rowModifier(onClick),
    )
}

fun Modifier.rowModifier(onClick: () -> Unit): Modifier =
    this
        .clickable { onClick() }
        .padding(vertical = 6.dp)

@Preview
@Composable
private fun SettingsPagePreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        SettingsFragmentPage(
            signInState = SignInState.SignedOut,
            isDebug = true,
            isUnrestrictedBattery = false,
            onBackPressed = {},
            openFragment = {},
            bottomInset = 0.dp,
        )
    }
}
