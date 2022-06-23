package au.com.shiftyjelly.pocketcasts.settings.status

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.support.Support
import au.com.shiftyjelly.pocketcasts.settings.status.ServiceStatusChecker.Check.Internet
import au.com.shiftyjelly.pocketcasts.settings.status.ServiceStatusChecker.Check.Urls
import au.com.shiftyjelly.pocketcasts.utils.AnalyticsHelper
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val serviceStatusChecker: ServiceStatusChecker,
    val support: Support
) : ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val services = mutableListOf(
        Service(
            title = LR.string.settings_status_service_internet,
            summary = LR.string.settings_status_service_internet_summary,
            help = LR.string.settings_status_service_internet_help,
            check = Internet
        ),
        Service(
            title = LR.string.settings_status_service_refresh,
            summary = LR.string.settings_status_service_refresh_summary,
            help = LR.string.settings_status_service_ad_blocker_help_singular,
            helpArgs = listOf("refresh.pocketcasts.com"),
            check = Urls(listOf("https://refresh.pocketcasts.com/health.html"))
        ),
        Service(
            title = LR.string.settings_status_service_account,
            summary = LR.string.settings_status_service_account_summary,
            help = LR.string.settings_status_service_ad_blocker_help_singular,
            helpArgs = listOf("api.pocketcasts.com"),
            check = Urls(listOf("https://api.pocketcasts.com/health"))
        ),
        Service(
            title = LR.string.settings_status_service_discover,
            summary = LR.string.settings_status_service_discover_summary,
            help = LR.string.settings_status_service_ad_blocker_help_plural,
            helpArgs = listOf("static.pocketcasts.com, cache.pocketcasts.com and podcasts.pocketcasts.com"),
            check = Urls(
                listOf(
                    "https://static.pocketcasts.com/discover/android/content.json",
                    "https://cache.pocketcasts.com/mobile/podcast/full/e7a6f7d0-02f2-0133-1c51-059c869cc4eb"
                )
            )
        ),
        Service(
            title = LR.string.settings_status_service_hosts,
            summary = LR.string.settings_status_service_hosts_summary,
            help = LR.string.settings_status_service_hosts_help,
            check = Urls(
                listOf("https://dts.podtrac.com/redirect.mp3/static.pocketcasts.com/assets/feeds/status/episode1.mp3")
            )
        )
    )

    // Backing property to avoid state updates from other classes
    private val _uiState = MutableStateFlow<StatusUiState>(StatusUiState.Welcome)
    val uiState: StateFlow<StatusUiState> = _uiState

    private var job: Job? = null

    fun run() {
        updateServicesUi(running = true)
        job?.cancel()
        job = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // mark all the services as queued for checking
                services.forEachIndexed { index, service ->
                    services[index] = service.copy(status = ServiceStatus.Queued)
                }
                updateServicesUi(running = true)
                // check each service
                services.forEachIndexed { index, service ->
                    // mark service as running check
                    services[index] = service.copy(status = ServiceStatus.Running)
                    updateServicesUi(running = true)
                    // run the check
                    val status = serviceStatusChecker.check(service.check)
                    // set the new service status for the UI
                    services[index] = service.copy(status = status)
                    updateServicesUi(running = true)
                }
                updateServicesUi(running = false)
            }
        }
    }

    fun sendReport(context: Context) {
        val log = "Status Report\n" + services.joinToString("\n") { it.toSummary(context) }
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, log)

        launch {
            try {
                val intent = support.sendEmail(subject = "Android status report.", intro = "Hi there, just needed help with something...", context)
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                UiUtil.displayDialogNoEmailApp(context)
            }
        }

        AnalyticsHelper.statusReportSent()
    }

    private fun updateServicesUi(running: Boolean) {
        _uiState.value = StatusUiState.ListServices(services = services.toList(), running = running)
    }
}

data class Service(
    @StringRes val title: Int,
    @StringRes val summary: Int,
    @StringRes val help: Int,
    val helpArgs: List<String> = emptyList(),
    val check: ServiceStatusChecker.Check,
    val status: ServiceStatus = ServiceStatus.Queued
) {

    fun toSummary(context: Context): String {
        return "${context.getString(title)}, $check, $status"
    }

    fun helpString(context: Context): String {
        return context.getString(help, *helpArgs.toTypedArray())
    }
}

sealed class StatusUiState {
    object Welcome : StatusUiState()
    data class ListServices(val services: List<Service>, val running: Boolean) : StatusUiState()
}
