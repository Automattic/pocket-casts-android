package au.com.shiftyjelly.pocketcasts.views.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPlural
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.getSerializableCompat
import au.com.shiftyjelly.pocketcasts.views.databinding.FragmentFilterSelectBinding
import au.com.shiftyjelly.pocketcasts.views.databinding.SettingsRowFilterBinding
import au.com.shiftyjelly.pocketcasts.views.helper.PlaylistHelper
import au.com.shiftyjelly.pocketcasts.views.viewmodels.FilterSelectViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class FilterSelectFragment private constructor() : BaseFragment() {
    interface Listener {
        fun filterSelectFragmentSelectionChanged(newSelection: List<String>)
        fun filterSelectFragmentGetCurrentSelection(): List<String>
    }

    enum class Source {
        AUTO_DOWNLOAD,
        PODCAST_SETTINGS,
    }

    companion object {

        private const val ARG_FILTER_ALL_PODCAST_FILTERS = "allPodcastsFilters"
        private const val ARG_FILTER_SOURCE = "source"

        fun newInstance(
            source: Source,
            shouldFilterPlaylistsWithAllPodcasts: Boolean = false
        ): Fragment =
            FilterSelectFragment().apply {
                arguments = bundleOf(
                    ARG_FILTER_ALL_PODCAST_FILTERS to shouldFilterPlaylistsWithAllPodcasts,
                    ARG_FILTER_SOURCE to source
                )
            }
    }

    private val viewModel: FilterSelectViewModel by viewModels()

    private lateinit var listener: Listener
    private var adapter: FilterSelectAdapter? = null
    private var binding: FragmentFilterSelectBinding? = null
    private var hasChanged = false

    @Inject lateinit var playlistManager: PlaylistManager

    val disposables = CompositeDisposable()

    val shouldFilterPlaylistsWithAllPodcasts: Boolean
        get() = arguments?.getBoolean(ARG_FILTER_ALL_PODCAST_FILTERS) ?: false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as Listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFilterSelectBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        playlistManager.observeAll().firstOrError()
            .zipWith(Single.fromCallable { listener.filterSelectFragmentGetCurrentSelection() })
            .map {
                val filters = it.first
                val selected = it.second
                filters
                    .filter { !shouldFilterPlaylistsWithAllPodcasts || !it.allPodcasts }
                    .map {
                        SelectableFilter(it, selected.contains(it.uuid))
                    }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onError = { Timber.e(it) },
                onSuccess = {
                    val adapter = FilterSelectAdapter(theme.isDarkTheme, it) {
                        val selectedList = it.map { it.uuid }
                        binding?.lblFiltersChosen?.text = resources.getStringPlural(count = selectedList.size, singular = LR.string.filters_chosen_singular, plural = LR.string.filters_chosen_plural)
                        listener.filterSelectFragmentSelectionChanged(selectedList)
                        hasChanged = true
                    }

                    val selected = it.filter { it.selected }
                    binding?.lblFiltersChosen?.text = resources.getStringPlural(count = selected.size, singular = LR.string.filters_chosen_singular, plural = LR.string.filters_chosen_plural)
                    binding?.recyclerView?.layoutManager = layoutManager
                    binding?.recyclerView?.adapter = adapter

                    updateSelectButtonText(adapter.selectedFilters.size, adapter.list.size)
                    binding?.btnSelect?.setOnClickListener {
                        if (adapter.selectedFilters.size == adapter.list.size) { // Everything is selected
                            adapter.deselectAll()
                        } else {
                            adapter.selectAll()
                        }

                        updateSelectButtonText(adapter.selectedFilters.size, adapter.list.size)
                    }

                    this.adapter = adapter
                }
            )
            .addTo(disposables)
    }

    private fun updateSelectButtonText(selectedSize: Int, listSize: Int) {
        val btnSelect = binding?.btnSelect ?: return
        if (selectedSize == listSize) { // Everything is selected
            btnSelect.text = getString(LR.string.select_none)
        } else {
            btnSelect.text = getString(LR.string.select_all)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposables.clear()
        binding = null
        if (hasChanged) {
            val source = arguments?.getSerializableCompat(ARG_FILTER_SOURCE, Source::class.java)
            if (source != null) {
                viewModel.trackFilterChange(source)
            }
        }
    }

    fun selectAll() {
        val itemCount = adapter?.list?.size ?: 0
        adapter?.selectAll()
        updateSelectButtonText(itemCount, itemCount)
    }

    fun deselectAll() {
        adapter?.deselectAll()
        updateSelectButtonText(0, adapter?.list?.size ?: 0)
    }
}

private data class SelectableFilter(val filter: Playlist, var selected: Boolean)
private class FilterSelectAdapter(val isDarkTheme: Boolean, val list: List<SelectableFilter>, val onSelectionChanged: (selected: List<Playlist>) -> Unit) : RecyclerView.Adapter<FilterSelectAdapter.FilterViewHolder>() {
    class FilterViewHolder(val binding: SettingsRowFilterBinding) : RecyclerView.ViewHolder(binding.root)

    val selectedFilters: List<Playlist>
        get() = list.filter { it.selected }.map { it.filter }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = SettingsRowFilterBinding.inflate(inflater, parent, false)
        return FilterViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.count()
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val item = list[position]
        holder.binding.lblTitle.text = item.filter.title
        holder.binding.checkbox.isChecked = item.selected
        PlaylistHelper.updateImageView(item.filter, holder.binding.imageView)
        holder.itemView.setOnClickListener {
            holder.binding.checkbox.isChecked = !holder.binding.checkbox.isChecked
        }
        holder.binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            item.selected = isChecked
            onSelectionChanged(selectedFilters)
        }
    }

    override fun onViewRecycled(holder: FilterViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.checkbox.setOnCheckedChangeListener(null)
    }

    fun selectAll() {
        list.forEach { it.selected = true }
        notifyDataSetChanged()
        onSelectionChanged(selectedFilters)
    }

    fun deselectAll() {
        list.forEach { it.selected = false }
        notifyDataSetChanged()
        onSelectionChanged(selectedFilters)
    }
}
