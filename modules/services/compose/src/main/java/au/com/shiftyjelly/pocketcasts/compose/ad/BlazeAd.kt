package au.com.shiftyjelly.pocketcasts.compose.ad

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BlazeAd(
    val id: String,
    val title: String,
    val ctaText: String,
    val ctaUrl: String,
    val imageUrl: String,
) : Parcelable
