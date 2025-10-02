7.100
-----
*   Bug Fixes
    *   Fix restoring large Up Next queues
        ([#4562](https://github.com/Automattic/pocket-casts-android/pull/4562))
    *   Fix done button overlapping with navigation bars
        ([#4553](https://github.com/Automattic/pocket-casts-android/pull/4553))
    *   Fix shared list under the mini player
        ([#4560](https://github.com/Automattic/pocket-casts-android/pull/4560))
    *   Fix transcript button incorrectly disabled
        ([#4555](https://github.com/Automattic/pocket-casts-android/pull/4555))

7.99
-----
*   Updates
    *   Improve episode rows swipe and multi-select animations.
        ([#4500](https://github.com/Automattic/pocket-casts-android/pull/4500))
    *   Improve user episode rows swipe and multi-select animations.
        ([#4501](https://github.com/Automattic/pocket-casts-android/pull/4501))
    *   Improve up next episode rows swipe and multi-select animations.
        ([#4502](https://github.com/Automattic/pocket-casts-android/pull/4502))
    *   Migrate from old Google login method to CredentialManager APIs
        ([#4523](https://github.com/Automattic/pocket-casts-android/pull/4523))
    *   Use CredentialManager on WearOS for Google login
        ([#4528](https://github.com/Automattic/pocket-casts-android/pull/4528))
    *   Enable saved passwords to be used for WearOS login
        ([#4548](https://github.com/Automattic/pocket-casts-android/pull/4548))
    *   Do not remove episodes from Up Next when downloaded files are deleted.
        ([#4549](https://github.com/Automattic/pocket-casts-android/pull/4549))

7.98
-----
*   Bug Fixes
    *   Fix Download notifications no longer open app
        ([#4449](https://github.com/Automattic/pocket-casts-android/pull/4449))
    *   Don't show notification to create free account when the user is already signed in
        ([#4468](https://github.com/Automattic/pocket-casts-android/pull/4468))

7.97
-----
*   New Features
    *   Onboarding Account Creation Improvements
        ([#4405](https://github.com/Automattic/pocket-casts-android/pull/4405))
    *   Onboarding Recommendation Changes
        ([#4431](https://github.com/Automattic/pocket-casts-android/pull/4431))
*   Updates
    *   Improved notification permission experience
        ([#4408](https://github.com/Automattic/pocket-casts-android/pull/4408))
*   Bug Fixes
    *   Fix File Settings bottom content being obstructed by the Mini Player.
        ([#4414](https://github.com/Automattic/pocket-casts-android/pull/4414))
    *   Fix podcast image shadow animation.
        ([#4429](https://github.com/Automattic/pocket-casts-android/pull/4429))
    *   Fix layout spacing in onboarding interests page
        ([#4471](https://github.com/Automattic/pocket-casts-android/pull/4471))
        

7.96
-----
*   New Features
    *   Add transcript sharing
        ([#4369](https://github.com/Automattic/pocket-casts-android/pull/4369))
*   Updates
    *   When Automotive doesn't have a browser, the URL and a QR code are shown instead
        ([#4362](https://github.com/Automattic/pocket-casts-android/pull/4362))

7.95
-----
*   Updates
    *   Open podcast search by tapping the Discover button when the Discover page is displayed
        ([#4272](https://github.com/Automattic/pocket-casts-android/pull/4272))
* Bug Fixes
    *   Fix missing file sizes on the episode page
        ([#4262](https://github.com/Automattic/pocket-casts-android/pull/4262))
    *   Fix the notification message for "Trending this week"
        ([#4273](https://github.com/Automattic/pocket-casts-android/pull/4273))
    *   Fix navigation bar drawing on top of the player after configuration changes.
        ([#4294](https://github.com/Automattic/pocket-casts-android/pull/4294))
    *   Fix notifications no longer open app when tapped
        ([#4365](https://github.com/Automattic/pocket-casts-android/pull/4365))

7.94
-----
*   Bug Fixes
    *   Fix player's playback position being incorrect after app restart
        ([#4208](https://github.com/Automattic/pocket-casts-android/pull/4208))
    *   Fix icon colors of the upgrade account banner
        ([#4246](https://github.com/Automattic/pocket-casts-android/pull/4246))

7.93
-----
*   New Features
    *   Display transcripts in the episode details UI
        ([#4143](https://github.com/Automattic/pocket-casts-android/pull/4143))
    *   Smart Categories - featured categories are now ordered by visits on the Discover tab
        ([#4186](https://github.com/Automattic/pocket-casts-android/pull/4186))
*   Updates
    *   New design for the Guest List and Network Highlight sections in Discover
        ([#4153](https://github.com/Automattic/pocket-casts-android/pull/4153))
    *   Bring back landscape UI
        ([#4156](https://github.com/Automattic/pocket-casts-android/pull/4156))
*   Bug Fixes
    *   Fix clip links causing app being unresponsive.
        ([#4217](https://github.com/Automattic/pocket-casts-android/pull/4217))
    *   Collapse player when opening a notification
        ([#4150](https://github.com/Automattic/pocket-casts-android/pull/4150))
    *   Fix episodes not being removed from the downloads list
        ([#4167](https://github.com/Automattic/pocket-casts-android/pull/4167))
    *   Fix revamped notifications showing up on wear and automotive
        ([#4177](https://github.com/Automattic/pocket-casts-android/pull/4177))
    *   Fix upsell notifications showing up even if the user already has an active subscription
        ([#4216](https://github.com/Automattic/pocket-casts-android/pull/4216))
    *   Fix navigation bar obstructing content on older Android systems
        ([#4219](https://github.com/Automattic/pocket-casts-android/pull/4219))

7.92
-----
*   Updates
    *   Improve transcripts parsing and loading
        ([#4115](https://github.com/Automattic/pocket-casts-android/pull/4115))
    *   Remove bottom padding with empty episode list
        ([#4121](https://github.com/Automattic/pocket-casts-android/pull/4121))
*   Bug Fixes
    *   Fix keyboard blocking WebView input fields in Help & Feedback page
        ([#4109](https://github.com/Automattic/pocket-casts-android/pull/4109))
    *   Fix player's theming responsiveness
        ([#4091](https://github.com/Automattic/pocket-casts-android/pull/4091))
    *   Respect embedded file artwork in media notifications
        ([#4103](https://github.com/Automattic/pocket-casts-android/pull/4103))
    *   Specify what files to be backed up on Android 12 and up
        ([#4119](https://github.com/Automattic/pocket-casts-android/pull/4119))
    *   Fix podcast selector overlapping system status bar on auto add to up next podcast selector
        ([#4132](https://github.com/Automattic/pocket-casts-android/pull/4132))
    *   Fix downloading episodes after backup and restore
        ([#4130](https://github.com/Automattic/pocket-casts-android/pull/4130))
    *   Add `@Transaction` to podcast queries that might result in large amount of data
        ([#4151](https://github.com/Automattic/pocket-casts-android/pull/4151))
    *   Fix issue with notification prompt showing too often
        ([#4162](https://github.com/Automattic/pocket-casts-android/pull/4162))
*   Hotfixes
    *   Fix broken downloads experience
        ([#4231](https://github.com/Automattic/pocket-casts-android/pull/4231))

7.91
-----
*   New Features
    *   Introduce new notifications and give better control over them
        ([#4107](https://github.com/Automattic/pocket-casts-android/pull/4107))
*   Updates
    *   Upgrade to the latest version of Jetpack Compose
        ([#4049](https://github.com/Automattic/pocket-casts-android/pull/4049))
    *   Improve accessibility of the follow button in some of the Discover sections
        ([#4050](https://github.com/Automattic/pocket-casts-android/pull/4050))
    *   Make full screen Player less sensitive to the hide fling gesture
        ([#4052](https://github.com/Automattic/pocket-casts-android/pull/4052))
    *   Lock general UI to portrait mode on regular phone devices
        ([#4083](https://github.com/Automattic/pocket-casts-android/pull/4083))
*   Bug Fixes
    *   Fix bottom sheet animation used by Up Next and other screens
        ([#4051](https://github.com/Automattic/pocket-casts-android/pull/4051))
    *   Fix duration render issue on podcasts without `<itunes:duration>` tag
        ([#4077](https://github.com/Automattic/pocket-casts-android/pull/4077))

7.90
-----
*   New Features
    *   Add Recently Played sorting option for podcasts
        ([#3816](https://github.com/Automattic/pocket-casts-android/pull/3816)) 
*   Updates
    *   Add dropshadow to the podcast artwork and move it below toolbar.
        ([#4006](https://github.com/Automattic/pocket-casts-android/pull/4006))
    *   Improve error handling when purchasing subscriptions.
        ([#4024](https://github.com/Automattic/pocket-casts-android/pull/4024))
*   Bug Fixes
    *   Updates datastore dependency to prevent crashes
        ([#4031](https://github.com/Automattic/pocket-casts-android/pull/4031)

7.89
-----
*   New Features
    *   Podcast recommendations
        ([#3946](https://github.com/Automattic/pocket-casts-android/pull/3946))
    *   Support for Podroll 
        ([#3950](https://github.com/Automattic/pocket-casts-android/pull/3950))
*   Updates
    *   Improve contrast of secondary texts on Upgrade page
        ([#3942](https://github.com/Automattic/pocket-casts-android/pull/3942))
    *   Improve Accessibility on the Upgrade page
        ([#3947](https://github.com/Automattic/pocket-casts-android/pull/3947))
    *   Fix issue with podcast description crashing the app
        ([#3994](https://github.com/Automattic/pocket-casts-android/pull/3994))

7.88
-----
*   New Features
    *   Add account encouragement flow
        ([#3909](https://github.com/Automattic/pocket-casts-android/pull/3909))
*   Bug Fixes
    *   Fix crash when updating playlist
        ([#3929](https://github.com/Automattic/pocket-casts-android/pull/3929))
    *   Fix podcast follow in the Discover tab not always working
        ([#3922](https://github.com/Automattic/pocket-casts-android/pull/3922))
    *   Fix missing fullscreen player controls in landscape mode
        ([#3917](https://github.com/Automattic/pocket-casts-android/pull/3917))
    *   Fix issue with disappearing file image after upload completes
        ([#3910](https://github.com/Automattic/pocket-casts-android/pull/3910))
    *   Fix issue with image artwork not loading in some cases
        ([#3970](https://github.com/Automattic/pocket-casts-android/pull/3970))

7.87
-----
*   New Features
    *   Add Greek locale support
        ([#3864](https://github.com/Automattic/pocket-casts-android/pull/3864))
*   Bug Fixes
    *   Fix episode sometimes not updating their metadata.
        ([#3867](https://github.com/Automattic/pocket-casts-android/pull/3867))

7.86
-----
*   New Features
    *   Add Creator Funding Support
        ([#3840](https://github.com/Automattic/pocket-casts-android/pull/3840))
*   Bug Fixes
    *   Fix share transcript text when API less than 33.
        ([#3789](https://github.com/Automattic/pocket-casts-android/pull/3789))
*   Updates
    *   Show a confirmation dialog when deleting more than three episode downloads.
        ([#3800](https://github.com/Automattic/pocket-casts-android/pull/3800))
    *   Add settings option to restore up next from local history.
        ([#3797](https://github.com/Automattic/pocket-casts-android/pull/3797))
        
7.85
-----
*   New Features
    *   Add smart folders support.
        ([#3720](https://github.com/Automattic/pocket-casts-android/pull/3720))
    *   Add generated transcripts support.
        ([#3772](https://github.com/Automattic/pocket-casts-android/pull/3772))
*   Bug Fixes
    *   Fixed an issue where bookmark confirmation button was obstructed by keyboard.
        ([#3761](https://github.com/Automattic/pocket-casts-android/pull/3761))
    *   Fix issues with Classic widget not syncing its state in some scenarios.
        ([#3777](https://github.com/Automattic/pocket-casts-android/pull/3777))
    *   Fix podcast grid padding
        ([#3781](https://github.com/Automattic/pocket-casts-android/pull/3781))
    *   Fix multi-select toolbar being drawn behind the status bar on older Android versions.
        ([#3792](https://github.com/Automattic/pocket-casts-android/pull/3792))
*   Updates
    *   Close episode details screen when archiving an episode. This reverts [#3473](https://github.com/Automattic/pocket-casts-android/pull/3473). 
        ([#3718](https://github.com/Automattic/pocket-casts-android/pull/3718))
    *   Update widget titles and add descriptions.
        ([#3769](https://github.com/Automattic/pocket-casts-android/pull/3769))
    *   Reorder podcasts sorting order based on their popularity. 
        ([#3733](https://github.com/Automattic/pocket-casts-android/pull/3733))
    *   Improve small widget accessibility.
        ([#3770](https://github.com/Automattic/pocket-casts-android/pull/3770))

7.84.1
-----
*   Bug Fixes
    *   Fixed an issue where the Up Next queue obstructed the bottom navigation bar in some cases.
        ([#3762](https://github.com/Automattic/pocket-casts-android/pull/3762))
    *   Fixed an issue where the navigation bar was drawn over the player in some cases.
        ([#3762](https://github.com/Automattic/pocket-casts-android/pull/3762))
    *   Fixed an issue where the mini player could sometimes hide behind the navigation bar.
        ([#3687](https://github.com/Automattic/pocket-casts-android/pull/3687))

7.84
-----
*   New Features
    *   Redesign podcast header UI
        ([#3677](https://github.com/Automattic/pocket-casts-android/pull/3671))
*   Bug Fixes
    *   Fix issue with playback not resuming when using headphone actions.
        ([#3665](https://github.com/Automattic/pocket-casts-android/pull/3665))
    *   Fix link not being interactive in a podcast description.
        ([#3666](https://github.com/Automattic/pocket-casts-android/pull/3666))
    *   Fix screen flickering when tapping on item in the navigation bar.
        ([#3674](https://github.com/Automattic/pocket-casts-android/pull/3674))
    *   Fix crash with podcast update when offline.
        ([#3718](https://github.com/Automattic/pocket-casts-android/pull/3718))

7.83
-----
*   New Features
    *   Add support for updating podcast feed
        ([#3569](https://github.com/Automattic/pocket-casts-android/pull/3569))
*   Bug Fixes
    *   Fix missing episodes with unfollowed podcasts
        ([#3546](https://github.com/Automattic/pocket-casts-android/pull/3546))
    *   Fix Media Notification artwork caching issues
        ([#3566](https://github.com/Automattic/pocket-casts-android/pull/3566))
    *   Fix issue causing the Account Details page to become unresponsive after viewing an offer
        ([#3574](https://github.com/Automattic/pocket-casts-android/pull/3574))
    *   Fix crash in Android 15 with the background task manager
        ([#3632](https://github.com/Automattic/pocket-casts-android/pull/3632))
*   Updates
    *   Filter out chapters that do not belong in table of contents. See [the specification](https://github.com/Podcastindex-org/podcast-namespace/blob/main/chapters/jsonChapters.md) for more details.
        ([#3556](https://github.com/Automattic/pocket-casts-android/pull/3556))
    *   Add OPML import from URL to the onboarding process.
        ([#3584](https://github.com/Automattic/pocket-casts-android/pull/3584))

7.82
-----
*   New Features
    *   Android 15 (SDK 35) upgrade 
        ([#3392](https://github.com/Automattic/pocket-casts-android/pull/3392))
*   Bug Fixes
    *   Fix status bar theming during onboarding.
        ([#3460](https://github.com/Automattic/pocket-casts-android/pull/3460))
    *   Fix issue with playback stopping when using Pixel Buds actions.
        ([#3493](https://github.com/Automattic/pocket-casts-android/pull/3493))
    *   Fix issue with sleep timer end of episode when has skip last set
        ([#3582](https://github.com/Automattic/pocket-casts-android/pull/3582))
*   Updates
    *   Remove audio and video clip sharing
        ([#3481](https://github.com/Automattic/pocket-casts-android/pull/3481))
    *   Do not close episode details screen when archiving an episode
        ([#3473](https://github.com/Automattic/pocket-casts-android/pull/3473))
    *   Rename Twitter to X
        ([#3489](https://github.com/Automattic/pocket-casts-android/pull/3489))

7.81
-----
*   New Features
    *   Add support for removing individual episodes from listening history.
        ([#3453](https://github.com/Automattic/pocket-casts-android/pull/3453))
*   Bug Fixes
    *   Fix the Galaxy Watch skip buttons not working.
        ([#3455](https://github.com/Automattic/pocket-casts-android/pull/3455)) 
    *   Add Pixel Watch skip buttons setting.
        ([#3474](https://github.com/Automattic/pocket-casts-android/pull/3474))
    *   Fix the cropping issue with the Add 5 Minutes sleep timer button.
        ([#3436](https://github.com/Automattic/pocket-casts-android/pull/3436))

7.80
-----
*   New Features
    *   Add support for podcast descriptions with HTML formatting 
        ([#3249](https://github.com/Automattic/pocket-casts-android/pull/3249))
*   Bug Fixes
    *   Fix global auto download setting was incorrectly overriding the podcast auto download setting
        ([#3342](https://github.com/Automattic/pocket-casts-android/pull/3342))
    *   Fix end of year translation
        ([#3387](https://github.com/Automattic/pocket-casts-android/pull/3387))
    *   Fix sleep timer was not stopping as expected
        ([#3377](https://github.com/Automattic/pocket-casts-android/pull/3377))
    *   Fix Galaxy Watch playback skip issues
        ([#3388](https://github.com/Automattic/pocket-casts-android/pull/3388))
*   Updates
    *   Add the ability to dismiss the low storage banner in download screen
        ([#3385](https://github.com/Automattic/pocket-casts-android/pull/3385))
    *   Disable Private Feed Sharing
        ([#3395](https://github.com/Automattic/pocket-casts-android/pull/3395))

7.79
-----
*   Bug Fixes
    *   Fix issue where seeking forward or backwards on hardware controls was not working on some headsets
        ([#3292](https://github.com/Automattic/pocket-casts-android/pull/3297))
*   Updates
    *   Improve log sharing
        ([#3336](https://github.com/Automattic/pocket-casts-android/pull/3336))
    *   Update account details header UI
        ([#3294](https://github.com/Automattic/pocket-casts-android/pull/3294))
    *   Improve the Up Next clear all button
        ([#3334](https://github.com/Automattic/pocket-casts-android/pull/3334))
*   Bug Fixes
    *   Fix podcast ratings not loading
        ([#3378](https://github.com/Automattic/pocket-casts-android/pull/3378))

7.78
-----
*   New Features
    *   Add episode basic authentication support
        ([#3213](https://github.com/Automattic/pocket-casts-android/pull/3213))
*   Bug Fixes
    *   Fix account details not working in Automotive app
        ([#3266](https://github.com/Automattic/pocket-casts-android/pull/3266))
*   Updates
    *   Update profile header UI
        ([#3235](https://github.com/Automattic/pocket-casts-android/pull/3235))
    *   Deferred addition to UpNext post update after sync 
        ([#3211](https://github.com/Automattic/pocket-casts-android/pull/312))

7.77
-----
*   New Features
    *   Add custom playback effects settings for podcast in the player
        ([#3042](https://github.com/Automattic/pocket-casts-android/issues/3042))
    *   Add Playback 2024 summary
        ([#3123](https://github.com/Automattic/pocket-casts-android/pull/3123))
    *   Add the ability to get notified when device is running on low storage and has downloaded episodes
        ([#3198](https://github.com/Automattic/pocket-casts-android/pull/3198))
    *   Add Up Next Shuffle
        ([#3199](https://github.com/Automattic/pocket-casts-android/pull/3199))
*   Updates
    *   Renamed the podcast action 'Subscribe' to 'Follow'
        ([#3120](https://github.com/Automattic/pocket-casts-android/pull/3120))
    *   Improve the performance of the refresh on Wear OS and Android Automotive
        ([#3171](https://github.com/Automattic/pocket-casts-android/pull/3171))
    *   Improve connection when some podcasts failed to play or download
        ([#3180](https://github.com/Automattic/pocket-casts-android/pull/3180))
    *   Add new podcast subscriptions to the top instead of bottom for Data Added podcast sort order 
        ([#3192](https://github.com/Automattic/pocket-casts-android/pull/3192)) 
*   Bug Fixes
    *   Improve syncing of deselected chapters
        ([#3256](https://github.com/Automattic/pocket-casts-android/pull/3256))
    *   Use red color for the notification icons
        ([#3154](https://github.com/Automattic/pocket-casts-android/pull/3154))
    *   Fix effects being lost when paused
        ([#3219](https://github.com/Automattic/pocket-casts-android/pull/3219))


7.76
-----
*   New Features
    *   Add download button to playback actions
        ([#3046](https://github.com/Automattic/pocket-casts-android/pull/3046))
    *   Add the ability to auto download episodes after subscribing to a podcast
        ([#3048](https://github.com/Automattic/pocket-casts-android/pull/3048))
    *   Add support for the Catalan laguage
        ([#3072](https://github.com/Automattic/pocket-casts-android/pull/3072))
    *   Add referrals to the app
        ([#3107](https://github.com/Automattic/pocket-casts-android/pull/3107))
*   Bug Fixes
    *   Keep the playing service in the foreground when casting
        ([#3094](https://github.com/Automattic/pocket-casts-android/pull/3094))
    *   Scroll profile tab app bar icons with the content in the landscape orientation
        ([#3138](https://github.com/Automattic/pocket-casts-android/pull/3138))
*   Updates
    *   Move Up Next clear queue button to app bar
        ([#3068](https://github.com/Automattic/pocket-casts-android/pull/3068))

7.75
-----
*   Updates
    *   Add an advanced setting to display artwork in episode listing
        ([#2958](https://github.com/Automattic/pocket-casts-android/pull/2958))
    *   Add a setting that controls whether the remaining playback time adjusts for playback speed and skipped chapters 
        ([#2955](https://github.com/Automattic/pocket-casts-android/pull/2955))
        ([#3044](https://github.com/Automattic/pocket-casts-android/pull/3044))
    *   Display buffer position more accurately for cached episode 
        ([#2987](https://github.com/Automattic/pocket-casts-android/pull/2987))
*   Bug Fixes
    *   Speed up listening history search
        ([#2979](https://github.com/Automattic/pocket-casts-android/pull/2979))
    *   Fix search podcast results scroll back to the start after subscribing
        ([#2923](https://github.com/Automattic/pocket-casts-android/pull/2923))
    *   Fix episodes stuck on the downloads page
        ([#3006](https://github.com/Automattic/pocket-casts-android/pull/3006))
    *   Display web-page based HTML transcripts in web view
        ([#2910](https://github.com/Automattic/pocket-casts-android/pull/2910))
*   New Features
    *   Add a progress bar in the browse view on Android Auto
        ([#2945](https://github.com/Automattic/pocket-casts-android/pull/2945))

7.74
-----
*   Bug Fixes
    *   Fix search term persists when navigating to different podcast page
        ([#2908](https://github.com/Automattic/pocket-casts-android/pull/2908))
    *   Fix the show notes web view jumping after loading
        ([#2925](https://github.com/Automattic/pocket-casts-android/pull/2925))
    *   Fix play button local file getting set to pause after episode completion
        ([#1627](https://github.com/Automattic/pocket-casts-android/pull/1627))
    *   Fix volume that was not returning to the original level after restarting sleep timer by shaking the device
        ([#2930](https://github.com/Automattic/pocket-casts-android/pull/2930))
*   New Features
    *   Add sleep timer settings to sleep timer bottom sheet
        ([#2829](https://github.com/Automattic/pocket-casts-android/pull/2829))
    *   Add Google Engage SDK integration 
        ([#2847](https://github.com/Automattic/pocket-casts-android/pull/2847))

7.73
-----
*   New Features
    *   Add local search in listening history
        ([#2794](https://github.com/Automattic/pocket-casts-android/pull/2794))
*   Updates
    *   Dark theme improvements on the podcast page
        ([#2811](https://github.com/Automattic/pocket-casts-android/pull/2811))
    *   Add a visibility animation to the submit button on the rating screen
        ([#2824](https://github.com/Automattic/pocket-casts-android/pull/2824))
    *   Improve performance of rendering artwork
        ([#2832](https://github.com/Automattic/pocket-casts-android/pull/2832))
*   Bug Fixes
    *   Fix issue when transcript type have an alternative valid mime type 
        [#2910](https://github.com/Automattic/pocket-casts-android/pull/2910)
    *   Fix display of html entities in transcripts
        [#2940](https://github.com/Automattic/pocket-casts-android/issues/2940)

7.72
-----
*   Bug Fixes
    *   Fix create filter crash
        ([#2677](https://github.com/Automattic/pocket-casts-android/pull/2677))
    *   Improved full screen player Talkback
        ([#2742](https://github.com/Automattic/pocket-casts-android/pull/2742))
    *   Fix multi-select toolbar action order
        ([#2737](https://github.com/Automattic/pocket-casts-android/pull/2737))
*   New Features
    *   Content is now shared through a new UI interface with support for different platforms including Instagram Stories
        ([#2707](https://github.com/Automattic/pocket-casts-android/pull/2707))
    *   Share clips using links, audio clips, and video clips
        ([#2707](https://github.com/Automattic/pocket-casts-android/pull/2707))
    *   Enables transcripts (Internal)
        ([#2747](https://github.com/Automattic/pocket-casts-android/pull/2747))

7.71
-----
*   New Features
    *   Adds the kids banner in profile
        ([#2591](https://github.com/Automattic/pocket-casts-android/pull/2591))
*   Bug Fixes
    *   Fix playback and download failures due to an insecure connection
        ([#2717](https://github.com/Automattic/pocket-casts-android/pull/2717))
    *   Update the episode star value in the Media Session to fix places where it's shown outside the app
        ([#2613](https://github.com/Automattic/pocket-casts-android/pull/2613))
    *   Fix content under the mini player on search screens
        ([#2649](https://github.com/Automattic/pocket-casts-android/pull/2649))
    *   Fix crash when clicking on chapter title within player
        ([#2657](https://github.com/Automattic/pocket-casts-android/pull/2657))
    *   Handle more podcast share links
        ([#2666](https://github.com/Automattic/pocket-casts-android/pull/2666))

7.70
-----
*   Updates
    *   Episodes shared from the player are updated with the playback status.
        ([#2499](https://github.com/Automattic/pocket-casts-android/pull/2499))
    *   Improved the podcast refresh performance.
        ([#2500](https://github.com/Automattic/pocket-casts-android/pull/2500))
    *   Rename Lifetime member to Pocket Casts Champion
        ([#2527](https://github.com/Automattic/pocket-casts-android/pull/2527))
*   New Features
    *   Add an option in your account settings to modify your avatar with Gravatar
        ([#2263](https://github.com/Automattic/pocket-casts-android/pull/2263))
    *   Add the ability to rate podcasts
        ([#2506](https://github.com/Automattic/pocket-casts-android/pull/2506))
*   Bug Fixes
    *   Fix autoplay for Android Auto
        ([#2528](https://github.com/Automattic/pocket-casts-android/pull/2528))

7.69
-----
* Updated
    *   App opened from a link with an episode timestamp no longer starts playing automatically.
        ([#2479](https://github.com/Automattic/pocket-casts-android/pull/2479))
*   Bug Fixes
    *   Links to episodes with playback position shared from the app are now correctly recognized.
        ([#2471](https://github.com/Automattic/pocket-casts-android/pull/2471))
    *   Fixed Help & feedback AI assistant going under the keyboard.
        ([#2496](https://github.com/Automattic/pocket-casts-android/pull/2496))
    *   Fixed Wear app episode download issue.
        ([#2495](https://github.com/Automattic/pocket-casts-android/pull/2495))
    *   Fix redirects to the app through https://pocketcasts.com/get links.
        ([#2559](https://github.com/Automattic/pocket-casts-android/pull/2559))

7.68
-----
*   Updates:
    *   Update Give rating view to include the average rating
        ([#2421](https://github.com/Automattic/pocket-casts-android/pull/2421))
    *   Removes download button from shelf list in Now Playing
        ([#2493](https://github.com/Automattic/pocket-casts-android/pull/2493))
*   Bug Fixes
    *   Fix playback speed label tap behavior
        ([#2439](https://github.com/Automattic/pocket-casts-android/pull/2439))

7.67
-----
*   New Features
    *   Adds advanced setting to enable caching playing episode in order to minimise streaming
        ([#2380](https://github.com/Automattic/pocket-casts-android/pull/2380))
*   Bug Fixes
    *   Fix sharing episodes when using multi-selection
        ([#2376](https://github.com/Automattic/pocket-casts-android/pull/2376))
    *   Fix deselected chapters not synced correctly between different platforms
        ([#2357](https://github.com/Automattic/pocket-casts-android/pull/2357))
    *   Adds download episode action button to shelf list in Now Playing
        ([#2325](https://github.com/Automattic/pocket-casts-android/pull/2325))
    *    Updates carousel title size
         ([#2401](https://github.com/Automattic/pocket-casts-android/pull/2401))

7.66
-----
*   New Features
    *   Adds an option to export the database and preferences from the Help & Feedback view
        ([#2309](https://github.com/Automattic/pocket-casts-android/pull/2309))
    *   Chapters tab is now accessible from episode details.
        ([#2318](https://github.com/Automattic/pocket-casts-android/pull/2318))
*   Health
    *   Increase target SDK version to 34 
        ([#2279](https://github.com/Automattic/pocket-casts-android/pull/2279))
    *   Optimize writing large no. of podcast episodes into DB
        ([#2324](https://github.com/Automattic/pocket-casts-android/pull/2324))
*   Bug Fixes
    *   Fix: Subscribe button is highlighted in green instead of gray in the carousel
        ([#2278](https://github.com/Automattic/pocket-casts-android/pull/2278))
    *   Fix syncing on app startup in the Automotive app
        ([#2277](https://github.com/Automattic/pocket-casts-android/pull/2277))
*   Updates:
    *   Playback speed can now be changed up to 5x.
        ([#1645](https://github.com/Automattic/pocket-casts-android/pull/1645))

7.65
-----
*   New Features
    *   Add a storage setting that attempts to fix missing downloads files.
        ([#2244](https://github.com/Automattic/pocket-casts-android/pull/2244))
    *   Add an advanced setting that prioritizes seek accuracy over speed during streaming.
        ([#2265](https://github.com/Automattic/pocket-casts-android/pull/2265))
    *   Add sleep timer settings that controls the auto restart sleep timer and shake to restart sleep timer
        ([#2273](https://github.com/Automattic/pocket-casts-android/pull/2273))
*   Health
    *   Increase minimum SDK version to 24 ([#2262](https://github.com/Automattic/pocket-casts-android/pull/2262))
*   Bug Fixes
    *   Fix Tesla podcast artwork not loading
        ([#2254](https://github.com/Automattic/pocket-casts-android/pull/2254))
    *   Fix timestamp parameter handling in shared links
        ([#2235](https://github.com/Automattic/pocket-casts-android/pull/2235))
    *   Fix the Google Podcasts OPML file import and include an error message if parsing fails.
        ([#2260](https://github.com/Automattic/pocket-casts-android/pull/2260))
    *   Improve intermediate caching of playing episode 
        ([#2242](https://github.com/Automattic/pocket-casts-android/pull/2242))    
    *   Fix 'No limit' episode archive setting not being respected 
        ([#2270](https://github.com/Automattic/pocket-casts-android/pull/2270))
    *   Fix badges visibility on the Podcasts tab
        ([#2284](https://github.com/Automattic/pocket-casts-android/pull/2284))
    *   Fix podcast title talkback on the Podcasts tab
        ([#2297](https://github.com/Automattic/pocket-casts-android/pull/2297))

7.64
-----
*   New Features
    *   Add three new widgets - small, medium, and large.
        ([#2216](https://github.com/Automattic/pocket-casts-android/pull/2216))
*   Updates:
    *   New design for the Podcasts grid layout
        ([#2165](https://github.com/Automattic/pocket-casts-android/pull/2165))
    *   Adds new dedicated tab for up next
        ([#2213](https://github.com/Automattic/pocket-casts-android/pull/2213))
    *   Mini player small design refresh
        ([#2214](https://github.com/Automattic/pocket-casts-android/pull/2214))
*   Bug Fixes
    *   Fix pull to refresh icon sometimes being stuck in a loading state
        ([#2164](https://github.com/Automattic/pocket-casts-android/pull/2164))
    *   Fix cases where podcasts were ignoring 'Never archive' setting in some cases
        ([#2194](https://github.com/Automattic/pocket-casts-android/pull/2194))
    *   Fix sleep timer end of episode was not keeping the last episode listened
        ([#2269](https://github.com/Automattic/pocket-casts-android/pull/2269))

7.63
-----
*   New Features
    *   Sleep timer: Restart timer when shaking phone
        ([#2054](https://github.com/Automattic/pocket-casts-android/pull/2054))
    *   Sleep timer: Start fading out audio when sleep timer has 5 seconds left
        ([#2069](https://github.com/Automattic/pocket-casts-android/pull/2069))
    *   Sleep timer: Add the option to select the number of episodes
        ([#2097](https://github.com/Automattic/pocket-casts-android/pull/2097))
    *   Enable toggling episode artwork separately in different contexts
        ([#2112](https://github.com/Automattic/pocket-casts-android/pull/2112))
    *   Sleep timer: Add the option to select the number of chapters
        ([#2115](https://github.com/Automattic/pocket-casts-android/pull/2115))
*   Updates:
    *   Improved updating podcast episode when subscribed to thousands podcasts. 
        ([#2082](https://github.com/Automattic/pocket-casts-android/pull/2082))
    *   Prioritize embedded chapters over remote ones.
        ([#2098](https://github.com/Automattic/pocket-casts-android/pull/2098))
*   Bug Fixes
    *   Fix flashing artwork
        ([#2086](https://github.com/Automattic/pocket-casts-android/pull/2086))
    *   Fix embedded artwork not being extracted when adding files
        ([#2124](https://github.com/Automattic/pocket-casts-android/pull/2124))
    *   Fix an issue when sorting podcast by episodes could hide some podcasts
        ([#2125](https://github.com/Automattic/pocket-casts-android/pull/2125))

7.62
-----
*   New Features
    *   Allow sharing of bookmarks from bookmarks list
        ([#2022](https://github.com/Automattic/pocket-casts-android/pull/2022))
    *   Add profile bookmark screen where all user bookmarks can be managed
        ([#2037](https://github.com/Automattic/pocket-casts-android/pull/2037))
    *   Sleep Timer restarts automatically if you play again within 5 minutes
        ([#2048](https://github.com/Automattic/pocket-casts-android/pull/2048))
*   Bug Fixes
    *   Improved player view in landscape mode. Added chapter artwork, podcast title, and improved video experience.
        ([#2044](https://github.com/Automattic/pocket-casts-android/pull/1944))
    *   Sleep Timer: End of episode option was not being preserved after switching to another episode from Up Next
        ([#2075](https://github.com/Automattic/pocket-casts-android/pull/2075))

7.61
-----
*   New Features
    *   Support embedded chapters from [Podcast Index](https://github.com/Podcastindex-org/podcast-namespace/blob/c40c127d9e1a3e9b800766d6f01e0dcd5b09ab2a/docs/1.0.md#chapters) and [Podlove](https://podlove.org/simple-chapters/).
        ([#1965](https://github.com/Automattic/pocket-casts-android/pull/1940))
*   Updates:
    *   Display episode's RSS artwork in more places in the app.
        ([#1943](https://github.com/Automattic/pocket-casts-android/pull/1943))
    *   Consolidate episode's embedded file artwork and RSS artwork into a single setting.
        ([#1987](https://github.com/Automattic/pocket-casts-android/pull/1987))
    *   Add alphabetical sort order for podcast episodes.
        ([#1969](https://github.com/Automattic/pocket-casts-android/pull/1969))
* Bug Fixes:
    *   Subscription cancellation redirects now to a correct page.
        ([#1973](https://github.com/Automattic/pocket-casts-android/pull/1973))
    *   Fix Trim Silence effect causing clicking sound at the end of episodes and preventing it from finishing.
        ([#2007](https://github.com/Automattic/pocket-casts-android/pull/2007))
    *   Fix not displaying chapter titles in the player.
        ([#2008](https://github.com/Automattic/pocket-casts-android/pull/2008))

7.60
-----
*   New Features
    *   Ability to deselect chapters entering Patron early access with full release for Plus users in next release
        ([#1940](https://github.com/Automattic/pocket-casts-android/pull/1940))
* Bug Fixes:
    *   Fix an issue where shared lists from the newsletter might not open.
        ([#1988](https://github.com/Automattic/pocket-casts-android/pull/1988))

7.59
-----
*   Updates:
    *   Navigate to a podcast when a podcast title in the player tapped.
        ([#1875](https://github.com/Automattic/pocket-casts-android/pull/1875))
    *   Enable updating playback speed in the media notification from 2x to 3x, 3x to 0.6x, 0.6x to 0.8x, and 0.8x to 1.0x.
        ([#1862](https://github.com/Automattic/pocket-casts-android/pull/1862))
    *   Add toggle for podcast artwork embedded in an episode's show notes
        ([#1854](https://github.com/Automattic/pocket-casts-android/pull/1854))
* Bug Fixes:
    *   Fixed an issue where bookmarks did not play when episodes where filtered out due to search queries.
        ([#1857](https://github.com/Automattic/pocket-casts-android/pull/1857))    
    *   Fixed an issue where bookmarks on description could become unresponsive.
        ([#1873](https://github.com/Automattic/pocket-casts-android/pull/1873))        
    *   Fixed an issue where bookmarks could display incorrect timestamps.
        ([#1876](https://github.com/Automattic/pocket-casts-android/pull/1876))
    *   Fixed: Notification center doesn't display playback speed below 1x
        ([#1862](https://github.com/Automattic/pocket-casts-android/pull/1862))

7.58
-----
*   Bug Fixes:
    *   Fixed: The total remaining time was incorrectly displayed for some languages when large font sizes were set on the device in Up Next
        ([#1815](https://github.com/Automattic/pocket-casts-android/pull/1815))
    *   Fixed: The back button in the search was lacking a description, causing an accessibility issue.
        ([#1846](https://github.com/Automattic/pocket-casts-android/pull/1846))
    *   Fixed a bug where podcast setting screen could become unresponsive.
        ([#1845](https://github.com/Automattic/pocket-casts-android/pull/1845))
    *   Fixed: Widget uses custom Headphone controls instead of skipping.
        ([#1853](https://github.com/Automattic/pocket-casts-android/pull/1853))

7.57
-----

*   Updates:
    *   Warn when switching to metered network if warn before using data setting is enabled
        ([#1640](https://github.com/Automattic/pocket-casts-android/pull/1640))
    *   Display episode artwork from podcast feed on Mini-player, Notification & Widget.
        ([#1599](https://github.com/Automattic/pocket-casts-android/pull/1599))
*   Bug Fixes:
    *   Fixed starring episode from full-screen player does not prevent episode from being archived
        ([#1735](https://github.com/Automattic/pocket-casts-android/pull/1735))
    *   Fixed "Hide playback notification on pause" setting not representing its state correctly
        ([#1769](https://github.com/Automattic/pocket-casts-android/pull/1769))
    *   Improved skipping behavior from bluetooth device 
        ([#1818](https://github.com/Automattic/pocket-casts-android/pull/1818))

7.56
-----

*   Updates:
    *   Use fling motion instead of scroll motion to open UpNext bottom sheet
        ([#1697](https://github.com/Automattic/pocket-casts-android/pull/1697))
*   Bug Fixes:
    *   Fixed incorrect podcast loading below the correct one on opening native podcast share url
        ([#1696](https://github.com/Automattic/pocket-casts-android/pull/1696))
    *   Fixed an issue where navigation and status bar could sometimes use incorrect theming with Up Next screen.
        ([#1697](https://github.com/Automattic/pocket-casts-android/pull/1697))

7.55
-----

*   Updates:
    *   Save selected tab on Player screen after screen rotation
        ([#1598](https://github.com/Automattic/pocket-casts-android/issues/1598))
*   Bug Fixes:
    *   Improve handling of user files on watch
        ([#1638](https://github.com/Automattic/pocket-casts-android/pull/1638))
    *   Fixed an issue with a missing accessibility label for the 'Up Next' screen
        ([#1657](https://github.com/Automattic/pocket-casts-android/pull/1662))
    *   Fixed an issue with incorrect playback speed shown on the media notification
        ([#1648](https://github.com/Automattic/pocket-casts-android/pull/1666))

7.54
-----

*   New Features
    *   Display episode artwork from podcast feed on full-screen player and episode screens
        ([#1599](https://github.com/Automattic/pocket-casts-android/pull/1599))
*   Bug Fixes:
    *   Fix user files not getting deleted after marking an episode as played
        ([#1548](https://github.com/Automattic/pocket-casts-android/pull/1548))
    *   Fix crash when database emits unexpected null value
        ([#1596](https://github.com/Automattic/pocket-casts-android/pull/1596))
    *   Fix text being cut off in discover search bar at high zoom 
        ([#1601](https://github.com/Automattic/pocket-casts-android/pull/1601))
    *   Fix issues where multi-select on one screen would affect another screen
        ([#1579](https://github.com/Automattic/pocket-casts-android/pull/1579),
         [#1580](https://github.com/Automattic/pocket-casts-android/pull/1580))
    *   Fix skip forward/backward commands from Pixel Buds
        ([#1620](https://github.com/Automattic/pocket-casts-android/pull/1620))
    *   Fix tap to view action from bookmark added notification
        ([#1614](https://github.com/Automattic/pocket-casts-android/pull/1614))
    *   Fix playback effects UI not updating on watch
        ([#1643](https://github.com/Automattic/pocket-casts-android/pull/1643))
*   Updates:
    *   Support dynamic colors for widget
        ([#1588](https://github.com/Automattic/pocket-casts-android/pull/1588))
    *   Add theme support to Up Next screen
        ([#1605](https://github.com/Automattic/pocket-casts-android/pull/1605))

7.53
-----
*   Bug Fixes:
    *   Ensure we have the most up-to-date episode urls before attempting playback
        ([#1561](https://github.com/Automattic/pocket-casts-android/pull/1561))
    *   Prevent crash if database is missing date episode is published
        ([#1573](https://github.com/Automattic/pocket-casts-android/pull/1573))

7.52
-----

* New Features:
    *   Bookmarks entering Patron early access with full release for Plus users in 7.53
        ([#1526](https://github.com/Automattic/pocket-casts-android/pull/1526))
    *   Enables Playback 2023
        ([#1537](https://github.com/Automattic/pocket-casts-android/pull/1537))
*   Bug Fixes:
    *   Avoid brief audio skip back when a streaming episode is downloaded
        ([#1510](https://github.com/Automattic/pocket-casts-android/pull/1510))
    *   Fix change email handling issues
        ([#1518](https://github.com/Automattic/pocket-casts-android/pull/1518))
    *   Fix Chapter Length Calculation Mismatch
        ([#1525](https://github.com/Automattic/pocket-casts-android/pull/1525))
    *   Fix multiselect not working in some cases
        ([#1535](https://github.com/Automattic/pocket-casts-android/pull/1535))
    *   Fix occasional crash on rearranging player shelf items 
        ([#1551](https://github.com/Automattic/pocket-casts-android/pull/1551))
    
7.51
-----

*   Updates:
    *   Take user to alarm permission when the app needs permission for sleep timer
        ([#1487](https://github.com/Automattic/pocket-casts-android/pull/1487))
*   Bug Fixes:
    *    Fix Play button color not updating after changing filter color 
         ([#1470](https://github.com/Automattic/pocket-casts-android/pull/1470))
    *    Hide Plus upgrade banner for Patron users
         ([#1508](https://github.com/Automattic/pocket-casts-android/pull/1508))

7.50
-----

*   Bug Fixes:
    *   Fixed chapter progress circle on full-screen player
        ([#1461](https://github.com/Automattic/pocket-casts-android/pull/1461))
    *   Fixed show notes loading issues
        ([#1436](https://github.com/Automattic/pocket-casts-android/pull/1436))
    *   Fixed discover search bar at high device zoom
        ([#1469](https://github.com/Automattic/pocket-casts-android/pull/1469))

7.49
-----

*   Bug Fixes:
    *   Improve multiselect handling in Up Next queue
        ([#1398](https://github.com/Automattic/pocket-casts-android/pull/1390))
    *   Improve the next episode action to remove the playing episode from the Up Next
        ([#1422](https://github.com/Automattic/pocket-casts-android/pull/1422))
    *   Fixed bug where theme selection UI was not functioning as intended
        ([#1399](https://github.com/Automattic/pocket-casts-android/pull/1399))
    *   Improve RTL handling in episode search box
        ([#1405](https://github.com/Automattic/pocket-casts-android/pull/1405))
*   New Feature:
    *   Enables Patron (Internal)
        ([#1442](https://github.com/Automattic/pocket-casts-android/pull/1442))

7.48
-----

*   New Feature:
    *   Suggest episodes to play in Automotive
        ([#1362](https://github.com/Automattic/pocket-casts-android/pull/1362))
*   Bug Fixes:
    *   Avoid memory leak when opening Up Next queue
        ([#1397](https://github.com/Automattic/pocket-casts-android/pull/1397))
    *   Improved the downloading of episode show notes
        ([#1390](https://github.com/Automattic/pocket-casts-android/pull/1390))

        
7.47
-----

*   Updates:
    *   Enable copying logs from in-app logs viewer
        ([#1298](https://github.com/Automattic/pocket-casts-android/pull/1298))
    *   Updated storage limit title
        ([#1342](https://github.com/Automattic/pocket-casts-android/pull/1342))
*   Bug Fixes:
    *   Fixed auto archive settings getting lost when switching languages
        ([#1234](https://github.com/Automattic/pocket-casts-android/pull/1234))
    *   Improved handling of enabling/disabling new episode notifications
        ([#1264](https://github.com/Automattic/pocket-casts-android/pull/1264))
    *   Fixed the artwork not appearing on the onboarding page
        ([#1299](https://github.com/Automattic/pocket-casts-android/pull/1299))

7.46.2
------
*   Updates:
    *   Removed login with email and password on WearOS app
        ([#1356](https://github.com/Automattic/pocket-casts-android/pull/1356))

7.46.1
------
*   Bug Fixes:
    *   Improved multi-select toolbars setup
        ([#1338](https://github.com/Automattic/pocket-casts-android/pull/1338))

7.46
-----

*   New Feature:
    *   Added a support section to the Automotive app
        ([#1277](https://github.com/Automattic/pocket-casts-android/pull/1277))
*   Bug Fixes:
    *    Fixed episodes being removed from the Up Next when clearing downloads
         ([#1280](https://github.com/Automattic/pocket-casts-android/pull/1280))
    *    Improved upgrade flow when signing in with Google account
         ([#1275](https://github.com/Automattic/pocket-casts-android/pull/1275))
    *    Improved performance when archiving multiple episodes
         ([1327](https://github.com/Automattic/pocket-casts-android/pull/1327))
    *    Avoided adding multiple toolbar observers, which was causing the app to lag and freeze
         ([#1333](https://github.com/Automattic/pocket-casts-android/pull/1333))

7.45.1
-----
*   Bug Fixes:
    *   Fixed issue that could cause app freeze when using multiselect
        ([#1315](https://github.com/Automattic/pocket-casts-android/pull/1315))

7.45
-----
*   Bug Fixes:
    *   Added audio ducking as an option when playing over notifications
        ([#1009](https://github.com/Automattic/pocket-casts-android/pull/1009))
    *   Fixed the podcast ratings stars showing if there isn't a rating
        ([#1237](https://github.com/Automattic/pocket-casts-android/pull/1237))
    *   Fixed swiping to open Up Next queue in landscape and on foldables
        ([#1209](https://github.com/Automattic/pocket-casts-android/pull/1209))
    *   Fixed upgrade flow when signing in with Google account
        ([#1204](https://github.com/Automattic/pocket-casts-android/pull/1204))
    *   Fixed watch background refresh description to not reference auto downloads
        ([#1212](https://github.com/Automattic/pocket-casts-android/pull/1212))
    *   Fixed bug where the Add File toolbar would disappear on orientation change
        ([#1235](https://github.com/Automattic/pocket-casts-android/pull/1235))

7.44
-----
*   New Feature:
    *   Added 3 episodes on the Filter AutoDownload
        ([#1169](https://github.com/Automattic/pocket-casts-android/pull/1169))
    *   Added capability to deselect all/below and above on the multiselect feature
        ([#1172](https://github.com/Automattic/pocket-casts-android/pull/1172))
    *   Added share option to episode swipe and multiselect menus
        ([#1190](https://github.com/Automattic/pocket-casts-android/pull/1190)),
        ([#1191](https://github.com/Automattic/pocket-casts-android/pull/1191))

7.43
-----
*   New Feature:
    *   Enabled the ratings feature
        ([#1159](https://github.com/Automattic/pocket-casts-android/pull/1159)).
    *   Add setting to autoplay episodes when up next queue is not being used
        ([#1170](https://github.com/Automattic/pocket-casts-android/pull/1170)).
    *   Add capability to add +1 minute on the sleep timer
        ([#1139](https://github.com/Automattic/pocket-casts-android/pull/1139)).
    * Adds +- 1 increments to the sleep timer if the custom timer is less than 5 Min
        ([#1144](https://github.com/Automattic/pocket-casts-android/pull/1144)).
*   Bug Fixes:
    *   Fixed crash that could occur when rearranging shelf items on the full screen player
        ([#1155](https://github.com/Automattic/pocket-casts-android/pull/1155)).
    *   Fixed the acknowledgements being hidden in the Automotive about page.
        ([#1166](https://github.com/Automattic/pocket-casts-android/pull/1166)).
    *   Wear OS: Fixed blank screen when showing episode details of cloud files
        ([#1181](https://github.com/Automattic/pocket-casts-android/pull/1181)).

7.42
-----

*   New Features:
    *   Wear OS app
        ([#1068](https://github.com/Automattic/pocket-casts-android/pull/1068)).
*   Updates:
    *   Use sha256 to encode email for gravatar
        ([#1080](https://github.com/Automattic/pocket-casts-android/pull/1080)).
*   Bug Fixes:
    *   Fixed the sign in notification appearing when there was a network or server issue
        ([#1101](https://github.com/Automattic/pocket-casts-android/pull/1101)).
    *   Fixed Extra Dark theme not applying proper background on some settings screens
        ([#987](https://github.com/Automattic/pocket-casts-android/pull/987)).
    *   Fixed Automotive seek bar and playback state issues
        ([#1077](https://github.com/Automattic/pocket-casts-android/pull/1077)).

7.41
-----

* Updates:
    *   Use regular Pocket Casts app icon by default, and allow pride icon to still be selected.
        ([#1079](https://github.com/Automattic/pocket-casts-android/pull/1079)).

7.40
-----
*   Updates:
    *   Automotive now has the option to clear your data before logging in
        ([#978](https://github.com/Automattic/pocket-casts-android/pull/978)).
*   Bug Fixes:
    *   Fixed the buffering and seeking issues with some Automotive manufacturers
        ([#977](https://github.com/Automattic/pocket-casts-android/pull/977)).
    *   Improved the resolution of the Gravatar image
        ([#973](https://github.com/Automattic/pocket-casts-android/pull/973)).
    *   Improved the Automotive user profile view
        ([#975](https://github.com/Automattic/pocket-casts-android/pull/975)).

7.39
-----
*   Bug Fixes:
    *   Fixed the discover categories sorting so that it is alphabetical in the device language
        ([#942](https://github.com/Automattic/pocket-casts-android/pull/942)).
    *   Fixed Japanese translations for the 'Show played episodes' setting in Automotive.
        ([#890](https://github.com/Automattic/pocket-casts-android/issues/955)).
    *   Fixed the listening stats.
        ([#960](https://github.com/Automattic/pocket-casts-android/pull/960)).
    *   Fixed crash on grouping by season
        ([#962](https://github.com/Automattic/pocket-casts-android/pull/962)).
    *   Fixed crash that could occur when signing out of the app
        ([#971](https://github.com/Automattic/pocket-casts-android/pull/971)).
    *   Fixed bug that prevented full logs from being displayed within the app
        ([#974](https://github.com/Automattic/pocket-casts-android/pull/974)).
    *   Fixed the skip buttons not appearing in the remote player views
        ([#976](https://github.com/Automattic/pocket-casts-android/pull/976)).
*   Updates
    *   Link users to support forum from within the app
        ([#950](https://github.com/Automattic/pocket-casts-android/pull/950)).
    *   Added a new pride icon
        ([#997](https://github.com/Automattic/pocket-casts-android/pull/997)).

7.38
-----
*   Updates:
    *   Added Advanced Settings section for experimental settings
        ([#885](https://github.com/Automattic/pocket-casts-android/pull/885/)).
    *   Added setting to disable sync on metered networks
        ([#885](https://github.com/Automattic/pocket-casts-android/pull/885/)).
    *   Added ability to view and share app logs from the "Help & feedback" screen
        ([911](https://github.com/Automattic/pocket-casts-android/pull/911)).
*   Bug Fixes:
    *   Fixed accessibility content desctiption for episode list
        ([#890](https://github.com/Automattic/pocket-casts-android/issues/890)).
    *   Improved the validation of the Automotive skip forward and back time settings
        ([#890](https://github.com/Automattic/pocket-casts-android/pull/892)).
    *   Fixed the show notes' timestamps not getting converted to a link if it contained another link
        ([#814](https://github.com/Automattic/pocket-casts-android/issues/814)).
    *   Prevented crash when signing out of Android Automotive and clearing data while playback is in progress
        ([#919](https://github.com/Automattic/pocket-casts-android/pull/919)).
    *   Fixed keyboard not appearing when creating or editing folders.
        ([#946](https://github.com/Automattic/pocket-casts-android/pull/946)).


7.37.1
-----
* Bug Fixes:
    *    Fixed an issue that could cause users to be repeatedly logged out of the app.
         ([#930](https://github.com/Automattic/pocket-casts-android/pull/930)).

7.37
-----
*   New Features:
    *   Added capability to sign into Pocket Casts using Google account
        ([#878](https://github.com/Automattic/pocket-casts-android/pull/878)).

7.35
-----
*   Updates
    *   The Automotive skip forward and backward time settings were improved
        ([#817](https://github.com/Automattic/pocket-casts-android/pull/817)).
    *   Fixed Automotive Up Next podcast images not loading
        ([#819](https://github.com/Automattic/pocket-casts-android/pull/819)).
    *   Fixed missing seek bar in the notification drawer and Android Automotive
        ([#822](https://github.com/Automattic/pocket-casts-android/pull/822)).
    *   Added option to clear data when signing out of Android Automotive
        ([828](https://github.com/Automattic/pocket-casts-android/pull/828)).
    *   Podcast carousel on Discover screen now scrolls automatically
        ([818](https://github.com/Automattic/pocket-casts-android/pull/818)).

7.34
-----
*   Updates
    *   Improved the Automotive account page styles
        ([#798](https://github.com/Automattic/pocket-casts-android/pull/798)).
    *   When Automotive doesn't have a browser, the URL and a QR code are shown instead.
        ([#800](https://github.com/Automattic/pocket-casts-android/pull/800)).

7.33
-----
*   New Features:
    *    Added search history
         ([#784](https://github.com/Automattic/pocket-casts-android/pull/784)).
*   Bug Fixes:
    *    App no longer crashes when the device browser has been disabled
         ([#762](https://github.com/Automattic/pocket-casts-android/issues/762)).
    *    Improve discovery of chromecast devices
         ([#780](https://github.com/Automattic/pocket-casts-android/issues/780)).

7.32
-----
*   Bug Fixes:
    *    Ask notifications permission for newly-installed apps on Android 13
         ([#723](https://github.com/Automattic/pocket-casts-android/issues/723)).

7.31
-----
*   New Features:
    *   Add support for HLS streams
        ([#679](https://github.com/Automattic/pocket-casts-android/pull/679)).
    *   Incorporated new onboarding flow throughout the app
        ([#694](https://github.com/Automattic/pocket-casts-android/pull/694)).
*   Updates
    *   Update styling of upgrade prompt on account details screen
        ([#706](https://github.com/Automattic/pocket-casts-android/pull/706)).
    *   Updated Android 13 media notification controls setting to hide custom actions
        ([#719](https://github.com/Automattic/pocket-casts-android/pull/719)).
*   Health:
    *   Switched to using the new user login and register endpoints.
        ([#685](https://github.com/Automattic/pocket-casts-android/pull/685)).
    *   Upgraded ExoPlayer to 2.18.2
        ([#707](https://github.com/Automattic/pocket-casts-android/pull/707)).
*   Bug Fixes:
    *   Improved handling of sync errors
        ([#711](https://github.com/Automattic/pocket-casts-android/pull/711)).
    *   App does a better job respecting the device's dark/light mode settings
        ([#710](https://github.com/Automattic/pocket-casts-android/pull/710)).
    *   Make it easier to tap newsletter toggle
        ([#714](https://github.com/Automattic/pocket-casts-android/pull/714)).
    *   Improved unresponsive media notifications
        ([#709](https://github.com/Automattic/pocket-casts-android/pull/709)).

7.30
-----
*   New Features:
    *   Add tappable links to podcast description
        ([#657](https://github.com/Automattic/pocket-casts-android/pull/657)).
    *   Added new Tasker "Query Podcasts", "Query Podcast Episodes", "Query Filter", "Query Filter Episodes" and "Add To Up Next" actions.
        ([#583](https://github.com/Automattic/pocket-casts-android/pull/583)).
    *   Add categories to recommendations screen
        ([#675](https://github.com/Automattic/pocket-casts-android/pull/675)).
    *   Improved the Android Automotive search
        ([#681](https://github.com/Automattic/pocket-casts-android/pull/681)).

7.29.2
-----
* Bug Fixes:
    *    Fix text on purchase button in new onboarding flow
         ([#698](https://github.com/Automattic/pocket-casts-android/pull/698)).


7.29.1
-----
* Bug Fixes:
    *   Fixed Norwegian translations
        ([#689](https://github.com/Automattic/pocket-casts-android/pull/689)).

7.29
-----

*   New Features:
    *   Onboarding flow for new users
        ([#645](https://github.com/Automattic/pocket-casts-android/pull/645)).
    *   Display gravatar on profile screen
        ([#644](https://github.com/Automattic/pocket-casts-android/pull/644)).
    *   Remove 2022 End of Year stats flow
        ([#672](https://github.com/Automattic/pocket-casts-android/pull/672)).
*   Bug Fixes:
    *   Fixed podcast date format
        ([#477](https://github.com/Automattic/pocket-casts-android/pull/477)).
    *   Fix unable to permanently change "Skip back time" setting
        ([#632](https://github.com/Automattic/pocket-casts-android/pull/632)).
    *   Included deep link support for share links
        ([#526](https://github.com/Automattic/pocket-casts-android/pull/526)).

7.28
-----
*   Bug Fixes:
    *   Fixed effects bottomsheet flickering to the expanded state while being dragged
        ([#575](https://github.com/Automattic/pocket-casts-android/pull/575)).
    *   Fixed miniplayer play icon animation on theme change
        ([#527](https://github.com/Automattic/pocket-casts-android/pull/527)).
    *   Fixed talkback issues
        ([#630](https://github.com/Automattic/pocket-casts-android/pull/630)).
    *   Fixed skip forward/ backward buttons not showing in media notification while casting
        ([#630](https://github.com/Automattic/pocket-casts-android/pull/630)).
    *   Fix media notification controls configuration to support 3 icons
        ([#641](https://github.com/Automattic/pocket-casts-android/pull/641)).

7.27
-----

*   New Features:
    * Add End of Year stats
      ([#410](https://github.com/Automattic/pocket-casts-android/issues/410)).
    * Support Android 13 per-app language preferences
      ([#519](https://github.com/Automattic/pocket-casts-android/pull/519)).
*   Bug Fixes:
    *   Fixed some layout issues in the EpisodeFragment
        ([#459](https://github.com/Automattic/pocket-casts-android/pull/459)).
    *   Fixed RTL support for notes
        ([#514](https://github.com/Automattic/pocket-casts-android/pull/514)).
    *   Allowed customization of actions through Settings in Media Notification Control for Android 13 users.
        ([#499](https://github.com/Automattic/pocket-casts-android/pull/540)).
    *   Fixed small "Go to Podcast" icon in Landscape
        ([#547](https://github.com/Automattic/pocket-casts-android/pull/547)).
    *   Added option to open player automatically when user start playing podcast.
        ([#23](https://github.com/Automattic/pocket-casts-android/pull/550))

7.26
-----

*   New Features:
    *   Added Tasker integration with "Play Filter" and "Control Playback" actions.
        ([#431](https://github.com/Automattic/pocket-casts-android/pull/431)).
    *   Import OPML from a URL
        ([#482](https://github.com/Automattic/pocket-casts-android/pull/482)).
    *   Redesign of the fullscreen player share option
        ([#451](https://github.com/Automattic/pocket-casts-android/pull/451)).
    *   Redesign of the fullscreen player long press option
        ([#483](https://github.com/Automattic/pocket-casts-android/pull/483)).
    *   Updated select filters title & hide podcast setting filter option when applicable
        ([#494](https://github.com/Automattic/pocket-casts-android/pull/494)).
*   Bug Fixes:
    *   Fixed Help & Feedback buttons being hidden when using text zoom.
        ([#446](https://github.com/Automattic/pocket-casts-android/pull/446)).
    *   Fixed when system bar didn't disappear on full screen video player
        ([#461](https://github.com/Automattic/pocket-casts-android/pull/461)).
    *   Fixed When no podcasts were selected for a filter, change the chip to 'All Your Podcasts'
        ([#460](https://github.com/Automattic/pocket-casts-android/pull/460)).
    *   Fixed background color for screens using the compose theme
        ([#432](https://github.com/Automattic/pocket-casts-android/pull/432)).
    *   Fixed full screen video player not closing the first time in landscape mode
        ([#464](https://github.com/Automattic/pocket-casts-android/pull/464)).
    *   Added ability to set playback effects in Tasker "Control Playback" action.
        ([#509](https://github.com/Automattic/pocket-casts-android/pull/509)).
    *   Fixed crashes with Tasker plugin actions when using minified code.
        ([#543](https://github.com/Automattic/pocket-casts-android/pull/543)).
    *   Fixed auto subscribe issue.
        ([#545](https://github.com/Automattic/pocket-casts-android/pull/545)).

7.25
-----

*   Bug Fixes:
    *   Allow Discover feed collection titles to wrap.
        ([#335](https://github.com/Automattic/pocket-casts-android/pull/335)).
    *   Allow select text in show notes from episode details view.
        ([#372](https://github.com/Automattic/pocket-casts-android/pull/372)).
    *   Added Automotive OS setting to show played episodes.
        ([#389](https://github.com/Automattic/pocket-casts-android/pull/389)).
    *   Added new episode lists to Automotive OS. Starred, Listening History, and Files.
        ([#403](https://github.com/Automattic/pocket-casts-android/pull/403)).
    *   Fixed skip backwards settings
        ([#425](https://github.com/Automattic/pocket-casts-android/pull/425)).

7.24.2
-----

*   New Features:
    *   Added a Halloween icon.
        ([#415](https://github.com/Automattic/pocket-casts-android/pull/415)).

*   Bug Fixes:
    *   Add missing POST_NOTIFICATIONS permission for Android 13
        ([#330](https://github.com/Automattic/pocket-casts-android/pull/436)).

7.24
-----

*   Bug Fixes:
    *   Fix Mini Player long press mark as played button.
        ([#330](https://github.com/Automattic/pocket-casts-android/pull/330)).
    *   Fix Filters tab not always displaying the list of filters when it should.
        ([#342](https://github.com/Automattic/pocket-casts-android/pull/342)).
    *   Add a helpful alert if the user is signed out in the background.
        ([#340](https://github.com/Automattic/pocket-casts-android/pull/340)).
    *   Fix in-app logs not updating appropriately.
    *   ([#395](https://github.com/Automattic/pocket-casts-android/pull/395)).
*   Health:
    *   Migrate app to Android 13 by targeting the SDK version 33.
        ([#312](https://github.com/Automattic/pocket-casts-android/pull/312)).

7.23.0
-----

*   Bug Fixes:
    *   Fix custom video file not opening in full-screen mode.
        ([#304](https://github.com/Automattic/pocket-casts-android/pull/304)).

7.22.0
-----

*   Bug Fixes:
    *   Fix playing on Chromecast always shows buffering.
        ([#254](https://github.com/Automattic/pocket-casts-android/pull/254)).
    *   Fix Plus subscription confirm button not always working.
        ([#284](https://github.com/Automattic/pocket-casts-android/pull/284)).


7.21.1
-----

*   Bug Fixes:
    *   Fix Chromecast not taking over playback after connection.
        ([#308](https://github.com/Automattic/pocket-casts-android/pull/308)).
    *   Fix the discover artwork scaling issue.
        ([#308](https://github.com/Automattic/pocket-casts-android/pull/308)).

7.21.0
-----

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

7.20.3
-----

*   Bug Fixes:
    *   Fixes folder mapping at the time of folders full sync.
        ([#214](https://github.com/Automattic/pocket-casts-android/pull/214)).

7.20.2
-----

*   Bug Fixes:
    *   Fix OPML import.
    *   Fix podcasts and folders rearrange crash.
        ([#200](https://github.com/Automattic/pocket-casts-android/issues/200)).

7.20.1
-----

*   New Features:
    *   Add localizations for English (UK), Arabic, and Norwegian.
*   Bug Fixes:
    *   Fix an issue where the podcasts order was being changed after migrating to the latest version.

7.20.0
-----

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

7.19.2 (2022-02-11)
-----

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
