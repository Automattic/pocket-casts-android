package au.com.shiftyjelly.pocketcasts.ui.images

import android.content.Context
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class PodcastImageLoaderThemed(context: Context) : PodcastImageLoader(context = context, isDarkTheme = Theme.isDark(context), transformations = listOf(ThemedImageTintTransformation(context)))
