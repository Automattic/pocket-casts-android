package au.com.shiftyjelly.pocketcasts.analytics

enum class AnalyticsEvent(val key: String) {

    /* App Lifecycle */
    APPLICATION_INSTALLED("application_installed"),
    APPLICATION_UPDATED("application_updated"),
    APPLICATION_OPENED("application_opened"),
    APPLICATION_CLOSED("application_closed"),

    /* Bookmarks */
    BOOKMARK_CREATED("bookmark_created"),
    BOOKMARK_UPDATE_TITLE("bookmark_update_title"),

    /* User lifecycle events */
    USER_SIGNED_IN("user_signed_in"),
    USER_SIGNED_IN_WATCH_FROM_PHONE("user_signed_in_watch_from_phone"),
    USER_SIGNIN_FAILED("user_signin_failed"),
    USER_SIGNIN_WATCH_FROM_PHONE_FAILED("user_signin_watch_from_phone_failed"),
    USER_ACCOUNT_DELETED("user_account_deleted"),
    USER_PASSWORD_UPDATED("user_password_updated"),
    USER_EMAIL_UPDATED("user_email_updated"),
    USER_PASSWORD_RESET("user_password_reset"),
    USER_ACCOUNT_CREATED("user_account_created"),
    USER_ACCOUNT_CREATION_FAILED("user_account_creation_failed"),
    USER_SIGNED_OUT("user_signed_out"),

    /* Signed out alert */
    SIGNED_OUT_ALERT_SHOWN("signed_out_alert_shown"),

    /* Plus Upsell */
    PLUS_PROMOTION_SHOWN("plus_promotion_shown"),
    PLUS_PROMOTION_DISMISSED("plus_promotion_dismissed"),
    PLUS_PROMOTION_UPGRADE_BUTTON_TAPPED("plus_promotion_upgrade_button_tapped"),
    PLUS_PROMOTION_NOT_NOW_BUTTON_TAPPED("plus_promotion_not_now_button_tapped"),

    /* Pull to refresh */
    PULLED_TO_REFRESH("pulled_to_refresh"),

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
    ACCOUNT_DETAILS_CANCEL_TAPPED("account_details_cancel_tapped"),
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
    PODCASTS_SCREEN_SORT_ORDER_CHANGED("podcasts_screen_sort_order_changed"),
    PODCASTS_SCREEN_EPISODE_GROUPING_CHANGED("podcasts_screen_episode_grouping_changed"),
    PODCASTS_SCREEN_TAB_TAPPED("podcasts_screen_tab_tapped"),

    /* Podcast Settings */
    PODCAST_SETTINGS_FEED_ERROR_TAPPED("podcast_settings_feed_error_tapped"),
    PODCAST_SETTINGS_FEED_ERROR_UPDATE_TAPPED("podcast_settings_feed_error_update_tapped"),
    PODCAST_SETTINGS_FEED_ERROR_FIX_SUCCEEDED("podcast_settings_feed_error_fix_succeeded"),
    PODCAST_SETTINGS_FEED_ERROR_FIX_FAILED("podcast_settings_feed_error_fix_failed"),
    PODCAST_SETTINGS_AUTO_DOWNLOAD_TOGGLED("podcast_settings_auto_download_toggled"),
    PODCAST_SETTINGS_NOTIFICATIONS_TOGGLED("podcast_settings_notifications_toggled"),
    PODCAST_SETTINGS_AUTO_ADD_UP_NEXT_TOGGLED("podcast_settings_auto_add_up_next_toggled"),
    PODCAST_SETTINGS_AUTO_ADD_UP_NEXT_POSITION_OPTION_CHANGED("podcast_settings_auto_add_up_next_position_option_changed"),
    PODCAST_SETTINGS_CUSTOM_PLAYBACK_EFFECTS_TOGGLED("podcast_settings_custom_playback_effects_toggled"),
    PODCAST_SETTINGS_SKIP_FIRST_CHANGED("podcast_settings_skip_first_changed"),
    PODCAST_SETTINGS_SKIP_LAST_CHANGED("podcast_settings_skip_last_changed"),
    PODCAST_SETTINGS_AUTO_ARCHIVE_TOGGLED("podcast_settings_auto_archive_toggled"),
    PODCAST_SETTINGS_AUTO_ARCHIVE_PLAYED_CHANGED("podcast_settings_auto_archive_played_changed"),
    PODCAST_SETTINGS_AUTO_ARCHIVE_INACTIVE_CHANGED("podcast_settings_auto_archive_inactive_changed"),
    PODCAST_SETTINGS_AUTO_ARCHIVE_EPISODE_LIMIT_CHANGED("podcast_settings_auto_archive_episode_limit_changed"),

