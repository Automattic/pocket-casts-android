package au.com.shiftyjelly.pocketcasts.settings.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.BuildConfig
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.LicensesFragment
import au.com.shiftyjelly.pocketcasts.settings.R
import au.com.shiftyjelly.pocketcasts.settings.components.RowTextButton
import au.com.shiftyjelly.pocketcasts.ui.extensions.getComposeThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeDrawable
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.utils.rateUs
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class AboutFragment : BaseFragment() {

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    @Inject lateinit var settings: Settings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(initialValue = 0)
        CallOnce {
            analyticsTracker.track(AnalyticsEvent.SETTINGS_ABOUT_SHOWN)
        }

        AppThemeWithBackground(theme.activeTheme) {
            AboutPage(
                openFragment = { fragment ->
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_ABOUT_LEGAL_AND_MORE_TAPPED, mapOf("row" to "acknowledgements"))
                    (activity as? FragmentHostListener)?.addFragment(fragment)
                },
                onBackPress = { closeFragment() },
                bottomInset = bottomInset.value.pxToDp(LocalContext.current).dp,
                onRateUsClick = {
                    analyticsTracker.track(AnalyticsEvent.RATE_US_TAPPED, mapOf("source" to SourceView.ABOUT.analyticsValue))
                },
                onShareWithFriendsClick = {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_ABOUT_SHARE_WITH_FRIENDS_TAPPED)
                },
                onWebsiteClick = {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_ABOUT_WEBSITE_TAPPED)
                },
                onInstagramClick = {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_ABOUT_INSTAGRAM_TAPPED)
                },
                onXClick = {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_ABOUT_X_TAPPED)
                },
                onAutomatticFamilyClick = {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_ABOUT_AUTOMATTIC_FAMILY_TAPPED)
                },
                onWorkWithUsClick = {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_ABOUT_WORK_WITH_US_TAPPED)
                },
                onTermsOfServiceClick = {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_ABOUT_LEGAL_AND_MORE_TAPPED, mapOf("row" to "terms_of_service"))
                },
                onPrivacyPolicyClick = {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_ABOUT_LEGAL_AND_MORE_TAPPED, mapOf("row" to "privacy_policy"))
                },
            )
        }
    }

    private fun closeFragment() {
        (activity as? FragmentHostListener)?.closeModal(this)
    }
}

private val icons = listOf(
    AppIcon(
        image = UR.attr.about_logo_jetpack,
        text = LR.string.settings_about_jetpack,
        url = "https://jetpack.com/",
        color = UR.attr.about_logo_jetpack_color,
        rotate = (-45..45).random(),
        x = 0.0,
        y = 0.0,
    ),
    AppIcon(
        image = UR.attr.about_logo_dayone,
        text = LR.string.settings_about_dayone,
        url = "https://dayoneapp.com/",
        color = UR.attr.about_logo_dayone_color,
        rotate = (-45..45).random(),
        x = 6.70,
        y = 9.40,
    ),
    AppIcon(
        image = UR.attr.about_logo_pocketcasts,
        text = LR.string.app_name,
        url = "https://www.pocketcasts.com/",
        color = UR.attr.about_logo_pocketcasts_color,
        rotate = 0,
        x = 3.37,
        y = 0.0,
    ),
    AppIcon(
        image = UR.attr.about_logo_woo,
        text = LR.string.settings_about_woo,
        url = "https://woocommerce.com/",
        color = UR.attr.about_logo_woo_color,
        rotate = (-45..45).random(),
        x = 2.13,
        y = 17.28,
    ),
    AppIcon(
        image = UR.attr.about_logo_simplenote,
        text = LR.string.settings_about_simplenote,
        url = "https://simplenote.com/",
        color = UR.attr.about_logo_simplenote_color,
        rotate = (-45..45).random(),
        x = 1.56,
        y = 0.0,
    ),
    AppIcon(
        image = UR.attr.about_logo_tumblr,
        text = LR.string.settings_about_tumblr,
        url = "https://tumblr.com/",
        color = UR.attr.about_logo_tumblr_color,
        rotate = (-45..45).random(),
        x = 1.225,
        y = 20.0,
    ),
)

