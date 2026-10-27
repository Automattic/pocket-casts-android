package au.com.shiftyjelly.pocketcasts.views.compose

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ApplicationProvider
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowData
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButton
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import com.automattic.eventhorizon.EventHorizon
import com.google.android.material.checkbox.MaterialCheckBox
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EpisodeRowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val publishedDate = Date()
    private val date = RelativeDateFormatter(context).format(publishedDate)
    private val calls = mutableListOf<String>()
    private var hostView: View? = null

    private val listener = object : PlayButton.OnClickListener {
        override var source = SourceView.UNKNOWN
        override fun onPlayClicked(episodeUuid: String) = Unit
        override fun onPauseClicked() = Unit
        override fun onPlayNext(episodeUuid: String) = Unit
        override fun onPlayLast(episodeUuid: String) = Unit
        override fun onDownload(episodeUuid: String) = Unit
        override fun onStopDownloading(episodeUuid: String) = Unit
        override fun onPlayedClicked(episodeUuid: String) = Unit
    }

    private fun episode(
        downloadStatus: EpisodeDownloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
        playingStatus: EpisodePlayingStatus = EpisodePlayingStatus.NOT_PLAYED,
        isStarred: Boolean = false,
    ) = PodcastEpisode(
        uuid = "uuid",
        title = "Episode title",
        publishedDate = publishedDate,
        duration = 3600.0,
        playedUpTo = if (playingStatus == EpisodePlayingStatus.IN_PROGRESS) 1200.0 else 0.0,
        playingStatus = playingStatus,
        downloadStatus = downloadStatus,
        isStarred = isStarred,
    )

    private fun setContent(
        episode: PodcastEpisode = episode(),
        rowData: EpisodeRowData = InitialRowData,
        isMultiSelecting: Boolean = false,
        isSelected: Boolean = false,
    ) {
        composeTestRule.setContent {
            hostView = LocalView.current
            CompositionLocalProvider(LocalContext provides ContextThemeWrapper(LocalContext.current, Theme.ThemeType.LIGHT.resourceId)) {
                AppTheme(Theme.ThemeType.LIGHT) {
                    EpisodeRow(
                        episode = episode,
                        rowData = rowData,
                        imageRequestFactory = PocketCastsImageRequestFactory(context),
                        swipeActions = { SwipeRowActions(ltr1 = SwipeAction.AddToUpNextTop) },
                        trailing = EpisodeRowTrailing.PlayButton(listener, EventHorizon {}),
                        onClick = { calls += "click" },
                        onLongClick = { calls += "longClick" },
                        onSwipeAction = { action -> calls += "swipe $action" },
                        isMultiSelecting = isMultiSelecting,
                        isSelected = isSelected,
                    )
                }
            }
        }
    }

    private fun row(description: String = "Episode title. $date. 1 hour.") = composeTestRule.onNodeWithContentDescription(description)

    @Test
    fun `describes the row like the view holder`() {
        setContent()

        row().assertExists()
    }

    @Test
    fun `describes up next, downloaded and starred rows`() {
        setContent(
            episode = episode(EpisodeDownloadStatus.Downloaded, isStarred = true),
            rowData = InitialRowData.copy(isInUpNext = true, hasBookmarks = true),
        )

        row("Episode title. $date. 1 hour. In up next. Downloaded. Starred.").assertExists()
    }

    @Test
    fun `describes in progress rows with the time left`() {
        setContent(episode = episode(playingStatus = EpisodePlayingStatus.IN_PROGRESS), rowData = InitialRowData.copy(isInUpNext = true))

        row("Episode title. $date. 40 minutes left. In progress.").assertExists()
    }

    @Test
    fun `row click and long click are forwarded`() {
        setContent()

        row().performClick()
        row().performTouchInput { longClick() }

        assertEquals(listOf("click", "longClick"), calls)
    }

    @Test
    fun `full swipe runs the first leading action`() {
        setContent()

        row().performTouchInput { swipeRight(startX = left, endX = right) }
        composeTestRule.waitForIdle()

        assertEquals(listOf("swipe AddToUpNextTop"), calls)
    }

    @Test
    fun `multi select locks the swipe`() {
        setContent(isMultiSelecting = true)

        row().performTouchInput { swipeRight(startX = left, endX = right) }
        composeTestRule.waitForIdle()

        assertTrue(calls.none { it.startsWith("swipe") })
    }

    @Test
    fun `multi select swaps the play button for a checked checkbox that clicks the row`() {
        setContent(isMultiSelecting = true, isSelected = true)

        composeTestRule.onNodeWithContentDescription("Download").assertDoesNotExist()
        val checkbox = requireNotNull(hostView).findCheckbox()
        assertTrue(checkbox.isChecked)

        composeTestRule.runOnUiThread { checkbox.performClick() }

        assertEquals(listOf("click"), calls)
        assertTrue(checkbox.isChecked)
    }

    @Test
    fun `play button shows outside multi select`() {
        setContent()

        composeTestRule.onNodeWithContentDescription("Download").assertExists()
        assertEquals(null, requireNotNull(hostView).findCheckboxOrNull())
    }

    private fun View.findCheckbox() = requireNotNull(findCheckboxOrNull())

    private fun View.findCheckboxOrNull(): MaterialCheckBox? = when (this) {
        is MaterialCheckBox -> this
        is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).findCheckboxOrNull() }
        else -> null
    }
}
