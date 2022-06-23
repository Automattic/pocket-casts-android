package au.com.shiftyjelly.pocketcasts.discover.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.extensions.updateSubscribeButtonIcon
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeDrawable
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.extensions.setRippleBackground
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class PodcastRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val imageLoader = PodcastImageLoaderThemed(context)
    private val lblTitle: TextView
    private val lblSubtitle: TextView
    private val imageView: ImageView
    private val btnSubscribe: ImageView

    init {
        LayoutInflater.from(context).inflate(R.layout.row_podcast, this, true)
        lblTitle = findViewById(R.id.lblTitle)
        lblSubtitle = findViewById(R.id.lblSubtitle)
        imageView = findViewById(R.id.imageView)
        btnSubscribe = findViewById(R.id.btnSubscribe)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val eightDp = 8.dpToPx(context)
        setPadding(eightDp, eightDp, eightDp, eightDp)
        orientation = HORIZONTAL
        clipToPadding = false
        clipChildren = false

        setRippleBackground()
    }

    var podcast: DiscoverPodcast? = null
        set(value) {
            field = value

            if (value != null) {
                lblTitle.text = value.title
                lblSubtitle.text = value.author
                imageLoader.loadSmallImage(podcast?.uuid).into(imageView)
                isVisible = true
                btnSubscribe.updateSubscribeButtonIcon(value.isSubscribed)
            } else {
                clear()
            }
        }

    var onSubscribeClicked: (() -> Unit)? = null
        set(value) {
            field = value
            btnSubscribe.setOnClickListener {
                btnSubscribe.updateSubscribeButtonIcon(true)
                onSubscribeClicked?.invoke()
            }
        }

    fun clear() {
        lblTitle.text = null
        lblSubtitle.text = null
        imageView.setImageResource(imageView.context.getThemeDrawable(UR.attr.defaultArtworkSmall))
    }
}