    /* Podcast subscribed/ Unsubscribed */
    PODCAST_SUBSCRIBED("podcast_subscribed"),
    PODCAST_UNSUBSCRIBED("podcast_unsubscribed"),

    /* Playback */
    PLAYBACK_PLAY("playback_play"),
    PLAYBACK_PAUSE("playback_pause"),
    PLAYBACK_SKIP_BACK("playback_skip_back"),
    PLAYBACK_SKIP_FORWARD("playback_skip_forward"),
    PLAYBACK_STOP("playback_stop"),
    PLAYBACK_SEEK("playback_seek"),
    PLAYBACK_EPISODE_AUTOPLAYED("playback_episode_autoplayed"),

    /* Privacy */
    PRIVACY_SETTINGS_SHOWN("privacy_settings_shown"),
    ANALYTICS_OPT_IN("analytics_opt_in"),
    ANALYTICS_OPT_OUT("analytics_opt_out"),
    SETTINGS_SHOW_PRIVACY_POLICY("settings_show_privacy_policy"),

    /* Filters */
    FILTER_AUTO_DOWNLOAD_LIMIT_UPDATED("filter_auto_download_limit_updated"),
    FILTER_AUTO_DOWNLOAD_UPDATED("filter_auto_download_updated"),
    FILTER_CREATED("filter_created"),
    FILTER_DELETED("filter_deleted"),
    FILTER_EDIT_DISMISSED("filter_edit_dismissed"),
    FILTER_LIST_REORDERED("filter_list_reordered"),
    FILTER_LIST_SHOWN("filter_list_shown"),
    FILTER_MULTI_SELECT_ENTERED("filter_multi_select_entered"),
    FILTER_MULTI_SELECT_EXITED("filter_multi_select_exited"),
    FILTER_SELECT_ALL_BUTTON_TAPPED("filter_select_all_button_tapped"),
    FILTER_SELECT_ALL_ABOVE("filter_select_all_above"),
    FILTER_SELECT_ALL_BELOW("filter_select_all_below"),
    FILTER_DESELECT_ALL_BUTTON_TAPPED("filter_deselect_all_button_tapped"),
    FILTER_DESELECT_ALL_BELOW("filter_deselect_all_below"),
    FILTER_DESELECT_ALL_ABOVE("filter_deselect_all_above"),
    FILTER_SHOWN("filter_shown"),
    FILTER_SORT_BY_CHANGED("filter_sort_by_changed"),
    FILTER_UPDATED("filter_updated"),

    /* Discover */
    DISCOVER_SHOWN("discover_shown"),
    DISCOVER_CATEGORY_SHOWN("discover_category_shown"),
    DISCOVER_FEATURED_PODCAST_TAPPED("discover_featured_podcast_tapped"),
    DISCOVER_FEATURED_PODCAST_SUBSCRIBED("discover_featured_podcast_subscribed"),
    DISCOVER_SHOW_ALL_TAPPED("discover_show_all_tapped"),
    DISCOVER_LIST_SHOW_ALL_TAPPED("discover_list_show_all_tapped"),
    DISCOVER_LIST_IMPRESSION("discover_list_impression"),
    DISCOVER_LIST_EPISODE_TAPPED("discover_list_episode_tapped"),
    DISCOVER_LIST_EPISODE_PLAY("discover_list_episode_play"),
    DISCOVER_LIST_PODCAST_TAPPED("discover_list_podcast_tapped"),
    DISCOVER_LIST_PODCAST_SUBSCRIBED("discover_list_podcast_subscribed"),
    DISCOVER_FEATURED_PAGE_CHANGED("discover_featured_page_changed"),
    DISCOVER_SMALL_LIST_PAGE_CHANGED("discover_small_list_page_changed"),
    DISCOVER_REGION_CHANGED("discover_region_changed"),
    DISCOVER_COLLECTION_LINK_TAPPED("discover_collection_link_tapped"),

