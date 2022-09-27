package au.com.shiftyjelly.pocketcasts.analytics

enum class AnalyticsEvent(val key: String) {

    /* App Lifecycle */
    APPLICATION_INSTALLED("application_installed"),
    APPLICATION_UPDATED("application_updated"),
    APPLICATION_OPENED("application_opened"),
    APPLICATION_CLOSED("application_closed"),

    /* User lifecycle events */
    USER_SIGNED_IN("user_signed_in"),
    USER_SIGNIN_FAILED("user_signin_failed"),
    USER_ACCOUNT_DELETED("user_account_deleted"),
    USER_PASSWORD_UPDATED("user_password_updated"),
    USER_EMAIL_UPDATED("user_email_updated"),
    USER_PASSWORD_RESET("user_password_reset"),
    USER_ACCOUNT_CREATED("user_account_created"),
    USER_ACCOUNT_CREATION_FAILED("user_account_creation_failed"),
    USER_SIGNED_OUT("user_signed_out"),

    /* Plus Upsell */
    PLUS_PROMOTION_SHOWN("plus_promotion_shown"),
    PLUS_PROMOTION_DISMISSED("plus_promotion_dismissed"),
    PLUS_PROMOTION_UPGRADE_BUTTON_TAPPED("plus_promotion_upgrade_button_tapped"),

    /* Setup Account */
    SETUP_ACCOUNT_SHOWN("setup_account_shown"),
    SETUP_ACCOUNT_DISMISSED("setup_account_dismissed"),
    SETUP_ACCOUNT_BUTTON_TAPPED("setup_account_button_tapped"),

    /* Sign in */
    SIGNIN_SHOWN("signin_shown"),
    SIGNIN_DISMISSED("signin_dismissed"),

    /* Select Account Type */
    SELECT_ACCOUNT_TYPE_SHOWN("select_account_type_shown"),
    SELECT_ACCOUNT_TYPE_DISMISSED("select_account_type_dismissed"),
    SELECT_ACCOUNT_TYPE_BUTTON_TAPPED("select_account_type_button_tapped"),

    /* Create Account */
    CREATE_ACCOUNT_SHOWN("create_account_shown"),
    CREATE_ACCOUNT_DISMISSED("create_account_dismissed"),
    CREATE_ACCOUNT_NEXT_BUTTON_TAPPED("create_account_next_button_tapped"),

    /* Terms of Use */
    TERMS_OF_USE_SHOWN("terms_of_use_shown"),
    TERMS_OF_USE_DISMISSED("terms_of_use_dismissed"),
    TERMS_OF_USE_ACCEPTED("terms_of_use_accepted"),
    TERMS_OF_USE_REJECTED("terms_of_use_rejected"),

    /* Select Payment Frequency */
    SELECT_PAYMENT_FREQUENCY_SHOWN("select_payment_frequency_shown"),
    SELECT_PAYMENT_FREQUENCY_DISMISSED("select_payment_frequency_dismissed"),
    SELECT_PAYMENT_FREQUENCY_NEXT_BUTTON_TAPPED("select_payment_frequency_next_button_tapped"),

    /* Confirm Payment */
    CONFIRM_PAYMENT_SHOWN("confirm_payment_shown"),
    CONFIRM_PAYMENT_DISMISSED("confirm_payment_dismissed"),
    CONFIRM_PAYMENT_CONFIRM_BUTTON_TAPPED("confirm_payment_confirm_button_tapped"),

    /* Purchase */
    PURCHASE_SUCCESSFUL("purchase_successful"),
    PURCHASE_CANCELLED("purchase_cancelled"),
    PURCHASE_FAILED("purchase_failed"),

    /* Newsletter Opt In */
    NEWSLETTER_OPT_IN_CHANGED("newsletter_opt_in_changed"),

    /* Forgot Password */
    FORGOT_PASSWORD_SHOWN("forgot_password_shown"),
    FORGOT_PASSWORD_DISMISSED("forgot_password_dismissed"),

    /* Account Updated */
    ACCOUNT_UPDATED_SHOWN("account_updated_shown"),
    ACCOUNT_UPDATED_DISMISSED("account_updated_dismissed"),

    /* Account Details */
    ACCOUNT_DETAILS_SHOW_PRIVACY_POLICY("account_details_show_privacy_policy"),
    ACCOUNT_DETAILS_SHOW_TOS("account_details_show_tos"),

    /* Podcasts List */
    PODCASTS_LIST_SHOWN("podcasts_list_shown"),
    PODCASTS_LIST_FOLDER_BUTTON_TAPPED("podcasts_list_folder_button_tapped"),
    PODCASTS_LIST_PODCAST_TAPPED("podcasts_list_podcast_tapped"),
    PODCASTS_LIST_FOLDER_TAPPED("podcasts_list_folder_tapped"),
    PODCASTS_LIST_OPTIONS_BUTTON_TAPPED("podcasts_list_options_button_tapped"),
    PODCASTS_LIST_REORDERED("podcasts_list_reordered"),
    PODCASTS_LIST_MODAL_OPTION_TAPPED("podcasts_list_modal_option_tapped"),
    PODCASTS_LIST_SORT_ORDER_CHANGED("podcasts_list_sort_order_changed"),
    PODCASTS_LIST_LAYOUT_CHANGED("podcasts_list_layout_changed"),
    PODCASTS_LIST_BADGES_CHANGED("podcasts_list_badges_changed"),

