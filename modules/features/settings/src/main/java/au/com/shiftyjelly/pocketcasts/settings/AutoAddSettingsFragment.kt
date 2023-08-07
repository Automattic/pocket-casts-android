package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralPodcastsSelected
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoAddUpNextLimitBehaviour
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.settings.databinding.AdapterAutoAddPodcastBinding
import au.com.shiftyjelly.pocketcasts.settings.databinding.AdapterHeaderBinding
import au.com.shiftyjelly.pocketcasts.settings.databinding.AdapterOptionRowBinding
import au.com.shiftyjelly.pocketcasts.settings.databinding.AdapterPlainTextRowBinding
import au.com.shiftyjelly.pocketcasts.settings.databinding.FragmentAutoAddSettingsBinding
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.AutoAddSettingsViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragmentSource
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class AutoAddSettingsFragment : BaseFragment(), PodcastSelectFragment.Listener {
    @Inject lateinit var settings: Settings

    private var _binding: FragmentAutoAddSettingsBinding? = null
    private val binding get() = _binding!!
    val viewModel by activityViewModels<AutoAddSettingsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onShown()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAutoAddSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setup(title = getString(LR.string.settings_auto_up_next_title), navigationIcon = BackArrow, activity = activity, theme = theme)

        val topAdapter = AutoAddTopAdapter()
        val headerRow = AutoAddTopSections.Header(getString(LR.string.settings_auto_up_next_podcasts))

        val autoAddAdapter = AutoAddPodcastAdapter(PodcastImageLoaderThemed(view.context)) {
            OptionsDialog()
                .setTitle(getString(LR.string.settings_auto_up_next_add_to))
                .addCheckedOption(
                    titleString = getString(LR.string.settings_auto_up_next_top),
                    checked = it.autoAddToUpNext == Podcast.AutoAddUpNext.PLAY_NEXT,
                    click = { viewModel.updatePodcast(it, Podcast.AutoAddUpNext.PLAY_NEXT) }
                )
                .addCheckedOption(
                    titleString = getString(LR.string.settings_auto_up_next_bottom),
                    checked = it.autoAddToUpNext == Podcast.AutoAddUpNext.PLAY_LAST,
                    click = { viewModel.updatePodcast(it, Podcast.AutoAddUpNext.PLAY_LAST) }
                )
                .show(childFragmentManager, "autoadd_options")
        }

        val concatAdapter = ConcatAdapter(topAdapter, autoAddAdapter)
        binding.recyclerView.adapter = concatAdapter

        viewModel.autoAddPodcasts.observe(viewLifecycleOwner) { state ->
            val limitRow = AutoAddTopSections.Option(
                IR.drawable.ic_upnext_playlast,
                getString(
                    LR.string.settings_auto_up_next_limit
                ),
                getString(
                    LR.string.episodes_plural, settings.autoAddUpNextLimit.flow.value
                )
            ) {
                val currentLimit = settings.autoAddUpNextLimit.flow.value
                OptionsDialog()
                    .setTitle(getString(LR.string.settings_auto_up_next_limit))
                    .addCheckedOption(titleString = getString(LR.string.episodes_plural, 10), checked = currentLimit == 10) { viewModel.autoAddUpNextLimitChanged(10) }
                    .addCheckedOption(titleString = getString(LR.string.episodes_plural, 20), checked = currentLimit == 20) { viewModel.autoAddUpNextLimitChanged(20) }
                    .addCheckedOption(titleString = getString(LR.string.episodes_plural, 50), checked = currentLimit == 50) { viewModel.autoAddUpNextLimitChanged(50) }
                    .addCheckedOption(titleString = getString(LR.string.episodes_plural, 100), checked = currentLimit == 100) { viewModel.autoAddUpNextLimitChanged(100) }
                    .addCheckedOption(titleString = getString(LR.string.episodes_plural, 200), checked = currentLimit == 200) { viewModel.autoAddUpNextLimitChanged(200) }
                    .addCheckedOption(titleString = getString(LR.string.episodes_plural, 500), checked = currentLimit == 500) { viewModel.autoAddUpNextLimitChanged(500) }
                    .addCheckedOption(titleString = getString(LR.string.episodes_plural, 1000), checked = currentLimit == 1000) { viewModel.autoAddUpNextLimitChanged(1000) }
                    .show(childFragmentManager, "autoadd_options")
            }

            val podcasts = state.autoAddPodcasts
            val optionSubtitle = when (state.behaviour) {
                AutoAddUpNextLimitBehaviour.ONLY_ADD_TO_TOP -> getString(LR.string.settings_auto_up_next_limit_reached_top)
                AutoAddUpNextLimitBehaviour.STOP_ADDING -> getString(LR.string.settings_auto_up_next_limit_reached_stop)
            }
            val optionRow = AutoAddTopSections.Option(null, getString(LR.string.settings_auto_up_next_limit_reached), optionSubtitle) {
                OptionsDialog()
                    .setTitle(getString(LR.string.settings_auto_up_next_add_to))
                    .addCheckedOption(titleId = LR.string.settings_auto_up_next_limit_reached_stop, checked = state.behaviour == AutoAddUpNextLimitBehaviour.STOP_ADDING) {
                        viewModel.autoAddUpNextLimitBehaviorChanged(AutoAddUpNextLimitBehaviour.STOP_ADDING)
                    }
                    .addCheckedOption(titleId = LR.string.settings_auto_up_next_limit_reached_top, checked = state.behaviour == AutoAddUpNextLimitBehaviour.ONLY_ADD_TO_TOP) {
                        viewModel.autoAddUpNextLimitBehaviorChanged(AutoAddUpNextLimitBehaviour.ONLY_ADD_TO_TOP)
                    }
                    .show(childFragmentManager, "autoadd_options")
            }
            val chosenText = resources.getStringPluralPodcastsSelected(podcasts.size)
            val podcastsChosenRow = AutoAddTopSections.Option(null, getString(LR.string.settings_choose_podcasts), chosenText) { openPodcastsList() }

            val topFooter = when (state.behaviour) {
                AutoAddUpNextLimitBehaviour.ONLY_ADD_TO_TOP -> {
                    AutoAddTopSections.Footer(getString(LR.string.settings_auto_up_next_limit_reached_top_summary, state.limit))
                }
                AutoAddUpNextLimitBehaviour.STOP_ADDING -> {
                    AutoAddTopSections.Footer(getString(LR.string.settings_auto_up_next_limit_reached_stop_summary, state.limit))
                }
            }

            val topSections = listOf(limitRow, optionRow, podcastsChosenRow, topFooter, headerRow)
            topAdapter.submitList(topSections)

            autoAddAdapter.submitList(podcasts)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }

    private fun openPodcastsList() {
        val fragment = PodcastSelectFragment.newInstance(
            tintColor = ThemeColor.primaryInteractive01(theme.activeTheme),
            showToolbar = true,
            source = PodcastSelectFragmentSource.AUTO_ADD
        )
        fragment.listener = this
        (activity as? FragmentHostListener)?.addFragment(fragment)
    }

    override fun podcastSelectFragmentSelectionChanged(newSelection: List<String>) {
        viewModel.selectionUpdated(newSelection)
    }

    override fun podcastSelectFragmentGetCurrentSelection(): List<String> {
        return viewModel.autoAddPodcasts.value?.autoAddPodcasts?.map { it.uuid } ?: emptyList()
    }
}

