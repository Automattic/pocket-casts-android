package au.com.shiftyjelly.pocketcasts.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NetworkWrapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isConnected() = Network.isConnected(context)
}
