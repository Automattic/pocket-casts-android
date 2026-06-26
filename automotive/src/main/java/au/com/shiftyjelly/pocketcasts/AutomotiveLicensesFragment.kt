package au.com.shiftyjelly.pocketcasts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.compose.AutomotiveTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.extensions.openUrl
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.style.LicenseHueResolver
import com.mikepenz.aboutlibraries.ui.compose.style.librariesStyle
import com.mikepenz.aboutlibraries.ui.compose.style.m2VariantColors
import com.mikepenz.aboutlibraries.ui.compose.style.m2VariantTextStyles
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.ui.compose.variant.Libraries
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryBadges
import com.mikepenz.aboutlibraries.util.withContext

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
        val colors = MaterialTheme.theme.colors
        val style = LibraryDefaults.librariesStyle(
            colors = LibraryDefaults.m2VariantColors(
                rowBackground = colors.primaryUi01,
                rowExpandedBackground = colors.primaryUi02,
                rowOnBackground = colors.primaryText01,
                rowSubtleContent = colors.primaryText02,
                licenseHueResolver = LicenseHueResolver { colors.primaryInteractive01 },
            ),
            textStyles = LibraryDefaults.m2VariantTextStyles(
                nameTextStyle = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Medium, fontSize = 32.sp),
                authorTextStyle = MaterialTheme.typography.caption.copy(fontSize = 24.sp),
                licenseTextStyle = MaterialTheme.typography.overline.copy(fontWeight = FontWeight.Medium, fontSize = 18.sp),
            ),
        )
        Libraries(
            libraries = produceLibraries { context ->
                val libs = Libs.Builder().withContext(context).build()
                // without displaying the artifact id, the libraries seem to appear twice
                libs.copy(libraries = libs.libraries.distinctBy { "${it.name}##${it.author}" })
            }.value?.libraries.orEmpty(),
            style = style,
            badges = LibraryBadges(
                version = false,
                author = true,
                license = true,
            ),
            onLibraryClick = { library ->
                library.website?.let { website ->
                    openUrl(website)
                    true
                } ?: false
            },
            modifier = modifier
                .fillMaxSize()
                .background(colors.primaryUi01)
                .padding(horizontal = 16.dp),
        )
    }
}
