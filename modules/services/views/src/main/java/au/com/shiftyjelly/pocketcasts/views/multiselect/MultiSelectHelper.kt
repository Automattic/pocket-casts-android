package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.images.R as IR

abstract class MultiSelectHelper<T> : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    interface Listener<in T> {
        fun multiSelectSelectAll()
        fun multiSelectSelectNone()
        fun multiSelectSelectAllUp(multiSelectable: T)
        fun multiSelectSelectAllDown(multiSelectable: T)
    }

    open lateinit var listener: Listener<T>

    private val _isMultiSelectingLive = MutableLiveData<Boolean>().apply { value = false }
    val isMultiSelectingLive: LiveData<Boolean> = _isMultiSelectingLive

    protected val selectedList: MutableList<T> = mutableListOf()
    protected val _selectedListLive = MutableLiveData<List<T>>().apply { value = listOf() }
    val selectedListLive: LiveData<List<T>> = _selectedListLive
    val selectedCount: LiveData<Int> = _selectedListLive.map { it.size }

    var isMultiSelecting: Boolean = false
        set(value) {
            field = value
            _isMultiSelectingLive.value = value
            selectedList.clear()
        }

    var coordinatorLayout: View? = null
    var context: Context? = null
    var source = SourceView.UNKNOWN

    abstract val toolbarActions: LiveData<List<MultiSelectAction>>
    abstract fun isSelected(multiSelectable: T): Boolean
    abstract fun onMenuItemSelected(
        itemId: Int,
        resources: Resources,
        fragmentManager: FragmentManager,
    ): Boolean

    fun defaultLongPress(
        multiSelectable: T,
        fragmentManager: FragmentManager,
        forceDarkTheme: Boolean = false,
    ) {
        if (!isMultiSelecting) {
            isMultiSelecting = !isMultiSelecting
            select(multiSelectable)

            FirebaseAnalyticsTracker.enteredMultiSelect()
        } else {
            OptionsDialog()
                .setForceDarkTheme(forceDarkTheme)
                .addTextOption(
                    titleId = R.string.select_all_above,
                    click = { selectAllAbove(multiSelectable) },
                    imageId = IR.drawable.ic_selectall_up
                )
                .addTextOption(
                    titleId = R.string.select_all_below,
                    click = { selectAllBelow(multiSelectable) },
                    imageId = IR.drawable.ic_selectall_down
                )
                .show(fragmentManager, "multi_select_select_dialog")
        }
    }

    fun select(multiSelectable: T) {
        if (!isSelected(multiSelectable)) {
            selectedList.add(multiSelectable)
        }
        _selectedListLive.value = selectedList
    }

    fun deselect(multiSelectable: T) {
        if (isSelected(multiSelectable)) {
            selectedList.remove(multiSelectable)
        }

        _selectedListLive.value = selectedList

        if (selectedList.isEmpty()) {
            closeMultiSelect()
        }
    }

    fun selectAll() {
        listener.multiSelectSelectAll()
    }

    fun unselectAll() {
        listener.multiSelectSelectNone()
    }

    private fun selectAllAbove(multiSelectable: T) {
        listener.multiSelectSelectAllUp(multiSelectable)
    }

    private fun selectAllBelow(multiSelectable: T) {
        listener.multiSelectSelectAllDown(multiSelectable)
    }

    fun selectAllInList(multiSelectables: List<T>) {
        val trimmed = multiSelectables.filter { !selectedList.contains(it) }
        selectedList.addAll(trimmed)
        _selectedListLive.value = selectedList
    }

    fun toggle(episode: T): Boolean {
        return if (isSelected(episode)) {
            deselect(episode)
            false
        } else {
            select(episode)
            true
        }
    }

    fun closeMultiSelect() {
        selectedList.clear()
        _selectedListLive.value = selectedList
        isMultiSelecting = false
    }

    protected fun showSnackBar(snackText: String) {
        coordinatorLayout?.let {
            val snackbar = Snackbar.make(it, snackText, Snackbar.LENGTH_LONG)
            snackbar.show()
        } ?: run { // If we don't have a coordinator layout, fallback to a toast
            context?.let { context ->
                Toast.makeText(context, snackText, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
