package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@RunWith(RobolectricTestRunner::class)
class AddFilePageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val state = AddFilePageState(
        titleRes = LR.string.profile_cloud_edit_file,
        name = "Episode",
        fileName = "episode.mp3",
        fileSize = "1 MB",
        fileTint = null,
        isArtworkVisible = false,
        artwork = null,
        imageButtonRes = LR.string.profile_files_add_custom_image,
        isLoading = false,
        isReadOnly = false,
        isUpgradeVisible = true,
        imageItem = null,
        colours = emptyList(),
        selectedIndex = 1,
    )

    @Test
    fun `shows the file details and forwards toolbar and image clicks`() {
        val clicks = mutableListOf<String>()
        setContent(state, onClick = { clicks += it })

        composeTestRule.onNodeWithText("Edit File").assertExists()
        composeTestRule.onNodeWithText("episode.mp3").assertExists()
        composeTestRule.onNodeWithText("1 MB").assertExists()
        composeTestRule.onNodeWithText("Add Custom Image").assertExists()
        composeTestRule.onNodeWithText("Learn more about Pocket Casts Plus").assertExists()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.onNodeWithContentDescription("File").performClick()

        assertEquals(listOf("back", "save", "image"), clicks)
    }

    @Test
    fun `hides the save action and the upgrade callout`() {
        setContent(state.copy(titleRes = null, isUpgradeVisible = false), onClick = {})

        assertEquals(0, composeTestRule.onAllNodesWithText("SAVE").fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("Learn more about Pocket Casts Plus").fetchSemanticsNodes().size)
    }

    private fun setContent(state: AddFilePageState, onClick: (String) -> Unit) {
        composeTestRule.setContent {
            ThemedContent {
                AddFilePage(
                    state = state,
                    onNavigationClick = { onClick("back") },
                    onSaveClick = { onClick("save") },
                    onNameChange = {},
                    onImageClick = { onClick("image") },
                    onImageButtonClick = { onClick("button") },
                    onColourClick = {},
                    onLockedColourClick = {},
                    onUpgradeClick = {},
                    onUpgradeCloseClick = {},
                )
            }
        }
    }

    @Composable
    private fun ThemedContent(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalContext provides ContextThemeWrapper(LocalContext.current, UR.style.ThemeLight)) {
            AppTheme(Theme.ThemeType.LIGHT, content)
        }
    }
}
