package au.com.shiftyjelly.pocketcasts.settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import au.com.shiftyjelly.pocketcasts.repositories.opml.OpmlImportTask
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class ImportOpmlUrlDialogFragment : DialogFragment() {
    @SuppressLint("PrivateResource")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(androidx.preference.R.layout.preference_dialog_edittext, null)
        view.findViewById<View>(android.R.id.message).isVisible = false
        val editText = view.findViewById<EditText>(android.R.id.edit).apply { requestFocus() }
        return AlertDialog.Builder(requireContext())
            .setTitle(LR.string.settings_import_url)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val url = editText.text.toString().toHttpUrlOrNull()
                if (url != null) {
                    OpmlImportTask.run(url, requireActivity())
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .apply { window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE) }
    }
}
