package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralPodcasts
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.extensions.hide
import au.com.shiftyjelly.pocketcasts.views.extensions.show
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class ShareListIncomingAdapter(
    private val clickListener: ClickListener,
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_PODCAST = 0
        private const val TYPE_HEADER = 1
        private const val TYPE_FOOTER = 2
    }

    var subscribedUuids = emptySet<String>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val imageLoader = PodcastImageLoaderThemed(context)
    private var title: String? = null
    private var description: String? = null
    private var podcasts: List<Podcast>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_PODCAST -> PodcastViewHolder(inflater.inflate(R.layout.discover_podcast_adapter, parent, false))
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.list_incoming_header, parent, false))
            TYPE_FOOTER -> FooterViewHolder(inflater.inflate(R.layout.list_incoming_footer, parent, false))
            else -> throw Exception("Shouldn't happen")
        }
    }

    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        val viewType = getItemViewType(position)
        when (viewType) {
            TYPE_PODCAST -> bindPodcastView(holder as PodcastViewHolder, position)
            TYPE_HEADER -> bindHeaderView(holder as HeaderViewHolder)
            TYPE_FOOTER -> bindFooterView(holder as FooterViewHolder)
        }
    }

    fun isSubscribedToAllPodcasts(): Boolean {
        return podcasts?.all { podcast -> subscribedUuids.contains(podcast.uuid) } ?: true
    }

    private fun bindPodcastView(holder: PodcastViewHolder, position: Int) {
        val podcasts = podcasts ?: return
        val podcast = podcasts[position - 1]
        val podcastUuid = podcast.uuid
        holder.titleText.text = podcast.title
        holder.descText.text = podcast.author

        val subscribed = subscribedUuids.contains(podcastUuid)
        updateTick(holder.tick, subscribed, podcast)

        holder.button.contentDescription = getPodcastDescription(podcast, subscribed)

        imageLoader
            .smallPlaceholder()
            .loadPodcastUuid(podcastUuid)
            .into(holder.image)
    }

    private fun getPodcastDescription(podcast: Podcast, subscribed: Boolean): String {
        if (podcast.title.isEmpty()) {
            return ""
        }

        // text for accessibility
        val text = StringBuilder()
        text.append(podcast.title)
            .append(" by ")
            .append(podcast.author)
            .append(if (subscribed) " button. You are subscribed. " else " button.")
        return text.toString()
    }

    private fun bindHeaderView(holder: HeaderViewHolder) {
        val podcasts = podcasts
        if (podcasts == null || podcasts.isEmpty()) {
            holder.itemView.hide()
        } else {
            holder.itemView.show()
            holder.titleText.text = title
            if (description.isNullOrBlank()) {
                holder.descText.hide()
            } else {
                holder.descText.show()
                holder.descText.text = description
            }
        }
    }

    private fun bindFooterView(holder: FooterViewHolder) {
        val podcasts = podcasts
        if (podcasts == null || podcasts.isEmpty()) {
            holder.countText.hide()
            holder.allButton.hide()
        } else {
            holder.countText.show()
            // hide the subscribe to all button when the user already is
            if (isSubscribedToAllPodcasts()) {
                holder.allButton.hide()
            } else {
                holder.allButton.show()
                holder.allButton.setText(LR.string.podcasts_share_subscribe_to_all)
                holder.allButton.setTextColor(context.getThemeColor(UR.attr.support_02))
            }
            holder.countText.text = context.resources.getStringPluralPodcasts(podcasts.size)
        }
    }

    override fun getItemCount(): Int {
        return (podcasts?.size ?: 0) + 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> TYPE_HEADER
            itemCount - 1 -> TYPE_FOOTER
            else -> TYPE_PODCAST
        }
    }

    private fun updateTick(imageView: ImageView, alreadyAdded: Boolean, podcast: Podcast) {
        if (alreadyAdded) {
            imageView.setImageResource(IR.drawable.ic_tick)
            ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(context.getThemeColor(UR.attr.support_02)))
            imageView.contentDescription = "Unsubscribe from " + podcast.title.ifEmpty { "podcast" } + " button."
            imageView.setOnClickListener { clickListener.onUnsubscribeClick(podcast) }
        } else {
            imageView.setImageResource(IR.drawable.ic_add_black_24dp)
            ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(context.getThemeColor(UR.attr.primary_icon_01)))
            imageView.contentDescription = "Subscribe to " + podcast.title.ifEmpty { "podcast" } + " button."
            imageView.setOnClickListener {
                updateTick(imageView, true, podcast)
                clickListener.onSubscribeClick(podcast)
            }
        }
    }

    fun load(title: String?, description: String?, podcasts: List<Podcast>?) {
        this.title = title
        this.description = description
        this.podcasts = podcasts
    }

    interface ClickListener {
        fun onPodcastClick(podcast: Podcast)
        fun onUnsubscribeClick(podcast: Podcast)
        fun onSubscribeToAllClick(podcasts: List<Podcast>)
        fun onSubscribeClick(podcast: Podcast)
    }

    inner class PodcastViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view), View.OnClickListener {
        var titleText = view.findViewById(R.id.podcast_title) as TextView
        var descText = view.findViewById(R.id.description) as TextView
        var image = view.findViewById(R.id.image) as ImageView
        var tick = view.findViewById(R.id.tick) as ImageView
        var button = view.findViewById(R.id.row_button) as View

        init {
            titleText.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            descText.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            image.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            button.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val podcasts = podcasts ?: return
            clickListener.onPodcastClick(podcasts[bindingAdapterPosition - 1])
        }
    }

    inner class HeaderViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        var titleText = view.findViewById(R.id.header_title) as TextView
        var descText = view.findViewById(R.id.header_description) as TextView
    }

    inner class FooterViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view), View.OnClickListener {
        var countText = view.findViewById(R.id.incoming_podcast_count) as TextView
        var allButton = view.findViewById(R.id.incoming_subscribe_to_all) as TextView

        init {
            allButton.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val podcasts = podcasts
            if (podcasts == null || podcasts.isEmpty()) {
                return
            }
            clickListener.onSubscribeToAllClick(podcasts)
        }
    }
}
