package au.com.shiftyjelly.pocketcasts.analytics

object AnalyticsParameter {

    const val LIST_ID = "list_id"
    const val LIST_DATE = "list_datetime"
    const val PODCAST_UUID = "podcast_uuid"
    const val FLOW = "flow"
    const val CATEGORIES = "categories"
    const val CATEGORY_ID = "category_id"
    const val NAME = "name"
    const val IS_SELECTED = "is_selected"
    const val SUBSCRIPTIONS = "subscriptions"
    const val UUID = "uuid"
    const val SOURCE = "source"
    const val BUTTON = "button"
    const val YEAR = "year"

    enum class SetupAccountButton(val value: String) {
        SignIn("sign_in"),
        CreateAccount("create_account"),
        ContinueWithGoogle("continue_with_google"),
        GetStarted("get_started"),
    }
}
