package au.com.shiftyjelly.pocketcasts.wear.networking

import androidx.annotation.VisibleForTesting
import au.com.shiftyjelly.pocketcasts.BuildConfig
import com.google.android.horologist.networks.ExperimentalHorologistNetworksApi
import com.google.android.horologist.networks.data.NetworkInfo
import com.google.android.horologist.networks.data.NetworkStatus
import com.google.android.horologist.networks.data.NetworkType
import com.google.android.horologist.networks.data.Networks
import com.google.android.horologist.networks.data.RequestType
import com.google.android.horologist.networks.data.RequestType.MediaRequest.Companion.DownloadRequest
import com.google.android.horologist.networks.data.RequestType.MediaRequest.Companion.LiveRequest
import com.google.android.horologist.networks.rules.Allow
import com.google.android.horologist.networks.rules.NetworkingRules
import com.google.android.horologist.networks.rules.RequestCheck
import timber.log.Timber

object PocketCastsNetworkingRules : NetworkingRules {
    override fun isHighBandwidthRequest(requestType: RequestType): Boolean {
        if (BuildConfig.DEBUG) {
            // For testing purposes fail if we get unknown requests
            if (requestType == RequestType.UnknownRequest) {
                Timber.e("Unknown request type. Requests should be one of: ${RequestType::class.java}")
            }
        }

        return requestType is RequestType.MediaRequest
    }

    override fun checkValidRequest(
        requestType: RequestType,
        currentNetworkInfo: NetworkInfo,
    ): RequestCheck = Allow

    override fun getPreferredNetwork(
        networks: Networks,
        requestType: RequestType,
    ): NetworkStatus? =

        when (requestType) {
            is RequestType.MediaRequest, RequestType.ImageRequest -> {
                getPreferredNetworkForMedia(networks, requestType)
            }

            is RequestType.ImageRequest,
            RequestType.ApiRequest -> {
                networks.networks.prefer(NetworkType.Wifi, NetworkType.Cell)
            }

            else -> {
                networks.networks.prefer(NetworkType.Wifi)
            }
        }

    private fun getPreferredNetworkForMedia(
        networks: Networks,
        requestType: RequestType,
    ): NetworkStatus? {

        // Always prefer Wifi if it is active
        networks.networks.firstOrNull { it.networkInfo is NetworkInfo.Wifi }?.let {
            return it
        }

        return when (requestType) {
            DownloadRequest -> {
                // For downloads force LTE as the backup to Wifi, to avoid slow downloads.
                networks.networks.firstOrNull {
                    it.networkInfo.type == NetworkType.Cell
                }
            }
            LiveRequest -> {
                // For live streaming, assume a low bandwidth and use power efficient BT
                networks.networks.firstOrNull {
                    it.networkInfo.type == NetworkType.BT
                }
            }
            else -> networks.networks.firstOrNull()
        }
    }
}

/**
 * @param types The preferred network types in order of preference.
 * @return The most preferred network type that is available. If none of the preferred types are
 * available, the first available network is returned. See test cases for examples.
 */
@OptIn(ExperimentalHorologistNetworksApi::class)
@VisibleForTesting
internal fun List<NetworkStatus>.prefer(vararg types: NetworkType): NetworkStatus? =
    types.firstNotNullOfOrNull { type ->
        firstOrNull { it.networkInfo.type == type }
    } ?: firstOrNull()
