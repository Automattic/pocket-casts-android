package au.com.shiftyjelly.pocketcasts.taskerplugin.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.flow.first

val String?.nullIfEmpty get() = if (isNullOrEmpty()) null else this

fun <T> tryOrNull(handleError: ((Throwable) -> T?)? = null, block: () -> T?): T? = try {
    block()
} catch (t: Throwable) {
    handleError?.invoke(t)
}

val screenSize
    @Composable
    get(): DpSize {
        val configuration = LocalConfiguration.current

        val screenHeight = configuration.screenHeightDp.dp
        val screenWidth = configuration.screenWidthDp.dp
        return DpSize(width = screenWidth, height = screenHeight)
    }

val Date.formattedForTasker get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(this)

val String.formattedForTasker get() = replace(",", "")

internal suspend fun PlaylistManager.findPlaylist(uuid: String) = smartPlaylistFlow(uuid).first() ?: manualPlaylistFlow(uuid).first()
