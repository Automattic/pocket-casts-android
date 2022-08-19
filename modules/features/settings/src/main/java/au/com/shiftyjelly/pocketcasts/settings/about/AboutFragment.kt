package au.com.shiftyjelly.pocketcasts.settings.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.BuildConfig
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.R
import au.com.shiftyjelly.pocketcasts.settings.components.RowTextButton
import au.com.shiftyjelly.pocketcasts.ui.extensions.getComposeThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeDrawable
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class AboutFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppThemeWithBackground(theme.activeTheme) {
                    AboutPage(onBackPressed = { closeFragment() })
                }
            }
        }
    }

    private fun closeFragment() {
        (activity as? FragmentHostListener)?.closeModal(this)
    }
}

private val icons = listOf(
    AppIcon(
        image = UR.attr.about_logo_wordpress,
        text = LR.string.settings_about_wordpress,
        url = "https://wordpress.com/",
        color = UR.attr.about_logo_wordpress_color,
        rotate = (-45..45).random(),
        x = 0.0,
        y = 23.20
    ),
    AppIcon(
        image = UR.attr.about_logo_jetpack,
        text = LR.string.settings_about_jetpack,
        url = "https://jetpack.com/",
        color = UR.attr.about_logo_jetpack_color,
        rotate = (-45..45).random(),
        x = 6.17,
        y = 0.0
    ),
    AppIcon(
        image = UR.attr.about_logo_dayone,
        text = LR.string.settings_about_dayone,
        url = "https://dayoneapp.com/",
        color = UR.attr.about_logo_dayone_color,
        rotate = (-45..45).random(),
        x = 3.83,
        y = 7.40
    ),
    AppIcon(
        image = UR.attr.about_logo_pocketcasts,
        text = LR.string.app_name,
        url = "https://www.pocketcasts.com/",
        color = UR.attr.about_logo_pocketcasts_color,
        rotate = 0,
        x = 2.77,
        y = 0.0
    ),
    AppIcon(
        image = UR.attr.about_logo_woo,
        text = LR.string.settings_about_woo,
        url = "https://woocommerce.com/",
        color = UR.attr.about_logo_woo_color,
        rotate = (-45..45).random(),
        x = 1.94,
        y = 17.28
    ),
    AppIcon(
        image = UR.attr.about_logo_simplenote,
        text = LR.string.settings_about_simplenote,
        url = "https://simplenote.com/",
        color = UR.attr.about_logo_simplenote_color,
        rotate = (-45..45).random(),
        x = 1.49,
        y = 0.0
    ),
    AppIcon(
        image = UR.attr.about_logo_tumblr,
        text = LR.string.settings_about_tumblr,
        url = "https://tumblr.com/",
        color = UR.attr.about_logo_tumblr_color,
        rotate = (-45..45).random(),
        x = 1.205,
        y = 19.9
    )
)

