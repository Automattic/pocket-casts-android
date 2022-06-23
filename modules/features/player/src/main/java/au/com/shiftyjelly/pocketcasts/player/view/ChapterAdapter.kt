package au.com.shiftyjelly.pocketcasts.player.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.AdapterChapterBinding

class ChapterViewHolder(val binding: AdapterChapterBinding) : RecyclerView.ViewHolder(binding.root)

class ChapterAdapter(val listener: ChapterListener) : ListAdapter<Chapter, ChapterViewHolder>(CHAPTER_ADAPTER_DIFF) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ChapterViewHolder(DataBindingUtil.inflate(inflater, R.layout.adapter_chapter, parent, false))
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        val chapter = getItem(position)
        holder.binding.chapter = chapter
        holder.binding.root.setOnClickListener {
            listener.onChapterClick(chapter)
        }
        holder.binding.chapterUrl.setOnClickListener {
            listener.onChapterUrlClick(chapter)
        }
        holder.binding.executePendingBindings()
    }
}

private val CHAPTER_ADAPTER_DIFF = object : DiffUtil.ItemCallback<Chapter>() {
    override fun areItemsTheSame(oldItem: Chapter, newItem: Chapter): Boolean {
        return oldItem.startTime == newItem.startTime
    }

    override fun areContentsTheSame(oldItem: Chapter, newItem: Chapter): Boolean {
        return oldItem == newItem
    }
}
