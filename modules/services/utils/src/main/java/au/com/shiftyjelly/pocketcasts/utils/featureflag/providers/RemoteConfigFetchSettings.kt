package au.com.shiftyjelly.pocketcasts.utils.featureflag.providers

interface RemoteConfigFetchSettings {
    fun getVersionCode(): Int
    fun getRemoteConfigFetchedVersionCode(): Int
    fun setRemoteConfigFetchedVersionCode(versionCode: Int)
}
