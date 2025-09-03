package au.com.shiftyjelly.pocketcasts.podcasts.view.notifications

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun EnableNotificationsPromptScreen(
    onCtaClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EnableNotificationsPromptScreenV2(
        onCtaClick = onCtaClick,
        modifier = modifier,
        onDismissClick = onDismissClick,
        isAccountCreationFlagEnabled = false,
        isNotificationSelected = false,
        isNewsletterSelected = false,
        onNotificationChange = {},
        onNewsletterChange = {},
        showNewsletterSection = false,
    )
}

@Composable
internal fun EnableNotificationsPromptScreenNewOnboarding(
    onCtaClick: () -> Unit,
    showNewsletterSection: Boolean,
    isNewsletterSelected: Boolean,
    onNewsletterChange: (Boolean) -> Unit,
    isNotificationSelected: Boolean,
    onNotificationChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    EnableNotificationsPromptScreenV2(
        onCtaClick = onCtaClick,
        modifier = modifier,
        isAccountCreationFlagEnabled = true,
        isNotificationSelected = isNotificationSelected,
        isNewsletterSelected = isNewsletterSelected,
        onNotificationChange = onNotificationChange,
        onNewsletterChange = onNewsletterChange,
        showNewsletterSection = showNewsletterSection,
    )
}

@Composable
private fun EnableNotificationsPromptScreenV2(
    onCtaClick: () -> Unit,
    isAccountCreationFlagEnabled: Boolean,
    showNewsletterSection: Boolean,
    isNewsletterSelected: Boolean,
    onNewsletterChange: (Boolean) -> Unit,
    isNotificationSelected: Boolean,
    onNotificationChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onDismissClick: (() -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        onDismissClick?.let {
            IconButton(
                onClick = it,
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(
                    painter = painterResource(IR.drawable.ic_close),
                    contentDescription = stringResource(LR.string.close),
                    tint = MaterialTheme.theme.colors.primaryText01,
                )
            }
            Spacer(modifier = Modifier.height(42.dp))
        } ?: Spacer(modifier = Modifier.height(84.dp))

        if (isLandscape) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(IR.drawable.android_mockup),
                    contentDescription = stringResource(LR.string.notification_mockup_image_description),
                )
                Spacer(modifier = Modifier.width(32.dp))
                Column(
                    modifier = Modifier.padding(vertical = 24.dp),
                ) {
                    TextH10(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(LR.string.notification_prompt_title),
                        color = MaterialTheme.theme.colors.primaryText01,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextP60(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        text = stringResource(LR.string.notification_prompt_message),
                        color = MaterialTheme.theme.colors.primaryText02,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.W500,
                        fontSize = 14.5.sp,
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    if (isAccountCreationFlagEnabled) {
                        if (showNewsletterSection) {
                            CheckboxRow(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                isSelected = isNewsletterSelected,
                                onSelectedChange = onNewsletterChange,
                                title = stringResource(LR.string.onboarding_notification_popup_newsletter),
                                subTitle = stringResource(LR.string.onboarding_notification_popup_newsletter_message),
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                        CheckboxRow(
                            modifier = Modifier.fillMaxWidth(),
                            isSelected = isNotificationSelected,
                            onSelectedChange = onNotificationChange,
                            title = stringResource(LR.string.onboarding_notification_popup_notifications),
                            subTitle = stringResource(LR.string.onboarding_notification_popup_notifications_message),
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        RowButton(
                            text = stringResource(LR.string.onboarding_notifications_popup_save),
                            textColor = MaterialTheme.theme.colors.primaryInteractive02,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W500,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
                            ),
                            includePadding = false,
                            onClick = onCtaClick,
                        )
                    } else {
                        RowButton(
                            text = stringResource(LR.string.notification_prompt_cta),
                            textColor = MaterialTheme.theme.colors.primaryInteractive02,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W500,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
                            ),
                            includePadding = false,
                            onClick = onCtaClick,
                        )
                    }
                }
            }
        } else {
            Image(
                painter = painterResource(IR.drawable.android_mockup),
                contentDescription = stringResource(LR.string.notification_mockup_image_description),
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextH10(
                modifier = Modifier.padding(horizontal = 22.dp),
                text = stringResource(LR.string.notification_prompt_title),
                color = MaterialTheme.theme.colors.primaryText01,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextP60(
                modifier = Modifier.padding(horizontal = 18.dp),
                text = stringResource(LR.string.notification_prompt_message),
                color = MaterialTheme.theme.colors.primaryText02,
                textAlign = TextAlign.Center,
                fontSize = 14.5.sp,
                lineHeight = 18.sp,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isAccountCreationFlagEnabled) {
                if (showNewsletterSection) {
                    CheckboxRow(
                        modifier = Modifier
                            .fillMaxWidth(),
                        isSelected = isNewsletterSelected,
                        onSelectedChange = onNewsletterChange,
                        title = stringResource(LR.string.onboarding_notification_popup_newsletter),
                        subTitle = stringResource(LR.string.onboarding_notification_popup_newsletter_message),
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                }
                CheckboxRow(
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = isNotificationSelected,
                    onSelectedChange = onNotificationChange,
                    title = stringResource(LR.string.onboarding_notification_popup_notifications),
                    subTitle = stringResource(LR.string.onboarding_notification_popup_notifications_message),
                )
                Spacer(modifier = Modifier.height(24.dp))
                RowButton(
                    text = stringResource(LR.string.onboarding_notifications_popup_save),
                    textColor = MaterialTheme.theme.colors.primaryInteractive02,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W500,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
                    ),
                    includePadding = false,
                    onClick = onCtaClick,
                )
            } else {
                RowButton(
                    text = stringResource(LR.string.notification_prompt_cta),
                    textColor = MaterialTheme.theme.colors.primaryInteractive02,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W500,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
                    ),
                    includePadding = false,
                    onClick = onCtaClick,
                )
            }
        }
    }
}

@Composable
private fun CheckboxRow(
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    title: String,
    subTitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .toggleable(
                value = isSelected,
                role = Role.Checkbox,
                onValueChange = onSelectedChange,
            ),
    ) {
        CircularCheckBox(
            modifier = Modifier.size(24.dp),
            isChecked = isSelected,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextH40(
                text = title,
                modifier = Modifier.fillMaxWidth(),
            )
            TextP60(
                text = subTitle,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.theme.colors.primaryText02,
            )
        }
    }
}

@Composable
private fun CircularCheckBox(
    isChecked: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (isChecked) {
                    Modifier.background(color = MaterialTheme.theme.colors.primaryIcon01)
                } else {
                    Modifier.border(width = 2.dp, color = MaterialTheme.theme.colors.primaryInteractive01, shape = CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isChecked) {
            Icon(
                painter = painterResource(IR.drawable.ic_checkmark_small),
                tint = MaterialTheme.theme.colors.primaryUi01,
                contentDescription = null,
            )
        }
    }
}

@Preview
@Composable
private fun EnableNotificationsPromptScreenPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) = AppThemeWithBackground(themeType) {
    EnableNotificationsPromptScreen(
        onDismissClick = {},
        onCtaClick = {},
    )
}

@Preview
@Composable
private fun EnableNotificationsPromptScreenV2Preview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) = AppThemeWithBackground(themeType) {
    EnableNotificationsPromptScreenNewOnboarding(
        onCtaClick = {},
        isNewsletterSelected = true,
        isNotificationSelected = false,
        onNotificationChange = {},
        onNewsletterChange = {},
        showNewsletterSection = true,
    )
}
