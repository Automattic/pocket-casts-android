package au.com.shiftyjelly.pocketcasts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.compose.AutomotiveTheme
import au.com.shiftyjelly.pocketcasts.helper.rememberQrPainter

/**
 * Fallback for when the Automotive platform doesn't have a web browser.
 */
class AutomotiveLinkFragment : Fragment() {

    companion object {
        const val ARGUMENT_URL = "url"

        fun newInstance(url: String): AutomotiveLinkFragment {
            return AutomotiveLinkFragment().apply {
                arguments = bundleOf(ARGUMENT_URL to url)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val url = arguments?.getString(ARGUMENT_URL) ?: ""
        return ComposeView(requireContext()).apply {
            setContent {
                AutomotiveTheme {
                    LinkPage(url = url)
                }
            }
        }
    }

    @Composable
    private fun LinkPage(url: String, modifier: Modifier = Modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.fillMaxSize().padding(16.dp)
        ) {
            Text(
                text = url,
                fontSize = 32.sp,
                textAlign = TextAlign.Center,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Image(
                painter = rememberQrPainter(content = url),
                contentDescription = null,
                modifier = Modifier.fillMaxHeight().aspectRatio(1f)
            )
        }
    }
}
