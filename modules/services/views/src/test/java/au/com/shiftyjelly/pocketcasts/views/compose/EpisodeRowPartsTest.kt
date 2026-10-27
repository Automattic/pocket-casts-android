package au.com.shiftyjelly.pocketcasts.views.compose

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.ThemeDarkColors
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EpisodeRowPartsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val colors = EpisodeRowColors(tint = Color.Red, theme = ThemeDarkColors)

    @Test
    fun `greys out text at half alpha`() {
        assertEquals(ThemeDarkColors.primaryText01.copy(alpha = 128 / 255f), colors.title(isGreyedOut = true))
        assertEquals(ThemeDarkColors.primaryText02.copy(alpha = 128 / 255f), colors.secondaryText(isGreyedOut = true))
        assertEquals(ThemeDarkColors.primaryText01, colors.title(isGreyedOut = false))
        assertEquals(ThemeDarkColors.primaryText02, colors.secondaryText(isGreyedOut = false))
    }

    @Test
    fun `highlights selected rows`() {
        assertEquals(ThemeDarkColors.primaryUi02Selected, colors.background(isSelected = true))
        assertEquals(ThemeDarkColors.primaryUi02, colors.background(isSelected = false))
    }

    @Test
    fun `tints downloaded status icons with support 02`() {
        assertEquals(ThemeDarkColors.support02, colors.statusIconTint(EpisodeStatusIconTint.Downloaded))
        assertEquals(ThemeDarkColors.primaryIcon02, colors.statusIconTint(EpisodeStatusIconTint.Secondary))
    }

    @Test
    fun `title is an accessibility heading without its own text node`() {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                EpisodeRowTitle("Episode title", Color.Black, starTint = Color.Yellow)
            }
        }

        composeTestRule.onNodeWithText("Episode title").assertDoesNotExist()
        composeTestRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
    }

    @Test
    fun `header summary is upper case and keeps the tinted prefix`() {
        val summary = SpannableString("Bonus • Today").apply {
            setSpan(ForegroundColorSpan(android.graphics.Color.RED), 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val text = summary.toUpperCaseAnnotatedString()

        assertEquals("BONUS • TODAY", text.text)
        assertEquals(listOf(Color.Red to (0 until 5)), text.spanStyles.map { it.item.color to (it.start until it.end) })
    }
}
