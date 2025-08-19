package au.com.shiftyjelly.pocketcasts.compose.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry

private const val ANIMATION_DURATION = 350

private val intOffsetAnimationSpec = tween<IntOffset>(ANIMATION_DURATION)

fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInToStart(): EnterTransition = slideIntoContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.Start,
    animationSpec = intOffsetAnimationSpec,
)

fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToStart(): ExitTransition = slideOutOfContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.Start,
    animationSpec = intOffsetAnimationSpec,
)

fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInToEnd(): EnterTransition = slideIntoContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.End,
    animationSpec = intOffsetAnimationSpec,
)

fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToEnd(): ExitTransition = slideOutOfContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.End,
    animationSpec = intOffsetAnimationSpec,
)
