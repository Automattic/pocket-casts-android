package au.com.shiftyjelly.pocketcasts.compose.components

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.Velocity

@Composable
fun rememberNestedScrollLockableInteropConnection(
    hostView: View = LocalView.current,
): NestedScrollLockableInteropConnection {
    val delegate = rememberNestedScrollInteropConnection(hostView)
    return remember(delegate) { NestedScrollLockableInteropConnection(delegate) }
}

class NestedScrollLockableInteropConnection internal constructor(
    private val delegate: NestedScrollConnection,
) : NestedScrollConnection {
    var isEnabled = true

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        return if (isEnabled) {
            delegate.onPreScroll(available, source)
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        return if (isEnabled) {
            delegate.onPostScroll(consumed, available, source)
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return if (isEnabled) {
            delegate.onPostFling(consumed, available)
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        return if (isEnabled) {
            delegate.onPreFling(available)
        } else {
            Velocity.Zero
        }
    }
}