@Composable
private fun AboutPage(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .verticalScroll(scrollState)
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_title_about),
            onNavigationClick = onBackPressed
        )
        Image(
            painter = painterResource(context.getThemeDrawable(UR.attr.logo_title_vertical)),
            contentDescription = stringResource(LR.string.settings_app_icon),
            modifier = Modifier.padding(top = 56.dp)
        )
        Text(
            text = stringResource(LR.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString()),
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.theme.colors.primaryText02
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 56.dp, bottom = 8.dp)
        )
        RowTextButton(
            text = stringResource(LR.string.settings_about_rate_us),
            onClick = { rateUs(context) }
        )
        RowTextButton(
            text = stringResource(LR.string.settings_about_share_with_friends),
            onClick = { shareWithFriends(context) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        RowTextButton(
            text = stringResource(LR.string.settings_about_website),
            secondaryText = "pocketcasts.com",
            onClick = { openUrl("https://www.pocketcasts.com", context) }
        )
        RowTextButton(
            text = stringResource(LR.string.settings_about_instagram),
            secondaryText = "@pocketcasts",
            onClick = { openUrl("https://www.instagram.com/pocketcasts/", context) }
        )
        RowTextButton(
            text = stringResource(LR.string.settings_about_twitter),
            secondaryText = "@pocketcasts",
            onClick = { openUrl("https://twitter.com/pocketcasts", context) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        AutomatticFamilyRow()
        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
        LegalAndMoreRow()
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Column(
            modifier = Modifier
                .clickable { openUrl("https://automattic.com/work-with-us/", context) }
                .fillMaxWidth()
                .padding(all = 14.dp)
        ) {
            Text(
                text = stringResource(LR.string.settings_about_work_with_us),
                fontSize = 17.sp,
                color = MaterialTheme.theme.colors.primaryText01
            )
            Text(
                text = stringResource(LR.string.settings_about_work_from_anywhere),
                fontSize = 14.sp,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.theme.colors.primaryText02
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun AutomatticFamilyRow() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    var appIconViewWidth = configuration.screenWidthDp
    if (appIconViewWidth > 500) {
        appIconViewWidth = 500
    }
    Box(
        modifier = Modifier
            .clickable { openUrl("https://automattic.com", context) }
            .height(192.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = stringResource(LR.string.settings_about_automattic_family),
            fontSize = 17.sp,
            color = MaterialTheme.theme.colors.primaryText01,
            modifier = Modifier.padding(all = 14.dp)
        )

        val circleWidth = appIconViewWidth / 6.0
        icons.forEach { icon ->
            AppLogoImage(
                width = circleWidth.dp,
                image = painterResource(context.getThemeDrawable(icon.image)),
                text = stringResource(icon.text),
                color = context.getComposeThemeColor(icon.color),
                rotateImage = icon.rotate,
                onClick = { openUrl(icon.url, context) },
                modifier = Modifier.offset(
                    x = (if (icon.x == 0.0) 0.0 else appIconViewWidth / icon.x).dp,
                    y = (192 - circleWidth - (if (icon.y == 0.0) 0.0 else appIconViewWidth / icon.y)).dp
                )
            )
        }
    }
}

@Composable
fun LegalAndMoreRow() {
    val context = LocalContext.current
    var legalExpanded by rememberSaveable { mutableStateOf(false) }
    val target = if (legalExpanded) 360f else 180f
    val rotation by animateFloatAsState(target)
    Row(
        modifier = Modifier
            .clickable { legalExpanded = !legalExpanded }
            .fillMaxWidth()
            .padding(all = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(LR.string.settings_about_legal),
            fontSize = 17.sp,
            color = MaterialTheme.theme.colors.primaryText01
        )
        Image(
            painter = painterResource(R.drawable.row_expand_arrow),
            contentDescription = stringResource(if (legalExpanded) LR.string.expanded else LR.string.collapsed),
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryText02),
            modifier = Modifier.rotate(rotation)
        )
    }
    AnimatedVisibility(visible = legalExpanded) {
        Column {
            RowTextButton(
                text = stringResource(LR.string.settings_about_terms_of_serivce),
                onClick = { openUrl(Settings.INFO_TOS_URL, context) }
            )
            RowTextButton(
                text = stringResource(LR.string.settings_about_privacy_policy),
                onClick = { openUrl(Settings.INFO_PRIVACY_URL, context) }
            )
            RowTextButton(
                text = stringResource(LR.string.settings_about_acknowledgements),
                onClick = { openAcknowledgements(context) }
            )
        }
    }
}

fun openAcknowledgements(context: Context) {
    OssLicensesMenuActivity.setActivityTitle(context.getString(LR.string.settings_licenses))
    context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
}

private fun shareWithFriends(context: Context) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_TEXT, context.getString(LR.string.settings_about_share_with_friends_message))
    try {
        context.startActivity(Intent.createChooser(intent, context.getString(LR.string.share)))
    } catch (e: IllegalStateException) {
        // Not attached to activity anymore
    }
}

private fun rateUs(context: Context) {
    val packageName = context.packageName.removeSuffix(".debug")
    val uri = Uri.parse("market://details?id=$packageName")
    val goToMarket = Intent(Intent.ACTION_VIEW, uri)
    // To count with Play market backstack, After pressing back button, to taken back to our application, we need to add following flags to intent.
    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    try {
        context.startActivity(goToMarket)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=$packageName")))
    }
}

private fun openUrl(url: String, context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Timber.i("Failed to open url $url")
    }
}

private data class AppIcon(@AttrRes val image: Int, @StringRes val text: Int, val url: String, @AttrRes val color: Int, val x: Double, val y: Double, val rotate: Int)

@Composable
private fun AppLogoImage(width: Dp, image: Painter, text: String, color: Color, modifier: Modifier = Modifier, rotateImage: Int = 0, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(width)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = image,
            contentDescription = text,
            modifier = Modifier
                .rotate(rotateImage.toFloat())
                .fillMaxWidth(0.7f)
        )
    }
}

@Preview
@Composable
fun AboutPagePreview() {
    AboutPage(onBackPressed = {})
}
