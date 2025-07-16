package au.com.shiftyjelly.pocketcasts.account.onboarding.import

import androidx.activity.SystemBarStyle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.SystemBarsStyles
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.singleAuto
import au.com.shiftyjelly.pocketcasts.compose.bars.transparent
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingImportOpmlUrl(
    isImporting: Boolean,
    onImport: (HttpUrl) -> Unit,
    onBackPress: () -> Unit,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = MaterialTheme.theme
    // Use secondaryUI01 so the status bar matches the ThemedTopAppBar
    val statusBar = SystemBarStyle.singleAuto(theme.colors.secondaryUi01) { theme.isDark }
    val navigationBar = SystemBarStyle.transparent { theme.isDark }
    onUpdateSystemBars(SystemBarsStyles(statusBar, navigationBar))

    var inputText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .windowInsetsPadding(WindowInsets.ime),
    ) {
        ThemedTopAppBar(
            onNavigationClick = onBackPress,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Image(
                painter = painterResource(IR.drawable.opml_from_url),
                contentDescription = null,
                modifier = Modifier.padding(top = 16.dp),
            )
            TextH10(
                text = stringResource(LR.string.onboarding_import_from_url),
            )

            AnimatedContent(
                targetState = isImporting,
            ) { importing ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (importing) {
                        TextP40(
                            text = stringResource(LR.string.onboarding_import_from_url_importing_message),
                        )
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        )
                    } else {
                        TextP40(
                            text = stringResource(LR.string.onboarding_import_from_url_description),
                        )
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { value ->
                                if (inputText != value) {
                                    showError = false
                                }
                                inputText = value
                            },
                            label = { Text("https://…") },
                            isError = showError,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        AnimatedVisibility(
                            visible = showError,
                        ) {
                            TextP40(
                                text = stringResource(LR.string.onboarding_import_from_url_error_message),
                                color = MaterialTheme.colors.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.weight(1f),
            )

            AnimatedVisibility(
                visible = !isImporting,
            ) {
                RowButton(
                    text = stringResource(LR.string.onboarding_recommendations_import),
                    onClick = {
                        val url = inputText.toHttpUrlOrNull()
                        if (url != null) {
                            onImport(url)
                        } else {
                            showError = true
                        }
                    },
                    includePadding = false,
                )
            }

            Spacer(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingImportOpmlUrlImportingPreview() {
    AppThemeWithBackground(themeType = Theme.ThemeType.LIGHT) {
        OnboardingImportOpmlUrl(
            isImporting = true,
            onImport = {},
            onBackPress = {},
            onUpdateSystemBars = {},
        )
    }
}

@Preview
@Composable
private fun OnboardingImportOpmlUrlPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType = themeType) {
        OnboardingImportOpmlUrl(
            isImporting = false,
            onImport = {},
            onBackPress = {},
            onUpdateSystemBars = {},
        )
    }
}
