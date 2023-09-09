package au.com.shiftyjelly.pocketcasts.settings

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.settings.about.AboutFragment
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
    openFragment: (Fragment) -> Unit
) {
    val context = LocalContext.current
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings),
            bottomShadow = true,
            onNavigationClick = onBackPressed,
        )

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.theme.colors.primaryUi02)
                .padding(vertical = 8.dp)
        ) {
            if (isDebug) {
                DeveloperRow(onClick = { openFragment(au.com.shiftyjelly.pocketcasts.settings.developer.DeveloperFragment()) })
            }

            if (isDebug) {
                BetaFeatures(onClick = { openFragment(BetaFeaturesFragment()) })
            }

            if (!isUnrestrictedBattery && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BatteryOptimizationRow(onClick = { openFragment(BatteryRestrictionsSettingsFragment.newInstance(closeButton = false)) })
            }

            if (!signInState.isSignedIn || signInState.isSignedInAsFree) {
                PlusRow(onClick = {
                    if (FeatureFlag.isEnabled(Feature.ADD_PATRON_ENABLED)) {
                        OnboardingLauncher.openOnboardingFlow(
                            context.getActivity(),
                            OnboardingFlow.Upsell(
                                OnboardingUpgradeSource.SETTINGS
                            )
                        )
                    } else {
                        openFragment(PlusSettingsFragment())
                    }
                })
            }

            GeneralRow(onClick = { openFragment(PlaybackSettingsFragment()) })
            NotificationRow(onClick = { openFragment(NotificationsSettingsFragment()) })
            AppearanceRow(
                isSignedInAsPlusOrPatron = signInState.isSignedInAsPlusOrPatron,
                onClick = { openFragment(AppearanceSettingsFragment.newInstance()) }
            )
            StorageAndDataUseRow(onClick = { openFragment(StorageSettingsFragment()) })
            AutoArchiveRow(onClick = { openFragment(AutoArchiveFragment()) })
            AutoDownloadRow(onClick = { openFragment(AutoDownloadSettingsFragment.newInstance()) })
            AutoAddToUpNextRow(onClick = { openFragment(AutoAddSettingsFragment()) })
            if (FeatureFlag.isEnabled(Feature.BOOKMARKS_ENABLED)) {
                HeadphoneControlsRow(onClick = { openFragment(HeadphoneControlsSettingsFragment()) })
            }
            HelpAndFeedbackRow(
                onClick = {
                    val intent = Intent(context, HelpActivity::class.java)
                    context.startActivity(intent)
                }
            )
            ImportAndExportOpmlRow(onClick = { openFragment(ExportSettingsFragment()) })
            AdvancedRow(onClick = { openFragment(AdvancedSettingsFragment()) })
            PrivacyRow(onClick = { openFragment(PrivacyFragment()) })
            AboutRow(onClick = { openFragment(AboutFragment()) })
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
        modifier = rowModifier(onClick)
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
        modifier = rowModifier(onClick)
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
        modifier = rowModifier(onClick)
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
        modifier = rowModifier(onClick)
    )
}

@Composable
private fun GeneralRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_playback),
        icon = painterResource(IR.drawable.ic_profile_settings),
        modifier = rowModifier(onClick)
    )
}

@Composable
private fun NotificationRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_notifications),
        icon = painterResource(SR.drawable.settings_notifications),
        modifier = rowModifier(onClick)
    )
}

@Composable
private fun AppearanceRow(isSignedInAsPlusOrPatron: Boolean, onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_appearance),
        icon = painterResource(SR.drawable.settings_appearance),
        primaryTextEndDrawable = if (isSignedInAsPlusOrPatron) null else IR.drawable.ic_plus,
        modifier = rowModifier(onClick)
    )
}

@Composable
private fun StorageAndDataUseRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_storage),
        icon = painterResource(SR.drawable.settings_storage),
        modifier = rowModifier(onClick)
    )
}

@Composable
private fun AutoArchiveRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_auto_archive),
        icon = painterResource(SR.drawable.settings_auto_archive),
        modifier = rowModifier(onClick)
    )
}

@Composable
private fun AutoDownloadRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_auto_download),
        icon = painterResource(SR.drawable.settings_auto_download),
        modifier = rowModifier(onClick)
    )
}

@Composable
private fun AutoAddToUpNextRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_auto_add_to_up_next),
        icon = painterResource(IR.drawable.ic_upnext_playlast),
        modifier = rowModifier(onClick)
    )
}

@Composable
private fun HeadphoneControlsRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_headphone_controls),
        icon = painterResource(IR.drawable.ic_headphone),
        modifier = rowModifier(onClick)
    )
}

@Composable
private fun HelpAndFeedbackRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_help),
        icon = painterResource(SR.drawable.settings_help),
        modifier = rowModifier(onClick)
    )
}

@Composable
private fun ImportAndExportOpmlRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_import_export),
        icon = painterResource(SR.drawable.settings_import_export),
        modifier = rowModifier(onClick)
    )
}

@Composable
private fun PrivacyRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_privacy),
        icon = painterResource(SR.drawable.whatsnew_privacy),
        modifier = rowModifier(onClick)
    )
}

@Composable
private fun AboutRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_about),
        icon = painterResource(SR.drawable.settings_about),
        modifier = rowModifier(onClick)
    )
}

@Composable
private fun AdvancedRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_title_advanced),
        icon = painterResource(SR.drawable.settings_advanced),
        modifier = rowModifier(onClick)
    )
}

private fun rowModifier(onClick: () -> Unit) =
    Modifier
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
            openFragment = {}
        )
    }
}