@Composable
private fun AboutPage(
    bottomInset: Dp,
    onBackPress: () -> Unit,
    onRateUsClick: () -> Unit,
    onShareWithFriendsClick: () -> Unit,
    onWebsiteClick: () -> Unit,
    onInstagramClick: () -> Unit,
    onXClick: () -> Unit,
    openFragment: (Fragment) -> Unit,
    onAutomatticFamilyClick: () -> Unit = {},
    onWorkWithUsClick: () -> Unit = {},
    onTermsOfServiceClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .background(MaterialTheme.theme.colors.primaryUi02),
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_title_about),
            onNavigationClick = onBackPress,
        )
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            item {
                Image(
                    painter = painterResource(context.getThemeDrawable(UR.attr.logo_title_vertical)),
                    contentDescription = stringResource(LR.string.settings_app_icon),
                    modifier = Modifier.padding(top = 56.dp),
                )
            }
            item {
                Text(
                    text = stringResource(LR.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString()),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.theme.colors.primaryText02,
                )
            }
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 56.dp, bottom = 8.dp),
                )
            }
            item {
                RowTextButton(
                    text = stringResource(LR.string.settings_about_rate_us),
                    onClick = {
                        onRateUsClick()
                        rateUs(context)
                    },
                )
            }
            item {
                RowTextButton(
                    text = stringResource(LR.string.settings_about_share_with_friends),
                    onClick = {
                        onShareWithFriendsClick()
                        shareWithFriends(context)
                    },
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item {
                RowTextButton(
                    text = stringResource(LR.string.settings_about_website),
                    secondaryText = "pocketcasts.com",
                    onClick = {
                        onWebsiteClick()
                        openUrl("https://www.pocketcasts.com", context)
                    },
                )
            }
            item {
                RowTextButton(
                    text = stringResource(LR.string.settings_about_instagram),
                    secondaryText = "@pocketcasts",
                    onClick = {
                        onInstagramClick()
                        openUrl("https://www.instagram.com/pocketcasts/", context)
                    },
                )
            }
            item {
                RowTextButton(
                    text = stringResource(LR.string.settings_about_x),
                    secondaryText = "@pocketcasts",
                    onClick = {
                        onXClick()
                        openUrl("https://x.com/pocketcasts", context)
                    },
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item {
                AutomatticFamilyRow(onAutomatticFamilyClick = onAutomatticFamilyClick)
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
            }
            item {
                LegalAndMoreRow(onTermsOfServiceClick, onPrivacyPolicyClick, openFragment)
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item {
                Column(
                    modifier = Modifier
                        .clickable {
                            onWorkWithUsClick()
                            openUrl("https://automattic.com/work-with-us/", context)
                        }
                        .fillMaxWidth()
                        .padding(all = 14.dp),
                ) {
                    Text(
                        text = stringResource(LR.string.settings_about_work_with_us),
                        fontSize = 17.sp,
                        color = MaterialTheme.theme.colors.primaryText01,
                    )
                    Text(
                        text = stringResource(LR.string.settings_about_work_from_anywhere),
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.theme.colors.primaryText02,
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                AutomatticLogo(onAutomatticFamilyClick = onAutomatticFamilyClick)
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AutomatticLogo(
    modifier: Modifier = Modifier,
    onAutomatticFamilyClick: () -> Unit = {},
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onAutomatticFamilyClick()
                openUrl("https://automattic.com", context)
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(IR.drawable.about_logo_automattic),
            contentDescription = stringResource(LR.string.settings_app_icon),
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryText01),
        )
    }
}

@Composable
private fun AutomatticFamilyRow(
    onAutomatticFamilyClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    var appIconViewWidth = configuration.screenWidthDp
    if (appIconViewWidth > 500) {
        appIconViewWidth = 500
    }
    Box(
        modifier = Modifier
            .clickable {
                onAutomatticFamilyClick()
                openUrl("https://automattic.com", context)
            }
            .height(180.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.TopStart,
    ) {
        Text(
            text = stringResource(LR.string.settings_about_automattic_family),
            fontSize = 17.sp,
            color = MaterialTheme.theme.colors.primaryText01,
            modifier = Modifier.padding(all = 14.dp),
        )

        val circleWidth = appIconViewWidth / 5.5
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
                    y = (180 - circleWidth - (if (icon.y == 0.0) 0.0 else appIconViewWidth / icon.y)).dp,
                ),
            )
        }
    }
}

@Composable
private fun LegalAndMoreRow(
    termsOfService: () -> Unit,
    privacyPolicy: () -> Unit,
    openFragment: (Fragment) -> Unit,
) {
    val context = LocalContext.current
    var legalExpanded by rememberSaveable { mutableStateOf(false) }
    val target = if (legalExpanded) 360f else 180f
    val rotation by animateFloatAsState(target)
    Row(
        modifier = Modifier
            .clickable { legalExpanded = !legalExpanded }
            .fillMaxWidth()
            .padding(all = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(LR.string.settings_about_legal),
            fontSize = 17.sp,
            color = MaterialTheme.theme.colors.primaryText01,
        )
        Image(
            painter = painterResource(R.drawable.row_expand_arrow),
            contentDescription = stringResource(if (legalExpanded) LR.string.expanded else LR.string.collapsed),
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryText02),
            modifier = Modifier.rotate(rotation),
        )
    }
    AnimatedVisibility(visible = legalExpanded) {
        Column {
            RowTextButton(
                text = stringResource(LR.string.settings_about_terms_of_serivce),
                onClick = {
                    termsOfService()
                    openUrl(Settings.INFO_TOS_URL, context)
                },
            )
            RowTextButton(
                text = stringResource(LR.string.settings_about_privacy_policy),
                onClick = {
                    privacyPolicy()
                    openUrl(Settings.INFO_PRIVACY_URL, context)
                },
            )
            RowTextButton(
                text = stringResource(LR.string.settings_about_acknowledgements),
                onClick = { openFragment(LicensesFragment()) },
            )
        }
    }
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

private fun openUrl(url: String, context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (e: Exception) {
        Timber.i("Failed to open url $url")
    }
}

private data class AppIcon(@AttrRes val image: Int, @StringRes val text: Int, val url: String, @AttrRes val color: Int, val x: Double, val y: Double, val rotate: Int)

@Composable
private fun AppLogoImage(
    width: Dp,
    image: Painter,
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rotateImage: Int = 0,
) {
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
                .fillMaxWidth(0.7f),
        )
    }
}

@Preview
@Composable
private fun AboutPagePreview() {
    AboutPage(
        onBackPress = {},
        bottomInset = 0.dp,
        openFragment = {},
        onRateUsClick = {},
        onShareWithFriendsClick = {},
        onWebsiteClick = {},
        onInstagramClick = {},
        onXClick = {},
        onAutomatticFamilyClick = {},
        onWorkWithUsClick = {},
    )
}