    /* Tab bar items */
    PODCASTS_TAB_OPENED("podcasts_tab_opened"),
    FILTERS_TAB_OPENED("filters_tab_opened"),
    DISCOVER_TAB_OPENED("discover_tab_opened"),
    PROFILE_TAB_OPENED("profile_tab_opened"),

    /* Table Swipe Actions for Podcast episodes */
    EPISODE_SWIPE_ACTION_PERFORMED("episode_swipe_action_performed"),

    /* Profile View */
    PROFILE_SHOWN("profile_shown"),
    PROFILE_ACCOUNT_BUTTON_TAPPED("profile_account_button_tapped"),
    PROFILE_SETTINGS_BUTTON_TAPPED("profile_settings_button_tapped"),
    PROFILE_REFRESH_BUTTON_TAPPED("profile_refresh_button_tapped"),

    /* Stats View */
    STATS_SHOWN("stats_shown"),
    STATS_DISMISSED("stats_dismissed"),

    /* Downloads View */
    DOWNLOADS_SHOWN("downloads_shown"),
    DOWNLOADS_OPTIONS_BUTTON_TAPPED("downloads_options_button_tapped"),
    DOWNLOADS_OPTIONS_MODAL_OPTION_TAPPED("downloads_options_modal_option_tapped"),
    DOWNLOADS_SELECT_ALL_TAPPED("downloads_select_all_tapped"),
    DOWNLOADS_SELECT_ALL_ABOVE_TAPPED("downloads_select_all_above_tapped"),
    DOWNLOADS_SELECT_ALL_BELOW_TAPPED("downloads_select_all_below_tapped"),
    DOWNLOADS_MULTI_SELECT_ENTERED("downloads_multi_select_entered"),
    DOWNLOADS_MULTI_SELECT_EXITED("downloads_multi_select_exited"),

    /* Downloads Clean Up View */
    DOWNLOADS_CLEAN_UP_SHOWN("downloads_clean_up_shown"),
    DOWNLOADS_CLEAN_UP_BUTTON_TAPPED("downloads_clean_up_button_tapped"),
    DOWNLOADS_CLEAN_UP_COMPLETED("downloads_clean_up_completed"),

    /* Listening History */
    LISTENING_HISTORY_SHOWN("listening_history_shown"),
    LISTENING_HISTORY_OPTIONS_BUTTON_TAPPED("listening_history_options_button_tapped"),
    LISTENING_HISTORY_OPTIONS_MODAL_OPTION_TAPPED("listening_history_options_modal_option_tapped"),
    LISTENING_HISTORY_SELECT_ALL_TAPPED("listening_history_select_all_tapped"),
    LISTENING_HISTORY_SELECT_ALL_ABOVE_TAPPED("listening_history_select_all_above_tapped"),
    LISTENING_HISTORY_SELECT_ALL_BELOW_TAPPED("listening_history_select_all_below_tapped"),
    LISTENING_HISTORY_MULTI_SELECT_ENTERED("listening_history_multi_select_entered"),
    LISTENING_HISTORY_MULTI_SELECT_EXITED("listening_history_multi_select_exited"),
    LISTENING_HISTORY_CLEARED("listening_history_cleared"),

    /* Uploaded Files */
    UPLOADED_FILES_SHOWN("uploaded_files_shown"),
    UPLOADED_FILES_OPTIONS_BUTTON_TAPPED("uploaded_files_options_button_tapped"),
    UPLOADED_FILES_OPTIONS_MODAL_OPTION_TAPPED("uploaded_files_options_modal_option_tapped"),
    UPLOADED_FILES_MULTI_SELECT_ENTERED("uploaded_files_multi_select_entered"),
    UPLOADED_FILES_SELECT_ALL_TAPPED("uploaded_files_select_all_tapped"),
    UPLOADED_FILES_SELECT_ALL_ABOVE_TAPPED("uploaded_files_select_all_above_tapped"),
    UPLOADED_FILES_SELECT_ALL_BELOW_TAPPED("uploaded_files_select_all_below_tapped"),
    UPLOADED_FILES_MULTI_SELECT_EXITED("uploaded_files_multi_select_exited"),
    UPLOADED_FILES_SORT_BY_CHANGED("uploaded_files_sort_by_changed"),
    UPLOADED_FILES_ADD_FILE_TAPPED("uploaded_files_add_file_tapped"),

    /* User File Details View */
    USER_FILE_DETAIL_SHOWN("user_file_detail_shown"),
    USER_FILE_DETAIL_DISMISSED("user_file_detail_dismissed"),
    USER_FILE_DETAIL_OPTION_TAPPED("user_file_detail_option_tapped"),
    USER_FILE_PLAY_PAUSE_BUTTON_TAPPED("user_file_play_pause_button_tapped"),
    USER_FILE_DELETED("user_file_deleted"),

