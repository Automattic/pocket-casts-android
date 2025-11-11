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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Duration as JavaDuration

@Singleton
class AppReviewManagerImpl(
    private val settings: Settings,
    private val clock: Clock,
    private val loopIdleDuration: Duration,
) : AppReviewManager {
    @Inject
    constructor(
        settings: Settings,
        clock: Clock,
    ) : this(
        settings = settings,
        clock = clock,
        loopIdleDuration = 5.seconds,
    )

    private val signalChannel = Channel<AppReviewSignal>()
    override val showPromptSignal: Flow<AppReviewSignal> get() = signalChannel.receiveAsFlow()

    private val isMonitoring = AtomicBoolean()

    override suspend fun monitorAppReviewReasons() {
        if (!isMonitoring.getAndSet(true)) {
            while (true) {
                val usedReasons = settings.appReviewSubmittedReasons.value
                if (usedReasons.containsAll(UserBasedReasons) || !isNotDeclinedTwiceIn60Days()) {
                    break
                }

                val reason = calculatePromptReviewReason()
                if (reason != null) {
                    triggerPrompt(reason)
                }
                delay(loopIdleDuration)
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
        if (!canDispatchSignal()) {
            return null
        }

        val usedReasons = settings.appReviewSubmittedReasons.value
        return UserBasedReasons
            .filterNot(usedReasons::contains)
            .firstOrNull(::isReasonApplicable)
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

    private fun canDispatchSignal(): Boolean {
        return FeatureFlag.isEnabled(Feature.IMPROVE_APP_RATINGS) &&
            is30DaysSinceLastPrompt() &&
            isNotDeclinedTwiceIn60Days() &&
            isNoErrorsInLast2Sessions() &&
            isNoCrashesIn7Days()
    }

    private fun is30DaysSinceLastPrompt(): Boolean {
        val thirtyDaysAgo = clock.instant().minus(30, ChronoUnit.DAYS)
        val lastReviewTimestamp = settings.appReviewLastPromptTimestamp.value ?: return true
        return thirtyDaysAgo.isAfter(lastReviewTimestamp)
    }

    private fun isNotDeclinedTwiceIn60Days(): Boolean {
        val declineTimestamps = settings.appReviewLastDeclineTimestamps.value.takeLast(2)
        return when (declineTimestamps.size) {
            0, 1 -> true
            else -> {
                val first = declineTimestamps[0]
                val second = declineTimestamps[1]
                JavaDuration.between(first, second).abs().toDays() > 60
            }
        }
    }

    private fun isNoErrorsInLast2Sessions(): Boolean {
        val recentSessions = settings.sessionIds.takeLast(2)
        return settings.appReviewErrorSessionIds.value.none(recentSessions::contains)
    }

    private fun isNoCrashesIn7Days(): Boolean {
        val sevenDaysAgo = clock.instant().minus(7, ChronoUnit.DAYS)
        val lastCrashTimestamp = settings.appReviewCrashTimestamp.value ?: return true
        return sevenDaysAgo.isAfter(lastCrashTimestamp)
    }

    private fun isReasonApplicable(reason: AppReviewReason) = when (reason) {
        AppReviewReason.ThirdEpisodeCompleted -> {
            settings.appReviewEpisodeCompletedTimestamps.value.size >= 3
        }

        AppReviewReason.EpisodeStarred -> {
            settings.appReviewEpisodeStarredTimestamp.value != null
        }

        AppReviewReason.ShowRated -> {
            settings.appReviewPodcastRatedTimestamp.value != null
        }

        AppReviewReason.FilterCreated -> {
            settings.appReviewPlaylistCreatedTimestamp.value != null
        }

        AppReviewReason.PlusUpgraded -> {
            val upgradeTimestamp = settings.appReviewPlusUpgradedTimestamp.value
            upgradeTimestamp != null && clock.instant().isAfter(upgradeTimestamp.plus(2, ChronoUnit.DAYS))
        }

        AppReviewReason.FolderCreated -> {
            settings.appReviewFolderCreatedTimestamp.value != null
        }

        AppReviewReason.BookmarkCreated -> {
            settings.appReviewBookmarkCreatedTimestamp.value != null
        }

        AppReviewReason.CustomThemeSet -> {
            settings.appReviewThemeChangedTimestamp.value != null
        }

        AppReviewReason.ReferralShared -> {
            settings.appReviewReferralSharedTimestamp.value != null
        }

        AppReviewReason.PlaybackShared -> {
            settings.appReviewPlaybackSharedTimestamp.value != null
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
