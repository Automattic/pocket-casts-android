package au.com.shiftyjelly.pocketcasts.player.view.chapters

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChaptersGeneratingTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `announces generating chapters once as a loading header`() {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.DARK) {
                ChaptersTheme {
                    ChaptersGenerating(
                        episodeUuid = "episode-uuid",
                        onTurnOffGeneratedClick = {},
                    )
                }
            }
        }

        composeTestRule.onAllNodesWithText(GENERATING).assertCountEquals(1)
        composeTestRule.onAllNodesWithContentDescription(GENERATING).assertCountEquals(0)
        composeTestRule.onNodeWithText(GENERATING).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate),
        )
    }

    private companion object {
        const val GENERATING = "Generating chapters…"
    }
}
