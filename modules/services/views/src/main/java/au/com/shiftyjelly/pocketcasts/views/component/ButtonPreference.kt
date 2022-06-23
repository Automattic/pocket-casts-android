package au.com.shiftyjelly.pocketcasts.views.component

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import au.com.shiftyjelly.pocketcasts.views.R

open class ButtonPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle) : Preference(context, attrs, defStyleAttr) {
    init {
        layoutResource = R.layout.preference_unsub_button
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.findViewById(R.id.button)?.setOnClickListener {
            onPreferenceClickListener?.onPreferenceClick(this)
        }
    }
}