private val PodcastAutoAddDiff = object : DiffUtil.ItemCallback<Podcast>() {
    override fun areItemsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
        return oldItem.uuid == newItem.uuid
    }

    override fun areContentsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
        return oldItem.autoAddToUpNext == newItem.autoAddToUpNext
    }
}

sealed class AutoAddTopSections {
    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AutoAddTopSections>() {
            override fun areItemsTheSame(oldItem: AutoAddTopSections, newItem: AutoAddTopSections): Boolean {
                return oldItem::class.java == newItem::class.java
            }

            override fun areContentsTheSame(oldItem: AutoAddTopSections, newItem: AutoAddTopSections): Boolean {
                if (oldItem::class.java != newItem::class.java) return false

                return when (oldItem) {
                    is PlainText -> oldItem.text == (newItem as PlainText).text
                    is Option -> oldItem.subtitle == (newItem as Option).subtitle
                    else -> oldItem == newItem
                }
            }
        }
    }

    data class PlainText(val text: String, val onClick: () -> Unit) : AutoAddTopSections()
    data class Option(val iconResId: Int?, val title: String, val subtitle: String, val onClick: () -> Unit) : AutoAddTopSections()
    data class Footer(val text: String) : AutoAddTopSections()
    data class Header(val text: String) : AutoAddTopSections()
}

