package au.com.shiftyjelly.pocketcasts.repositories.appreview

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AppReviewReason
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.model.ReviewErrorCode
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
import timber.log.Timber
import com.google.android.play.core.review.ReviewManager as GoogleReviewManager
import java.time.Duration as JavaDuration

@Singleton
class AppReviewManagerImpl(
    private val settings: Settings,
    private val clock: Clock,
    private val tracker: AnalyticsTracker,
    private val googleManager: GoogleReviewManager,
    private val loopIdleDuration: Duration,
) : AppReviewManager {
    @Inject
    constructor(
        settings: Settings,
        clock: Clock,
        tracker: AnalyticsTracker,
        googleManager: GoogleReviewManager,
    ) : this(
        settings = settings,
        clock = clock,
        tracker = tracker,
        googleManager = googleManager,
        loopIdleDuration = 5.seconds,
    )

    private val signalChannel = Channel<AppReviewSignal>()
    override val showPromptSignal: Flow<AppReviewSignal> get() = signalChannel.receiveAsFlow()

    private val isMonitoring = AtomicBoolean()

    private val trackedFailureEvents = mutableSetOf<AppReviewDeclineReason>()

    override suspend fun monitorAppReviewReasons() {
        if (!isMonitoring.getAndSet(true)) {
            while (true) {
                when (val triggerData = calculateTriggerData()) {
                    is AppReviewTriggerData.Success -> {
                        Timber.d("App review triggered: ${triggerData.reason}")
                        triggerPrompt(triggerData.reason, triggerData.reviewInfo)
                    }

                    is AppReviewTriggerData.Failure -> {
                        Timber.d("App review not triggered: ${triggerData.reason}")
                        if (triggerData.reason.shouldCleanUpData) {
                            clearAllUnusedReasons()
                        }
                        if (triggerData.reason.analyticsValue != null && trackedFailureEvents.add(triggerData.reason)) {
                            tracker.track(
                                AnalyticsEvent.USER_SATISFACTION_SURVEY_NOT_SHOWN,
                                mapOf("policy" to triggerData.reason.analyticsValue),
                            )
                        }
                        if (triggerData.reason.isFinal) {
                            break
                        }
                    }
                }
                delay(loopIdleDuration)
            }
        }
    }

    private suspend fun calculateTriggerData(): AppReviewTriggerData {
        if (!FeatureFlag.isEnabled(Feature.IMPROVE_APP_RATINGS)) {
            return AppReviewTriggerData.Failure(AppReviewDeclineReason.FeatureNotEnabled)
        }

        if (areAllAppReviewReasonsUsed()) {
            return AppReviewTriggerData.Failure(AppReviewDeclineReason.AllReasonsUsed)
        }

        val usedReasons = settings.appReviewSubmittedReasons.value
        val promptReason = UserBasedReasons
            .filterNot(usedReasons::contains)
            .firstOrNull(::isReasonApplicable)
        if (promptReason == null) {
            return AppReviewTriggerData.Failure(AppReviewDeclineReason.NoReasonApplicable)
        }

        if (isDeclinedTwiceIn60Days()) {
            return AppReviewTriggerData.Failure(AppReviewDeclineReason.PromptDeclinedMultipleTimes)
        }

        val reviewInfo = runCatching { googleManager.requestReview() }.getOrElse { error ->
            val reason = if (error is ReviewException) {
                when (error.errorCode) {
                    ReviewErrorCode.INTERNAL_ERROR -> AppReviewDeclineReason.GoogleInternal
                    ReviewErrorCode.INVALID_REQUEST -> AppReviewDeclineReason.GoogleInvalidRequest
                    ReviewErrorCode.PLAY_STORE_NOT_FOUND -> AppReviewDeclineReason.GooglePlayStoreNotFound
                    else -> AppReviewDeclineReason.GoogleUnknown
                }
            } else {
                AppReviewDeclineReason.GoogleUnknown
            }
            return AppReviewTriggerData.Failure(reason)
        }

        if (isPromptedInLast30Days()) {
            return AppReviewTriggerData.Failure(AppReviewDeclineReason.PromptShownRecently)
        }

        if (hasFailedInLast2Sessions()) {
            return AppReviewTriggerData.Failure(AppReviewDeclineReason.ErrorInRecentSessions)
        }

        if (hasCrashedInLast7Days()) {
            return AppReviewTriggerData.Failure(AppReviewDeclineReason.CrashedRecently)
        }

        return AppReviewTriggerData.Success(promptReason, reviewInfo)
    }

    suspend fun triggerPrompt(reason: AppReviewReason, reviewInfo: ReviewInfo) {
        val result = suspendCancellableCoroutine { continuation ->
            val data = AppReviewSignalImpl(
                reason = reason,
                reviewInfo = reviewInfo,
                continuation = continuation,
            )
            if (signalChannel.trySend(data).isFailure) {
                continuation.resume(AppReviewSignal.Result.Ignored)
            }
        }
        processSignalResult(result, reason)
    }

    private fun processSignalResult(result: AppReviewSignal.Result, reason: AppReviewReason) {
        when (result) {
            AppReviewSignal.Result.Consumed -> {
                settings.appReviewLastPromptTimestamp.set(clock.instant(), updateModifiedAt = false)
                clearAllUnusedReasons()

                if (reason != AppReviewReason.DevelopmentTrigger) {
                    val usedReasons = settings.appReviewSubmittedReasons.value
                    settings.appReviewSubmittedReasons.set(usedReasons + reason, updateModifiedAt = false)
                }
            }

            AppReviewSignal.Result.Ignored -> Unit
        }
    }

    private fun areAllAppReviewReasonsUsed(): Boolean {
        return settings.appReviewSubmittedReasons.value.containsAll(UserBasedReasons)
    }

    private fun isPromptedInLast30Days(): Boolean {
        val thirtyDaysAgo = clock.instant().minus(30, ChronoUnit.DAYS)
        val lastReviewTimestamp = settings.appReviewLastPromptTimestamp.value ?: return false
        return !lastReviewTimestamp.isBefore(thirtyDaysAgo)
    }

    private fun isDeclinedTwiceIn60Days(): Boolean {
        val declineTimestamps = settings.appReviewLastDeclineTimestamps.value.takeLast(2)
        return when (declineTimestamps.size) {
            0, 1 -> false
            else -> {
                val first = declineTimestamps[0]
                val second = declineTimestamps[1]
                JavaDuration.between(first, second).abs().toDays() <= 60
            }
        }
    }

    private fun hasFailedInLast2Sessions(): Boolean {
        val recentSessions = settings.sessionIds.takeLast(2)
        return settings.appReviewErrorSessionIds.value.any(recentSessions::contains)
    }

    private fun hasCrashedInLast7Days(): Boolean {
        val sevenDaysAgo = clock.instant().minus(7, ChronoUnit.DAYS)
        val lastCrashTimestamp = settings.appReviewCrashTimestamp.value ?: return false
        return !lastCrashTimestamp.isBefore(sevenDaysAgo)
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

        AppReviewReason.EndOfYearShared -> {
            settings.appReviewEndOfYearSharedTimestamp.value != null
        }

        AppReviewReason.EndOfYearCompleted -> {
            settings.appReviewEndOfYearCompletedTimestamp.value != null
        }

        AppReviewReason.DevelopmentTrigger -> {
            true
        }
    }

    fun clearSettings() {
        with(settings) {
            appReviewEpisodeCompletedTimestamps.set(emptyList(), updateModifiedAt = false)
            appReviewEpisodeStarredTimestamp.set(null, updateModifiedAt = false)
            appReviewPodcastRatedTimestamp.set(null, updateModifiedAt = false)
            appReviewPlaylistCreatedTimestamp.set(null, updateModifiedAt = false)
            appReviewPlusUpgradedTimestamp.set(null, updateModifiedAt = false)
            appReviewFolderCreatedTimestamp.set(null, updateModifiedAt = false)
            appReviewBookmarkCreatedTimestamp.set(null, updateModifiedAt = false)
            appReviewThemeChangedTimestamp.set(null, updateModifiedAt = false)
            appReviewReferralSharedTimestamp.set(null, updateModifiedAt = false)
            appReviewEndOfYearSharedTimestamp.set(null, updateModifiedAt = false)
            appReviewEndOfYearCompletedTimestamp.set(null, updateModifiedAt = false)
            appReviewLastPromptTimestamp.set(null, updateModifiedAt = false)
            appReviewLastDeclineTimestamps.set(emptyList(), updateModifiedAt = false)
            appReviewCrashTimestamp.set(null, updateModifiedAt = false)
            appReviewSubmittedReasons.set(emptyList(), updateModifiedAt = false)
        }
    }

    private fun clearAllUnusedReasons() {
        val usedReasons = settings.appReviewSubmittedReasons.value
        val unusedReasons = UserBasedReasons - usedReasons
        with(settings) {
            unusedReasons.forEach { reason ->
                when (reason) {
                    AppReviewReason.ThirdEpisodeCompleted -> {
                        appReviewEpisodeCompletedTimestamps.set(emptyList(), updateModifiedAt = false)
                    }

                    AppReviewReason.EpisodeStarred -> {
                        appReviewEpisodeStarredTimestamp.set(null, updateModifiedAt = false)
                    }

                    AppReviewReason.ShowRated -> {
                        appReviewPodcastRatedTimestamp.set(null, updateModifiedAt = false)
                    }

                    AppReviewReason.FilterCreated -> {
                        appReviewPlaylistCreatedTimestamp.set(null, updateModifiedAt = false)
                    }

                    AppReviewReason.PlusUpgraded -> {
                        appReviewPlusUpgradedTimestamp.set(null, updateModifiedAt = false)
                    }

                    AppReviewReason.FolderCreated -> {
                        appReviewFolderCreatedTimestamp.set(null, updateModifiedAt = false)
                    }

                    AppReviewReason.BookmarkCreated -> {
                        appReviewBookmarkCreatedTimestamp.set(null, updateModifiedAt = false)
                    }

                    AppReviewReason.CustomThemeSet -> {
                        appReviewThemeChangedTimestamp.set(null, updateModifiedAt = false)
                    }

                    AppReviewReason.ReferralShared -> {
                        appReviewReferralSharedTimestamp.set(null, updateModifiedAt = false)
                    }

                    AppReviewReason.EndOfYearShared -> {
                        appReviewEndOfYearSharedTimestamp.set(null, updateModifiedAt = false)
                    }

                    AppReviewReason.EndOfYearCompleted -> {
                        appReviewEndOfYearCompletedTimestamp.set(null, updateModifiedAt = false)
                    }

                    AppReviewReason.DevelopmentTrigger -> Unit
                }
            }
        }
    }
}

