package au.com.shiftyjelly.pocketcasts.discover.view

import android.content.res.ColorStateList
import android.text.TextUtils
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.extensions.updateSubscribeButtonIcon
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import com.google.android.material.chip.Chip
import java.util.Locale
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class CarouselItemViewHolder(val theme: Theme, itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val cardBack: View = itemView.findViewById(R.id.cardBack)
    private val imageView: ImageView = itemView.findViewById(R.id.imageView)
    private val lblTitle: TextView = itemView.findViewById(R.id.lblTitle)
    private val lblSubtitle: TextView = itemView.findViewById(R.id.lblSubtitle)
    private val lblTagline: Chip = itemView.findViewById(R.id.lblTagline)
    private val lblRanking: TextView = itemView.findViewById(R.id.lblRank)
    val btnSubscribe: ImageButton = itemView.findViewById(R.id.btnSubscribe)

    private val imageLoader = PodcastImageLoaderThemed(itemView.context)

    private fun reset() {
        setBackingGradient(0)
        imageView.setImageDrawable(null)
        lblTitle.text = null
        lblSubtitle.text = null
        lblTagline.text = null
    }

    private fun setBackingGradient(@ColorInt color: Int) {
        cardBack.setBackgroundColor(ThemeColor.podcastUi03(theme.activeTheme, color))
    }

    fun setRanking(text: String?) {
        lblRanking.text = text
        lblRanking.visibility = if (TextUtils.isEmpty(text)) View.GONE else View.VISIBLE
    }

    fun setTaglineText(text: String?) {
        lblTagline.text = text?.uppercase(Locale.getDefault())
        lblTagline.isVisible = text != null
        lblTagline.setTextColor(ThemeColor.contrast02(theme.activeTheme))
        lblTagline.chipBackgroundColor = ColorStateList.valueOf(ThemeColor.contrast04(theme.activeTheme))
    }

    var podcast: DiscoverPodcast? = null
        set(value) {
            field = value
            if (value != null) {
                imageLoader.loadPodcastUuid(value.uuid).into(imageView)
                lblTitle.text = value.title
                lblTitle.setTextColor(ThemeColor.contrast01(theme.activeTheme))
                lblSubtitle.text = value.author
                lblSubtitle.setTextColor(ThemeColor.contrast03(theme.activeTheme))
                setBackingGradient(value.color)
                btnSubscribe.updateSubscribeButtonIcon(
                    subscribed = value.isSubscribed,
                    colorSubscribed = UR.attr.contrast_02,
                    colorUnsubscribed = UR.attr.contrast_02
                )
            } else {
                reset()
            }
        }
}