    /* Mini Player */
    MINI_PLAYER_LONG_PRESS_MENU_SHOWN("mini_player_long_press_menu_shown"),
    MINI_PLAYER_LONG_PRESS_MENU_OPTION_TAPPED("mini_player_long_press_menu_option_tapped"),
    MINI_PLAYER_LONG_PRESS_MENU_DISMISSED("mini_player_long_press_menu_dismissed"),

    /* Up Next */
    UP_NEXT_SHOWN("up_next_shown"),
    UP_NEXT_QUEUE_CLEARED("up_next_queue_cleared"),
    UP_NEXT_NOW_PLAYING_TAPPED("up_next_now_playing_tapped"),
    UP_NEXT_QUEUE_EPISODE_TAPPED("up_next_queue_episode_tapped"),
    UP_NEXT_QUEUE_EPISODE_LONG_PRESSED("up_next_queue_episode_long_pressed"),
    UP_NEXT_MULTI_SELECT_ENTERED("up_next_multi_select_entered"),
    UP_NEXT_SELECT_ALL_TAPPED("up_next_select_all_tapped"),
    UP_NEXT_MULTI_SELECT_EXITED("up_next_multi_select_exited"),
    UP_NEXT_QUEUE_REORDERED("up_next_queue_reordered"),
    UP_NEXT_DISMISSED("up_next_dismissed"),

    /* Player */
    PLAYER_SHOWN("player_shown"),
    PLAYER_DISMISSED("player_dismissed"),
    PLAYER_TAB_SELECTED("player_tab_selected"),
    PLAYER_SHOW_NOTES_LINK_TAPPED("player_show_notes_link_tapped"),
    PLAYER_CHAPTER_SELECTED("player_chapter_selected"),
    PLAYER_PREVIOUS_CHAPTER_TAPPED("player_previous_chapter_tapped"),
    PLAYER_NEXT_CHAPTER_TAPPED("player_next_chapter_tapped"),

    /* Player - Sleep Timer */
    PLAYER_SLEEP_TIMER_ENABLED("player_sleep_timer_enabled"),
    PLAYER_SLEEP_TIMER_EXTENDED("player_sleep_timer_extended"),
    PLAYER_SLEEP_TIMER_CANCELLED("player_sleep_timer_cancelled"),

    /* Player - Playback effects */
    PLAYBACK_EFFECT_SPEED_CHANGED("playback_effect_speed_changed"),
    PLAYBACK_EFFECT_TRIM_SILENCE_TOGGLED("playback_effect_trim_silence_toggled"),
    PLAYBACK_EFFECT_TRIM_SILENCE_AMOUNT_CHANGED("playback_effect_trim_silence_amount_changed"),
    PLAYBACK_EFFECT_VOLUME_BOOST_TOGGLED("playback_effect_volume_boost_toggled"),

    /* Player - Shelf */
    PLAYER_SHELF_ACTION_TAPPED("player_shelf_action_tapped"),
    PLAYER_SHELF_OVERFLOW_MENU_SHOWN("player_shelf_overflow_menu_shown"),
    PLAYER_SHELF_OVERFLOW_MENU_REARRANGE_STARTED("player_shelf_overflow_menu_rearrange_started"),
    PLAYER_SHELF_OVERFLOW_MENU_REARRANGE_ACTION_MOVED("player_shelf_overflow_menu_rearrange_action_moved"),
    PLAYER_SHELF_OVERFLOW_MENU_REARRANGE_FINISHED("player_shelf_overflow_menu_rearrange_finished"),

