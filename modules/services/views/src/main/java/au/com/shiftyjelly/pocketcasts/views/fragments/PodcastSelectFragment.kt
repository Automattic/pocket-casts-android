package au.com.shiftyjelly.pocketcasts.views.fragments

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import androidx.core.os.BundleCompat
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.views.databinding.SettingsFragmentPodcastSelectBinding
import au.com.shiftyjelly.pocketcasts.views.viewmodels.PodcastSelectViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class PodcastSelectFragment : BaseFragment() {
    companion object {
        private const val NEW_INSTANCE_ARG = "tintColor"

        fun newInstance(
            @ColorInt tintColor: Int? = null,
            showToolbar: Boolean = false,
            source: PodcastSelectFragmentSource,
        ): PodcastSelectFragment =
            PodcastSelectFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(
                        NEW_INSTANCE_ARG,
                        PodcastSelectFragmentArgs(
                            tintColor = tintColor,
                            showToolbar = showToolbar,
                            source = source,
                        ),
                    )
                }
            }

        private fun extractArgs(bundle: Bundle?): PodcastSelectFragmentArgs? =
            bundle?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARG, PodcastSelectFragmentArgs::class.java) }
    }

    interface Listener {
        fun podcastSelectFragmentSelectionChanged(newSelection: List<String>)
        fun podcastSelectFragmentGetCurrentSelection(): List<String>
    }

    lateinit var listener: Listener
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

        val args = extractArgs(arguments)
            ?: throw IllegalStateException("${this::class.java.simpleName} is missing arguments. It must be created with newInstance function")

        source = args.source

        source?.let { viewModel.trackOnShown(it) }

        binding.rootComposeView.setContentWithViewCompositionStrategy {
            AppTheme(theme.activeTheme) {
                val podcastsState = viewModel.selectablePodcasts.collectAsState()
                val podcasts = podcastsState.value
                val selectedCount = podcasts.count { it.selected }
                val selectButtonText = if (podcasts.size == selectedCount) {
                    stringResource(LR.string.select_none)
                } else {
                    stringResource(LR.string.select_all)
                }

                LaunchedEffect(Unit) {
                    val selectedUuids = listener.podcastSelectFragmentGetCurrentSelection()
                    viewModel.loadSelectablePodcasts(selectedUuids)
                }

                LaunchedEffect(podcasts) {
                    val selected = podcasts.filter { it.selected }.map { it.podcast }
                    onSelectionChanged(selected)
                }

                PodcastSelectPage(
                    args.showToolbar,
                    args.tintColor,
                    selectedCount,
                    selectButtonText,
                    podcasts,
                    onSelectClick = {
                        if (podcasts.size == selectedCount) {
                            source?.let { viewModel.trackOnSelectNoneTapped(it) }
                            viewModel.deselectAll()
                        } else {
                            source?.let { viewModel.trackOnSelectAllTapped(it) }
                            viewModel.selectAll()
                        }
                    },
                    onBackPressed = {
                        onBackPressed()
                    },
                    onPodcastToggled = { row, enabled ->
                        source?.let { viewModel.trackOnPodcastToggled(it, row.podcast.uuid, enabled) }
                        viewModel.togglePodcastSelection(row.podcast.uuid)
                        userChanged = true
                    },
                )
            }
        }
    }

    private fun onSelectionChanged(selected: List<Podcast>) {
        val selectedList = selected.map { it.uuid }
        listener.podcastSelectFragmentSelectionChanged(selectedList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        source?.let { viewModel.trackOnDismissed(it) }
        if (userChanged) {
            val selectedCount = viewModel.selectablePodcasts.value.count { it.selected }
            val props = buildMap { put("number_selected", selectedCount) }
            viewModel.trackChange(source, props)
        }
        binding = null
    }

    fun selectAll() {
        viewModel.selectAll()
    }

    fun deselectAll() {
        viewModel.deselectAll()
    }

    fun userChanged() = userChanged
}

data class SelectablePodcast(val podcast: Podcast, var selected: Boolean)

@Parcelize
private data class PodcastSelectFragmentArgs(
    val tintColor: Int?,
    val showToolbar: Boolean,
    val source: PodcastSelectFragmentSource,
) : Parcelable

enum class PodcastSelectFragmentSource(val analyticsValue: String) {
    AUTO_ADD("auto_add"),
    DOWNLOADS("downloads"),
    NOTIFICATIONS("notifications"),
    FILTERS("filters"),
}
