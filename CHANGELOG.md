# Release notes

### 7.21.0

*   Bug Fixes:    
    *   Fix the mini player's play icon showing the wrong icon.
        ([#208](https://github.com/Automattic/pocket-casts-android/pull/208)).
    *   Fix dark theme background color.
        ([#206](https://github.com/Automattic/pocket-casts-android/issues/206)).
    *   Fix embedded artwork not showing on player screen.
        ([#16](https://github.com/Automattic/pocket-casts-android/issues/16)).
    *   Fix long podcast website links overflowing.
        ([#230](https://github.com/Automattic/pocket-casts-android/issues/230)).
    *   Add a confirmation prompt for "End playback & clear Up Next" to prevent accidental actions.
        ([#237](https://github.com/Automattic/pocket-casts-android/issues/237)).
    *   Improve the mini player long press dialog.
        ([#240](https://github.com/Automattic/pocket-casts-android/issues/240)).
    *   Fix OPML import failing with invalid XML characters.
        ([#51](https://github.com/Automattic/pocket-casts-android/issues/51)).
    *   Fix the listening stats not being sent to the server correctly.
        ([#238](https://github.com/Automattic/pocket-casts-android/issues/238)).
    *   Fix tapping on the cast notification.
        ([#26](https://github.com/Automattic/pocket-casts-android/issues/26)).
    *   Fix episode row buffering state.
        ([#53](https://github.com/Automattic/pocket-casts-android/issues/53)).
    *   Fix playing on Chromecast always shows buffering.
        ([#254](https://github.com/Automattic/pocket-casts-android/pull/254)).

### 7.20.3

*   Bug Fixes:
    *   Fixes folder mapping at the time of folders full sync.
        ([#214](https://github.com/Automattic/pocket-casts-android/pull/214)).

### 7.20.2

*   Bug Fixes:    
    *   Fix OPML import.
    *   Fix podcasts and folders rearrange crash.
        ([#200](https://github.com/Automattic/pocket-casts-android/issues/200)).

### 7.20.1

*   New Features:
    *   Add localizations for English (UK), Arabic, and Norwegian.
*   Bug Fixes:
    *   Fix an issue where the podcasts order was being changed after migrating to the latest version.

### 7.20.0

*   New Features:
    *   Folders!
*   Health:
    *   Remove old notification events
        ([#3303](https://github.com/shiftyjelly/pocketcasts-android/pull/3303)).
    *   Add extra logs to help track playback issues
        ([#3304](https://github.com/shiftyjelly/pocketcasts-android/pull/3304)).
    *   Split the app into modules ready for the Wear OS app
        ([#3313](https://github.com/shiftyjelly/pocketcasts-android/pull/3313)).
    *   Migrate to non-transitive R classes
        ([#3314](https://github.com/shiftyjelly/pocketcasts-android/issues/3314)).
    *   Improve speed of Automotive app
        ([#3330](https://github.com/shiftyjelly/pocketcasts-android/pull/3330)).
*   Bug Fixes:    
    *   Import latest localizations.
    *   Up Next total time duration is no longer limited to 596h 31m.
    *   Fix video player out of memory issues
        ([#3339](https://github.com/shiftyjelly/pocketcasts-android/pull/3339)).
    *   Fix Android Auto playback speed not working
        ([#3366](https://github.com/shiftyjelly/pocketcasts-android/issues/3366)).
    *   Fix the podcast page going blank when sharing
        ([#3324](https://github.com/shiftyjelly/pocketcasts-android/issues/3324)).
    *   Fix sign in and create email invalid state with white space
        ([#3306](https://github.com/shiftyjelly/pocketcasts-android/issues/3306)).
    *   Fix playback starting when disconnecting from bluetooth and can't be stopped
        ([#3311](https://github.com/shiftyjelly/pocketcasts-android/issues/3311)).
    *   Improve the search failed error message
        ([#3300](https://github.com/shiftyjelly/pocketcasts-android/pull/3300)).
    *   Fix status bar color issue
        ([#3299](https://github.com/shiftyjelly/pocketcasts-android/pull/3299)).
    *   Fix progress bar issue when playback is paused
        ([#3295](https://github.com/shiftyjelly/pocketcasts-android/issues/3295)).
    *   Fix discover single episode colors
        ([#3288](https://github.com/shiftyjelly/pocketcasts-android/issues/3288)).    
    *   Fix podcast website url
        ([#3290](https://github.com/shiftyjelly/pocketcasts-android/issues/3290)).
    *   Fix localizations
        ([#3277](https://github.com/shiftyjelly/pocketcasts-android/pull/3277)).
    *   Improve warning when streaming will use metered data 
        ([#3246](https://github.com/shiftyjelly/pocketcasts-android/pull/3426)).
    *   Fix crash when switching to podcast without chapters 
        ([#3450](https://github.com/shiftyjelly/pocketcasts-android/pull/3450)).
    *   Improve sharing a list of podcasts
        ([#97](https://github.com/shiftyjelly/pocketcasts-android/pull/97)).
    *   Fix displaying previous show notes briefly when switching episodes.
        ([#35](https://github.com/Automattic/pocket-casts-android/pull/35)).
    *   Fix same chapter click behaviour 
        ([#59](https://github.com/Automattic/pocket-casts-android/pull/59)).
    *   Replace duration with time left on episode screen from listening history  
        ([#83](https://github.com/Automattic/pocket-casts-android/pull/83)).
    *   Fix back navigation for full screen player 'Rearrange Actions' page
        ([#76](https://github.com/Automattic/pocket-casts-android/pull/76)).
    *   Prevent video player controls from getting stuck on the screen
        ([#77](https://github.com/Automattic/pocket-casts-android/pull/77)).
    *   Fix showing paused downloads as in progress
        ([#113](https://github.com/Automattic/pocket-casts-android/pull/113)).
    *   Fix issues with discover feed state when scrolling through the feed
        ([#120](https://github.com/Automattic/pocket-casts-android/pull/120)).
    *   Improve manage downloads screen
        ([#117](https://github.com/Automattic/pocket-casts-android/pull/117)).

### 7.19.2 (2022-02-11)

*   New Features:
    *   Add support for episode lists in the discover section.
    *   Add two new localizations French (Canada) and Spanish (Mexico).
*   Health:
    *   Migrate app to Android 12 by targeting the SDK version 31.
*   Bug Fixes:    
    *   Import latest localizations.
    *   Fix localization layout
        ([#3173](https://github.com/shiftyjelly/pocketcasts-android/pull/3173)).
    *   Fix user file colors can't be changes
        ([#3136](https://github.com/shiftyjelly/pocketcasts-android/issues/3136)).
    *   Fix podcast search view requiring a double tap to open the keyboard.
    *   Fix while playing a cloud file it jumps back with a refresh
        ([#3140](https://github.com/shiftyjelly/pocketcasts-android/issues/3140)).
    *   Fix unsubscribing from a podcast doesn't remove it from a filter
        ([#3150](https://github.com/shiftyjelly/pocketcasts-android/issues/3150)).
    *   Fix full screen video player media controls not hiding automatically
        ([#3148](https://github.com/shiftyjelly/pocketcasts-android/issues/3148)).
    *   Fix create account next button not working when using 1Password
        ([#3167](https://github.com/shiftyjelly/pocketcasts-android/issues/3167)).
        