    /* Multi Select View */
    MULTI_SELECT_VIEW_OVERFLOW_MENU_SHOWN("multi_select_view_overflow_menu_shown"),
    MULTI_SELECT_VIEW_OVERFLOW_MENU_REARRANGE_STARTED("multi_select_view_overflow_menu_rearrange_started"),
    MULTI_SELECT_VIEW_OVERFLOW_MENU_REARRANGE_ACTION_MOVED("multi_select_view_overflow_menu_rearrange_action_moved"),
    MULTI_SELECT_VIEW_OVERFLOW_MENU_REARRANGE_FINISHED("multi_select_view_overflow_menu_rearrange_finished"),

    /* Episode */
    EPISODE_DOWNLOAD_DELETED("episode_download_deleted"),
    EPISODE_BULK_DOWNLOAD_DELETED("episode_bulk_download_deleted"),
    EPISODE_DELETED_FROM_CLOUD("episode_deleted_from_cloud"),
    EPISODE_DOWNLOAD_QUEUED("episode_download_queued"),
    EPISODE_BULK_DOWNLOAD_QUEUED("episode_bulk_download_queued"),
    EPISODE_DOWNLOAD_FINISHED("episode_download_finished"),
    EPISODE_DOWNLOAD_FAILED("episode_download_failed"),
    EPISODE_DOWNLOAD_CANCELLED("episode_download_cancelled"),
    EPISODE_UPLOAD_QUEUED("episode_upload_queued"),
    EPISODE_UPLOAD_CANCELLED("episode_upload_cancelled"),
    EPISODE_UPLOAD_FINISHED("episode_upload_finished"),
    EPISODE_UPLOAD_FAILED("episode_upload_failed"),
    EPISODE_STARRED("episode_starred"),
    EPISODE_BULK_STARRED("episode_bulk_starred"),
    EPISODE_UNSTARRED("episode_unstarred"),
    EPISODE_BULK_UNSTARRED("episode_bulk_unstarred"),
    EPISODE_ARCHIVED("episode_archived"),
    EPISODE_BULK_ARCHIVED("episode_bulk_archived"),
    EPISODE_UNARCHIVED("episode_unarchived"),
    EPISODE_BULK_UNARCHIVED("episode_bulk_unarchived"),
    EPISODE_MARKED_AS_PLAYED("episode_marked_as_played"),
    EPISODE_BULK_MARKED_AS_PLAYED("episode_bulk_marked_as_played"),
    EPISODE_MARKED_AS_UNPLAYED("episode_marked_as_unplayed"),
    EPISODE_BULK_MARKED_AS_UNPLAYED("episode_bulk_marked_as_unplayed"),
    EPISODE_ADDED_TO_UP_NEXT("episode_added_to_up_next"),
    EPISODE_BULK_ADD_TO_UP_NEXT("episode_bulk_add_to_up_next"),
    EPISODE_REMOVED_FROM_UP_NEXT("episode_removed_from_up_next"),
    PODCAST_SHARED("podcast_shared"),

    /* Episode Details */
    EPISODE_DETAIL_SHOWN("episode_detail_shown"),
    EPISODE_DETAIL_DISMISSED("episode_detail_dismissed"),
    EPISODE_DETAIL_PODCAST_NAME_TAPPED("episode_detail_podcast_name_tapped"),
    EPISODE_DETAIL_SHOW_NOTES_LINK_TAPPED("episode_detail_show_notes_link_tapped"),

    /* Recommendations */
    RECOMMENDATIONS_SHOWN("recommendations_shown"),
    RECOMMENDATIONS_DISMISSED("recommendations_dismissed"),
    RECOMMENDATIONS_SEARCH_TAPPED("recommendations_search_tapped"),
    RECOMMENDATIONS_IMPORT_TAPPED("recommendations_import_tapped"),
    RECOMMENDATIONS_MORE_TAPPED("recommendations_more_tapped"),
    RECOMMENDATIONS_CONTINUE_TAPPED("recommendations_continue_tapped"),

