package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import com.google.android.material.snackbar.Snackbar
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import au.com.shiftyjelly.pocketcasts.images.R as IR

abstract class MultiSelectHelper<T> : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    interface Listener<in T> {
        fun multiSelectSelectAll()
        fun multiSelectSelectNone()
        fun multiSelectSelectAllUp(multiSelectable: T)
        fun multiSelectSelectAllDown(multiSelectable: T)
        fun multiDeselectAllBelow(multiSelectable: T)
        fun multiDeselectAllAbove(multiSelectable: T)
    }

    var listener: Listener<T>? = null

    private val _isMultiSelectingLive = MutableLiveData<Boolean>().apply { value = false }
    val isMultiSelectingLive: LiveData<Boolean> = _isMultiSelectingLive

    protected val selectedSet: LinkedHashSet<T> = linkedSetOf<T>()
    protected val _selectedListLive = MutableLiveData<List<T>>().apply { value = listOf() }
    val selectedListLive: LiveData<List<T>> = _selectedListLive
    val selectedCount: LiveData<Int> = _selectedListLive.map { it.size }

    var isMultiSelecting: Boolean = false
        set(value) {
            field = value
            _isMultiSelectingLive.value = value
            selectedSet.clear()
            _selectedListLive.value = emptyList()
        }

    var coordinatorLayout: View? = null
    var context: Context? = null
    open var source = SourceView.UNKNOWN

    abstract val maxToolbarIcons: Int
    abstract val toolbarActions: LiveData<List<MultiSelectAction>>
    abstract fun isSelected(multiSelectable: T): Boolean
    abstract fun onMenuItemSelected(
        itemId: Int,
        resources: Resources,
        activity: FragmentActivity,
    ): Boolean
    abstract fun deselect(multiSelectable: T)

    fun defaultLongPress(
        multiSelectable: T,
        fragmentManager: FragmentManager,
        forceDarkTheme: Boolean = false,
    ) {
        if (!isMultiSelecting) {
            isMultiSelecting = !isMultiSelecting
            select(multiSelectable)
        } else {
            val selectAllAbove = if (isSelected(multiSelectable) && selectedSet.size > 1) {
                R.string.deselect_all_above
            } else {
                R.string.select_all_above
            }

            val selectAll = if (selectedSet.contains(multiSelectable) && selectedSet.size > 1) {
                R.string.deselect_all
            } else {
                R.string.select_all
            }

            val selectAllBelow = if (isSelected(multiSelectable) && selectedSet.size > 1) {
                R.string.deselect_all_below
            } else {
                R.string.select_all_below
            }

            OptionsDialog()
                .setForceDarkTheme(forceDarkTheme)
                .addTextOption(
                    titleId = selectAllAbove,
                    click = { toggleSelectAllAbove(multiSelectable) },
                    imageId = if (isSelected(multiSelectable) && selectedSet.size > 1) IR.drawable.ic_deselectall_up else IR.drawable.ic_selectall_up,

                )
                .addTextOption(
                    titleId = selectAll,
                    click = { toggleSelectAll(multiSelectable) },
                    imageId = if (selectedSet.contains(multiSelectable) && selectedSet.size > 1) IR.drawable.ic_deselectall else IR.drawable.ic_selectall,
                )
                .addTextOption(
                    titleId = selectAllBelow,
                    click = { toggleSelectAllBelow(multiSelectable) },
                    imageId = if (isSelected(multiSelectable) && selectedSet.size > 1) IR.drawable.ic_deselectall_down else IR.drawable.ic_selectall_down,
                )
                .show(fragmentManager, "multi_select_select_dialog")
        }
    }

    fun select(multiSelectable: T) {
        if (!isSelected(multiSelectable)) {
            selectedSet.add(multiSelectable)
        }
        _selectedListLive.value = selectedSet.toList()
    }

    fun selectAll() {
        listener?.multiSelectSelectAll()
    }

    fun unselectAll() {
        listener?.multiSelectSelectNone()
    }

    private fun selectAllAbove(multiSelectable: T) {
        listener?.multiSelectSelectAllUp(multiSelectable)
    }

    private fun selectAllBelow(multiSelectable: T) {
        listener?.multiSelectSelectAllDown(multiSelectable)
    }

    fun selectAllInList(multiSelectables: List<T>) {
        val trimmed = multiSelectables.filter { !selectedSet.contains(it) }
        selectedSet.addAll(trimmed)
        _selectedListLive.value = selectedSet.toList()
    }

    fun deselectAllInList(multiSelectables: List<T>) {
        selectedSet.removeAll(multiSelectables)
        _selectedListLive.value = selectedSet.toList()
    }

    private fun toggleSelectAll(multiSelectable: T) {
        if (selectedSet.contains(multiSelectable) && selectedSet.size > 1) {
            deselectAll()
        } else {
            selectAll()
        }
    }
    private fun toggleSelectAllAbove(multiSelectable: T) {
        if (isSelected(multiSelectable) && selectedSet.size > 1) {
            deselectAllAbove(multiSelectable)
        } else {
            selectAllAbove(multiSelectable)
        }
    }
    private fun toggleSelectAllBelow(multiSelectable: T) {
        if (isSelected(multiSelectable) && selectedSet.size > 1) {
            deselectAllBelow(multiSelectable)
        } else {
            selectAllBelow(multiSelectable)
        }
    }

    fun toggle(multiSelectable: T): Boolean {
        return if (isSelected(multiSelectable)) {
            deselect(multiSelectable)
            false
        } else {
            select(multiSelectable)
            true
        }
    }

    private fun deselectAll() {
        listener?.multiSelectSelectNone()
    }

    private fun deselectAllAbove(deselectAllBelow: T) {
        listener?.multiDeselectAllAbove(deselectAllBelow)
    }

    private fun deselectAllBelow(deselectAllBelow: T) {
        listener?.multiDeselectAllBelow(deselectAllBelow)
    }

    fun closeMultiSelect() {
        selectedSet.clear()
        _selectedListLive.value = selectedSet.toList()
        isMultiSelecting = false
    }

    protected fun showSnackBar(snackText: String) {
        coordinatorLayout?.let {
            val snackbar = Snackbar.make(it, snackText, Snackbar.LENGTH_LONG)
            snackbar.show()
        } ?: run {
            // If we don't have a coordinator layout, fallback to a toast
            context?.let { context ->
                Toast.makeText(context, snackText, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun cleanup() {
        listener = null
        coordinatorLayout = null
        context = null
    }
}
