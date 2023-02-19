package au.com.shiftyjelly.pocketcasts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.compose.AutomotiveTheme
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.extensions.openUrl
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.util.withContext

class AutomotiveLicensesFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AutomotiveTheme {
                    LicensesPage()
                }
            }
        }
    }

    @Composable
    private fun LicensesPage(modifier: Modifier = Modifier) {
        LibrariesContainer(
            modifier = modifier.fillMaxSize(),
            showAuthor = true,
            showVersion = false,
            showLicenseBadges = false,
            colors = LibraryDefaults.libraryColors(
                backgroundColor = MaterialTheme.colors.background,
                contentColor = MaterialTheme.theme.colors.primaryText01
            ),
            itemContentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            librariesBlock = { context ->
                val libs = Libs.Builder().withContext(context).build()
                // without displaying the artifact id the libraries seem to appear twice
                libs.copy(libs.libraries.distinctBy { "${it.name}##${it.author}" })
            },
            onLibraryClick = { library ->
                val website = library.website ?: return@LibrariesContainer
                openUrl(website)
            }
        )
    }
}
