package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeDisplayMode
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeForTier
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.settings.whatsnew.WhatsNewViewModel.UiState
import au.com.shiftyjelly.pocketcasts.settings.whatsnew.WhatsNewViewModel.WhatsNewFeature
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier

@Composable
fun WhatsNewPage(
    viewModel: WhatsNewViewModel = hiltViewModel(),
    onConfirm: (WhatsNewViewModel.NavigationState) -> Unit,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    when (state) {
        is UiState.Loading -> Unit
        is UiState.Loaded -> {
            val uiState = state as UiState.Loaded
            WhatsNewPageLoaded(
                state = uiState,
                header = {
                    when (uiState.feature) {
                        WhatsNewFeature.AutoPlay -> AutoPlayHeader()
                        WhatsNewFeature.Bookmarks -> BookmarksHeader(onClose)
                    }
                },
                onConfirm = { viewModel.onConfirm() },
                onClose = onClose,
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationState
            .collect { navigationState ->
                onConfirm(navigationState)
            }
    }
}

@Composable
private fun WhatsNewPageLoaded(
    state: UiState.Loaded,
    header: @Composable () -> Unit,
    onConfirm: () -> Unit,
    onClose: () -> Unit,
) {
    var closing by remember { mutableStateOf(false) }
    val targetAlpha = if (closing) 0f else 0.66f
    val scrimAlpha: Float by animateFloatAsState(
        targetValue = targetAlpha,
        finishedListener = { onClose() }
    )

    val performClose = {
        closing = true
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = performClose,
            )
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .fillMaxSize()
    ) {
        Column(Modifier.background(MaterialTheme.theme.colors.primaryUi01)) {

            // Hide the header graphic if the phone is in landscape mode so there is room for the text
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                header()
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(all = 16.dp),
            ) {

                WhatsNewSubscriptionBadge(state.tier)

                Spacer(
                    modifier = Modifier.height(
                        if (state.tier == UserTier.Free) 0.dp else 16.dp
                    )
                )

                TextH20(
                    text = stringResource(id = state.feature.title),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.theme.colors.primaryText01,
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextP40(
                    text = stringResource(state.feature.message),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.theme.colors.primaryText02,
                )

                Spacer(modifier = Modifier.height(16.dp))

                RowButton(
                    text = stringResource(state.feature.confirmButtonTitle),
                    colors = when (state.feature) {
                        WhatsNewFeature.AutoPlay -> ButtonDefaults.buttonColors()
                        WhatsNewFeature.Bookmarks -> ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.theme.colors.primaryText01,
                            contentColor = MaterialTheme.theme.colors.primaryInteractive02
                        )
                    },
                    onClick = onConfirm,
                    includePadding = false,
                    modifier = Modifier
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                state.feature.closeButtonTitle?.let {
                    RowTextButton(
                        text = stringResource(it),
                        fontSize = 15.sp,
                        onClick = performClose,
                        includePadding = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun WhatsNewSubscriptionBadge(tier: UserTier) = when (tier) {
    UserTier.Patron -> SubscriptionBadgeForTier(
        tier = Subscription.SubscriptionTier.PATRON,
        displayMode = SubscriptionBadgeDisplayMode.Colored
    )
    UserTier.Plus -> SubscriptionBadgeForTier(
        tier = Subscription.SubscriptionTier.PLUS,
        displayMode = SubscriptionBadgeDisplayMode.Colored
    )
    UserTier.Free -> Box {}
}

@Composable
@Preview
private fun WhatsNewAutoPlayPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        WhatsNewPageLoaded(
            state = UiState.Loaded(
                feature = WhatsNewFeature.AutoPlay,
                tier = UserTier.Free,
            ),
            header = { AutoPlayHeader() },
            onConfirm = {},
            onClose = {}
        )
    }
}

@Composable
@Preview
private fun WhatsNewBookmarksPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        WhatsNewPageLoaded(
            state = UiState.Loaded(
                feature = WhatsNewFeature.Bookmarks,
                tier = UserTier.Plus,
            ),
            header = { BookmarksHeader(onClose = {}) },
            onConfirm = {},
            onClose = {}
        )
    }
}
