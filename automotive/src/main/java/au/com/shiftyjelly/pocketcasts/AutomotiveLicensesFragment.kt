package au.com.shiftyjelly.pocketcasts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
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
        LibrariesContainer(
            showAuthor = true,
            showVersion = false,
            showLicenseBadges = false,
            colors = LibraryDefaults.libraryColors(
                libraryContentColor = MaterialTheme.theme.colors.primaryText01,
            ),
            libraries = produceLibraries { context ->
                val libs = Libs.Builder().withContext(context).build()
                // without displaying the artifact id the libraries seem to appear twice
                libs.copy(
                    libs.libraries.distinctBy { "${it.name}##${it.author}" }.toImmutableList(),
                )
            }.value,
            onLibraryClick = { library: Library ->
                val website = library.website ?: return@LibrariesContainer
                openUrl(website)
            },
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        )
    }
}
