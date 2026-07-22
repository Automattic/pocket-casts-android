package au.com.shiftyjelly.pocketcasts.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.images.GravatarProfileImage
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvTopBar(
    tabs: List<TvTab>,
    selectedTabIndex: Int,
    profile: TvProfileState,
    onTabSelect: (Int) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvProfileButton(
            profile = profile,
            onClick = onProfileClick,
        )
        Spacer(modifier = Modifier.weight(1f))
        TvTabBar(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelect = onTabSelect,
        )
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(IR.drawable.ic_pocket_casts_logo),
            contentDescription = stringResource(LR.string.app_name),
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
private fun TvProfileButton(
    profile: TvProfileState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.colors(
            containerColor = TvColors.Gray,
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = TvColors.Dark,
        ),
        modifier = modifier.size(40.dp),
    ) {
        when (profile) {
            is TvProfileState.SignedIn -> {
                GravatarProfileImage(
                    email = profile.email,
                    contentDescription = stringResource(LR.string.profile),
                    placeholder = { TvProfilePlaceholderIcon(contentDescription = null) },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            }

            is TvProfileState.SignedOut -> TvProfilePlaceholderIcon(contentDescription = stringResource(LR.string.profile))
        }
    }
}

@Composable
private fun TvProfilePlaceholderIcon(contentDescription: String?, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(IR.drawable.ic_profile),
        contentDescription = contentDescription,
        modifier = modifier.padding(8.dp),
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvTopBarPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            var selectedIndex by remember { mutableIntStateOf(1) }
            TvTopBar(
                tabs = TvTab.entries,
                selectedTabIndex = selectedIndex,
                profile = TvProfileState.SignedOut,
                onTabSelect = { selectedIndex = it },
                onProfileClick = {},
            )
        }
    }
}