    /* Podcast List Share */
    SHARE_PODCASTS_SHOWN("share_podcasts_shown"),
    SHARE_PODCASTS_PODCASTS_SELECTED("share_podcasts_podcasts_selected"),
    SHARE_PODCASTS_LIST_PUBLISH_STARTED("share_podcasts_list_publish_started"),
    SHARE_PODCASTS_LIST_PUBLISH_SUCCEEDED("share_podcasts_list_publish_succeeded"),
    SHARE_PODCASTS_LIST_PUBLISH_FAILED("share_podcasts_list_publish_failed"),

    /* Incoming Share List */
    INCOMING_SHARE_LIST_SHOWN("incoming_share_list_shown"),
    INCOMING_SHARE_LIST_SUBSCRIBED_ALL("incoming_share_list_subscribed_all"),

    /* End of Year */
    END_OF_YEAR_MODAL_SHOWN("end_of_year_modal_shown"),
    END_OF_YEAR_STORIES_SHOWN("end_of_year_stories_shown"),
    END_OF_YEAR_STORIES_DISMISSED("end_of_year_stories_dismissed"),
    END_OF_YEAR_STORIES_FAILED_TO_LOAD("end_of_year_stories_failed_to_load"),
    END_OF_YEAR_STORY_REPLAY_BUTTON_TAPPED("end_of_year_story_replay_button_tapped"),
    END_OF_YEAR_STORY_SHOWN("end_of_year_story_shown"),
    END_OF_YEAR_STORY_SHARE("end_of_year_story_share"),
    END_OF_YEAR_STORY_SHARED("end_of_year_story_shared"),
    END_OF_YEAR_STORY_RETRY_BUTTON_TAPPED("end_of_year_story_retry_button_tapped"),
    END_OF_YEAR_PROFILE_CARD_TAPPED("end_of_year_profile_card_tapped"),

    /* Welcome */
    WELCOME_SHOWN("welcome_shown"),
    WELCOME_IMPORT_TAPPED("welcome_import_tapped"),
    WELCOME_DISCOVER_TAPPED("welcome_discover_tapped"),
    WELCOME_DISMISSED("welcome_dismissed"),

    /* Import */
    ONBOARDING_IMPORT_SHOWN("onboarding_import_shown"),
    ONBOARDING_IMPORT_APP_SELECTED("onboarding_import_app_selected"),
    ONBOARDING_IMPORT_OPEN_APP_SELECTED("onboarding_import_open_app_selected"),
    ONBOARDING_IMPORT_DISMISSED("onboarding_import_dismissed"),

    /* Cancel Subscription Confirmation */
    CANCEL_CONFIRMATION_VIEW_SHOWN("cancel_confirmation_view_shown"),
    CANCEL_CONFIRMATION_VIEW_DISMISSED("cancel_confirmation_view_dismissed"),
    CANCEL_CONFIRMATION_STAY_BUTTON_TAPPED("cancel_confirmation_stay_button_tapped"),
    CANCEL_CONFIRMATION_CANCEL_BUTTON_TAPPED("cancel_confirmation_cancel_button_tapped"),

    /* Settings - About */
    SETTINGS_ABOUT_SHOWN("settings_about_shown"),

    /* Settings - Appearance */
    SETTINGS_APPEARANCE_SHOWN("settings_appearance_shown"),
    SETTINGS_APPEARANCE_FOLLOW_SYSTEM_THEME_TOGGLED("settings_appearance_follow_system_theme_toggled"),
    SETTINGS_APPEARANCE_THEME_CHANGED("settings_appearance_theme_changed"),
    SETTINGS_APPEARANCE_APP_ICON_CHANGED("settings_appearance_app_icon_changed"),
    SETTINGS_APPEARANCE_REFRESH_ALL_ARTWORK_TAPPED("settings_appearance_refresh_all_artwork_tapped"),
    SETTINGS_APPEARANCE_USE_EMBEDDED_ARTWORK_TOGGLED("settings_appearance_use_embedded_artwork_toggled"),
    SETTINGS_APPEARANCE_SHOW_ARTWORK_ON_LOCK_SCREEN_TOGGLED("settings_appearance_show_artwork_on_lock_screen_toggled"),

