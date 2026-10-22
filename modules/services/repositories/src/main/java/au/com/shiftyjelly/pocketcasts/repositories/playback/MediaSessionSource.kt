package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.analytics.SourceView

/**
 * Package name of the Android Auto app, which connects to the media session when a phone is projected to a car.
 */
internal const val ANDROID_AUTO_PACKAGE_NAME = "com.google.android.projection.gearhead"

/**
 * Resolves the analytics source for a media session command.
 *
 * On Android Automotive OS every media session command comes from the car, so it's reported as
 * [SourceView.AUTOMOTIVE]. Commands sent by Android Auto are reported as [SourceView.ANDROID_AUTO].
 * Every other controller keeps [default].
 */
internal fun resolveMediaSessionSource(
    controllerPackageName: String?,
    isAutomotive: Boolean,
    default: SourceView,
): SourceView = when {
    isAutomotive -> SourceView.AUTOMOTIVE
    controllerPackageName == ANDROID_AUTO_PACKAGE_NAME -> SourceView.ANDROID_AUTO
    else -> default
}
