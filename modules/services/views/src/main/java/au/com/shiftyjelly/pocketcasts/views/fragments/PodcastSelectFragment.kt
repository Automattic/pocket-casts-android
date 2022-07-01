package au.com.shiftyjelly.pocketcasts.views.fragments

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralPodcastsSelected
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.views.databinding.SettingsFragmentPodcastSelectBinding
import au.com.shiftyjelly.pocketcasts.views.databinding.SettingsRowPodcastBinding
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

private const val ARG_TINT_COLOR = "tintColor"
private const val ARG_TOOLBAR = "toolbar"

@AndroidEntryPoint
class PodcastSelectFragment : BaseFragment() {
    companion object {
        fun newInstance(@ColorInt tintColor: Int?, showToolbar: Boolean = false): PodcastSelectFragment {
            val bundle = Bundle()
            if (tintColor != null) {
                bundle.putInt(ARG_TINT_COLOR, tintColor)
                bundle.putBoolean(ARG_TOOLBAR, showToolbar)
            }
            val fragment = PodcastSelectFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    interface Listener {
        fun podcastSelectFragmentSelectionChanged(newSelection: List<String>)
        fun podcastSelectFragmentGetCurrentSelection(): List<String>
    }

    lateinit var listener: Listener
    private var adapter: PodcastSelectAdapter? = null
    private var binding: SettingsFragmentPodcastSelectBinding? = null

    @Inject lateinit var podcastManager: PodcastManager
    val disposables = CompositeDisposable()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment is Listener) {
            listener = parentFragment as Listener
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = SettingsFragmentPodcastSelectBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        val tintColor = arguments?.getInt(ARG_TINT_COLOR)
        val imageLoader = PodcastImageLoaderThemed(view.context)

        binding.toolbarLayout.isVisible = arguments?.getBoolean(ARG_TOOLBAR, false) ?: false
        if (binding.toolbarLayout.isVisible) {
            (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        }
        if (tintColor != null) {
            binding.btnSelect.setTextColor(tintColor)
        }
        podcastManager.findSubscribedRx()
            .zipWith(Single.fromCallable { listener.podcastSelectFragmentGetCurrentSelection() })
            .map { pair ->
                val podcasts = pair.first
                val selected = pair.second
                return@map podcasts
                    .sortedBy { PodcastsSortType.cleanStringForSort(it.title) }
                    .map { SelectablePodcast(it, selected.contains(it.uuid)) }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onError = { Timber.e(it) },
                onSuccess = {
                    val adapter = PodcastSelectAdapter(it, tintColor, imageLoader) {
                        val selectedList = it.map { it.uuid }
                        binding.lblPodcastsChosen.text = resources.getStringPluralPodcastsSelected(selectedList.size)
                        listener.podcastSelectFragmentSelectionChanged(selectedList)
                    }

                    val selected = it.filter { it.selected }
                    binding.lblPodcastsChosen.text = resources.getStringPluralPodcastsSelected(selected.size)
                    binding.recyclerView.layoutManager = layoutManager
                    binding.recyclerView.adapter = adapter

                    updateSelectButtonText(adapter.selectedPodcasts.size, adapter.list.size)
                    binding.btnSelect.setOnClickListener {
                        if (adapter.selectedPodcasts.size == adapter.list.size) { // Everything is selected
                            adapter.deselectAll()
                        } else {
                            adapter.selectAll()
                        }

                        updateSelectButtonText(adapter.selectedPodcasts.size, adapter.list.size)
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

private data class SelectablePodcast(val podcast: Podcast, var selected: Boolean)
private class PodcastSelectAdapter(val list: List<SelectablePodcast>, @ColorInt val tintColor: Int?, val imageLoader: PodcastImageLoader, val onSelectionChanged: (selected: List<Podcast>) -> Unit) : RecyclerView.Adapter<PodcastSelectAdapter.PodcastViewHolder>() {

    class PodcastViewHolder(val binding: SettingsRowPodcastBinding) : RecyclerView.ViewHolder(binding.root)

    val selectedPodcasts: List<Podcast>
        get() = list.filter { it.selected }.map { it.podcast }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodcastViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = SettingsRowPodcastBinding.inflate(inflater, parent, false)
        return PodcastViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.count()
    }

    override fun onBindViewHolder(holder: PodcastViewHolder, position: Int) {
        val item = list[position]
        holder.binding.lblTitle.text = item.podcast.title
        holder.binding.lblSubtitle.text = item.podcast.author
        holder.binding.checkbox.isChecked = item.selected

        imageLoader.loadSmallImage(item.podcast).into(holder.binding.imageView)

        holder.itemView.setOnClickListener {
            holder.binding.checkbox.isChecked = !holder.binding.checkbox.isChecked
        }
        holder.binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            item.selected = isChecked
            onSelectionChanged(selectedPodcasts)
        }

        if (tintColor != null) {
            holder.binding.checkbox.buttonTintList = ColorStateList.valueOf(tintColor)
        }
    }

    override fun onViewRecycled(holder: PodcastViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.checkbox.setOnCheckedChangeListener(null)
    }

    fun selectAll() {
        list.forEach { it.selected = true }
        notifyDataSetChanged()
        onSelectionChanged(selectedPodcasts)
    }

    fun deselectAll() {
        list.forEach { it.selected = false }
        notifyDataSetChanged()
        onSelectionChanged(selectedPodcasts)
    }
}
