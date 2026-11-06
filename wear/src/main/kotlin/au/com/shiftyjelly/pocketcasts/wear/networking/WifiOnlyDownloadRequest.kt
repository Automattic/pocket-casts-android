package au.com.shiftyjelly.pocketcasts.wear.networking

import com.google.android.horologist.networks.data.RequestType

object WifiOnlyDownloadRequest : RequestType {
    override fun toString() = "media-download-wifi-only"
}
