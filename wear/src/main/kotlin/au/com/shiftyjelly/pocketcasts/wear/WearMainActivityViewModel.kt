package au.com.shiftyjelly.pocketcasts.wear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.WatchSync
import com.google.android.gms.wearable.DataClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WearMainActivityViewModel @Inject constructor(
    private val watchSync: WatchSync
) : ViewModel() {

    val phoneSyncDataListener = DataClient.OnDataChangedListener { dataEventBuffer ->
        viewModelScope.launch {
            watchSync.processDataChange(dataEventBuffer)
        }
    }

    fun checkLatestSyncData() {
        viewModelScope.launch {
            watchSync.processLatestData()
        }
    }
}
