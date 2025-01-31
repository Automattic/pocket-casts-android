package au.com.shiftyjelly.pocketcasts.profile

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.ThemeColors
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.patronPurple
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.profile.winback.WinbackInitParams
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AccountSections(
    state: AccountSectionsState,
    onChangeAvatar: (String) -> Unit,
    onChangeEmail: () -> Unit,
    onChangePassword: () -> Unit,
    onUpgradeToPatron: () -> Unit,
    onCancelSubscription: (WinbackInitParams) -> Unit,
    onChangeNewsletterSubscription: (Boolean) -> Unit,
    onShowPrivacyPolicy: () -> Unit,
    onShowTermsOfUse: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    config: AccountSectionsConfig = AccountSectionsConfig(),
) {
    val sectionGroups = remember(state.availableSections) {
        AccountSection.groupedSections.mapNotNull { sections ->
            sections.filter { it in state.availableSections }.takeIf { it.isNotEmpty() }
        }
    }
    Column(
        modifier = modifier,
    ) {
        sectionGroups.forEachIndexed { groupIndex, group ->
            group.forEachIndexed { sectionIndex, section ->
                HorizontalDivider()
                when (section) {
                    AccountSection.ChangeAvatar -> ButtonSection(
                        section = section,
                        config = config,
                        onClick = { state.email?.let(onChangeAvatar) },
                    )
                    AccountSection.ChangeEmail -> ButtonSection(
                        section = section,
                        config = config,
                        onClick = onChangeEmail,
                    )
                    AccountSection.ChangePassword -> ButtonSection(
                        section = section,
                        config = config,
                        onClick = onChangePassword,
                    )
                    AccountSection.UpgradeToPatron -> ButtonSection(
                        section = section,
                        config = config,
                        onClick = onUpgradeToPatron,
                    )
                    AccountSection.CancelSubscription -> ButtonSection(
                        section = section,
                        config = config,
                        onClick = { onCancelSubscription(state.winbackInitParams) },
                    )
                    AccountSection.Newsletter -> SwitchSection(
                        isToggled = state.isSubscribedToNewsLetter,
                        section = section,
                        config = config,
                        onChange = onChangeNewsletterSubscription,
                    )
                    AccountSection.PrivacyPolicy -> ButtonSection(
                        section = section,
                        config = config,
                        onClick = onShowPrivacyPolicy,
                    )
                    AccountSection.TermsOfUse -> ButtonSection(
                        section = section,
                        config = config,
                        onClick = onShowTermsOfUse,
                    )
                    AccountSection.SignOut -> ButtonSection(
                        section = section,
                        config = config,
                        onClick = onSignOut,
                    )
                    AccountSection.DeleteAccount -> ButtonSection(
                        section = section,
                        config = config,
                        onClick = onDeleteAccount,
                    )
                }
                if (sectionIndex == group.lastIndex) {
                    HorizontalDivider()
                }
            }
            if (groupIndex != sectionGroups.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(MaterialTheme.theme.colors.primaryUi03),
                )
            }
        }
    }
}

internal data class AccountSectionsConfig(
    val iconSize: Dp = 24.dp,
    val fontScale: Float = 1f,
)

internal data class AccountSectionsState(
    val isSubscribedToNewsLetter: Boolean,
    val email: String?,
    val winbackInitParams: WinbackInitParams,
    val availableSections: List<AccountSection>,
) {
    constructor(
        isSubscribedToNewsLetter: Boolean,
        email: String?,
        winbackInitParams: WinbackInitParams,
        canChangeCredentials: Boolean,
        canUpgradeAccount: Boolean,
        canCancelSubscription: Boolean,
    ) : this(
        isSubscribedToNewsLetter = isSubscribedToNewsLetter,
        email = email,
        winbackInitParams = winbackInitParams,
        availableSections = buildList {
            addAll(AccountSection.entries)
            if (email == null) {
                remove(AccountSection.ChangeAvatar)
            }
            if (!canChangeCredentials) {
                remove(AccountSection.ChangeEmail)
                remove(AccountSection.ChangePassword)
            }
            if (!canUpgradeAccount) {
                remove(AccountSection.UpgradeToPatron)
            }
            if (!canCancelSubscription) {
                remove(AccountSection.CancelSubscription)
            }
        },
    )

    companion object {
        fun empty() = AccountSectionsState(
            isSubscribedToNewsLetter = false,
            email = null,
            winbackInitParams = WinbackInitParams.Empty,
            canChangeCredentials = false,
            canUpgradeAccount = false,
            canCancelSubscription = false,
        )
    }
}

