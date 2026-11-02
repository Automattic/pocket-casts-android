package au.com.shiftyjelly.pocketcasts.views.compose

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeRowData
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButton
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import com.automattic.eventhorizon.EventHorizon
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EpisodeRowVariantsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val publishedDate = Date()
    private val date = RelativeDateFormatter(context).format(publishedDate)
    private val calls = mutableListOf<String>()

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

    private val userEpisode = UserEpisode(uuid = "uuid", title = "File title", publishedDate = publishedDate, duration = 3600.0)

    private fun setUserContent(rowData: UserEpisodeRowData?) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                UserEpisodeRow(
                    episode = userEpisode,
                    rowData = rowData,
                    imageRequestFactory = PocketCastsImageRequestFactory(context),
                    swipeActions = { SwipeRowActions(rtl1 = SwipeAction.DeleteUserEpisode) },
                    trailing = EpisodeRowTrailing.PlayButton(listener, EventHorizon {}),
                    onClick = { calls += "click" },
                    onLongClick = { calls += "longClick" },
                    onSwipeAction = {},
                )
            }
        }
    }

    private fun setUnavailableContent() {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                UnavailableEpisodeRow(
                    episode = ManualPlaylistEpisode(
                        playlistUuid = "playlist",
                        episodeUuid = "uuid",
                        podcastUuid = "podcast",
                        title = "Unavailable title",
                        addedAt = publishedDate.toInstant(),
                        publishedAt = publishedDate.toInstant(),
                        downloadUrl = null,
                        episodeSlug = "slug",
                        podcastSlug = "slug",
                        sortPosition = 0,
                        isSynced = true,
                    ),
                    imageRequestFactory = PocketCastsImageRequestFactory(context),
                    swipeActions = SwipeRowActions(rtl1 = SwipeAction.RemoveFromPlaylist),
                    onClick = { calls += "click" },
                    onSwipeAction = {},
                )
            }
        }
    }

    @Test
    fun `describes user episodes with the file status text`() {
        val rowData = UserEpisodeRowData(
            episode = userEpisode.copy(serverStatus = UserEpisodeServerStatus.UPLOADING),
            downloadProgress = 0,
            uploadProgress = 65,
            playbackState = PlaybackState(),
            isInUpNext = false,
            hasBookmarks = false,
        )
        setUserContent(rowData)

        composeTestRule.onNodeWithContentDescription("File title $date Uploading… (65%)").assertExists()
    }

    @Test
    fun `describes user episodes before the row data arrives`() {
        setUserContent(rowData = null)

        composeTestRule.onNodeWithContentDescription("File title $date 1h").performClick()
        composeTestRule.onNodeWithContentDescription("File title $date 1h").performTouchInput { longClick() }

        assertEquals(listOf("click", "longClick"), calls)
    }

    @Test
    fun `unavailable rows read their texts and only support clicks`() {
        setUnavailableContent()

        val row = composeTestRule.onNodeWithText("Unavailable title", substring = true)
        row.assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnLongClick))
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(date, "Unavailable title", "Unavailable").map(::AnnotatedString)))
        row.performClick()

        assertEquals(listOf("click"), calls)
    }
}
