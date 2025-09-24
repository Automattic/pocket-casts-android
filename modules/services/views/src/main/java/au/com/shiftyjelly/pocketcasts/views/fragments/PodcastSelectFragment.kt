package au.com.shiftyjelly.pocketcasts.views.fragments

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralPodcastsSelected
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.views.databinding.SettingsFragmentPodcastSelectBinding
import au.com.shiftyjelly.pocketcasts.views.databinding.SettingsRowPodcastBinding
import au.com.shiftyjelly.pocketcasts.views.viewmodels.PodcastSelectViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class PodcastSelectFragment : BaseFragment() {
    companion object {
        private const val NEW_INSTANCE_ARG = "tintColor"

        fun createArgs(
            @ColorInt tintColor: Int? = null,
            showToolbar: Boolean = false,
            source: PodcastSelectFragmentSource,
        ) = Bundle().apply {
            putParcelable(
                NEW_INSTANCE_ARG,
                PodcastSelectFragmentArgs(
                    tintColor = tintColor,
                    showToolbar = showToolbar,
                    source = source,
                ),
            )
        }

        fun newInstance(
            @ColorInt tintColor: Int? = null,
            showToolbar: Boolean = false,
            source: PodcastSelectFragmentSource,
        ): PodcastSelectFragment = PodcastSelectFragment().apply {
            arguments = createArgs(
                tintColor = tintColor,
                showToolbar = showToolbar,
                source = source,
            )
        }

        private fun extractArgs(bundle: Bundle?): PodcastSelectFragmentArgs? = bundle?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARG, PodcastSelectFragmentArgs::class.java) }
    }

    interface Listener {
        fun podcastSelectFragmentSelectionChanged(newSelection: List<String>)
        fun podcastSelectFragmentGetCurrentSelection(): List<String>
    }

    lateinit var listener: Listener
    private var adapter: PodcastSelectAdapter? = null
    private var binding: SettingsFragmentPodcastSelectBinding? = null
    private var source: PodcastSelectFragmentSource? = null
    private var userChanged = false

    private val viewModel: PodcastSelectViewModel by viewModels()

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

        val args = extractArgs(arguments)
            ?: throw IllegalStateException("${this::class.java.simpleName} is missing arguments. It must be created with newInstance function")

        source = args.source

        source?.let { viewModel.trackOnShown(it) }

        val imageRequestFactory = PocketCastsImageRequestFactory(requireContext()).themed()
        binding.toolbarLayout.isVisible = args.showToolbar
        if (binding.toolbarLayout.isVisible) {
            (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        }
        if (args.tintColor != null) {
            binding.btnSelect.setTextColor(args.tintColor)
        }

        val selectedUuids = listener.podcastSelectFragmentGetCurrentSelection()
        viewModel.loadSelectablePodcasts(selectedUuids)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectablePodcasts.collect { podcastList ->
                        val adapter = PodcastSelectAdapter(
                            podcastList,
                            args.tintColor,
                            imageRequestFactory,
                            onPodcastToggled = { podcastUuid, enabled ->
                                source?.let { viewModel.trackOnPodcastToggled(it, podcastUuid, enabled) }
                            },
                            onSelectionChanged = {
                                val selectedList = it.map { it.uuid }
                                binding.lblPodcastsChosen.text =
                                    resources.getStringPluralPodcastsSelected(selectedList.size)
                                listener.podcastSelectFragmentSelectionChanged(selectedList)
                                userChanged = true
                            },
                        )

                        val selectedCount = podcastList.count { it.selected }
                        binding.lblPodcastsChosen.text =
                            resources.getStringPluralPodcastsSelected(selectedCount)
                        binding.recyclerView.layoutManager = layoutManager
                        binding.recyclerView.adapter = adapter

                        updateSelectButtonText(adapter.selectedPodcasts.size, adapter.list.size)
                        binding.btnSelect.setOnClickListener {
                            if (adapter.selectedPodcasts.size == adapter.list.size) {
                                source?.let { viewModel.trackOnSelectNoneTapped(it) }
                                adapter.deselectAll()
                            } else {
                                source?.let { viewModel.trackOnSelectAllTapped(it) }
                                adapter.selectAll()
                            }

                            updateSelectButtonText(adapter.selectedPodcasts.size, adapter.list.size)
                        }

                        this@PodcastSelectFragment.adapter = adapter
                    }
                }
            }
        }
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
        source?.let { viewModel.trackOnDismissed(it) }
        if (userChanged) {
            val props = buildMap {
                adapter?.selectedPodcasts?.size?.let {
                    put("number_selected", it)
                }
            }
            viewModel.trackChange(source, props)
        }
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

    fun userChanged() = userChanged
}

data class SelectablePodcast(val podcast: Podcast, var selected: Boolean)
private class PodcastSelectAdapter(val list: List<SelectablePodcast>, @ColorInt val tintColor: Int?, imageRequestFactory: PocketCastsImageRequestFactory, val onPodcastToggled: (uuid: String, enabled: Boolean) -> Unit, val onSelectionChanged: (selected: List<Podcast>) -> Unit) : RecyclerView.Adapter<PodcastSelectAdapter.PodcastViewHolder>() {
    val imageRequestFactory = imageRequestFactory.smallSize()

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

        imageRequestFactory.create(item.podcast).loadInto(holder.binding.imageView)

        holder.itemView.setOnClickListener {
            holder.binding.checkbox.isChecked = !holder.binding.checkbox.isChecked
        }
        holder.binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            item.selected = isChecked
            onPodcastToggled(item.podcast.uuid, isChecked)
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

@Parcelize
private data class PodcastSelectFragmentArgs(
    val tintColor: Int?,
    val showToolbar: Boolean,
    val source: PodcastSelectFragmentSource,
) : Parcelable

enum class PodcastSelectFragmentSource(val analyticsValue: String) {
    AUTO_ADD("auto_add"),
    NOTIFICATIONS("notifications"),
    FILTERS("filters"),
}
