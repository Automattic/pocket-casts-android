package au.com.shiftyjelly.pocketcasts.views.fragments

import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PodcastSelectPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val first = Podcast(uuid = "1", title = "First", author = "Author 1")
    private val second = Podcast(uuid = "2", title = "Second", author = "Author 2")

    @Test
    fun `shows the podcasts and the selection text`() {
        setContent(selected = setOf("1"))

        composeTestRule.onNodeWithText("1 podcast selected").assertExists()
        composeTestRule.onNodeWithText("First").assertExists()
        composeTestRule.onNodeWithText("Second").assertExists()
        composeTestRule.onNodeWithText("Select all").assertExists()
    }

    @Test
    fun `toggles a podcast when its row is tapped`() {
        val toggles = mutableListOf<Pair<String, Boolean>>()
        setContent(selected = setOf("1"), onToggle = { podcast, selected -> toggles += podcast.uuid to selected })

        composeTestRule.onNodeWithText("Second").performClick()
        composeTestRule.onNodeWithText("First").performClick()

        assertEquals(listOf("2" to true, "1" to false), toggles)
    }

    @Test
    fun `offers select none when everything is selected`() {
        var clicks = 0
        setContent(selected = setOf("1", "2"), onSelectButtonClick = { clicks++ })

        composeTestRule.onNodeWithText("Select none").performClick()

        assertEquals(1, clicks)
    }

    private fun setContent(
        selected: Set<String>,
        onToggle: (Podcast, Boolean) -> Unit = { _, _ -> },
        onSelectButtonClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            val materialContext = ContextThemeWrapper(LocalContext.current, com.google.android.material.R.style.Theme_MaterialComponents_Light)
            CompositionLocalProvider(LocalContext provides materialContext) {
                AppTheme(Theme.ThemeType.LIGHT) {
                    PodcastSelectPage(
                        podcasts = listOf(first, second),
                        selectedUuids = selected,
                        selectedText = if (selected.size == 1) "1 podcast selected" else "${selected.size} podcasts selected",
                        tintColor = null,
                        showToolbar = true,
                        bottomInset = 0.dp,
                        onToggle = onToggle,
                        onSelectButtonClick = onSelectButtonClick,
                        onNavigationClick = {},
                    )
                }
            }
        }
    }
}
