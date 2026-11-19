package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.ReadSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal fun testSubscription(tier: SubscriptionTier) = Subscription(
    tier = tier,
    billingCycle = BillingCycle.Monthly,
    platform = SubscriptionPlatform.Android,
    expiryDate = Instant.now(),
    isAutoRenewing = true,
    giftDays = 0,
)

internal class GeneratedChaptersFixture(
    initialSubscription: Subscription? = testSubscription(SubscriptionTier.Plus),
    initialShowGeneratedChapters: Boolean = true,
) {
    val subscriptionFlow = MutableStateFlow(initialSubscription)
    val showGeneratedChaptersFlow = MutableStateFlow(initialShowGeneratedChapters)

    private val subscriptionSetting = mock<ReadSetting<Subscription?>> {
        on { flow } doReturn subscriptionFlow
        on { value } doAnswer { subscriptionFlow.value }
    }
    private val showGeneratedChaptersSetting = mock<UserSetting<Boolean>> {
        on { flow } doReturn showGeneratedChaptersFlow
        on { value } doAnswer { showGeneratedChaptersFlow.value }
    }
    val settings = mock<Settings> {
        on { cachedSubscription } doReturn subscriptionSetting
        on { showGeneratedChapters } doReturn showGeneratedChaptersSetting
    }

    val availability = GeneratedChaptersAvailability(settings)
}
