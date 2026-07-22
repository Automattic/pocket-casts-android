package au.com.shiftyjelly.pocketcasts.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.BuildConfig
import au.com.shiftyjelly.pocketcasts.component.TvModal
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.images.GravatarProfileImage
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
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
            GravatarProfileImage(
                email = profile.email,
                contentDescription = null,
                placeholder = { TvProfileModalAvatarPlaceholder() },
                modifier = Modifier
                    .size(107.dp)
                    .clip(CircleShape),
            )
            Text(
                text = profile.email,
                color = Color.White,
                style = TvTextStyles.ModalEmail,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            TvProfileModalButton(
                text = stringResource(LR.string.tv_profile_starred_episodes),
                onClick = onStarredEpisodes,
                modifier = Modifier.focusRequester(focusRequester),
            )
            TvProfileModalButton(
                text = stringResource(LR.string.profile_navigation_listening_history),
                onClick = onListeningHistory,
            )
            TvProfileModalButton(
                text = stringResource(LR.string.log_out),
                onClick = onLogOut,
            )
        }

        is TvProfileState.SignedOut -> {
            TvProfileModalButton(
                text = stringResource(LR.string.log_in),
                onClick = onLogIn,
                modifier = Modifier.focusRequester(focusRequester),
            )
            TvProfileModalButton(
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
        color = TvColors.TextSecondary,
        style = TvTextStyles.ModalFootnote,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun TvProfileModalAvatarPlaceholder(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Gray, CircleShape),
    ) {
        Icon(
            painter = painterResource(IR.drawable.ic_profile),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(48.dp),
        )
    }
}

@Composable
private fun TvProfileModalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        colors = TvButtonDefaults.filledButtonColors(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun TvProfileModalSignedOutPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .background(TvColors.Dark)
                    .padding(horizontal = 53.dp, vertical = 40.dp),
            ) {
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
}

@Preview
@Composable
private fun TvProfileModalSignedInPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .background(TvColors.Dark)
                    .padding(horizontal = 53.dp, vertical = 40.dp),
            ) {
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
}
