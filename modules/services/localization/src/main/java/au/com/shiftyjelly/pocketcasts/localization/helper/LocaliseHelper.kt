package au.com.shiftyjelly.pocketcasts.localization.helper
import android.content.res.Resources
import au.com.shiftyjelly.pocketcasts.localization.R

object LocaliseHelper {

    val filterToId = mapOf(
        "In Progress" to R.string.filters_title_in_progress,
        "New Releases" to R.string.filters_title_new_releases,
        "Starred" to R.string.filters_title_starred
    )

    val stringToId = mapOf(
        "browse by category" to R.string.discover_browse_by_category,
        "arts" to R.string.discover_category_arts,
        "business" to R.string.discover_category_business,
        "comedy" to R.string.discover_category_comedy,
        "education" to R.string.discover_category_education,
        "fiction" to R.string.discover_category_fiction,
        "government" to R.string.discover_category_government,
        "health & fitness" to R.string.discover_category_health,
        "history" to R.string.discover_category_history,
        "kids & family" to R.string.discover_category_kids,
        "leisure" to R.string.discover_category_leisure,
        "music" to R.string.discover_category_music,
        "news" to R.string.discover_category_news,
        "religion & spirituality" to R.string.discover_category_religion,
        "science" to R.string.discover_category_science,
        "society & culture" to R.string.discover_category_society,
        "sports" to R.string.discover_category_sports,
        "technology" to R.string.discover_category_technology,
        "true crime" to R.string.discover_category_crime,
        "tv & film" to R.string.discover_category_tv,
        "featured" to R.string.discover_featured,
        "trending" to R.string.discover_trending,
        "australia" to R.string.discover_region_australia,
        "austria" to R.string.discover_region_austria,
        "belgium" to R.string.discover_region_belgium,
        "brazil" to R.string.discover_region_brazil,
        "canada" to R.string.discover_region_canada,
        "china" to R.string.discover_region_china,
        "switzerland" to R.string.discover_region_switzerland,
        "germany" to R.string.discover_region_germany,
        "denmark" to R.string.discover_region_denmark,
        "spain" to R.string.discover_region_spain,
        "finland" to R.string.discover_region_finland,
        "france" to R.string.discover_region_france,
        "ireland" to R.string.discover_region_ireland,
        "india" to R.string.discover_region_india,
        "italy" to R.string.discover_region_italy,
        "japan" to R.string.discover_region_japan,
        "south korea" to R.string.discover_region_south_korea,
        "netherlands" to R.string.discover_region_netherlands,
        "new zealand" to R.string.discover_region_new_zealand,
        "norway" to R.string.discover_region_norway,
        "mexico" to R.string.discover_region_mexico,
        "portugal" to R.string.discover_region_portugal,
        "poland" to R.string.discover_region_poland,
        "russia" to R.string.discover_region_russia,
        "south africa" to R.string.discover_region_south_africa,
        "sweden" to R.string.discover_region_sweden,
        "united kingdom" to R.string.discover_region_united_kingdom,
        "united states" to R.string.discover_region_united_states,
        "singapore" to R.string.discover_region_singapore,
        "philippines" to R.string.discover_region_philippines,
        "hong kong" to R.string.discover_region_hong_kong,
        "saudi arabia" to R.string.discover_region_saudi_arabia,
        "turkey" to R.string.discover_region_turkey,
        "israel" to R.string.discover_region_israel,
        "czechia" to R.string.discover_region_czechia,
        "taiwan" to R.string.discover_region_taiwan,
        "ukraine" to R.string.discover_region_ukraine,
        "worldwide" to R.string.discover_region_worldwide,
        "monthly" to R.string.plus_monthly,
        "yearly" to R.string.plus_yearly,
        "top" to R.string.discover_top,
        "popular" to R.string.discover_popular,
        "popular in [regionname]" to R.string.discover_popular_in
    )

    private val serverMessageIdToStringId = mapOf(
        "login_password_incorrect" to R.string.server_login_password_incorrect,
        "login_permission_denied_not_admin" to R.string.server_login_permission_denied_not_admin,
        "login_account_locked" to R.string.server_login_account_locked,
        "login_email_blank" to R.string.server_login_email_blank,
        "login_password_blank" to R.string.server_login_password_blank,
        "login_email_not_found" to R.string.server_login_email_not_found,
        "login_thanks_signing_up" to R.string.server_login_thanks_signing_up,
        "login_unable_to_create_account" to R.string.server_login_unable_to_create_account,
        "login_password_invalid" to R.string.server_login_password_invalid,
        "login_email_invalid" to R.string.server_login_email_invalid,
        "login_email_taken" to R.string.server_login_email_taken,
        "login_user_register_failed" to R.string.server_login_user_register_failed,
        "files_invalid_content_type" to R.string.server_files_invalid_content_type,
        "files_invalid_user" to R.string.server_files_invalid_user,
        "files_file_too_large" to R.string.server_files_file_too_large,
        "files_storage_limit_exceeded" to R.string.server_files_storage_limit_exceeded,
        "files_title_required" to R.string.server_files_title_required,
        "files_uuid_required" to R.string.server_files_uuid_required,
        "files_upload_failed_generic" to R.string.server_files_upload_failed_generic,
        "promo_already_plus" to R.string.server_promo_already_plus,
        "promo_code_expired_or_invalid" to R.string.server_promo_code_expired_or_invalid,
        "promo_already_redeemed" to R.string.server_promo_already_redeemed
    )

    fun serverMessageIdToMessage(serverMessageId: String?, getResourceString: (Int) -> String?) =
        serverMessageId?.let { serverMessageIdString ->
            serverMessageIdToStringId[serverMessageIdString]?.let { androidId ->
                getResourceString(androidId)
            }
        }

    fun tryToLocalise(text: String, resources: Resources, args: List<String>? = null): String {
        val stringLower = text.lowercase()
        return stringToId[stringLower]?.let { if (args.isNullOrEmpty()) resources.getString(it) else resources.getString(it, *args.toTypedArray()) } ?: text
    }
}

fun String.tryToLocalise(resources: Resources, args: List<String>? = null): String {
    return LocaliseHelper.tryToLocalise(this, resources, args)
}

fun String.tryToLocaliseFilters(resources: Resources): String {
    return LocaliseHelper.filterToId[this]?.let { resources.getString(it) } ?: this
}
