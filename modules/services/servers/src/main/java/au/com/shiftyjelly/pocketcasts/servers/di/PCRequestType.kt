package au.com.shiftyjelly.pocketcasts.servers.di

/**
 * This is for mapping horologists RequestType from parts of the app that cannot depend
 * on horologist because it requires api 26. In addition, this avoids propogating
 * @OptIn(ExperimentalHorologistApi::class) throughout the app.
 */
enum class PCRequestType {
    Download,
    Api,
}
