package au.com.shiftyjelly.pocketcasts.views.compose

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteActionProvider
import androidx.mediarouter.app.MediaRouteButton
import au.com.shiftyjelly.pocketcasts.views.extensions.updateColor
import com.google.android.gms.cast.framework.CastButtonFactory
import timber.log.Timber

@Composable
fun ToolbarCastButton(
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        return
    }
    val currentOnClick by rememberUpdatedState(onClick)
    AndroidView(
        factory = { context ->
            val button = MediaRouteActionProvider(context).onCreateActionView() as MediaRouteButton
            try {
                CastButtonFactory.setUpMediaRouteButton(context, button)
            } catch (e: Throwable) {
                Timber.e(e)
            }
            button.setOnClickListener { currentOnClick() }
            button
        },
        update = { button -> button.updateColor(tint.toArgb()) },
        modifier = modifier.fillMaxHeight(),
    )
}
