package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.util.withContext
import kotlinx.collections.immutable.toImmutableList

class LicensesFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        AppThemeWithBackground(theme.activeTheme) {
            LicensesPage(onBackPressed = { closeFragment() })
        }
    }

    private fun closeFragment() {
        (activity as? FragmentHostListener)?.closeModal(this)
    }

    @Composable
    private fun LicensesPage(onBackPressed: () -> Unit, modifier: Modifier = Modifier) {
        Column {
            ThemedTopAppBar(
                title = stringResource(R.string.settings_about_acknowledgements),
                onNavigationClick = onBackPressed,
            )
            LibrariesContainer(
                modifier = modifier.fillMaxSize(),
                showAuthor = true,
                showVersion = false,
                showLicenseBadges = true,
                colors = LibraryDefaults.libraryColors(
                    backgroundColor = MaterialTheme.colors.background,
                    contentColor = MaterialTheme.theme.colors.primaryText01,
                ),
                librariesBlock = { context ->
                    val libs = Libs.Builder().withContext(context).build()
                    // without displaying the artifact id the libraries seem to appear twice
                    libs.copy(
                        libs.libraries.distinctBy { "${it.name}##${it.author}" }.toImmutableList(),
                    )
                },
            )
        }
    }
}
