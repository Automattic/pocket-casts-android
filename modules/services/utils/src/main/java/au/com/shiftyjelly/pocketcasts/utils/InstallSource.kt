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
        fun fromInstallerPackageName(packageName: String?) = when {
            packageName == null -> Unknown
            packageName == "com.android.vending" -> GooglePlay
            packageName.startsWith("com.amazon.") -> AmazonAppstore
            else -> Other
        }
    }
}
