package au.com.shiftyjelly.pocketcasts.discover.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.extensions.updateSubscribeButtonIcon
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.views.extensions.setRippleBackground
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class PodcastGridRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val imageLoader = PodcastImageLoaderThemed(context)
    private val lblTitle: TextView
    private val lblSubtitle: TextView
    private val btnSubscribe: ImageButton
    private val imagePodcast: ImageView
    private var imageSize: Int? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.item_grid, this, true)
        lblTitle = findViewById(R.id.lblTitle)
        lblSubtitle = findViewById(R.id.lblSubtitle)
        btnSubscribe = findViewById(R.id.btnSubscribe)
        imagePodcast = findViewById(R.id.imagePodcast)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
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
                loadImage()
                isVisible = true
                btnSubscribe.updateSubscribeButtonIcon(subscribed = value.isSubscribed, colorSubscribed = UR.attr.contrast_01, colorUnsubscribed = UR.attr.contrast_01)
            } else {
                clear()
            }
        }

    fun updateImageSize(imageSize: Int) {
        this.imageSize = imageSize
        loadImage()
    }

    private fun loadImage() {
        val podcast = podcast
        val imageSize = imageSize
        if (podcast != null && imageSize != null) {
            imageLoader.loadCoil(podcastUuid = podcast.uuid, size = imageSize).into(imagePodcast)
        }
    }

    var onSubscribeClicked: (() -> Unit)? = null
        set(value) {
            field = value
            btnSubscribe.setOnClickListener {
                btnSubscribe.updateSubscribeButtonIcon(subscribed = true, colorSubscribed = UR.attr.contrast_01, colorUnsubscribed = UR.attr.contrast_01)
                onSubscribeClicked?.invoke()
            }
        }

    var onPodcastClicked: (() -> Unit)? = null
        set(value) {
            field = value
            this.setOnClickListener {
                onPodcastClicked?.invoke()
            }
        }

    fun clear() {
        lblTitle.text = null
        lblSubtitle.text = null
        imagePodcast.setImageBitmap(null)
        isVisible = false
    }
}
