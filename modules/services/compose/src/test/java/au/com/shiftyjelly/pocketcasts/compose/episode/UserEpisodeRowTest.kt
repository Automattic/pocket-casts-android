package au.com.shiftyjelly.pocketcasts.compose.episode

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeRowData
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import au.com.shiftyjelly.pocketcasts.images.R as IR

@RunWith(RobolectricTestRunner::class)
class UserEpisodeRowTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `shows the upload progress of a file`() {
        val episode = UserEpisode(uuid = "1", publishedDate = Date(), serverStatus = UserEpisodeServerStatus.UPLOADING)
        val data = UserEpisodeRowData(episode, downloadProgress = 0, uploadProgress = 25, playbackState = PlaybackState(), isInUpNext = false, hasBookmarks = false)

        val status = userEpisodeRowStatus(data, context)

        assertEquals(0.25f, status.progress)
        assertEquals(null, status.iconId)
    }

    @Test
    fun `shows the downloaded and uploaded icons of a file`() {
        val episode = UserEpisode(uuid = "1", publishedDate = Date(), serverStatus = UserEpisodeServerStatus.UPLOADED, downloadStatus = EpisodeDownloadStatus.Downloaded)
        val data = UserEpisodeRowData(episode, downloadProgress = 0, uploadProgress = 0, playbackState = PlaybackState(), isInUpNext = true, hasBookmarks = false)

        val status = userEpisodeRowStatus(data, context)

        assertEquals(IR.drawable.ic_downloaded, status.iconId)
        assertEquals(true, status.isUploaded)
        assertEquals(true, status.isInUpNext)
    }
}
