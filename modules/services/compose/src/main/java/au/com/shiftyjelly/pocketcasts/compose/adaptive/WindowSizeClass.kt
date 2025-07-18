package au.com.shiftyjelly.pocketcasts.compose.adaptive

import androidx.window.core.layout.WindowSizeClass

fun WindowSizeClass.isAtLeastMediumWidth() = isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
fun WindowSizeClass.isAtMostMediumWidth() = isWidthAtMostBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

fun WindowSizeClass.isAtLeastMediumHeight() = isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
fun WindowSizeClass.isAtMostMediumHeight() = isHeightAtMostBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)

fun WindowSizeClass.isAtLeastExpandedWidth() = isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
fun WindowSizeClass.isAtMostExpandedWidth() = isWidthAtMostBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

fun WindowSizeClass.isAtLeastExpandedHeight() = isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND)
fun WindowSizeClass.isAtMostExpandedHeight() = isHeightAtMostBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND)

private fun WindowSizeClass.isWidthAtMostBreakpoint(widthBreakpoint: Int) = minWidthDp <= widthBreakpoint
private fun WindowSizeClass.isHeightAtMostBreakpoint(heightBreakpoint: Int) = minHeightDp <= heightBreakpoint
