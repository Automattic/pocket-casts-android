package au.com.shiftyjelly.pocketcasts.views.swipe

import android.content.Context
import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRowAction
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRowState

fun SwipeAction.toSwipeRowAction(
    context: Context,
    onClick: (SwipeRowState) -> Unit,
) = SwipeRowAction(
    iconId = imageDrawableId(),
    contentDescription = contentDescription(context),
    backgroundColor = Color(backgroundTint(context)),
    iconTint = Color(imageTint(context)),
    onClick = onClick,
)

fun SwipeRowActions.leadingSwipeRowActions(
    context: Context,
    onAction: (SwipeAction) -> Unit,
) = listOfNotNull(ltr1, ltr2, ltr3).toSwipeRowActions(context, onAction)

fun SwipeRowActions.trailingSwipeRowActions(
    context: Context,
    onAction: (SwipeAction) -> Unit,
) = listOfNotNull(rtl1, rtl2, rtl3).toSwipeRowActions(context, onAction)

private fun List<SwipeAction>.toSwipeRowActions(
    context: Context,
    onAction: (SwipeAction) -> Unit,
) = map { action -> action.toSwipeRowAction(context) { onAction(action) } }
