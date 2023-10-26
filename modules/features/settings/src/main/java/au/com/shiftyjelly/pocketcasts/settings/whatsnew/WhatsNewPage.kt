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
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

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
                        is WhatsNewFeature.AutoPlay -> AutoPlayHeader()
                        is WhatsNewFeature.Bookmarks -> BookmarksHeader(onClose)
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
                    text = getButtonTitle(state),
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
private fun getButtonTitle(
    state: UiState.Loaded,
): String = when (state.feature) {
    WhatsNewFeature.AutoPlay -> stringResource(state.feature.confirmButtonTitle)
    is WhatsNewFeature.Bookmarks -> {
        when {
            state.feature.isUserEntitled -> stringResource(state.feature.confirmButtonTitle)
            state.feature.hasFreeTrial -> stringResource(LR.string.profile_start_free_trial)
            else -> {
                if (state.feature.subscriptionTier != null) {
                    stringResource(
                        LR.string.upgrade_to,
                        when (state.feature.subscriptionTier) {
                            Subscription.SubscriptionTier.PATRON -> stringResource(LR.string.pocket_casts_patron_short)
                            Subscription.SubscriptionTier.PLUS -> stringResource(LR.string.pocket_casts_plus_short)
                            Subscription.SubscriptionTier.UNKNOWN -> stringResource(LR.string.pocket_casts_plus_short)
                        }
                    )
                } else {
                    Timber.e("Subscription tier is null. This should not happen when user is not entitled to a feature.")
                    ""
                }
            }
        }
    }
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
                feature = WhatsNewFeature.Bookmarks(
                    title = LR.string.whats_new_bookmarks_title,
                    message = LR.string.whats_new_bookmarks_body,
                    confirmButtonTitle = LR.string.whats_new_bookmarks_try_now_button,
                    hasFreeTrial = false,
                    isUserEntitled = true,
                    subscriptionTier = Subscription.SubscriptionTier.PLUS,
                ),
                tier = UserTier.Plus,
            ),
            header = { BookmarksHeader(onClose = {}) },
            onConfirm = {},
            onClose = {}
        )
    }
}
