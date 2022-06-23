package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.databinding.AdapterMultiselectItemBinding
import au.com.shiftyjelly.pocketcasts.views.databinding.AdapterMultiselectTitleBinding
import au.com.shiftyjelly.pocketcasts.views.extensions.setRippleBackground
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private val MULTI_SELECT_ACTION_DIFF = object : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return if (oldItem is MultiSelectAction && newItem is MultiSelectAction) {
            oldItem.groupId == newItem.groupId
        } else {
            return oldItem == newItem
        }
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return oldItem == newItem
    }
}

class MultiSelectAdapter(val editable: Boolean, val listener: ((MultiSelectAction) -> Unit)? = null, val dragListener: ((ItemViewHolder) -> Unit)?) : ListAdapter<Any, RecyclerView.ViewHolder>(MULTI_SELECT_ACTION_DIFF) {
    var playable: Playable? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var normalBackground = Color.TRANSPARENT

    data class Title(@StringRes val title: Int)

    class TitleViewHolder(val binding: AdapterMultiselectTitleBinding) : RecyclerView.ViewHolder(binding.root)

    inner class ItemViewHolder(val binding: AdapterMultiselectItemBinding) : RecyclerView.ViewHolder(binding.root), MultiSelectTouchCallback.ItemTouchHelperViewHolder {

        override fun onItemDrag() {
            AnimatorSet().apply {
                val backgroundView = itemView

                val elevation = ObjectAnimator.ofPropertyValuesHolder(backgroundView, PropertyValuesHolder.ofFloat(View.TRANSLATION_Z, 16.dpToPx(backgroundView.resources.displayMetrics).toFloat()))

                val color = ObjectAnimator.ofInt(backgroundView, "backgroundColor", normalBackground, itemView.context.getThemeColor(UR.attr.primary_ui_05))
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
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.adapter_multiselect_item -> {
                val binding = AdapterMultiselectItemBinding.inflate(layoutInflater, parent, false)
                ItemViewHolder(binding)
            }
            R.layout.adapter_multiselect_title -> {
                val binding = AdapterMultiselectTitleBinding.inflate(layoutInflater, parent, false)
                TitleViewHolder(binding)
            }
            else -> throw IllegalStateException("Unknown view type in shelf")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)

        if (item is MultiSelectAction && holder is ItemViewHolder) {
            holder.binding.lblTitle.setText(item.title)
            holder.binding.imgIcon.setImageResource(item.iconRes)
            holder.binding.dragHandle.isVisible = editable

            if (listener != null) {
                holder.itemView.setOnClickListener { listener.invoke(item) }
            }

            holder.binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    dragListener?.invoke(holder)
                }
                false
            }
        } else if (item is Title && holder is TitleViewHolder) {
            holder.binding.lblTitle.setText(item.title)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Title -> R.layout.adapter_multiselect_title
            is MultiSelectAction -> R.layout.adapter_multiselect_item
            else -> throw IllegalStateException("Unknown item type in shelf")
        }
    }
}
