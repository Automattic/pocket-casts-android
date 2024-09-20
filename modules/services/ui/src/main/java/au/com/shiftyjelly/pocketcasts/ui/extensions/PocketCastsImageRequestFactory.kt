package au.com.shiftyjelly.pocketcasts.ui.extensions

import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.ui.images.ThemedImageTintTransformation
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

fun PocketCastsImageRequestFactory.themed() = copy(
    isDarkTheme = Theme.isDark(context),
    transformations = listOf(ThemedImageTintTransformation(context)),
)
