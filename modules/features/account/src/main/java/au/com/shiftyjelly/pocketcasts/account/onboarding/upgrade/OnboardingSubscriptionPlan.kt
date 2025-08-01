package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.account.R
import au.com.shiftyjelly.pocketcasts.compose.patronPurple
import au.com.shiftyjelly.pocketcasts.compose.plusGold
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.payment.PricingPhase
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule.RecurrenceMode
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@ConsistentCopyVisibility
data class OnboardingSubscriptionPlan private constructor(
    val key: SubscriptionPlan.Key,
    val pricingPhase: PricingPhase,
    val discountedPricingPhase: PricingPhase?,
) {
    val highlightedPrice
        get() = when (key.offer) {
            SubscriptionOffer.IntroOffer -> requireNotNull(discountedPricingPhase).price
            SubscriptionOffer.Trial,
            SubscriptionOffer.Referral,
            SubscriptionOffer.Winback,
            null,
            -> pricingPhase.price
        }

    val crossedPrice
        get() = when (key.offer) {
            SubscriptionOffer.IntroOffer -> pricingPhase.price
            SubscriptionOffer.Trial,
            SubscriptionOffer.Referral,
            SubscriptionOffer.Winback,
            null,
            -> null
        }

    val shortNameRes
        get() = when (key.tier) {
            SubscriptionTier.Plus -> LR.string.pocket_casts_plus_short
            SubscriptionTier.Patron -> LR.string.pocket_casts_patron_short
        }

    val badgeIconRes
        get() = when (key.tier) {
            SubscriptionTier.Plus -> IR.drawable.ic_plus
            SubscriptionTier.Patron -> IR.drawable.ic_patron
        }

    val pageTitle
        get() = when (key.tier) {
            SubscriptionTier.Plus -> LR.string.onboarding_upgrade_generic_title
            SubscriptionTier.Patron -> LR.string.onboarding_upgrade_patron_title
        }

    val pricePerPeriodText
        @Composable get() = when (key.offer) {
            SubscriptionOffer.IntroOffer -> when (key.billingCycle) {
                BillingCycle.Monthly -> stringResource(LR.string.plus_per_month, requireNotNull(discountedPricingPhase).price.formattedPrice)
                BillingCycle.Yearly -> stringResource(LR.string.plus_per_year, requireNotNull(discountedPricingPhase).price.formattedPrice)
            }

            SubscriptionOffer.Trial,
            SubscriptionOffer.Referral,
            SubscriptionOffer.Winback,
            null,
            -> when (key.billingCycle) {
                BillingCycle.Monthly -> stringResource(LR.string.plus_per_month, pricingPhase.price.formattedPrice)
                BillingCycle.Yearly -> stringResource(LR.string.plus_per_year, pricingPhase.price.formattedPrice)
            }
        }

    val pricePerPeriodWithSlashText
        @Composable get() = when (key.billingCycle) {
            BillingCycle.Monthly -> "/ ${stringResource(LR.string.plus_month)}"
            BillingCycle.Yearly -> "/ ${stringResource(LR.string.plus_year)}"
        }

    val planDescriptionText
        @Composable get() = when (key.offer) {
            SubscriptionOffer.IntroOffer -> {
                val yearFromNow = ZonedDateTime.now().plusYears(1)
                val formattedDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(yearFromNow)
                stringResource(
                    LR.string.onboarding_plus_recurring_after_intro_offer,
                    pricingPhase.price.formattedPrice,
                    formattedDate,
                )
            }

            SubscriptionOffer.Trial -> {
                val discountedPhase = requireNotNull(discountedPricingPhase)
                val recurringPeriods = (discountedPhase.schedule.recurrenceMode as RecurrenceMode.Recurring).value
                val chronoUnit = when (discountedPhase.schedule.period) {
                    PricingSchedule.Period.Daily -> ChronoUnit.DAYS
                    PricingSchedule.Period.Weekly -> ChronoUnit.WEEKS
                    PricingSchedule.Period.Monthly -> ChronoUnit.MONTHS
                    PricingSchedule.Period.Yearly -> ChronoUnit.YEARS
                }
                val dateFromNow = ZonedDateTime.now().plus(recurringPeriods.toLong(), chronoUnit)
                val formattedDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(dateFromNow)

                stringResource(
                    LR.string.onboarding_plus_recurring_after_free_trial,
                    discountedPhase.schedule.period.toText(recurringPeriods),
                    formattedDate,
                )
            }

            SubscriptionOffer.Referral,
            SubscriptionOffer.Winback,
            null,
            -> {
                val firstLine = stringResource(
                    when (key.billingCycle) {
                        BillingCycle.Monthly -> LR.string.plus_renews_automatically_monthly
                        BillingCycle.Yearly -> LR.string.plus_renews_automatically_yearly
                    },
                )
                val secondLine = stringResource(LR.string.onboarding_plus_can_be_canceled_at_any_time)
                "$firstLine.\n$secondLine"
            }
        }

    val offerBadgeText
        @Composable get() = when (key.offer) {
            SubscriptionOffer.IntroOffer -> stringResource(LR.string.half_price_first_year)
            SubscriptionOffer.Trial -> {
                val discountedPhase = requireNotNull(discountedPricingPhase)
                val recurringPeriods = (discountedPhase.schedule.recurrenceMode as RecurrenceMode.Recurring).value

                stringResource(LR.string.plus_trial_duration_free_trial, discountedPhase.schedule.period.toText(recurringPeriods))
            }

            SubscriptionOffer.Referral -> null
            SubscriptionOffer.Winback -> null
            null -> null
        }

    val offerBadgeColorRes
        get() = when (key.tier) {
            SubscriptionTier.Plus -> UR.color.plus_gold
            SubscriptionTier.Patron -> UR.color.patron_purple
        }

    val offerBadgeTextColorRes
        get() = when (key.tier) {
            SubscriptionTier.Plus -> UR.color.black
            SubscriptionTier.Patron -> UR.color.white
        }

    @Composable
    fun ctaButtonText(isRenewingSubscription: Boolean) = if (isRenewingSubscription) {
        stringResource(LR.string.renew_your_subscription)
    } else {
        when (key.offer) {
            SubscriptionOffer.Trial -> stringResource(LR.string.profile_start_free_trial)
            SubscriptionOffer.IntroOffer,
            SubscriptionOffer.Referral,
            SubscriptionOffer.Winback,
            null,
            -> stringResource(LR.string.subscribe_to, stringResource(shortNameRes))
        }
    }

    val ctaButtonBackgroundColor
        get() = when (key.tier) {
            SubscriptionTier.Plus -> Color.plusGold
            SubscriptionTier.Patron -> Color.patronPurple
        }

    val ctaButtonTextColor
        get() = when (key.tier) {
            SubscriptionTier.Plus -> Color.Black
            SubscriptionTier.Patron -> Color.White
        }

    val featureItems: List<UpgradeFeatureItem>
        get() {
            val items = when (key.tier) {
                SubscriptionTier.Plus -> PlusUpgradeFeatureItem.entries.filter {
                    !FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE) || it != PlusUpgradeFeatureItem.BannerAds
                }

                SubscriptionTier.Patron -> PatronUpgradeFeatureItem.entries
            }
            return items.filter { item ->
                when (key.billingCycle) {
                    BillingCycle.Monthly -> item.isMonthlyFeature
                    BillingCycle.Yearly -> item.isYearlyFeature
                }
            }
        }

    val backgroundGlowsRes
        get() = when (key.tier) {
            SubscriptionTier.Plus -> R.drawable.upgrade_background_plus_glows
            SubscriptionTier.Patron -> R.drawable.upgrade_background_patron_glows
        }

    fun customFeatureTitle(source: OnboardingUpgradeSource) = when (key.tier) {
        SubscriptionTier.Plus -> when (source) {
            OnboardingUpgradeSource.BANNER_AD -> LR.string.banner_ad_plus_prompt
            OnboardingUpgradeSource.SKIP_CHAPTERS -> if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
                LR.string.onboarding_preselect_chapters_title
            } else {
                LR.string.skip_chapters_plus_prompt
            }

            OnboardingUpgradeSource.UP_NEXT_SHUFFLE -> if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
                LR.string.onboarding_shuffle_title
            } else {
                LR.string.up_next_shuffle_plus_prompt
            }

            OnboardingUpgradeSource.THEMES -> LR.string.themes_plus_prompt
            OnboardingUpgradeSource.ICONS -> LR.string.icons_plus_prompt
            OnboardingUpgradeSource.FILES -> LR.string.files_plus_prompt
            OnboardingUpgradeSource.FOLDERS,
            OnboardingUpgradeSource.FOLDERS_PODCAST_SCREEN,
            OnboardingUpgradeSource.SUGGESTED_FOLDERS,
            -> if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
                LR.string.onboarding_folders_title
            } else {
                LR.string.folders_plus_prompt
            }

            OnboardingUpgradeSource.BOOKMARKS,
            OnboardingUpgradeSource.BOOKMARKS_SHELF_ACTION,
            -> if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
                LR.string.onboarding_bookmarks_title
            } else {
                LR.string.onboarding_plus_features_title
            }

            else -> if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
                LR.string.onboarding_upgrade_generic_title
            } else {
                LR.string.onboarding_plus_features_title
            }
        }

        SubscriptionTier.Patron -> if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
            LR.string.onboarding_upgrade_patron_title
        } else {
            LR.string.onboarding_patron_features_title
        }
    }

    companion object {
        fun create(plan: SubscriptionPlan.Base): OnboardingSubscriptionPlan {
            return OnboardingSubscriptionPlan(plan.key, plan.pricingPhase, discountedPricingPhase = null)
        }

        fun create(plan: SubscriptionPlan.WithOffer): PaymentResult<OnboardingSubscriptionPlan> {
            return when (plan.offer) {
                SubscriptionOffer.IntroOffer -> {
                    when {
                        plan.pricingPhases.size != 2 -> PaymentResult.Failure(
                            PaymentResultCode.DeveloperError,
                            "${plan.offer} should have 2 pricing phases",
                        )

                        plan.pricingPhases[0].schedule.recurrenceMode !is RecurrenceMode.Recurring -> PaymentResult.Failure(
                            PaymentResultCode.DeveloperError,
                            "${plan.offer} should have recurring schedule",
                        )

                        plan.pricingPhases[1].schedule.recurrenceMode != RecurrenceMode.Infinite -> PaymentResult.Failure(
                            PaymentResultCode.DeveloperError,
                            "${plan.offer} should have infinite second period",
                        )

                        else -> PaymentResult.Success(
                            OnboardingSubscriptionPlan(plan.key, plan.pricingPhases[1], plan.pricingPhases[0]),
                        )
                    }
                }

                SubscriptionOffer.Trial -> {
                    when {
                        plan.pricingPhases.size != 2 -> PaymentResult.Failure(
                            PaymentResultCode.DeveloperError,
                            "${plan.offer} should have 2 pricing phases",
                        )

                        plan.pricingPhases[0].price.amount.stripTrailingZeros() != BigDecimal.ZERO -> PaymentResult.Failure(
                            PaymentResultCode.DeveloperError,
                            "${plan.offer} should have free initial period. Found ${plan.pricingPhases[0].price}",
                        )

                        plan.pricingPhases[0].schedule.recurrenceMode !is RecurrenceMode.Recurring -> PaymentResult.Failure(
                            PaymentResultCode.DeveloperError,
                            "${plan.offer} should have recurring initial period",
                        )

                        plan.pricingPhases[1].schedule.recurrenceMode != RecurrenceMode.Infinite -> PaymentResult.Failure(
                            PaymentResultCode.DeveloperError,
                            "${plan.offer} should have infinite second period",
                        )

                        else -> PaymentResult.Success(
                            OnboardingSubscriptionPlan(plan.key, plan.pricingPhases[1], plan.pricingPhases[0]),
                        )
                    }
                }

                SubscriptionOffer.Referral, SubscriptionOffer.Winback -> PaymentResult.Failure(
                    PaymentResultCode.DeveloperError,
                    "Can't crate an onboarding offer from ${plan.offer}",
                )
            }
        }
    }
}

@Composable
private fun PricingSchedule.Period.toText(count: Int) = when (this) {
    PricingSchedule.Period.Daily -> pluralStringResource(LR.plurals.day_with_count, count, count)
    PricingSchedule.Period.Weekly -> pluralStringResource(LR.plurals.week_with_count, count, count)
    PricingSchedule.Period.Monthly -> pluralStringResource(LR.plurals.month_with_count, count, count)
    PricingSchedule.Period.Yearly -> pluralStringResource(LR.plurals.year_with_count, count, count)
}
