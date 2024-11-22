package au.com.shiftyjelly.pocketcasts.profile

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ProfileSections(
    sections: List<ProfileSection>,
    onClick: (ProfileSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        sections.forEach { section ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clickable(
                        interactionSource = remember(::MutableInteractionSource),
                        indication = ripple(
                            color = MaterialTheme.theme.colors.primaryIcon01,
                        ),
                        onClick = { onClick(section) },
                    )
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                    }
                    .padding(16.dp),
            ) {
                Icon(
                    painter = painterResource(section.iconId),
                    contentDescription = null,
                    tint = MaterialTheme.theme.colors.primaryInteractive01,
                )
                Spacer(
                    modifier = Modifier.width(24.dp),
                )
                TextP40(
                    text = stringResource(section.labelId),
                )
            }
            HorizontalDivider()
        }
    }
}

internal enum class ProfileSection(
    @DrawableRes val iconId: Int,
    @StringRes val labelId: Int,
) {
    Stats(
        iconId = IR.drawable.ic_stats,
        labelId = LR.string.profile_navigation_stats,
    ),
    Downloads(
        iconId = IR.drawable.ic_profile_download,
        labelId = LR.string.profile_navigation_downloads,
    ),
    CloudFiles(
        iconId = IR.drawable.ic_file,
        labelId = LR.string.profile_navigation_files,
    ),
    Starred(
        iconId = IR.drawable.ic_starred,
        labelId = LR.string.profile_navigation_starred,
    ),
    Bookmarks(
        iconId = IR.drawable.ic_bookmark,
        labelId = LR.string.bookmarks,
    ),
    ListeningHistory(
        iconId = IR.drawable.ic_listen_history,
        labelId = LR.string.profile_navigation_listening_history,
    ),
    Help(
        iconId = IR.drawable.ic_help,
        labelId = LR.string.settings_title_help,
    ),
}

@Preview
@Composable
private fun ProfileSectionsPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AppThemeWithBackground(theme) {
        ProfileSections(
            sections = ProfileSection.entries,
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
