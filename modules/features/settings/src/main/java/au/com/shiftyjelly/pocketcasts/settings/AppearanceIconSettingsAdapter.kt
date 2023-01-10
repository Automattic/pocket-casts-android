package au.com.shiftyjelly.pocketcasts.settings

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.settings.databinding.AdapterAppearanceAppiconItemBinding
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.AppIcon
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import kotlin.math.min
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class AppearanceIconSettingsAdapter(
    private var mainWidth: Int?,
    private var isPlusSignedIn: Boolean,
    private var selectedAppIcon: AppIcon.AppIconType,
    private val list: List<AppIcon.AppIconType>,
    private val clickListener: (AppIcon.AppIconType, AppIcon.AppIconType, Boolean) -> Unit
) : RecyclerView.Adapter<AppearanceIconSettingsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AdapterAppearanceAppiconItemBinding.inflate(inflater, parent, false)
        val view = binding.root

        mainWidth?.let { mainWidth ->
            val numColumns = 3f
            val maxCellWidth = 124.dpToPx(parent.context).toFloat()
            val peekWidth = 30.dpToPx(parent.context).toFloat()
            val calculatedWidth = ((mainWidth - peekWidth - (numColumns + 1)) / numColumns)
            val cellWidth = min(maxCellWidth, calculatedWidth)
            view.updateLayoutParams {
                width = cellWidth.toInt()
            }
        }
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appIcon = list[position]
        val selected = (appIcon == selectedAppIcon)
        holder.bind(appIcon, selected)
    }

    override fun getItemCount() = list.size

    fun updatePlusSignedIn(value: Boolean) {
        isPlusSignedIn = value
        notifyDataSetChanged()
    }

    fun selectedIconIndex(): Int? {
        var index = 0
        for (appIcon in list) {
            if (appIcon == selectedAppIcon) {
                return index
            }
            ++index
        }
        return null
    }

    fun updateAppIcon(newAppIcon: AppIcon.AppIconType) {
        selectedAppIcon = newAppIcon
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: AdapterAppearanceAppiconItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private val selectColor = itemView.context.getThemeColor(UR.attr.support_01)
        private val deselectColor = itemView.context.getThemeColor(UR.attr.primary_ui_04)
        private val strokeWidth = 4.dpToPx(itemView.context)

        init {
            binding.appIconItem.setOnClickListener(this)
        }

        fun bind(appIcon: AppIcon.AppIconType, selected: Boolean) {
            val showOption = !appIcon.isPlus || isPlusSignedIn
            binding.appIconItem.alpha = if (showOption) 1.0f else 0.65f

            val drawable = AppCompatResources.getDrawable(itemView.context, appIcon.settingsIcon)
            binding.imgIcon.setImageDrawable(drawable)
            var tickDrawable: Drawable? = null
            if (showOption && selected) {
                tickDrawable = AppCompatResources.getDrawable(itemView.context, R.drawable.ic_circle_tick)
            } else if (!showOption) {
                tickDrawable = AppCompatResources.getDrawable(itemView.context, R.drawable.ic_plus_bubble)
            }
            binding.imgTick.setImageDrawable(tickDrawable)
            binding.imgTick.contentDescription = binding.imgTick.resources.getString(if (selected) LR.string.on else LR.string.off)
            binding.imgLock.isVisible = !showOption
            binding.txtTitle.setText(appIcon.labelId)

            binding.outlinePanel1.setSelectedWithColors(selected, selectColor, deselectColor, strokeWidth)
        }

        override fun onClick(view: View) {
            if (bindingAdapterPosition == RecyclerView.NO_POSITION) {
                return
            }
            val beforeAppIcon = selectedAppIcon
            val afterAppIcon = list[bindingAdapterPosition]
            val validAppIcon = !afterAppIcon.isPlus || isPlusSignedIn

            selectedAppIcon = afterAppIcon
            clickListener(beforeAppIcon, afterAppIcon, validAppIcon)
            notifyDataSetChanged()
        }
    }
}