    /* Starred */
    STARRED_SHOWN("starred_shown"),
    STARRED_MULTI_SELECT_ENTERED("starred_multi_select_entered"),
    STARRED_MULTI_SELECT_EXITED("starred_multi_select_exited"),
    STARRED_SELECT_ALL_TAPPED("starred_select_all_tapped"),
    STARRED_SELECT_ALL_ABOVE_TAPPED("starred_select_all_above_tapped"),
    STARRED_SELECT_ALL_BELOW_TAPPED("starred_select_all_below_tapped"),

    /* Folder */
    FOLDER_CREATE_SHOWN("folder_create_shown"),
    FOLDER_PODCAST_PICKER_SEARCH_PERFORMED("folder_podcast_picker_search_performed"),
    FOLDER_PODCAST_PICKER_SEARCH_CLEARED("folder_podcast_picker_search_cleared"),
    FOLDER_PODCAST_PICKER_FILTER_CHANGED("folder_podcast_picker_filter_changed"),
    FOLDER_CREATE_NAME_SHOWN("folder_create_name_shown"),
    FOLDER_CREATE_COLOR_SHOWN("folder_create_color_shown"),
    FOLDER_SAVED("folder_saved"),
    FOLDER_SHOWN("folder_shown"),
    FOLDER_OPTIONS_BUTTON_TAPPED("folder_options_button_tapped"),
    FOLDER_ADD_PODCASTS_BUTTON_TAPPED("folder_add_podcasts_button_tapped"),
    FOLDER_OPTIONS_MODAL_OPTION_TAPPED("folder_options_modal_option_tapped"),
    FOLDER_SORT_BY_CHANGED("folder_sort_by_changed"),
    FOLDER_EDIT_SHOWN("folder_edit_shown"),
    FOLDER_EDIT_DISMISSED("folder_edit_dismissed"),
    FOLDER_EDIT_DELETE_BUTTON_TAPPED("folder_edit_delete_button_tapped"),
    FOLDER_DELETED("folder_deleted"),
    FOLDER_CHOOSE_PODCASTS_SHOWN("folder_choose_podcasts_shown"),
    FOLDER_CHOOSE_PODCASTS_DISMISSED("folder_choose_podcasts_dismissed"),
    FOLDER_CHOOSE_SHOWN("folder_choose_shown"),
    FOLDER_CHOOSE_FOLDER_TAPPED("folder_choose_folder_tapped"),
    FOLDER_PODCAST_MODAL_OPTION_TAPPED("folder_podcast_modal_option_tapped"),

    /* Podcast screen */
    PODCAST_SCREEN_SHOWN("podcast_screen_shown"),
    PODCAST_SCREEN_FOLDER_TAPPED("podcast_screen_folder_tapped"),
    PODCAST_SCREEN_SETTINGS_TAPPED("podcast_screen_settings_tapped"),
    PODCAST_SCREEN_NOTIFICATIONS_TAPPED("podcast_screen_notifications_tapped"),
    PODCAST_SCREEN_SUBSCRIBE_TAPPED("podcast_screen_subscribe_tapped"),
    PODCAST_SCREEN_UNSUBSCRIBE_TAPPED("podcast_screen_unsubscribe_tapped"),
    PODCAST_SCREEN_SEARCH_PERFORMED("podcast_screen_search_performed"),
    PODCAST_SCREEN_SEARCH_CLEARED("podcast_screen_search_cleared"),
    PODCAST_SCREEN_OPTIONS_TAPPED("podcast_screen_options_tapped"),
    PODCAST_SCREEN_TOGGLE_ARCHIVED("podcast_screen_toggle_archived"),
    PODCAST_SCREEN_TOGGLE_SUMMARY("podcast_screen_toggle_summary"),
    PODCAST_SCREEN_SHARE_TAPPED("podcast_screen_share_tapped"),

    /* Playback */
    PLAY("play"),
    PAUSE("pause"),
    SKIP_BACK("skip_back"),
    SKIP_FORWARD("skip_forward"),

    /* Discover */
    DISCOVER_CATEGORY_SHOWN("discover_category_shown"),
    DISCOVER_FEATURED_PODCAST_TAPPED("discover_featured_podcast_tapped"),
    DISCOVER_FEATURED_PODCAST_SUBSCRIBED("discover_featured_podcast_subscribed"),
    DISCOVER_LIST_SHOW_ALL_TAPPED("discover_list_show_all_tapped"),
    DISCOVER_LIST_IMPRESSION("discover_list_impression"),
    DISCOVER_LIST_EPISODE_TAPPED("discover_list_episode_tapped"),
    DISCOVER_LIST_PODCAST_TAPPED("discover_list_podcast_tapped"),
    DISCOVER_LIST_PODCAST_SUBSCRIBED("discover_list_podcast_subscribed"),
}
