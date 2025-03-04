package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import java.time.Instant
import java.util.Date
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SuggestedFoldersPopupPolicyTest {
    private val clock = MutableClock()
    private var currentSubscriptionStatus: SubscriptionStatus? = null

    private lateinit var policy: SuggestedFoldersPopupPolicy

    @Before
    fun setUp() {
        var currentTimestamp: Instant? = null
        val timestampSetting = mock<UserSetting<Instant?>>()
        whenever(timestampSetting.value) doAnswer { currentTimestamp }
        whenever(timestampSetting.set(any(), any(), any(), any())) doAnswer { answer ->
            currentTimestamp = answer.arguments[0] as Instant?
        }

        var currentCount: Int = 0
        val countSetting = mock<UserSetting<Int>>()
        whenever(countSetting.value) doAnswer { currentCount }
        whenever(countSetting.set(any(), any(), any(), any())) doAnswer { answer ->
            currentCount = answer.arguments[0] as Int
        }

        val subscriptionStatusSetting = mock<UserSetting<SubscriptionStatus?>>()
        whenever(subscriptionStatusSetting.value) doAnswer { currentSubscriptionStatus }

        policy = SuggestedFoldersPopupPolicy(
            settings = mock<Settings> {
                on { suggestedFoldersDismissTimestamp } doReturn timestampSetting
                on { suggestedFoldersDismissCount } doReturn countSetting
                on { cachedSubscriptionStatus } doReturn subscriptionStatusSetting
            },
            clock = clock,
        )
    }

    @Test
    fun `initial policy for unsinged unser`() {
        currentSubscriptionStatus = null

        assertTrue(policy.isEligibleForPopup())
    }

    @Test
    fun `initial policy for free unser`() {
        currentSubscriptionStatus = SubscriptionStatus.Free()

        assertTrue(policy.isEligibleForPopup())
    }

    @Test
    fun `initial policy for paid unser`() {
        currentSubscriptionStatus = SubscriptionStatus.Paid(
            expiryDate = Date(),
            tier = SubscriptionTier.PLUS,
            platform = SubscriptionPlatform.ANDROID,
            autoRenew = false,
            index = 0,
        )

        assertFalse(policy.isEligibleForPopup())
    }

    @Test
    fun `policy after using it once`() {
        policy.markPolicyUsed()

        assertFalse(policy.isEligibleForPopup())
    }

    @Test
    fun `policy after using it once before 7 days pass`() {
        policy.markPolicyUsed()
        clock += 7.days

        assertFalse(policy.isEligibleForPopup())
    }

    @Test
    fun `policy after using it once after 7 days pass`() {
        policy.markPolicyUsed()
        clock += 7.days + 1.milliseconds

        assertTrue(policy.isEligibleForPopup())
    }

    @Test
    fun `policy after using it twice`() {
        policy.markPolicyUsed()
        clock += 7.days + 1.milliseconds
        policy.markPolicyUsed()

        assertFalse(policy.isEligibleForPopup())
    }

    @Test
    fun `policy after using it twice and a lot time passes`() {
        policy.markPolicyUsed()
        clock += 7.days + 1.milliseconds
        policy.markPolicyUsed()
        clock += 10_000.days

        assertFalse(policy.isEligibleForPopup())
    }
}
