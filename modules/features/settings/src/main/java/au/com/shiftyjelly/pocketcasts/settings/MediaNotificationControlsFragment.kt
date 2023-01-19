package au.com.shiftyjelly.pocketcasts.settings

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls.Companion.MAX_VISIBLE_OPTIONS
import au.com.shiftyjelly.pocketcasts.settings.databinding.AdapterMediaActionItemBinding
import au.com.shiftyjelly.pocketcasts.settings.databinding.AdapterMediaActionTitleBinding
import au.com.shiftyjelly.pocketcasts.settings.databinding.FragmentMediaNotificationControlsBinding
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.extensions.setRippleBackground
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.settings.R as SR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class MediaNotificationControlsFragment : BaseFragment(), MediaActionTouchCallback.ItemTouchHelperAdapter {
    private var items = emptyList<Any>()

    @Inject
    lateinit var settings: Settings

    private lateinit var itemTouchHelper: ItemTouchHelper
    private val adapter = MediaActionAdapter(dragListener = this::onMediaActionItemStartDrag)
    private val mediaTitle = MediaActionTitle(R.string.settings_media_actions_prioritise, R.string.settings_your_top_actions_will_be_available_in_your_notif_and_android_auto_player)
    private val otherActionsTitle = MediaActionTitle(R.string.settings_other_media_actions)
    private var binding: FragmentMediaNotificationControlsBinding? = null
    private var dragStartPosition: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMediaNotificationControlsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        val toolbar = binding.toolbar
        toolbar.setTitle(R.string.settings_media_actions_customise)
        toolbar.setTitleTextColor(toolbar.context.getThemeColor(UR.attr.secondary_text_01))
        toolbar.setNavigationOnClickListener {
            (activity as? FragmentHostListener)?.closeModal(this)
        }
        toolbar.navigationIcon?.setTint(ThemeColor.secondaryIcon01(theme.activeTheme))

        val backgroundColor = view.context.getThemeColor(UR.attr.primary_ui_01)

        view.setBackgroundColor(backgroundColor)
        adapter.selectedBackground = ColorUtils.calculateCombinedColor(backgroundColor, view.context.getThemeColor(UR.attr.primary_ui_02_selected))

        val recyclerView = binding.recyclerView
        recyclerView.adapter = adapter
        (recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        (recyclerView.itemAnimator as? SimpleItemAnimator)?.changeDuration = 0
        updateMediaActionsVisibility(settings.getVisibleCustomMediaActionsCount() == 0)

        val callback = MediaActionTouchCallback(listener = this)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            settings.defaultMediaNotificationControlsFlow.collect {
                val itemsPlusTitles = mutableListOf<Any>()
                itemsPlusTitles.addAll(it)
                itemsPlusTitles.add(3, otherActionsTitle)
                itemsPlusTitles.add(0, mediaTitle)
                items = itemsPlusTitles
                adapter.submitList(items)
            }
        }

        with(binding.composeView) {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppThemeWithBackground(theme.activeTheme) {
                    HideMediaControlsSettingsRow(
                        shouldHideCustomActions = settings.maxCustomMediaActionsCountFlow.collectAsState().value == 0,
                        onHideCustomActionsToggled = { hideCustomActions ->
                            settings.setVisibleCustomMediaActionsCount(if (hideCustomActions) 0 else MAX_VISIBLE_OPTIONS)
                            updateMediaActionsVisibility(hideCustomActions)
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun HideMediaControlsSettingsRow(
        shouldHideCustomActions: Boolean,
        onHideCustomActionsToggled: (Boolean) -> Unit
    ) {
        Column {
            SettingRow(
                primaryText = stringResource(LR.string.settings_media_actions_hide_title),
                secondaryText = stringResource(LR.string.settings_media_actions_hide_subtitle),
                toggle = SettingRowToggle.Switch(checked = shouldHideCustomActions),
                indent = false,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .toggleable(
                        value = shouldHideCustomActions,
                        role = Role.Switch
                    ) { onHideCustomActionsToggled(!shouldHideCustomActions) }
            )
        }
    }

    private fun updateMediaActionsVisibility(hide: Boolean) {
        val binding = binding ?: return
        with(binding) {
            recyclerView.alpha = if (hide) 0.4f else 1.0f
            overlay.visibility = if (hide) View.VISIBLE else View.GONE
        }
    }

    override fun onMediaActionItemMove(fromPosition: Int, toPosition: Int) {
        val listData = items.toMutableList()

        Timber.d("Swapping $fromPosition to $toPosition")
        Timber.d("List: $listData")

        if (fromPosition < toPosition) {
            for (index in fromPosition until toPosition) {
                Collections.swap(listData, index, index + 1)
            }
        } else {
            for (index in fromPosition downTo toPosition + 1) {
                Collections.swap(listData, index, index - 1)
            }
        }

        // Make sure the titles are in the right spot
        listData.remove(otherActionsTitle)
        listData.remove(mediaTitle)
        listData.add(3, otherActionsTitle)
        listData.add(0, mediaTitle)

        adapter.submitList(listData)
        items = listData.toList()

        Timber.d("Swapped: $items")
    }

    override fun onMediaActionItemStartDrag(viewHolder: MediaActionAdapter.ItemViewHolder) {
        dragStartPosition = viewHolder.bindingAdapterPosition
        itemTouchHelper.startDrag(viewHolder)
    }

    override fun onMediaActionItemTouchHelperFinished(position: Int) {
        settings.setMediaNotificationControlItems(items.filterIsInstance<Settings.MediaNotificationControls>().map { it.key })
    }
}

data class MediaActionTitle(@StringRes val title: Int, @StringRes val subTitle: Int? = null)

private val MEDIA_ACTION_ITEM_DIFF = object : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return if (oldItem is Settings.MediaNotificationControls && newItem is Settings.MediaNotificationControls) {
            oldItem.key == newItem.key
        } else {
            return oldItem == newItem
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return true
    }
}

class MediaActionAdapter(val listener: ((Settings.MediaNotificationControls) -> Unit)? = null, val dragListener: ((ItemViewHolder) -> Unit)?) : ListAdapter<Any, RecyclerView.ViewHolder>(MEDIA_ACTION_ITEM_DIFF) {
    var playable: Playable? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var normalBackground = Color.TRANSPARENT
    var selectedBackground = Color.BLACK

    class TitleViewHolder(val binding: AdapterMediaActionTitleBinding) : RecyclerView.ViewHolder(binding.root)

    inner class ItemViewHolder(val binding: AdapterMediaActionItemBinding) : RecyclerView.ViewHolder(binding.root), MediaActionTouchCallback.ItemTouchHelperViewHolder {

        override fun onItemDrag() {
            AnimatorSet().apply {
                val backgroundView = itemView

                val elevation = ObjectAnimator.ofPropertyValuesHolder(backgroundView, PropertyValuesHolder.ofFloat(View.TRANSLATION_Z, 16.dpToPx(backgroundView.resources.displayMetrics).toFloat()))

                val color = ObjectAnimator.ofInt(backgroundView, "backgroundColor", normalBackground, selectedBackground)
                color.setEvaluator(ArgbEvaluator())

                playTogether(elevation, color)
                start()
            }
        }

        override fun onItemSwipe() {
        }

        override fun onItemClear() {
            AnimatorSet().apply {
                val backgroundView = itemView
                val elevation = ObjectAnimator.ofPropertyValuesHolder(backgroundView, PropertyValuesHolder.ofFloat(View.TRANSLATION_Z, 0.toFloat()))

                backgroundView.setRippleBackground(false)
                play(elevation)
                start()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            SR.layout.adapter_media_action_item -> {
                val binding = AdapterMediaActionItemBinding.inflate(inflater, parent, false)
                ItemViewHolder(binding)
            }
            SR.layout.adapter_media_action_title -> {
                val binding = AdapterMediaActionTitleBinding.inflate(inflater, parent, false)
                TitleViewHolder(binding)
            }
            else -> throw IllegalStateException("Unknown view type in shelf")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)

        if (item is Settings.MediaNotificationControls && holder is ItemViewHolder) {
            val binding = holder.binding

            binding.lblTitle.setText(item.controlName)
            binding.imgIcon.setImageResource(item.iconRes)

            if (listener != null) {
                holder.itemView.setOnClickListener { listener.invoke(item) }
            }

            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    dragListener?.invoke(holder)
                }
                false
            }
        } else if (item is MediaActionTitle && holder is TitleViewHolder) {
            val binding = holder.binding

            binding.lblTitle.setText(item.title)

            if (item.subTitle != null) {
                binding.lblSubtitle.isVisible = true
                holder.binding.lblSubtitle.setText(item.subTitle)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MediaActionTitle -> SR.layout.adapter_media_action_title
            is Settings.MediaNotificationControls -> SR.layout.adapter_media_action_item
            else -> throw IllegalStateException("Unknown item type in shelf")
        }
    }
}
