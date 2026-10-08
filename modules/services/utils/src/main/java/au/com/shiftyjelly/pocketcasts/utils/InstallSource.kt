package au.com.shiftyjelly.pocketcasts.utils

enum class InstallSource(
    val analyticsValue: String,
) {
    GooglePlay(
        analyticsValue = "google_play",
    ),
    AmazonAppstore(
        analyticsValue = "amazon_appstore",
    ),
    Other(
        analyticsValue = "other",
    ),
    Unknown(
        analyticsValue = "unknown",
    ),
    ;

    companion object {
        fun fromInstallerPackageName(packageName: String?) = when (packageName) {
            "com.android.vending" -> GooglePlay
            "com.amazon.venezia" -> AmazonAppstore
            null -> Unknown
            else -> Other
        }
    }
}
