package au.com.shiftyjelly.pocketcasts.payment.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult

fun BillingResult.isOk() = responseCode == BillingClient.BillingResponseCode.OK