    /* Settings - Auto add */
    SETTINGS_AUTO_ADD_UP_NEXT_SHOWN("settings_auto_add_up_next_shown"),
    SETTINGS_AUTO_ADD_UP_NEXT_AUTO_ADD_LIMIT_CHANGED("settings_auto_add_up_next_auto_add_limit_changed"),
    SETTINGS_AUTO_ADD_UP_NEXT_LIMIT_REACHED_CHANGED("settings_auto_add_up_next_limit_reached_changed"),
    SETTINGS_AUTO_ADD_UP_NEXT_PODCASTS_CHANGED("settings_auto_add_up_next_podcasts_changed"),
    SETTINGS_AUTO_ADD_UP_NEXT_PODCAST_POSITION_OPTION_CHANGED("settings_auto_add_up_next_podcast_position_option_changed"),

    /* Settings - Auto archive */
    SETTINGS_AUTO_ARCHIVE_SHOWN("settings_auto_archive_shown"),
    SETTINGS_AUTO_ARCHIVE_PLAYED_CHANGED("settings_auto_archive_played_changed"),
    SETTINGS_AUTO_ARCHIVE_INACTIVE_CHANGED("settings_auto_archive_inactive_changed"),
    SETTINGS_AUTO_ARCHIVE_INCLUDE_STARRED_TOGGLED("settings_auto_archive_include_starred_toggled"),

    /* Settings - Auto download */
    SETTINGS_AUTO_DOWNLOAD_SHOWN("settings_auto_download_shown"),
    SETTINGS_AUTO_DOWNLOAD_UP_NEXT_TOGGLED("settings_auto_download_up_next_toggled"),
    SETTINGS_AUTO_DOWNLOAD_NEW_EPISODES_TOGGLED("settings_auto_download_new_episodes_toggled"),
    SETTINGS_AUTO_DOWNLOAD_PODCASTS_CHANGED("settings_auto_download_podcasts_changed"),
    SETTINGS_AUTO_DOWNLOAD_FILTERS_CHANGED("settings_auto_download_filters_changed"),
    SETTINGS_AUTO_DOWNLOAD_ONLY_ON_WIFI_TOGGLED("settings_auto_download_only_on_wifi_toggled"),
    SETTINGS_AUTO_DOWNLOAD_ONLY_WHEN_CHARGING_TOGGLED("settings_auto_download_only_when_charging_toggled"),
    SETTINGS_AUTO_DOWNLOAD_STOP_ALL_DOWNLOADS("settings_auto_download_stop_all_downloads"),
    SETTINGS_AUTO_DOWNLOAD_CLEAR_DOWNLOAD_ERRORS("settings_auto_download_clear_download_errors"),

    /* Settings - Files */
    SETTINGS_FILES_SHOWN("settings_files_shown"),
    SETTINGS_FILES_AUTO_ADD_UP_NEXT_TOGGLED("settings_files_auto_add_up_next_toggled"),
    SETTINGS_FILES_DELETE_LOCAL_FILE_AFTER_PLAYING_TOGGLED("settings_files_delete_local_file_after_playing_toggled"),
    SETTINGS_FILES_DELETE_CLOUD_FILE_AFTER_PLAYING_TOGGLED("settings_files_delete_cloud_file_after_playing_toggled"),
    SETTINGS_FILES_AUTO_UPLOAD_TO_CLOUD_TOGGLED("settings_files_auto_upload_to_cloud_toggled"),
    SETTINGS_FILES_AUTO_DOWNLOAD_FROM_CLOUD_TOGGLED("settings_files_auto_download_from_cloud_toggled"),
    SETTINGS_FILES_ONLY_ON_WIFI_TOGGLED("settings_files_only_on_wifi_toggled"),

