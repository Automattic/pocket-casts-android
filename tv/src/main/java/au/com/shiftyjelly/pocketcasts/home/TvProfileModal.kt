package au.com.shiftyjelly.pocketcasts.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.BuildConfig
import au.com.shiftyjelly.pocketcasts.component.TvModal
import au.com.shiftyjelly.pocketcasts.component.TvModalButton
import au.com.shiftyjelly.pocketcasts.component.TvModalSurface
import au.com.shiftyjelly.pocketcasts.compose.images.GravatarProfileImage
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvProfileModal(
    profile: TvProfileState,
    onDismissRequest: () -> Unit,
    onLogIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onStarredEpisodes: () -> Unit,
    onListeningHistory: () -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvModal(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        TvProfileModalContent(
            profile = profile,
            onLogIn = onLogIn,
            onCreateAccount = onCreateAccount,
            onStarredEpisodes = onStarredEpisodes,
            onListeningHistory = onListeningHistory,
            onLogOut = onLogOut,
        )
    }
}

@Composable
private fun ColumnScope.TvProfileModalContent(
    profile: TvProfileState,
    onLogIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onStarredEpisodes: () -> Unit,
    onListeningHistory: () -> Unit,
    onLogOut: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(profile is TvProfileState.SignedIn) {
        focusRequester.requestFocus()
    }

    when (profile) {
        is TvProfileState.SignedIn -> {
            TvProfileModalAvatar(email = profile.email)
            if (profile.email != null) {
                Text(
                    text = profile.email,
                    color = MaterialTheme.tvColors.textPrimary,
                    style = MaterialTheme.tvTypography.headline.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
            TvModalButton(
                text = stringResource(LR.string.tv_profile_starred_episodes),
                onClick = onStarredEpisodes,
                modifier = Modifier.focusRequester(focusRequester),
            )
            TvModalButton(
                text = stringResource(LR.string.profile_navigation_listening_history),
                onClick = onListeningHistory,
            )
            TvModalButton(
                text = stringResource(LR.string.log_out),
                onClick = onLogOut,
            )
        }

        is TvProfileState.SignedOut -> {
            TvModalButton(
                text = stringResource(LR.string.log_in),
                onClick = onLogIn,
                modifier = Modifier.focusRequester(focusRequester),
            )
            TvModalButton(
                text = stringResource(LR.string.create_account),
                onClick = onCreateAccount,
            )
        }
    }

    Text(
        text = stringResource(
            LR.string.settings_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE.toString(),
        ),
        color = MaterialTheme.tvColors.textSecondary,
        style = MaterialTheme.tvTypography.caption1,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun TvProfileModalAvatar(email: String?, modifier: Modifier = Modifier) {
    val avatarModifier = modifier
        .size(AvatarSize)
        .clip(CircleShape)
    if (email != null) {
        GravatarProfileImage(
            email = email,
            contentDescription = null,
            // The placeholder composes outside the sized image slot, so it needs its own size.
            placeholder = { TvProfileModalAvatarPlaceholder(modifier = Modifier.size(AvatarSize)) },
            modifier = avatarModifier,
        )
    } else {
        TvProfileModalAvatarPlaceholder(modifier = avatarModifier)
    }
}

@Composable
private fun TvProfileModalAvatarPlaceholder(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(MaterialTheme.tvColors.backgroundOverlay, CircleShape),
    ) {
        Icon(
            painter = painterResource(IR.drawable.ic_profile),
            contentDescription = null,
            tint = MaterialTheme.tvColors.textPrimary,
            modifier = Modifier.size(48.dp),
        )
    }
}

private val AvatarSize = 107.dp

@Preview
@Composable
private fun TvProfileModalSignedOutPreview() {
    TvTheme {
        TvModalSurface {
            TvProfileModalContent(
                profile = TvProfileState.SignedOut,
                onLogIn = {},
                onCreateAccount = {},
                onStarredEpisodes = {},
                onListeningHistory = {},
                onLogOut = {},
            )
        }
    }
}

@Preview
@Composable
private fun TvProfileModalSignedInPreview() {
    TvTheme {
        TvModalSurface {
            TvProfileModalContent(
                profile = TvProfileState.SignedIn(email = "user@example.com"),
                onLogIn = {},
                onCreateAccount = {},
                onStarredEpisodes = {},
                onListeningHistory = {},
                onLogOut = {},
            )
        }
    }
}
