package au.com.shiftyjelly.pocketcasts.repositories.appreview

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AppReviewReason
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.time.Clock
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class AppReviewManagerImpl @Inject constructor(
    private val settings: Settings,
    private val clock: Clock,
) : AppReviewManager {
    private val signalChannel = Channel<AppReviewSignal>()
    override val showPromptSignal: Flow<AppReviewSignal> get() = signalChannel.receiveAsFlow()

    private val isMonitoring = AtomicBoolean()

    override suspend fun monitorAppReviewReasons() {
        if (!isMonitoring.getAndSet(true)) {
            while (true) {
                val usedReasons = settings.appReviewSubmittedReasons.value
                if (usedReasons.containsAll(UserBasedReasons)) {
                    break
                }

                delay(5.seconds)
                val reason = calculatePromptReviewReason()
                if (reason != null) {
                    triggerPrompt(reason)
                }
            }
        }
    }

    suspend fun triggerPrompt(reason: AppReviewReason) {
        val result = suspendCancellableCoroutine { continuation ->
            val data = AppReviewSignalImpl(
                reason = reason,
                continuation = continuation,
            )
            if (signalChannel.trySend(data).isFailure) {
                continuation.resume(AppReviewSignal.Result.Ignored)
            }
        }
        processSignalResult(result, reason)
    }

    private fun calculatePromptReviewReason(): AppReviewReason? {
        if (!FeatureFlag.isEnabled(Feature.IMPROVE_APP_RATINGS)) {
            return null
        }

        if (!hasEnoughTimePassedSinceLastPrompt()) {
            return null
        }

        val usedReasons = settings.appReviewSubmittedReasons.value
        val reason = UserBasedReasons
            .filterNot(usedReasons::contains)
            .firstOrNull(::isReasonApplicable)
        return reason
    }

    private fun processSignalResult(result: AppReviewSignal.Result, reason: AppReviewReason) {
        when (result) {
            AppReviewSignal.Result.Consumed -> {
                settings.appReviewLastPromptTimestamp.set(clock.instant(), updateModifiedAt = false)

                if (reason != AppReviewReason.DevelopmentTrigger) {
                    val usedReasons = settings.appReviewSubmittedReasons.value
                    settings.appReviewSubmittedReasons.set(usedReasons + reason, updateModifiedAt = false)
                }
            }

            AppReviewSignal.Result.Ignored -> Unit
        }
    }

    private fun hasEnoughTimePassedSinceLastPrompt(): Boolean {
        val now = clock.instant()
        val lastReviewTimestamp = settings.appReviewLastPromptTimestamp.value ?: return true
        return now.minus(30, ChronoUnit.DAYS).isAfter(lastReviewTimestamp)
    }

    private fun isReasonApplicable(reason: AppReviewReason) = when (reason) {
        AppReviewReason.ThirdEpisodeCompleted -> {
            settings.appReviewEpisodeCompletedTimestamps.value.size >= 3
        }

        AppReviewReason.EpisodeStarred -> {
            false
        }

        AppReviewReason.ShowRated -> {
            false
        }

        AppReviewReason.FilterCreated -> {
            false
        }

        AppReviewReason.PlusUpgraded -> {
            false
        }

        AppReviewReason.FolderCreated -> {
            false
        }

        AppReviewReason.BookmarkCreated -> {
            false
        }

        AppReviewReason.CustomThemeSet -> {
            false
        }

        AppReviewReason.ReferralShared -> {
            false
        }

        AppReviewReason.DevelopmentTrigger -> {
            true
        }
    }
}

private class AppReviewSignalImpl(
    override val reason: AppReviewReason,
    private val continuation: CancellableContinuation<AppReviewSignal.Result>,
) : AppReviewSignal {
    override fun consume() {
        if (continuation.isActive) {
            runCatching { continuation.resume(AppReviewSignal.Result.Consumed) }
        }
    }

    override fun ignore() {
        if (continuation.isActive) {
            runCatching { continuation.resume(AppReviewSignal.Result.Ignored) }
        }
    }
}

private val UserBasedReasons = AppReviewReason.entries - AppReviewReason.DevelopmentTrigger
