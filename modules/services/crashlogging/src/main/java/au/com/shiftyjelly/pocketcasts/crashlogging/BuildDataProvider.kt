package au.com.shiftyjelly.pocketcasts.crashlogging

interface BuildDataProvider {
    val buildPlatform: String
    val installSource: String?
}
