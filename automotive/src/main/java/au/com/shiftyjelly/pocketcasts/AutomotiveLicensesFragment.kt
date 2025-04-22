package au.com.shiftyjelly.pocketcasts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.compose.AutomotiveTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.extensions.openUrl
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.libraryColors
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.util.withContext
import kotlinx.collections.immutable.toImmutableList

class AutomotiveLicensesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AutomotiveTheme {
            LicensesPage()
        }
    }

    @Composable
    private fun LicensesPage(modifier: Modifier = Modifier) {
        val librariesBlock: () -> Libs = {
            val libs = Libs.Builder().withContext(requireContext()).build()
            // without displaying the artifact id the libraries seem to appear twice
            libs.copy(
                libs.libraries.distinctBy { "${it.name}##${it.author}" }.toImmutableList(),
            )
        }

        LibrariesContainer(
            modifier = modifier.fillMaxSize(),
            showAuthor = true,
            showVersion = false,
            showLicenseBadges = false,
            colors = LibraryDefaults.libraryColors(
                backgroundColor = MaterialTheme.colors.background,
                contentColor = MaterialTheme.theme.colors.primaryText01,
            ),
            padding = LibraryDefaults.libraryPadding(
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            ),
            librariesBlock = librariesBlock,
            onLibraryClick = { library: Library ->
                val website = library.website ?: return@LibrariesContainer
                openUrl(website)
            },
        )
    }
}
