package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.SegmentedTabBar
import au.com.shiftyjelly.pocketcasts.compose.components.SegmentedTabBarDefaults
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.text.rememberThemeAttributeColor
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel.PlaybackEffectsSettingsTab
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.Debouncer
import au.com.shiftyjelly.pocketcasts.utils.extensions.roundedSpeed
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.automattic.eventhorizon.PlaybackContentType
import com.automattic.eventhorizon.PlaybackEffectSettingsViewAppearedEvent
import com.automattic.eventhorizon.PlaybackEffectSpeedChangedEvent
import com.automattic.eventhorizon.PlaybackEffectTrimSilenceAmountChangedEvent
import com.automattic.eventhorizon.PlaybackEffectTrimSilenceToggledEvent
import com.automattic.eventhorizon.PlaybackEffectVolumeBoostToggledEvent
import com.automattic.eventhorizon.SettingType
import com.automattic.eventhorizon.Trackable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class EffectsFragment : BaseDialogFragment() {

    @Inject lateinit var stats: StatsManager

    @Inject lateinit var settings: Settings

    @Inject lateinit var playbackManager: PlaybackManager

    override val statusBarIconColor = StatusBarIconColor.Light

    override val includeNavigationBarPadding = false

    private val viewModel: PlayerViewModel by activityViewModels()
    private var playbackSpeedTrackingDebouncer: Debouncer = Debouncer()

    private var speedText by mutableStateOf("")
    private var trimMode by mutableStateOf(TrimMode.OFF)
    private var isVolumeBoosted by mutableStateOf(false)
    private var backgroundColor by mutableStateOf(Color.Black)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            viewModel.trackPlaybackEffectsEvent { sourceView, contentType, settingType ->
                PlaybackEffectSettingsViewAppearedEvent(
                    source = sourceView.analyticsValue,
                    contentType = contentType,
                    settings = settingType,
                )
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel.effectsLive.value?.let { update(it) } // Make sure the window is the correct size before opening or else it won't expand properly
        viewModel.effectsLive.observe(viewLifecycleOwner) { podcastEffectsData ->
            update(podcastEffectsData)
            ensureExpanded()
        }
        viewModel.playingEpisodeLive.observe(viewLifecycleOwner) { (_, backgroundColor) ->
            setDialogTint(backgroundColor)
            this.backgroundColor = Color(backgroundColor)
        }

        return contentWithoutConsumedInsets {
            AppTheme(theme.activeTheme) {
                EffectsPage(
                    speedText = speedText,
                    trimMode = trimMode,
                    trimDetail = trimDetail(),
                    isVolumeBoosted = isVolumeBoosted,
                    colors = EffectsColors(
                        background = backgroundColor,
                        contrast01 = rememberThemeAttributeColor(UR.attr.player_contrast_01),
                        contrast02 = rememberThemeAttributeColor(UR.attr.player_contrast_02),
                        contrast05 = rememberThemeAttributeColor(UR.attr.player_contrast_05),
                        switchTint = Color(theme.playerHighlightColor(viewModel.podcast)),
                        switchOff = Color(ThemeColor.playerContrast01(theme.activeTheme)),
                    ),
                    onSpeedDownClick = { onSpeedChange { it.playbackSpeed - 0.1 } },
                    onSpeedUpClick = { onSpeedChange { it.playbackSpeed + 0.1 } },
                    onSpeedClick = {
                        onSpeedChange {
                            when (it.playbackSpeed) {
                                1.0 -> 1.5
                                1.5 -> 2.0
                                else -> 1.0
                            }
                        }
                    },
                    onTrimToggle = ::onTrimToggle,
                    onTrimModeSelect = ::onTrimModeSelect,
                    onVolumeBoostToggle = ::onVolumeBoostToggle,
                    settingsTabBar = { EffectsSettingsTabBar() },
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Bottom))
                        .nestedScroll(rememberNestedScrollInteropConnection())
                        .verticalScroll(rememberScrollState())
                        .onSizeChanged { view?.post(::resettleExpanded) },
                )
            }
        }
    }

    private fun resettleExpanded() {
        val behavior = bottomSheetView()?.let(BottomSheetBehavior<View>::from) ?: return
        if (behavior.state == BottomSheetBehavior.STATE_SETTLING) {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun update(podcastEffectsData: PlayerViewModel.PodcastEffectsData) {
        val effects = podcastEffectsData.effects
        speedText = String.format("%.1fx", effects.playbackSpeed)
        trimMode = effects.trimMode
        isVolumeBoosted = effects.isVolumeBoosted
    }

    private fun trimDetail(): String {
        return if (trimMode != TrimMode.OFF) {
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

    private fun onSpeedChange(amount: (PlaybackEffects) -> Double) {
        val (podcast, effects) = viewModel.effectsLive.value ?: return
        changePlaybackSpeed(effects, podcast, amount(effects))
    }

    private fun changePlaybackSpeed(effects: PlaybackEffects, podcast: Podcast, amount: Double) {
        val speed = amount.roundedSpeed()
        effects.playbackSpeed = speed
        speedText = String.format("%.1fx", effects.playbackSpeed)
        viewModel.saveEffects(effects, podcast)

        viewLifecycleOwner.lifecycleScope.launch {
            playbackSpeedTrackingDebouncer.debounce {
                viewModel.effectsLive.value?.effects?.playbackSpeed?.roundedSpeed()?.let { currentSpeed ->
                    trackPlaybackEffectsEvent { sourceView, contentType, settingType ->
                        PlaybackEffectSpeedChangedEvent(
                            speed = currentSpeed,
                            source = sourceView.analyticsValue,
                            contentType = contentType,
                            settings = settingType,
                        )
                    }
                }
            }
        }
    }

    private fun onTrimToggle(isChecked: Boolean) {
        val (podcast, effects) = viewModel.effectsLive.value ?: return
        trackPlaybackEffectsEvent { sourceView, contentType, settingType ->
            PlaybackEffectTrimSilenceToggledEvent(
                enabled = isChecked,
                source = sourceView.analyticsValue,
                contentType = contentType,
                settings = settingType,
            )
        }
        if (effects.trimMode == TrimMode.OFF && isChecked) {
            effects.trimMode = TrimMode.LOW
        } else if (effects.trimMode != TrimMode.OFF && !isChecked) {
            effects.trimMode = TrimMode.OFF
        }
        trimMode = effects.trimMode
        viewModel.saveEffects(effects, podcast)
    }

    private fun onTrimModeSelect(newTrimMode: TrimMode) {
        val (podcast, effects) = viewModel.effectsLive.value ?: return
        if (effects.trimMode != newTrimMode) {
            effects.trimMode = newTrimMode
            trimMode = newTrimMode
            trackPlaybackEffectsEvent { sourceView, contentType, settingType ->
                PlaybackEffectTrimSilenceAmountChangedEvent(
                    amount = newTrimMode.analyticsValue,
                    source = sourceView.analyticsValue,
                    contentType = contentType,
                    settings = settingType,
                )
            }
            viewModel.saveEffects(effects, podcast)
        }
    }

    private fun onVolumeBoostToggle(isChecked: Boolean) {
        val (podcast, effects) = viewModel.effectsLive.value ?: return
        trackPlaybackEffectsEvent { sourceView, contentType, settingType ->
            PlaybackEffectVolumeBoostToggledEvent(
                enabled = isChecked,
                source = sourceView.analyticsValue,
                contentType = contentType,
                settings = settingType,
            )
        }
        effects.isVolumeBoosted = isChecked
        isVolumeBoosted = isChecked
        viewModel.saveEffects(effects, podcast)
    }

    private fun trackPlaybackEffectsEvent(event: (SourceView, PlaybackContentType, SettingType) -> Trackable) {
        viewModel.trackPlaybackEffectsEvent(event)
    }

    @Composable
    private fun EffectsSettingsTabBar() {
        val podcastHeaderBackgroundColor by remember {
            viewModel.playingEpisodeLive.asFlow()
                .map { it.second }
                .distinctUntilChangedBy { it }
        }.collectAsStateWithLifecycle(viewModel.playingEpisodeLive.value?.second)

        val podcastEffectsData by remember {
            viewModel.effectsLive.asFlow()
                .distinctUntilChanged { t1, t2 ->
                    t1.podcast.uuid == t2.podcast.uuid &&
                        t1.podcast.playbackEffects.toData() == t2.podcast.playbackEffects.toData() &&
                        t1.podcast.overrideGlobalEffects == t2.podcast.overrideGlobalEffects &&
                        t1.podcast.overrideGlobalEffectsModified == t2.podcast.overrideGlobalEffectsModified
                }
        }.collectAsStateWithLifecycle(viewModel.effectsLive.value)
        val podcast = podcastEffectsData?.podcast ?: return

        if (podcastEffectsData?.showCustomEffectsSettings == true) {
            val selectedTabTextColor = podcastHeaderBackgroundColor?.let {
                Color(android.graphics.Color.parseColor(ColorUtils.colorIntToHexString(it)))
            } ?: Color.Black
            EffectsSettingsSegmentedTabBar(
                selectedItem = if (podcast.overrideGlobalEffects) {
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