class AutoAddTopAdapter : ListAdapter<AutoAddTopSections, RecyclerView.ViewHolder>(AutoAddTopSections.DIFF) {
    class PlainTextViewHolder(val binding: AdapterPlainTextRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AutoAddTopSections.PlainText) {
            binding.lblText.text = item.text
            binding.root.setOnClickListener { item.onClick() }
        }
    }

    class OptionViewHolder(val binding: AdapterOptionRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AutoAddTopSections.Option) {
            if (item.iconResId != null) {
                binding.imgIcon.setImageResource(item.iconResId)
            } else {
                binding.imgIcon.setImageDrawable(null)
            }
            binding.lblTitle.text = item.title
            binding.lblSubtitle.text = item.subtitle
            binding.root.setOnClickListener { item.onClick() }
        }
    }

    class HeaderViewHolder(val binding: AdapterHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AutoAddTopSections.Header) {
            binding.lblHeader.text = item.text
        }
    }

    class FooterViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(item: AutoAddTopSections.Footer) {
            textView.text = item.text
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is AutoAddTopSections.PlainText -> R.layout.adapter_plain_text_row
            is AutoAddTopSections.Option -> R.layout.adapter_option_row
            is AutoAddTopSections.Header -> R.layout.adapter_header
            is AutoAddTopSections.Footer -> R.layout.adapter_footer
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.adapter_plain_text_row -> PlainTextViewHolder(AdapterPlainTextRowBinding.inflate(inflater, parent, false))
            R.layout.adapter_option_row -> OptionViewHolder(AdapterOptionRowBinding.inflate(inflater, parent, false))
            R.layout.adapter_header -> HeaderViewHolder(AdapterHeaderBinding.inflate(inflater, parent, false))
            R.layout.adapter_footer -> FooterViewHolder(inflater.inflate(viewType, parent, false) as TextView)
            else -> throw IllegalStateException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is PlainTextViewHolder -> holder.bind(item as AutoAddTopSections.PlainText)
            is OptionViewHolder -> holder.bind(item as AutoAddTopSections.Option)
            is HeaderViewHolder -> holder.bind(item as AutoAddTopSections.Header)
            is FooterViewHolder -> holder.bind(item as AutoAddTopSections.Footer)
        }
    }
}

class AutoAddPodcastAdapter(val imageLoader: PodcastImageLoader, val onClick: (Podcast) -> Unit) : ListAdapter<Podcast, AutoAddPodcastAdapter.ViewHolder>(PodcastAutoAddDiff) {
    class ViewHolder(val binding: AdapterAutoAddPodcastBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdapterAutoAddPodcastBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val podcast = getItem(position)

        holder.binding.apply {
            imageLoader.load(podcast).into(imageView)
            lblTitle.text = podcast.title
            val resources = holder.itemView.resources
            lblSubtitle.text = when (podcast.autoAddToUpNext) {
                Podcast.AutoAddUpNext.PLAY_NEXT -> resources.getString(LR.string.settings_auto_up_next_to_top)
                Podcast.AutoAddUpNext.PLAY_LAST -> resources.getString(LR.string.settings_auto_up_next_to_bottom)
                Podcast.AutoAddUpNext.OFF -> null
            }
            root.setOnClickListener { onClick(podcast) }
        }
    }
}