private class AppReviewSignalImpl(
    override val reason: AppReviewReason,
    override val reviewInfo: ReviewInfo,
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

    override fun toString(): String {
        return "AppReviewSignalImpl(reason=$reason, reviewInfo=$reviewInfo)"
    }
}

private enum class AppReviewDeclineReason(
    /**
     * Whether the event loop should be stopped when this is a decline reason.
     */
    val isFinal: Boolean,
    /**
     * Whether unused app review reasons should be cleared. This prevents dispatching lingering prompts.
     * Cleanup occurs when: (1) a prompt is successfully shown, or (2) prompting fails due to temporary
     * conditions like crashes or errors, ensuring stale reasons don't trigger prompts once the condition
     * is resolved.
     */
    val shouldCleanUpData: Boolean,
    val analyticsValue: String?,
) {
    FeatureNotEnabled(
        isFinal = true,
        shouldCleanUpData = false,
        analyticsValue = null,
    ),
    CrashedRecently(
        isFinal = false,
        shouldCleanUpData = true,
        analyticsValue = "crashed_recently",
    ),
    ErrorInRecentSessions(
        isFinal = false,
        shouldCleanUpData = true,
        analyticsValue = "error_in_recent_sessions",
    ),
    PromptDeclinedMultipleTimes(
        isFinal = true,
        shouldCleanUpData = false,
        analyticsValue = "prompt_declined_multiple_times",
    ),
    PromptShownRecently(
        isFinal = false,
        shouldCleanUpData = true,
        analyticsValue = "prompt_shown_recently",
    ),
    AllReasonsUsed(
        isFinal = true,
        shouldCleanUpData = false,
        analyticsValue = null,
    ),
    NoReasonApplicable(
        isFinal = false,
        shouldCleanUpData = false,
        analyticsValue = null,
    ),
    GoogleInternal(
        isFinal = true,
        shouldCleanUpData = false,
        analyticsValue = "google_internal",
    ),
    GoogleInvalidRequest(
        isFinal = true,
        shouldCleanUpData = false,
        analyticsValue = "google_invalid_request",
    ),
    GooglePlayStoreNotFound(
        isFinal = true,
        shouldCleanUpData = false,
        analyticsValue = "google_play_store_not_found",
    ),
    GoogleUnknown(
        isFinal = true,
        shouldCleanUpData = false,
        analyticsValue = "google_unknown",
    ),
}

private sealed interface AppReviewTriggerData {
    data class Success(
        val reason: AppReviewReason,
        val reviewInfo: ReviewInfo,
    ) : AppReviewTriggerData

    data class Failure(
        val reason: AppReviewDeclineReason,
    ) : AppReviewTriggerData
}

private val UserBasedReasons = AppReviewReason.entries - AppReviewReason.DevelopmentTrigger
