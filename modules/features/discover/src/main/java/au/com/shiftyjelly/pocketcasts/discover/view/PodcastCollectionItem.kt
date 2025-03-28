package au.com.shiftyjelly.pocketcasts.discover.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import au.com.shiftyjelly.pocketcasts.discover.databinding.PodcastCollectionItemBinding
import au.com.shiftyjelly.pocketcasts.discover.extensions.updateSubscribeButtonIcon
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeDrawable
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class PodcastCollectionItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val imageRequestFactory = PocketCastsImageRequestFactory(context).smallSize().themed()
    private var binding: PodcastCollectionItemBinding? = null

    init {
        binding = PodcastCollectionItemBinding.inflate(LayoutInflater.from(context), this, true)
    }

    var podcast: DiscoverPodcast? = null
        set(value) {
            field = value

            if (value != null) {
                val binding = binding ?: return

                binding.lblTitle.text = value.title
                binding.lblSubtitle.text = value.author
                imageRequestFactory.createForPodcast(podcast?.uuid).loadInto(binding.imageView)
                binding.btnSubscribe.updateSubscribeButtonIcon(value.isSubscribed)
            } else {
                clear()
            }
        }

    var onSubscribeClicked: (() -> Unit)? = null
        set(value) {
            field = value
            binding?.btnSubscribe?.setOnClickListener {
                binding?.btnSubscribe?.updateSubscribeButtonIcon(true)
                onSubscribeClicked?.invoke()
            }
        }

    fun clear() {
        val binding = binding ?: return

        binding.lblTitle.text = null
        binding.lblSubtitle.text = null
        binding.imageView.setImageResource(binding.imageView.context.getThemeDrawable(UR.attr.defaultArtworkSmall))
    }
}
