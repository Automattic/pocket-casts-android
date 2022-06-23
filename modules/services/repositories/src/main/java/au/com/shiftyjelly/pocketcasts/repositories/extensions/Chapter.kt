package au.com.shiftyjelly.pocketcasts.repositories.extensions

import android.content.Context
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.to.Chapter

fun Chapter.lengthTimeString(context: Context): String {
    return TimeHelper.getTimeDurationShortString(this.lengthTime.toLong(), context, "")
}
