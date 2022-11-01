package au.com.shiftyjelly.pocketcasts.endofyear

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.R
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import timber.log.Timber
import java.io.File
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class StoriesFragment : BaseDialogFragment() {
    private val viewModel: StoriesViewModel by viewModels()
    override val statusBarColor: StatusBarColor
        get() = StatusBarColor.Custom(Color.BLACK, true)

    private val shareLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        /* Share activity dismissed, start paused story */
        viewModel.start()
    }

    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogThemeBlack)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme(theme.activeTheme) {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    StoriesScreen(
                        viewModel = viewModel,
                        onCloseClicked = { dismiss() },
                        onShareClicked = { onCaptureBitmap ->
                            viewModel.onShareClicked(
                                onCaptureBitmap,
                                requireContext(),
                                ::showShareForFile
                            )
                        }
                    )
                }
            }
        }
    }

    private fun showShareForFile(file: File) {
        val context = requireContext()
        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/png"
            val uri = FileUtil.createUriWithReadPermissions(file, intent, requireContext())
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            shareLauncher.launch(Intent.createChooser(intent, context.getString(LR.string.end_of_year_share_via)))
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    companion object {
        fun newInstance() = StoriesFragment()
    }
}
