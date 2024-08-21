package au.com.shiftyjelly.pocketcasts.engage

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import au.com.shiftyjelly.pocketcasts.engage.EngageSdkBridge.Companion.TAG
import com.google.android.engage.service.Intents
import timber.log.Timber

internal class EngageSdkBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Timber.tag(TAG).d("Received Engage SDK broadcast: $action")
        when (action) {
            Intents.ACTION_PUBLISH_RECOMMENDATION -> EngageSdkWorkers.enqueueOneOffRecommendationsWork(context)
            Intents.ACTION_PUBLISH_CONTINUATION -> EngageSdkWorkers.enqueueOneOffContinuationWork(context)
            Intents.ACTION_PUBLISH_FEATURED -> EngageSdkWorkers.enqueueOneOffFeaturedWork(context)
            else -> {
                Timber.tag(TAG).d("Unexpected Engage SDK broadcast action: $action")
            }
        }
    }

    companion object {
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        fun register(context: Context) {
            val receiver = EngageSdkBroadcastReceiver()
            val intentFilter = IntentFilter().apply {
                addAction(Intents.ACTION_PUBLISH_RECOMMENDATION)
                addAction(Intents.ACTION_PUBLISH_CONTINUATION)
                addAction(Intents.ACTION_PUBLISH_FEATURED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, intentFilter)
            }
        }
    }
}