internal enum class AccountSection(
    @DrawableRes val iconId: Int,
    @StringRes val titleId: Int,
    @StringRes val descriptionId: Int? = null,
    val iconTint: @Composable ThemeColors.() -> Color = { primaryInteractive01 },
    val titleColor: @Composable ThemeColors.() -> Color = { primaryInteractive01 },
    val descriptionColor: @Composable ThemeColors.() -> Color = { primaryInteractive01 },
) {
    ChangeAvatar(
        iconId = IR.drawable.ic_profile_circle,
        titleId = LR.string.profile_change_avatar,
    ),
    ChangeEmail(
        iconId = IR.drawable.ic_mail,
        titleId = LR.string.profile_change_email_address,
    ),
    ChangePassword(
        iconId = IR.drawable.ic_password,
        titleId = LR.string.profile_change_password,
    ),
    UpgradeToPatron(
        iconId = IR.drawable.ic_patron,
        titleId = LR.string.profile_upgrade_to_patron,
        iconTint = { Color.patronPurple },
        titleColor = { Color.patronPurple },
    ),
    CancelSubscription(
        iconId = IR.drawable.ic_subscription_cancel,
        titleId = LR.string.profile_cancel_subscription,
    ),
    Newsletter(
        iconId = IR.drawable.ic_newsletter,
        titleId = LR.string.profile_pocket_casts_newsletter,
        descriptionId = LR.string.profile_newsletter_detail,
        titleColor = { primaryText01 },
        descriptionColor = { primaryText02 },
    ),
    PrivacyPolicy(
        iconId = IR.drawable.ic_privacy_policy,
        titleId = LR.string.profile_privacy_policy,
    ),
    TermsOfUse(
        iconId = IR.drawable.ic_terms_conditions,
        titleId = LR.string.profile_terms_of_use,
    ),
    SignOut(
        iconId = IR.drawable.ic_signout,
        titleId = LR.string.profile_sign_out,
        iconTint = { support05 },
        titleColor = { support05 },
    ),
    DeleteAccount(
        iconId = IR.drawable.ic_delete,
        titleId = LR.string.profile_delete_account,
        iconTint = { support05 },
        titleColor = { support05 },
    ),
    ;

    companion object {
        val groupedSections = listOf(
            listOf(
                ChangeAvatar,
                ChangeEmail,
                ChangePassword,
                UpgradeToPatron,
                CancelSubscription,
                Newsletter,
            ),
            listOf(
                PrivacyPolicy,
                TermsOfUse,
            ),
            listOf(
                SignOut,
            ),
            listOf(
                DeleteAccount,
            ),
        )
    }
}

@Composable
private fun ButtonSection(
    section: AccountSection,
    config: AccountSectionsConfig,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(
                interactionSource = remember(::MutableInteractionSource),
                indication = ripple(
                    color = section.iconTint(MaterialTheme.theme.colors),
                ),
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
            }
            .padding(16.dp),
    ) {
        Icon(
            painter = painterResource(section.iconId),
            contentDescription = null,
            tint = section.iconTint(MaterialTheme.theme.colors),
            modifier = Modifier.size(config.iconSize),
        )
        Spacer(
            modifier = Modifier.width(16.dp),
        )
        TextH40(
            text = stringResource(section.titleId),
            color = section.titleColor(MaterialTheme.theme.colors),
            fontScale = config.fontScale,
        )
    }
}

@Composable
private fun SwitchSection(
    isToggled: Boolean,
    section: AccountSection,
    config: AccountSectionsConfig,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .height(IntrinsicSize.Max)
            .clickable(
                interactionSource = remember(::MutableInteractionSource),
                indication = ripple(
                    color = section.iconTint(MaterialTheme.theme.colors),
                ),
                onClick = { onChange(!isToggled) },
            )
            .semantics(mergeDescendants = true) {
                role = Role.Switch
            }
            .padding(16.dp),
    ) {
        Icon(
            painter = painterResource(section.iconId),
            contentDescription = null,
            tint = section.iconTint(MaterialTheme.theme.colors),
            modifier = Modifier.size(config.iconSize),
        )
        Spacer(
            modifier = Modifier.width(16.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth(fraction = 0.75f),
        ) {
            TextH40(
                text = stringResource(section.titleId),
                color = section.titleColor(MaterialTheme.theme.colors),
                fontScale = config.fontScale,
            )
            if (section.descriptionId != null) {
                TextP50(
                    text = stringResource(section.descriptionId),
                    color = section.descriptionColor(MaterialTheme.theme.colors),
                    modifier = Modifier.padding(top = 4.dp),
                    fontScale = config.fontScale,
                )
            }
        }
        Spacer(
            modifier = Modifier.weight(1f),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxHeight(),
        ) {
            Switch(
                checked = isToggled,
                onCheckedChange = onChange,
            )
        }
    }
}

@Preview
@Composable
private fun AccountSectionsPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        Box(
            modifier = Modifier.background(MaterialTheme.theme.colors.primaryUi02),
        ) {
            AccountSections(
                state = AccountSectionsState(
                    isSubscribedToNewsLetter = false,
                    email = "",
                    availableSections = AccountSection.entries,
                    winbackInitParams = WinbackInitParams.Empty,
                ),
                onChangeAvatar = { },
                onChangeEmail = { },
                onChangePassword = { },
                onUpgradeToPatron = { },
                onCancelSubscription = { },
                onChangeNewsletterSubscription = { },
                onShowPrivacyPolicy = { },
                onShowTermsOfUse = { },
                onSignOut = { },
                onDeleteAccount = { },
            )
        }
    }
}
