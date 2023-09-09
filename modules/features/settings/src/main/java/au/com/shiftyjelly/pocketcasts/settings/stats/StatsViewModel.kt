package au.com.shiftyjelly.pocketcasts.settings.stats

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.to.StatsBundle
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.settings.util.FunnyTimeConverter
import au.com.shiftyjelly.pocketcasts.utils.timeIntervalSinceNow
import au.com.shiftyjelly.pocketcasts.views.review.InAppReviewHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    val statsManager: StatsManager,
    val episodeManager: EpisodeManager,
    val settings: Settings,
    val syncManager: SyncManager,
    val application: Application,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val inAppReviewHelper: InAppReviewHelper,
) : ViewModel() {

    sealed class State {
        object Loading : State()
        object Error : State()
        data class Loaded(
            val totalListened: Long,
            val skipping: Long,
            val variableSpeed: Long,
            val trimSilence: Long,
            val autoSkipping: Long,
            val totalSaved: Long,
            val funnyText: String,
            val startedAt: Date?,
            val showAppReviewDialog: Boolean = false,
        ) : State()
    }

    private val mutableState = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State>
        get() = mutableState

    fun loadStats() {
        viewModelScope.launch {
            try {
                val serverStats: StatsBundle? = if (syncManager.isLoggedIn()) {
                    if (mutableState.value is State.Error) {
                        mutableState.value = State.Loading
                    }
                    statsManager.getServerStats()
                } else {
                    null
                }
                val stats = statsManager.mergeStats(serverStats?.values, statsManager.localStatsInServerFormat)

                val totalListened = stats[StatsBundle.SERVER_KEY_TOTAL_LISTENED] ?: 0
                val skipping = stats[StatsBundle.SERVER_KEY_SKIPPING] ?: 0
                val variableSpeed = stats[StatsBundle.SERVER_KEY_VARIABLE_SPEED] ?: 0
                val trimSilence = stats[StatsBundle.SERVER_KEY_SILENCE_REMOVAL] ?: 0
                val autoSkipping = stats[StatsBundle.SERVER_KEY_AUTO_SKIPPING] ?: 0

                val funnyText = FunnyTimeConverter().timeSecsToFunnyText(totalListened, application.resources)

                mutableState.value = State.Loaded(
                    totalListened = totalListened,
                    skipping = skipping,
                    variableSpeed = variableSpeed,
                    trimSilence = trimSilence,
                    autoSkipping = autoSkipping,
                    totalSaved = skipping + variableSpeed + trimSilence + autoSkipping,
                    funnyText = funnyText,
                    startedAt = serverStats?.startedAt
                )
                withContext(ioDispatcher) {
                    serverStats?.startedAt?.let { showAppReviewDialogIfPossible(it) }
                }
            } catch (e: Exception) {
                Timber.e(e)
                mutableState.value = State.Error
            }
        }
    }

    /*
     * If the user has listened to more than 2.5 hours the past 7 days
     * and has been using the app for more than a week
     * we request them to review the app
     */
    private suspend fun showAppReviewDialogIfPossible(statsStartedAt: Date) {
        val currentState = mutableState.value as? State.Loaded ?: return
        val playedUptoSumInSecs =
            episodeManager.calculatePlayedUptoSumInSecsWithinDays(DAYS_LIMIT_FOR_PLAYED_UPTO)
        val daysSinceStarted =
            if (statsStartedAt.time == 0L) {
                val message = "statsStartedAt is Unix epoch, which the server returns until the user has " +
                    "listening history synced. Treating that as if the user has used the app for 0 days"
                Timber.i(message)
                0
            } else {
                TimeUnit.MILLISECONDS.toDays(statsStartedAt.timeIntervalSinceNow())
            }
        val showAppReviewDialog =
            TimeUnit.SECONDS.toHours(playedUptoSumInSecs.toLong()) > SUM_PLAYED_UPTO_MIN_HOURS &&
                daysSinceStarted > MIN_DAYS_STATS_STARTED
        mutableState.update { currentState.copy(showAppReviewDialog = showAppReviewDialog) }
    }

    fun launchAppReviewDialog(activity: AppCompatActivity) {
        viewModelScope.launch {
            inAppReviewHelper.launchReviewDialog(
                activity = activity,
                delayInMs = IN_APP_REVIEW_LAUNCH_DELAY_IN_MS,
                sourceView = SourceView.STATS
            )
        }
    }

    companion object {
        private const val SUM_PLAYED_UPTO_MIN_HOURS = 2.5
        private const val DAYS_LIMIT_FOR_PLAYED_UPTO = 7
        private const val MIN_DAYS_STATS_STARTED = 7
        private const val IN_APP_REVIEW_LAUNCH_DELAY_IN_MS = 1000L
    }
}
