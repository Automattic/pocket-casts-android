package au.com.shiftyjelly.pocketcasts.player.view

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.compose.components.SegmentedTabBar
import au.com.shiftyjelly.pocketcasts.compose.components.SegmentedTabBarDefaults
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentEffectsBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel.PlaybackEffectsSettingsTab
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.Debouncer
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.extensions.roundedSpeed
import au.com.shiftyjelly.pocketcasts.views.extensions.updateTint
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class EffectsFragment :
    BaseDialogFragment(),
    CompoundButton.OnCheckedChangeListener,
    MaterialButtonToggleGroup.OnButtonCheckedListener,
    View.OnClickListener {

    @Inject lateinit var stats: StatsManager

    @Inject lateinit var settings: Settings

    @Inject lateinit var playbackManager: PlaybackManager

    override val statusBarIconColor = StatusBarIconColor.Light

    private val viewModel: PlayerViewModel by activityViewModels()
    private var binding: FragmentEffectsBinding? = null
    private val trimToggleGroupButtonIds = arrayOf(R.id.trimLow, R.id.trimMedium, R.id.trimHigh)
    private var playbackSpeedTrackingDebouncer: Debouncer = Debouncer()

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            viewModel.trackPlaybackEffectsEvent(AnalyticsEvent.PLAYBACK_EFFECT_SETTINGS_VIEW_APPEARED)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentEffectsBinding.inflate(inflater, container, false)

        binding?.setupEffectsSettingsSegmentedTabBar()

        viewModel.effectsLive.value?.let { update(it) } // Make sure the window is the correct size before opening or else it won't expand properly
        viewModel.effectsLive.observe(viewLifecycleOwner) { podcastEffectsPair ->
            update(podcastEffectsPair)
            ensureExpanded()
        }

        updateTrimState()

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun update(podcastEffectsData: PlayerViewModel.PodcastEffectsData) {
        val effects = podcastEffectsData.effects

        val binding = binding ?: return

        binding.lblSpeed.text = String.format("%.1fx", effects.playbackSpeed)

        binding.btnSpeedUp.setOnClickListener(this)
        binding.btnSpeedDown.setOnClickListener(this)
        binding.lblSpeed.setOnClickListener(this)

        val trimSilence = effects.trimMode != TrimMode.OFF
        binding.switchTrim.setOnCheckedChangeListener(null)
        binding.switchTrim.isChecked = trimSilence
        binding.switchTrim.setOnCheckedChangeListener(this)

        binding.trimToggleGroup.removeOnButtonCheckedListener(this)
        if (trimSilence) {
            val toCheck = trimToggleGroupButtonIds[effects.trimMode.ordinal - 1]
            if (binding.trimToggleGroup.checkedButtonId != toCheck) {
                binding.trimToggleGroup.check(toCheck)
            }
        }
        binding.trimToggleGroup.addOnButtonCheckedListener(this)

        updateTrimState()

        binding.switchVolume.setOnCheckedChangeListener(null)
        binding.switchVolume.isChecked = effects.isVolumeBoosted
        binding.switchVolume.setOnCheckedChangeListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.playingEpisodeLive.observe(viewLifecycleOwner) { (_, backgroundColor) ->
            setDialogTint(backgroundColor)

            val tintColor = theme.playerHighlightColor(viewModel.podcast)
            val playerContrast01 = ThemeColor.playerContrast01(theme.activeTheme)

            binding?.switchTrim?.updateTint(tintColor, playerContrast01)
            binding?.switchVolume?.updateTint(tintColor, playerContrast01)

            val trimButtonTextColor = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked), // Enabled
                    intArrayOf(),
                ),
                intArrayOf(
                    backgroundColor,
                    playerContrast01,
                ),
            )
            binding?.trimLow?.setTextColor(trimButtonTextColor)
            binding?.trimMedium?.setTextColor(trimButtonTextColor)
            binding?.trimHigh?.setTextColor(trimButtonTextColor)
        }
    }

    private fun changePlaybackSpeed(effects: PlaybackEffects, podcast: Podcast, amount: Double) {
        val binding = binding ?: return

        val speed = amount.roundedSpeed()
        effects.playbackSpeed = speed
        binding.lblSpeed.text = String.format("%.1fx", effects.playbackSpeed)
        viewModel.saveEffects(effects, podcast)

        viewLifecycleOwner.lifecycleScope.launch {
            playbackSpeedTrackingDebouncer.debounce {
                viewModel.effectsLive.value?.effects?.playbackSpeed?.roundedSpeed()?.let { currentSpeed ->
                    trackPlaybackEffectsEvent(
                        event = AnalyticsEvent.PLAYBACK_EFFECT_SPEED_CHANGED,
                        props = mapOf(PlaybackManager.SPEED_KEY to currentSpeed),
                    )
                }
            }
        }
    }

    private fun updateTrimState() {
        val binding = binding ?: return

        val checked = binding.switchTrim.isChecked
        val context = binding.root.context

        binding.effectsConstraint.updatePadding(bottom = if (checked) 16.dpToPx(context) else 68.dpToPx(context))

        binding.trimToggleGroup.isVisible = checked
        binding.detailSilenceLabel.text = if (checked) {
            val timeSaved = stats.timeSavedSilenceRemovalSecs
            val formattedTime = TimeHelper.formattedSeconds(timeSaved.toDouble(), "%d hours, %d minutes, %d seconds")
            if (timeSaved > 3600) {
                getString(LR.string.player_time_saved_detail, formattedTime)
            } else {
                getString(LR.string.player_time_saved_no_hour)
            }
        } else {
            getString(LR.string.player_trim_silence_detail)
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        val binding = binding ?: return
        val (podcast, effects) = viewModel.effectsLive.value ?: return

        if (buttonView.id == binding.switchTrim.id) {
            trackPlaybackEffectsEvent(AnalyticsEvent.PLAYBACK_EFFECT_TRIM_SILENCE_TOGGLED, mapOf(PlaybackManager.ENABLED_KEY to isChecked))
            if (effects.trimMode == TrimMode.OFF && isChecked) {
                effects.trimMode = TrimMode.LOW
                this.binding?.trimToggleGroup?.check(R.id.trimLow)
            } else if (effects.trimMode != TrimMode.OFF && !isChecked) {
                effects.trimMode = TrimMode.OFF
            }
            viewModel.saveEffects(effects, podcast)

            updateTrimState()
        } else if (buttonView.id == binding.switchVolume.id) {
            trackPlaybackEffectsEvent(AnalyticsEvent.PLAYBACK_EFFECT_VOLUME_BOOST_TOGGLED, mapOf(PlaybackManager.ENABLED_KEY to isChecked))
            effects.isVolumeBoosted = isChecked
            viewModel.saveEffects(effects, podcast)
        }
    }

    override fun onButtonChecked(group: MaterialButtonToggleGroup, checkedId: Int, isChecked: Boolean) {
        val binding = binding ?: return
        val (podcast, effects) = viewModel.effectsLive.value ?: return

        if (group.id == binding.trimToggleGroup.id && isChecked) {
            val index = trimToggleGroupButtonIds.indexOf(checkedId)
            val newTrimMode = TrimMode.values()[index + 1]
            if (effects.trimMode != newTrimMode) {
                effects.trimMode = newTrimMode
                trackPlaybackEffectsEvent(AnalyticsEvent.PLAYBACK_EFFECT_TRIM_SILENCE_AMOUNT_CHANGED, mapOf(PlaybackManager.AMOUNT_KEY to newTrimMode.analyticsVale))
                viewModel.saveEffects(effects, podcast)
            }
        }
    }

    override fun onClick(view: View) {
        val binding = binding ?: return
        val (podcast, effects) = viewModel.effectsLive.value ?: return

        when (view.id) {
            binding.btnSpeedUp.id -> changePlaybackSpeed(effects, podcast, effects.playbackSpeed + 0.1)
            binding.btnSpeedDown.id -> changePlaybackSpeed(effects, podcast, effects.playbackSpeed - 0.1)
            binding.lblSpeed.id -> {
                when (effects.playbackSpeed) {
                    1.0 -> changePlaybackSpeed(effects, podcast, 1.5)
                    1.5 -> changePlaybackSpeed(effects, podcast, 2.0)
                    else -> changePlaybackSpeed(effects, podcast, 1.0)
                }
            }
        }
    }

    private fun trackPlaybackEffectsEvent(event: AnalyticsEvent, props: Map<String, Any> = emptyMap()) {
        viewModel.trackPlaybackEffectsEvent(event, props)
    }

    private fun FragmentEffectsBinding.setupEffectsSettingsSegmentedTabBar() {
        effectsSettingsSegmentedTabBar.setContent {
            val podcastHeaderBackgroundColor by remember {
                viewModel.playingEpisodeLive.asFlow()
                    .map { it.second }
                    .distinctUntilChangedBy { it }
            }.collectAsStateWithLifecycle(null)

            val podcastEffectsData by remember {
                viewModel.effectsLive.asFlow()
                    .distinctUntilChanged { t1, t2 ->
                        t1.podcast.uuid == t2.podcast.uuid &&
                            t1.podcast.playbackEffects.toData() == t2.podcast.playbackEffects.toData() &&
                            t1.podcast.overrideGlobalEffects == t2.podcast.overrideGlobalEffects &&
                            t1.podcast.overrideGlobalEffectsModified == t2.podcast.overrideGlobalEffectsModified
                    }
            }.collectAsStateWithLifecycle(null)
            val podcast = podcastEffectsData?.podcast ?: return@setContent

            if (podcastEffectsData?.showCustomEffectsSettings == true) {
                val selectedTabTextColor = podcastHeaderBackgroundColor?.let {
                    Color(android.graphics.Color.parseColor(ColorUtils.colorIntToHexString(it)))
                } ?: Color.Black
                EffectsSettingsSegmentedTabBar(
                    selectedItem = if (podcastEffectsData?.podcast?.overrideGlobalEffects == true) {
                        PlaybackEffectsSettingsTab.ThisPodcast
                    } else {
                        PlaybackEffectsSettingsTab.AllPodcasts
                    },
                    selectedTabTextColor = selectedTabTextColor,
                    onItemSelect = {
                        viewModel.onEffectsSettingsSegmentedTabSelected(podcast, PlaybackEffectsSettingsTab.entries[it])
                    },
                    modifier = Modifier
                        .padding(top = 24.dp),
                )
            }
        }
    }

    @Composable
    private fun EffectsSettingsSegmentedTabBar(
        selectedTabTextColor: Color,
        selectedItem: PlaybackEffectsSettingsTab,
        onItemSelect: (Int) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        SegmentedTabBar(
            items = PlaybackEffectsSettingsTab.entries.map { stringResource(it.labelResId) },
            selectedIndex = PlaybackEffectsSettingsTab.entries.indexOf(selectedItem),
            colors = SegmentedTabBarDefaults.colors.copy(
                selectedTabBackgroundColor = MaterialTheme.theme.colors.playerContrast01,
                selectedTabTextColor = selectedTabTextColor,
                borderColor = MaterialTheme.theme.colors.playerContrast03,
            ),
            cornerRadius = 120.dp,
            modifier = modifier.fillMaxWidth(),
            onSelectItem = onItemSelect,
        )
    }

    @Preview(widthDp = 360)
    @Composable
    private fun EffectsSettingsSegmentedBarPreview() {
        EffectsSettingsSegmentedTabBar(
            selectedItem = PlaybackEffectsSettingsTab.AllPodcasts,
            selectedTabTextColor = Color.Black,
            onItemSelect = {},
        )
    }
}
