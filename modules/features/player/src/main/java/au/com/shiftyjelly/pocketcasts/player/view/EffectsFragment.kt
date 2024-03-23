package au.com.shiftyjelly.pocketcasts.player.view

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentEffectsBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.extensions.roundedSpeed
import au.com.shiftyjelly.pocketcasts.views.extensions.applyColor
import au.com.shiftyjelly.pocketcasts.views.extensions.updateTint
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class EffectsFragment : BaseDialogFragment(), CompoundButton.OnCheckedChangeListener, MaterialButtonToggleGroup.OnButtonCheckedListener, View.OnClickListener {

    @Inject lateinit var stats: StatsManager

    @Inject lateinit var settings: Settings

    @Inject lateinit var playbackManager: PlaybackManager

    override val statusBarColor: StatusBarColor? = null

    private val viewModel: PlayerViewModel by activityViewModels()
    private lateinit var imageRequestFactory: PocketCastsImageRequestFactory
    private var binding: FragmentEffectsBinding? = null
    private val trimToggleGroupButtonIds = arrayOf(R.id.trimLow, R.id.trimMedium, R.id.trimHigh)
    private var updatedSpeed: Double? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        imageRequestFactory = PocketCastsImageRequestFactory(context, cornerRadius = 4).themed()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentEffectsBinding.inflate(inflater, container, false)

        viewModel.effectsLive.value?.let { update(it) } // Make sure the window is the correct size before opening or else it won't expand properly
        viewModel.effectsLive.observe(viewLifecycleOwner) { podcastEffectsPair ->
            update(podcastEffectsPair)
            ensureExpanded()
        }

        updateTrimState()

        return binding?.root
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        trackSpeedChangeIfNeeded()
        binding = null
    }

    private fun update(podcastEffectsPair: PlayerViewModel.PodcastEffectsPair) {
        val podcast = podcastEffectsPair.podcast
        val effects = podcastEffectsPair.effects

        val binding = binding ?: return

        binding.globalEffectsCard.isVisible = podcast.overrideGlobalEffects

        imageRequestFactory.create(podcast).loadInto(binding.podcastEffectsImage)

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

        binding.btnClear.setOnClickListener(this)
    }

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.playingEpisodeLive.observe(viewLifecycleOwner) { (_, backgroundColor) ->
            applyColor(theme, backgroundColor)

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
        updatedSpeed = speed
        binding.lblSpeed.text = String.format("%.1fx", effects.playbackSpeed)
        viewModel.saveEffects(effects, podcast)

        binding.btnSpeedUp.announceForAccessibility("Playback speed ${binding.lblSpeed.text}")
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
            binding.btnClear.id -> viewModel.clearPodcastEffects(podcast)
            binding.lblSpeed.id -> {
                when (effects.playbackSpeed) {
                    1.0 -> changePlaybackSpeed(effects, podcast, 1.5)
                    1.5 -> changePlaybackSpeed(effects, podcast, 2.0)
                    else -> changePlaybackSpeed(effects, podcast, 1.0)
                }
            }
        }
    }

    private fun trackSpeedChangeIfNeeded() {
        updatedSpeed?.let { trackPlaybackEffectsEvent(AnalyticsEvent.PLAYBACK_EFFECT_SPEED_CHANGED, mapOf(PlaybackManager.SPEED_KEY to it)) }
    }

    private fun trackPlaybackEffectsEvent(event: AnalyticsEvent, props: Map<String, Any> = emptyMap()) {
        playbackManager.trackPlaybackEffectsEvent(event, props, SourceView.PLAYER_PLAYBACK_EFFECTS)
    }
}
