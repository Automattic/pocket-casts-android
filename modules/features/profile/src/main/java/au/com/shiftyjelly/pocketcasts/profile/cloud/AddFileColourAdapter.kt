package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.profile.databinding.AdapterAddFileImageBinding
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.ui.R as UR
import au.com.shiftyjelly.pocketcasts.views.R as VR

val diffCallback = object : DiffUtil.ItemCallback<AddFileColourAdapter.Item>() {
    override fun areItemsTheSame(oldItem: AddFileColourAdapter.Item, newItem: AddFileColourAdapter.Item): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: AddFileColourAdapter.Item, newItem: AddFileColourAdapter.Item): Boolean {
        return if (oldItem is AddFileColourAdapter.Item.Image && newItem is AddFileColourAdapter.Item.Image) {
            oldItem.bitmap?.sameAs(newItem.bitmap) ?: false
        } else if (oldItem is AddFileColourAdapter.Item.Colour && newItem is AddFileColourAdapter.Item.Colour) {
            oldItem.tintColorIndex == newItem.tintColorIndex
        } else {
            false
        }
    }
}

class AddFileColourAdapter(val onSelectedChange: (Item) -> Unit, val onLockedItemTapped: () -> Unit) : ListAdapter<AddFileColourAdapter.Item, AddFileColourAdapter.ViewHolder>(diffCallback) {
    sealed class Item {
        data class Image(val bitmap: Bitmap?) : Item()
        data class Colour(val tintColorIndex: Int, val color: Int, val lockable: Boolean) : Item()
    }

    var locked: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var selectedIndex = 1
        set(value) {
            field = value
            if (value < itemCount) {
                onSelectedChange(getItem(value))
            }
            notifyDataSetChanged()
        }

    class ViewHolder(val binding: AdapterAddFileImageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AdapterAddFileImageBinding.inflate(inflater, parent, false)
        binding.root.setBackgroundResource(VR.drawable.filter_circle)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.isVisible = true

        val view = holder.binding.imgBackground
        view.imageTintList = null

        val colorClickListener: (View?) -> Unit = {
            val oldPosition = selectedIndex
            selectedIndex = position
            notifyItemChanged(oldPosition)
            notifyItemChanged(position)
        }

        if (item is Item.Image) {
            if (item.bitmap != null) {
                view.setImageBitmap(item.bitmap)
                holder.itemView.backgroundTintList = null
            } else {
                holder.itemView.isVisible = false
            }
            holder.itemView.setOnClickListener(colorClickListener)
        } else if (item is Item.Colour) {
            holder.itemView.backgroundTintList = ColorStateList.valueOf(item.color)
            if (locked && item.lockable) {
                view.setImageResource(IR.drawable.ic_locked)
                view.imageTintList = ColorStateList.valueOf(view.context.getThemeColor(UR.attr.contrast_01))
                holder.itemView.setOnClickListener { onLockedItemTapped() }
            } else {
                view.setImageDrawable(null)
                holder.itemView.setOnClickListener(colorClickListener)
            }
        }

        holder.binding.imgSelected.isVisible = position == selectedIndex
    }
}
