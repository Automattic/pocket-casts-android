package au.com.shiftyjelly.pocketcasts.wear.networking

import androidx.annotation.VisibleForTesting
import au.com.shiftyjelly.pocketcasts.BuildConfig
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.horologist.networks.data.NetworkInfo
import com.google.android.horologist.networks.data.NetworkStatus
import com.google.android.horologist.networks.data.NetworkType
import com.google.android.horologist.networks.data.Networks
import com.google.android.horologist.networks.data.RequestType
import com.google.android.horologist.networks.rules.Allow
import com.google.android.horologist.networks.rules.NetworkingRules
import com.google.android.horologist.networks.rules.RequestCheck

object PocketCastsNetworkingRules : NetworkingRules {
    override fun isHighBandwidthRequest(requestType: RequestType): Boolean {
        if (BuildConfig.DEBUG) {
            // For testing purposes fail if we get unknown requests
            check(requestType != RequestType.UnknownRequest) {
                "Unknown request type. Failing on debug builds."
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
        }.also { networkStatus ->
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Preferred network according to networking rules: $networkStatus")
        }

    private fun getPreferredNetworkForMedia(
        networks: Networks,
        requestType: RequestType,
    ): NetworkStatus? {
        val mediaRequestType = (requestType as? RequestType.MediaRequest)?.type
        return when (mediaRequestType) {
            // Not sure if MediaRequestType.Live will occur for us, but if it does, it seems like it
            // should be treated the same as a MediaRequestType.Stream
            RequestType.MediaRequest.MediaRequestType.Live,
            RequestType.MediaRequest.MediaRequestType.Stream ->
                // For streaming, assume a low bandwidth and use power efficient BT if available
                networks.networks.firstOrNull {
                    it.networkInfo.type == NetworkType.BT
                }

            RequestType.MediaRequest.MediaRequestType.Download -> null
            null -> null
        }
            // Otherwise, prefer faster networks if available
            ?: networks.networks.prefer(NetworkType.Wifi, NetworkType.Cell)
    }
}

/**
 * @param types The preferred network types in order of preference.
 * @return The most preferred network type that is available. If none of the preferred types are
 * available, the first available network is returned. See test cases for examples.
 */
@VisibleForTesting
internal fun List<NetworkStatus>.prefer(vararg types: NetworkType): NetworkStatus? =
    types.firstNotNullOfOrNull { type ->
        firstOrNull { it.networkInfo.type == type }
    } ?: firstOrNull()