    /* Settings - General */
    SETTINGS_GENERAL_SHOWN("settings_general_shown"),
    SETTINGS_GENERAL_ROW_ACTION_CHANGED("settings_general_row_action_changed"),
    SETTINGS_GENERAL_EPISODE_GROUPING_CHANGED("settings_general_episode_grouping_changed"),
    SETTINGS_GENERAL_ARCHIVED_EPISODES_CHANGED("settings_general_archived_episodes_changed"),
    SETTINGS_GENERAL_UP_NEXT_SWIPE_CHANGED("settings_general_up_next_swipe_changed"),
    SETTINGS_GENERAL_SKIP_FORWARD_CHANGED("settings_general_skip_forward_changed"),
    SETTINGS_GENERAL_SKIP_BACK_CHANGED("settings_general_skip_back_changed"),
    SETTINGS_GENERAL_KEEP_SCREEN_AWAKE_TOGGLED("settings_general_keep_screen_awake_toggled"),
    SETTINGS_GENERAL_OPEN_PLAYER_AUTOMATICALLY_TOGGLED("settings_general_open_player_automatically_toggled"),
    SETTINGS_GENERAL_INTELLIGENT_PLAYBACK_TOGGLED("settings_general_intelligent_playback_toggled"),
    SETTINGS_GENERAL_PLAY_UP_NEXT_ON_TAP_TOGGLED("settings_general_play_up_next_on_tap_toggled"),
    SETTINGS_GENERAL_MEDIA_NOTIFICATION_CONTROLS_SHOWN("settings_general_media_notification_controls_shown"),
    SETTINGS_GENERAL_MEDIA_NOTIFICATION_CONTROLS_SHOW_CUSTOM_TOGGLED("settings_general_media_notification_controls_show_custom_toggled"),
    SETTINGS_GENERAL_MEDIA_NOTIFICATION_CONTROLS_ORDER_CHANGED("settings_general_media_notification_controls_order_changed"),
    SETTINGS_GENERAL_AUTOPLAY_TOGGLED("settings_general_autoplay_toggled"),

    /* Settings - Help */
    SETTINGS_HELP_SHOWN("settings_help_shown"),
    SETTINGS_LEAVE_FEEDBACK("settings_leave_feedback"),
    SETTINGS_GET_SUPPORT("settings_get_support"),

    /* Settings - Import - Export */
    SETTINGS_IMPORT_SHOWN("settings_import_shown"),
    SETTINGS_IMPORT_SELECT_FILE("settings_import_select_file"),
    SETTINGS_IMPORT_BY_URL("settings_import_by_url"),
    SETTINGS_IMPORT_EXPORT_EMAIL_TAPPED("settings_import_export_email_tapped"),
    SETTINGS_IMPORT_EXPORT_FILE_TAPPED("settings_import_export_file_tapped"),
    SETTINGS_IMPORT_EXPORT_STARTED("settings_import_export_started"),
    SETTINGS_IMPORT_EXPORT_FINISHED("settings_import_export_finished"),
    SETTINGS_IMPORT_EXPORT_FAILED("settings_import_export_failed"),
    OPML_IMPORT_STARTED("opml_import_started"),
    OPML_IMPORT_FAILED("opml_import_failed"),
    OPML_IMPORT_FINISHED("opml_import_finished"),

    /* Settings - Notifications */
    SETTINGS_NOTIFICATIONS_SHOWN("settings_notifications_shown"),
    SETTINGS_NOTIFICATIONS_NEW_EPISODES_TOGGLED("settings_notifications_new_episodes_toggled"),
    SETTINGS_NOTIFICATIONS_PODCASTS_CHANGED("settings_notifications_podcasts_changed"),
    SETTINGS_NOTIFICATIONS_ACTIONS_CHANGED("settings_notifications_actions_changed"),
    SETTINGS_NOTIFICATIONS_ADVANCED_SETTINGS_TAPPED("settings_notifications_advanced_settings_tapped"),
    SETTINGS_NOTIFICATIONS_PLAY_OVER_NOTIFICATIONS_TOGGLED("settings_notifications_play_over_notifications_toggled"),
    SETTINGS_NOTIFICATIONS_HIDE_PLAYBACK_NOTIFICATION_ON_PAUSE("settings_notifications_hide_playback_notification_on_pause"),
    SETTINGS_NOTIFICATIONS_SOUND_CHANGED("settings_notifications_sound_changed"),
    SETTINGS_NOTIFICATIONS_VIBRATION_CHANGED("settings_notifications_vibration_changed"),

