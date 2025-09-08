package au.com.shiftyjelly.pocketcasts.account.watchsync

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapNotNull

class WearAuthManager(
    private val context: Context,
) : DataClient.OnDataChangedListener {

    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val _refreshToken = MutableStateFlow<String?>(null)

    val refreshToken: Flow<String> = _refreshToken.mapNotNull { it }

    /**
     * Start listening for Data Layer events.
     */
    fun startListening() {
        dataClient.addListener(this)
    }

    /**
     * Handle incoming Data Layer events.
     */
    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == "/custom_auth_v2"
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val refreshToken = dataMap.getString("refreshToken")
                val timestamp = dataMap.getLong("timestamp")
                val signature = dataMap.getString("signature")

                if (isValid(refreshToken, timestamp, signature)) {
                    refreshToken?.let {
                        storeCredentials(it)
                        _refreshToken.value = it
                    }
                }
            }
        }
    }

    /**
     * Validate received credentials.
     */
    private fun isValid(
        token: String?,
        timestamp: Long,
        signature: String?
    ): Boolean {
        if (token.isNullOrEmpty() || signature.isNullOrEmpty()) {
            return false
        }
        // Signature must match the mobile app’s signature
        if (signature != computeAppSignature()) {
            return false
        }
        // Reject if data is older than 1 hour
        val ageMillis = System.currentTimeMillis() - timestamp
        return ageMillis < 60 * 60 * 1000
    }

    /**
     * Compute this Wear app’s own signing certificate fingerprint,
     * to verify the mobile app signature.
     */
    private fun computeAppSignature(): String {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNATURES
        )
        val certBytes = packageInfo.signatures.orEmpty()
            .first()
            .toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(certBytes)
        return android.util.Base64.encodeToString(digest, android.util.Base64.NO_WRAP)
    }

    private fun storeCredentials(token: String) {
        context.getSharedPreferences("custom_auth", Context.MODE_PRIVATE)
            .edit()
            .putString("refreshToken", token)
            .apply()
    }

    fun stopListening() {
        dataClient.removeListener(this)
    }
}