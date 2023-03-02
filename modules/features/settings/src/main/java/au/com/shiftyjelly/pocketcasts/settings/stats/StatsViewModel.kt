package au.com.shiftyjelly.pocketcasts.settings.stats

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.StatsBundle
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.servers.account.SyncAccountManager
import au.com.shiftyjelly.pocketcasts.settings.util.FunnyTimeConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    val statsManager: StatsManager,
    val settings: Settings,
    val syncAccountManager: SyncAccountManager,
    val application: Application
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
            val startedAt: Date?
        ) : State()
    }

    private val mutableState = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State>
        get() = mutableState

    fun loadStats() {
        viewModelScope.launch {
            try {
                val serverStats: StatsBundle? = if (syncAccountManager.isLoggedIn()) {
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
            } catch (e: Exception) {
                Timber.e(e)
                mutableState.value = State.Error
            }
        }
    }
}