    /* Settings - Plus */
    SETTINGS_PLUS_SHOWN("settings_plus_shown"),
    SETTINGS_PLUS_UPGRADE_BUTTON_TAPPED("settings_plus_upgrade_button_tapped"),
    SETTINGS_PLUS_LEARN_MORE_TAPPED("settings_plus_learn_more_tapped"),

    /* Settings - Storage & Data Use */
    SETTINGS_STORAGE_SHOWN("settings_storage_shown"),
    SETTINGS_STORAGE_CLEAR_DOWNLOAD_CACHE("settings_storage_clear_download_cache"),
    SETTINGS_STORAGE_LOCATION("settings_storage_location"),
    SETTINGS_STORAGE_SET_FOLDER_LOCATION("settings_storage_set_folder_location"),
    SETTINGS_STORAGE_BACKGROUND_REFRESH_TOGGLED("settings_storage_background_refresh_toggled"),
    SETTINGS_STORAGE_WARN_BEFORE_USING_DATA_TOGGLED("settings_storage_warn_before_using_data_toggled"),

    /* Settings - Advanced */
    SETTINGS_ADVANCED_SHOWN("settings_advanced_shown"),
    SETTINGS_ADVANCED_SYNC_ON_METERED("settings_advanced_sync_on_metered"),

    /* Search */
    SEARCH_SHOWN("search_shown"),
    SEARCH_DISMISSED("search_dismissed"),
    SEARCH_PERFORMED("search_performed"),
    SEARCH_FAILED("search_failed"),
    SEARCH_RESULT_TAPPED("search_result_tapped"),

    /* Search - Full List */
    SEARCH_LIST_SHOWN("search_list_shown"),

    /* Search History */
    SEARCH_HISTORY_CLEARED("search_history_cleared"),
    SEARCH_HISTORY_ITEM_TAPPED("search_history_item_tapped"),
    SEARCH_HISTORY_ITEM_DELETE_BUTTON_TAPPED("search_history_item_delete_button_tapped"),

    /* Chromecast */
    CHROMECAST_VIEW_SHOWN("chromecast_view_shown"),
    CHROMECAST_STARTED_CASTING("chromecast_started_casting"),
    CHROMECAST_STOPPED_CASTING("chromecast_stopped_casting"),

    /* Ratings */
    RATING_STARS_TAPPED("rating_stars_tapped"),

    /* Wear Main List Screen */
    WEAR_MAIN_LIST_SHOWN("wear_main_list_shown"),
    WEAR_MAIN_LIST_NOW_PLAYING_TAPPED("wear_main_list_now_playing_tapped"),
    WEAR_MAIN_LIST_PODCASTS_TAPPED("wear_main_list_podcasts_tapped"),
    WEAR_MAIN_LIST_DOWNLOADS_TAPPED("wear_main_list_downloads_tapped"),
    WEAR_MAIN_LIST_FILTERS_TAPPED("wear_main_list_filters_tapped"),
    WEAR_MAIN_LIST_FILES_TAPPED("wear_main_list_files_tapped"),
    WEAR_MAIN_LIST_SETTINGS_TAPPED("wear_main_list_settings_tapped"),

    /* Wear Sign In */
    WEAR_REQUIRE_PLUS_SHOWN("wear_require_plus_shown"),
    WEAR_SIGNIN_SHOWN("wear_signin_shown"),
    WEAR_SIGNIN_GOOGLE_TAPPED("wear_signin_google_tapped"),
    WEAR_SIGNIN_PHONE_TAPPED("wear_signin_phone_tapped"),
    WEAR_SIGNIN_EMAIL_TAPPED("wear_signin_email_tapped"),

    /* Whats New Popup */
    WHATSNEW_SHOWN("whatsnew_shown"),
    WHATSNEW_DISMISSED("whatsnew_dismissed"),
    WHATSNEW_CONFIRM_BUTTON_TAPPED("whatsnew_confirm_button_tapped"),
}
