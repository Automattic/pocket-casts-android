package au.com.shiftyjelly.pocketcasts.filters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.filters.databinding.FiltersRowFiltersBinding
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralEpisodes
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.views.helper.PlaylistHelper
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

private val differ: DiffUtil.ItemCallback<Playlist> = object : DiffUtil.ItemCallback<Playlist>() {
    override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
        return oldItem.uuid == newItem.uuid
    }

    override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
        return oldItem == newItem
    }
}

class FiltersAdapter(val generateCountFlowable: (Playlist) -> (Flowable<Int>), val onRowClick: (Playlist) -> Unit) : ListAdapter<Playlist, FiltersAdapter.FilterViewHolder>(differ) {

    class FilterViewHolder(val binding: FiltersRowFiltersBinding) : RecyclerView.ViewHolder(binding.root) {
        var playlistDisposable: Disposable? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val binding = FiltersRowFiltersBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FilterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val playlist = getItem(position)
        PlaylistHelper.updateImageView(playlist, holder.binding.imgIcon)
        holder.binding.lblTitle.text = playlist.title
        holder.itemView.setOnClickListener { onRowClick(playlist) }

        holder.playlistDisposable?.dispose()
        holder.playlistDisposable = generateCountFlowable(playlist)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { holder.binding.lblSubtitle.text = holder.itemView.context.resources.getStringPluralEpisodes(it) }
            .subscribe()
    }

    override fun onViewRecycled(holder: FilterViewHolder) {
        super.onViewRecycled(holder)
        holder.playlistDisposable?.dispose()
    }
}
