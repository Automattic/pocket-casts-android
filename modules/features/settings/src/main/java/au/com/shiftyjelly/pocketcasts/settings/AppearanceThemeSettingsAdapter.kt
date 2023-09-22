package au.com.shiftyjelly.pocketcasts.settings

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import au.com.shiftyjelly.pocketcasts.settings.databinding.AdapterAppearanceThemeItemBinding
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import kotlin.math.min
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class AppearanceThemeSettingsAdapter(
    private var mainWidth: Int?,
    private var isPlusOrPatronSignedIn: Boolean,
    private var selectedTheme: Theme.ThemeType,
    private var list: List<Theme.ThemeType>,
    private val clickListener: (Theme.ThemeType, Theme.ThemeType, Boolean) -> Unit
) : RecyclerView.Adapter<AppearanceThemeSettingsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AdapterAppearanceThemeItemBinding.inflate(inflater, parent, false)
        val view = binding.root

        mainWidth?.let { mainWidth ->
            val numColumns = 3f
            val maxCellWidth = 124.dpToPx(parent.context).toFloat()
            val peekWidth = 30.dpToPx(parent.context).toFloat()
            val calculatedWidth = ((mainWidth - peekWidth - (numColumns + 1)) / numColumns)
            val cellWidth = min(maxCellWidth, calculatedWidth)
            view.updateLayoutParams {
                width = cellWidth.toInt()
                height = (cellWidth * (210f / 124f)).toInt()
            }
        }
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val theme = list[position]
        val selected = (theme == selectedTheme)
        holder.bind(theme, selected)
    }

    override fun getItemCount() = list.size

    fun updatePlusSignedIn(value: Boolean) {
        isPlusOrPatronSignedIn = value
        notifyDataSetChanged()
    }

    fun selectedThemeIndex(): Int? {
        var index = 0
        for (theme in list) {
            if (theme == selectedTheme) {
                return index
            }
            ++index
        }
        return null
    }

    fun updateTheme(newTheme: Theme.ThemeType) {
        selectedTheme = newTheme
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: AdapterAppearanceThemeItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private val selectColor = itemView.context.getThemeColor(UR.attr.support_01)
        private val deselectColor = itemView.context.getThemeColor(UR.attr.primary_ui_04)
        private val strokeWidth = 4.dpToPx(itemView.context)

        init {
            binding.themeItem.setOnClickListener(this)
        }

        fun bind(theme: Theme.ThemeType, selected: Boolean) {
            val showOption = !theme.isPlus || isPlusOrPatronSignedIn
            binding.themeItem.alpha = if (showOption) 1.0f else 0.65f

            val drawable = AppCompatResources.getDrawable(itemView.context, theme.iconResourceId)
            binding.imgIcon.setImageDrawable(drawable)
            var tickDrawable: Drawable? = null
            if (showOption && selected) {
                tickDrawable = AppCompatResources.getDrawable(itemView.context, R.drawable.ic_circle_tick)
            } else if (!showOption) {
                tickDrawable = AppCompatResources.getDrawable(itemView.context, R.drawable.ic_locked_plus)
            }
            binding.imgTick.setImageDrawable(tickDrawable)
            binding.imgTick.contentDescription = binding.imgTick.resources.getString(if (selected) LR.string.on else LR.string.off)
            binding.imgLock.isVisible = !showOption
            binding.txtTitle.setText(theme.labelId)

            binding.outlinePanel1.setSelectedWithColors(selected, selectColor, deselectColor, strokeWidth)
        }

        override fun onClick(view: View) {
            if (bindingAdapterPosition == NO_POSITION) {
                return
            }
            val beforeTheme = selectedTheme
            val afterTheme = list[bindingAdapterPosition]
            val validTheme = !afterTheme.isPlus || isPlusOrPatronSignedIn

            selectedTheme = if (validTheme) {
                afterTheme
            } else {
                beforeTheme
            }
            clickListener(beforeTheme, afterTheme, validTheme)
            notifyDataSetChanged()
        }
    }
}
