package au.com.shiftyjelly.pocketcasts.player.view.shelf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel.Companion.moreActionsTitle
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel.Companion.shortcutTitle
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfRowItem
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import android.graphics.Color as AndroidColor
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ShelfPage(
    theme: Theme,
    playerViewModel: PlayerViewModel,
    onBackPressed: () -> Unit,
) {
    val iconColorInt = ThemeColor.playerContrast01(theme.activeTheme)
    val backgroundColorInt = theme.playerBackground2Color(playerViewModel.podcast)
    val toolbarColorInt = theme.playerBackgroundColor(playerViewModel.podcast)
    Column(
        modifier = Modifier
            .background(Color(AndroidColor.parseColor(ColorUtils.colorIntToHexString(backgroundColorInt)))),
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.rearrange_actions),
            backgroundColor = Color(AndroidColor.parseColor(ColorUtils.colorIntToHexString(toolbarColorInt))),
            textColor = MaterialTheme.theme.colors.playerContrast01,
            iconColor = Color(AndroidColor.parseColor(ColorUtils.colorIntToHexString(iconColorInt))),
            bottomShadow = true,
            onNavigationClick = { onBackPressed() },
        )

        val selectedColorInt = theme.playerHighlight7Color(playerViewModel.podcast)
        val selectedBackgroundInt = ColorUtils.calculateCombinedColor(backgroundColorInt, selectedColorInt)

        val shelfItems by playerViewModel.shelfLive.asFlow()
            .map {
                buildList<ShelfRowItem> {
                    addAll(it)
                    add(4, moreActionsTitle)
                    add(0, shortcutTitle)
                }
            }
            .collectAsStateWithLifecycle(emptyList<ShelfRowItem>())

        val episode by playerViewModel.playingEpisodeLive.asFlow()
            .map { (episode, _) -> episode }
            .distinctUntilChangedBy { it.uuid }
            .collectAsStateWithLifecycle(null)

        if (episode == null) return@Column

        MenuShelfItems(
            shelfItems = shelfItems,
            episode = episode as BaseEpisode,
            normalBackgroundColor = Color(AndroidColor.parseColor(ColorUtils.colorIntToHexString(backgroundColorInt))),
            selectedBackgroundColor = Color(AndroidColor.parseColor(ColorUtils.colorIntToHexString(selectedBackgroundInt))),
            isEditable = true,
        )
    }
}
