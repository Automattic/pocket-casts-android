package au.com.shiftyjelly.pocketcasts.endofyear

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.ShareCompat
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
                    StoriesPage(
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
            val uri = FileUtil.getUriForFile(context, file)

            val chooserIntent = ShareCompat.IntentBuilder(context)
                .setType("image/png")
                .addStream(uri)
                .setChooserTitle(LR.string.end_of_year_share_via)
                .createChooserIntent()

            shareLauncher.launch(chooserIntent)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    companion object {
        fun newInstance() = StoriesFragment()
    }
}
