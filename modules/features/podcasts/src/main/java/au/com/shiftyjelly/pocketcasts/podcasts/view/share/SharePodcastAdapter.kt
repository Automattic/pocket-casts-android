package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterSharePodcastBinding
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed

class SharePodcastAdapter(
    context: Context,
    private val clickListener: ClickListener
) : ListAdapter<Podcast, SharePodcastAdapter.ViewHolder>(podcastDiff) {

    companion object {
        private const val ACCESSIBILITY_SELECTED = ". Podcast already selected. Unselect"
        private const val ACCESSIBILITY_UNSELECTED = ". Select"
    }

    var selectedPodcastUuids = emptySet<String>()

    private val imageLoader = PodcastImageLoaderThemed(context)

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AdapterSharePodcastBinding.inflate(inflater, parent, false)
        return ViewHolder(binding, clickListener)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).adapterId
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val podcast = getItem(position)
        holder.podcast = podcast

        val title = podcast.title
        holder.titleText.text = title

        if (selectedPodcastUuids.contains(podcast.uuid)) {
            holder.checkbox.isSelected = true
            holder.imageView.scaleX = 0.7f
            holder.imageView.scaleY = 0.7f
            holder.button.contentDescription = title + ACCESSIBILITY_SELECTED
        } else {
            holder.checkbox.isSelected = false
            holder.imageView.scaleX = 1f
            holder.imageView.scaleY = 1f
            holder.button.contentDescription = title
            holder.button.contentDescription = title + ACCESSIBILITY_UNSELECTED
        }

        imageLoader
            .load(podcast) { holder.titleText.visibility = View.GONE }
            .into(holder.imageView)
    }

    interface ClickListener {
        fun onPodcastSelected(podcast: Podcast)
        fun onPodcastUnselected(podcast: Podcast)
    }

    class ViewHolder(binding: AdapterSharePodcastBinding, private val clickListener: ClickListener) : RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {

        var podcast: Podcast? = null

        val titleText = binding.titleText
        val checkbox = binding.checkbox
        val button = binding.button
        val imageView = binding.imageView

        init {
            checkbox.isSelected = true

            checkbox.setOnClickListener(this)
            button.setOnClickListener(this)
            button.setOnLongClickListener(this)
        }

        override fun onClick(view: View) {
            val sizeFrom: Float
            val sizeTo: Float
            imageView.scaleX = 1f
            imageView.scaleY = 1f
            val title = podcast?.title ?: ""
            if (checkbox.isSelected) {
                checkbox.isSelected = false
                podcast?.let { clickListener.onPodcastUnselected(it) }
                button.contentDescription = title + ACCESSIBILITY_UNSELECTED
                sizeFrom = 0.7f
                sizeTo = 1f
            } else {
                checkbox.isSelected = true
                podcast?.let { clickListener.onPodcastSelected(it) }
                button.contentDescription = title + ACCESSIBILITY_SELECTED
                sizeFrom = 1f
                sizeTo = 0.7f
            }
            val animation = ScaleAnimation(sizeFrom, sizeTo, sizeFrom, sizeTo, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
            animation.duration = 200
            animation.fillAfter = true
            animation.interpolator = AccelerateDecelerateInterpolator()
            imageView.startAnimation(animation)
        }

        override fun onLongClick(v: View): Boolean {
            if (checkbox.isSelected) {
                return false
            }
            onClick(v)
            return true
        }
    }
}

private val podcastDiff = object : DiffUtil.ItemCallback<Podcast>() {
    override fun areItemsTheSame(oldPodcast: Podcast, newPodcast: Podcast): Boolean {
        return oldPodcast.uuid == newPodcast.uuid
    }

    override fun areContentsTheSame(oldPodcast: Podcast, newPodcast: Podcast): Boolean {
        return oldPodcast == newPodcast
    }
}
