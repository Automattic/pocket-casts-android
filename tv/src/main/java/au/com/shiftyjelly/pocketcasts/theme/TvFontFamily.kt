package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import au.com.shiftyjelly.pocketcasts.R

@OptIn(ExperimentalTextApi::class)
private fun googleSans(weight: Int) = Font(
    resId = R.font.google_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

internal val GoogleSansFontFamily = FontFamily(
    googleSans(400),
    googleSans(500),
    googleSans(510),
    googleSans(700),
)
