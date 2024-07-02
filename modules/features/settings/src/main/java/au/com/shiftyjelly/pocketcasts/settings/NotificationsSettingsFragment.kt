package au.com.shiftyjelly.pocketcasts.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreference
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.NotificationSound
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.NewEpisodeNotificationAction
import au.com.shiftyjelly.pocketcasts.preferences.model.NotificationVibrateSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.extensions.findToolbar
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragmentSource
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.MultiChoiceListener
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.afollestad.materialdialogs.list.updateListItemsMultiChoice
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class NotificationsSettingsFragment :
    PreferenceFragmentCompat(),
    PodcastSelectFragment.Listener,
    CoroutineScope,
    HasBackstack {

    companion object {
        const val REQUEST_CODE_ALERT_RINGTONE = 16
    }

    @Inject lateinit var podcastManager: PodcastManager

    @Inject lateinit var settings: Settings

    @Inject lateinit var notificationHelper: NotificationHelper

    @Inject lateinit var theme: Theme

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    private var screen: PreferenceScreen? = null
    private var notificationPodcasts: PreferenceScreen? = null
    private var ringtonePreference: Preference? = null
    private var vibratePreference: ListPreference? = null
    private var enabledPreference: SwitchPreference? = null
    private var systemSettingsPreference: Preference? = null
    private var notificationActions: PreferenceScreen? = null
    private var playOverNotificationPreference: ListPreference? = null
    private var hidePlaybackNotificationsPreference: SwitchPreference? = null

    private val toolbar
        get() = view?.findViewById<Toolbar>(R.id.toolbar)

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findToolbar().setup(title = getString(LR.string.settings_title_notifications), navigationIcon = BackArrow, activity = activity, theme = theme)
        analyticsTracker.track(AnalyticsEvent.SETTINGS_NOTIFICATIONS_SHOWN)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.bottomInset.collect {
                    view.updatePadding(bottom = it)
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_notifications, rootKey)

        val manager = preferenceManager
        screen = manager.findPreference("episodeNotificationsScreen")
        enabledPreference = manager.findPreference("episodeNotificationsOn")
        notificationPodcasts = manager.findPreference("notificationPodcasts")
        ringtonePreference = manager.findPreference("notificationRingtone")
        vibratePreference = manager.findPreference("notificationVibrate")
        notificationActions = manager.findPreference("notificationActions")
        systemSettingsPreference = manager.findPreference("openSystemSettings")
        playOverNotificationPreference = manager.findPreference("overrideNotificationAudio")
        hidePlaybackNotificationsPreference = manager.findPreference("hideNotificationOnPause")

        // turn preferences off by default, because they are enable async, we don't want this view to remove them from the screen after it loads as it looks jarring
        enabledPreferences(false)

        // add a listener for this preference if the SDK we're on supports it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            systemSettingsPreference?.setOnPreferenceClickListener {
                analyticsTracker.track(AnalyticsEvent.SETTINGS_NOTIFICATIONS_ADVANCED_SETTINGS_TAPPED)
                notificationHelper.openEpisodeNotificationSettings(activity)
                true
            }
        }

        hidePlaybackNotificationsPreference?.setOnPreferenceChangeListener { _, newValue ->
            val newBool = (newValue as? Boolean) ?: throw IllegalStateException("Invalid value for hide notification on pause preference: $newValue")
            settings.hideNotificationOnPause.set(newBool, updateModifiedAt = true)
            analyticsTracker.track(
                AnalyticsEvent.SETTINGS_NOTIFICATIONS_HIDE_PLAYBACK_NOTIFICATION_ON_PAUSE,
                mapOf("enabled" to newBool),
            )
            true
        }

        vibratePreference?.setOnPreferenceChangeListener { _, newValue ->
            val newSetting = (newValue as? String)?.let {
                NotificationVibrateSetting.values().find { it.intValue.toString() == newValue }
            }
            settings.notificationVibrate.set(
                value = newSetting ?: NotificationVibrateSetting.DEFAULT,
                updateModifiedAt = false,
            )
            changeVibrateSummary()
            analyticsTracker.track(
                AnalyticsEvent.SETTINGS_NOTIFICATIONS_VIBRATION_CHANGED,
                mapOf("value" to (newSetting?.analyticsString ?: "unknown")),
            )
            true
        }
        playOverNotificationPreference?.setOnPreferenceChangeListener { _, newValue ->
            val playOverNotificationSetting = (newValue as? String)
                ?.let { PlayOverNotificationSetting.fromPreferenceString(it) }
                ?: throw IllegalStateException("Invalid value for play over notification preference: $newValue")

            settings.playOverNotification.set(playOverNotificationSetting, updateModifiedAt = true)
            changePlayOverNotificationSummary()

            analyticsTracker.track(
                AnalyticsEvent.SETTINGS_NOTIFICATIONS_PLAY_OVER_NOTIFICATIONS_TOGGLED,
                mapOf(
                    "enabled" to (playOverNotificationSetting != PlayOverNotificationSetting.NEVER),
                    "value" to playOverNotificationSetting.analyticsString,
                ),
            )

            true
        }
        changePodcastsSummary()
    }

    private fun openSelectPodcasts() {
        val fragment = PodcastSelectFragment.newInstance(source = PodcastSelectFragmentSource.NOTIFICATIONS)
        childFragmentManager.beginTransaction()
            .replace(UR.id.frameChildFragment, fragment)
            .addToBackStack("podcastSelect")
            .commit()
        toolbar?.title = getString(LR.string.settings_select_podcasts)
    }

    override fun podcastSelectFragmentSelectionChanged(newSelection: List<String>) {
        launch(Dispatchers.Default) {
            podcastManager.findSubscribed().forEach {
                podcastManager.updateShowNotifications(it, newSelection.contains(it.uuid))
            }
        }
    }

    override fun podcastSelectFragmentGetCurrentSelection(): List<String> {
        return runBlocking {
            async(Dispatchers.Default) {
                val uuids = podcastManager.findSubscribed().filter { it.isShowNotifications }.map { it.uuid }
                uuids
            }.await()
        }
    }

    override fun onBackPressed(): Boolean {
        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStack()
            toolbar?.title = getString(LR.string.settings_title_notifications)
            return true
        }

        return false
    }

    @Suppress("DEPRECATION")
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val ringtoneKey = ringtonePreference?.key
        if (preference.key == ringtoneKey) {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)

            val existingValue = settings.notificationSound.value.path
            // Select "Silent" if empty
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, if (existingValue.isEmpty()) null else Uri.parse(existingValue))

            startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE)
            return true
        } else {
            return super.onPreferenceTreeClick(preference)
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_ALERT_RINGTONE && data != null) {
            val ringtone = data.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            val value = ringtone?.toString() ?: ""
            context?.let {
                settings.notificationSound.set(NotificationSound(value, it), updateModifiedAt = false)
                ringtonePreference?.summary = getRingtoneValue(value)
                analyticsTracker.track(AnalyticsEvent.SETTINGS_NOTIFICATIONS_SOUND_CHANGED)
            } ?: Timber.e("Context was null when trying to set notification sound")
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun setupActions() {
        changeActionsSummary()
        notificationActions?.setOnPreferenceClickListener {
            val initialActions = settings.newEpisodeNotificationActions.value
            val selectedActions = initialActions.toMutableList()
            val initialSelection = selectedActions.map(NewEpisodeNotificationAction::ordinal).toIntArray()
            val onSelect: MultiChoiceListener = { dialog, _, items ->
                selectedActions.clear()
                selectedActions.addAll(NewEpisodeNotificationAction.fromLabels(items.map { it.toString() }, resources))
                changeActionsDialog(selectedActions, dialog)
            }

            activity?.let { activity ->
                val dialog = MaterialDialog(activity)
                    .listItemsMultiChoice(
                        items = NewEpisodeNotificationAction.labels(resources),
                        waitForPositiveButton = false,
                        allowEmptySelection = true,
                        initialSelection = initialSelection,
                        selection = onSelect,
                    )
                    .show {
                        title(res = LR.string.settings_notification_actions_title)
                        positiveButton(
                            res = LR.string.ok,
                            click = {
                                val madeChange = initialActions != selectedActions
                                if (madeChange) {
                                    trackActionsChange(selectedActions)
                                }
                                settings.newEpisodeNotificationActions.set(selectedActions, updateModifiedAt = true)
                                changeActionsSummary()
                            },
                        )
                        negativeButton(res = LR.string.cancel)
                    }
                changeActionsDialog(selectedActions, dialog)
            }

            true
        }
    }

    private fun trackActionsChange(selectedActions: MutableList<NewEpisodeNotificationAction>) {
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_NOTIFICATIONS_ACTIONS_CHANGED,
            mapOf(
                "action_archive" to selectedActions.contains(NewEpisodeNotificationAction.Archive),
                "action_download" to selectedActions.contains(NewEpisodeNotificationAction.Download),
                "action_play" to selectedActions.contains(NewEpisodeNotificationAction.Play),
                "action_play_next" to selectedActions.contains(NewEpisodeNotificationAction.PlayNext),
                "action_play_last" to selectedActions.contains(NewEpisodeNotificationAction.PlayLast),
            ),
        )
    }

    private fun changeActionsSummary() {
        val userActions = settings.newEpisodeNotificationActions.value
        val actionStrings = userActions.joinToString { resources.getString(it.labelId) }
        notificationActions?.summary = if (userActions.isEmpty()) resources.getString(LR.string.none) else actionStrings
    }

    private fun changeActionsDialog(actions: MutableList<NewEpisodeNotificationAction>, dialog: MaterialDialog) {
        val onSelect: MultiChoiceListener = { dialogSelected, _, items ->
            actions.clear()
            actions.addAll(NewEpisodeNotificationAction.fromLabels(items.map { it.toString() }, resources))
            changeActionsDialog(actions, dialogSelected)
        }

        if (actions.size < 3) {
            dialog.updateListItemsMultiChoice(items = NewEpisodeNotificationAction.labels(resources), disabledIndices = intArrayOf(), selection = onSelect)
        } else {
            val disabled = NewEpisodeNotificationAction.entries.filter { it !in actions }.map(NewEpisodeNotificationAction::ordinal)
            dialog.updateListItemsMultiChoice(items = NewEpisodeNotificationAction.labels(resources), disabledIndices = disabled.toIntArray(), selection = onSelect)
        }
    }

    private fun enabledPreferences(enabled: Boolean) {
        val notificationPodcasts = notificationPodcasts ?: return
        val notificationActions = notificationActions ?: return
        val systemSettingsPreference = systemSettingsPreference ?: return
        val ringtonePreference = ringtonePreference ?: return
        val vibratePreference = vibratePreference ?: return
        val category = findPreference<PreferenceCategory>("new_episodes_category") ?: return
        if (enabled) {
            if (findPreference<PreferenceScreen>("notificationPodcasts") == null) {
                category.addPreference(notificationPodcasts)
            }
            if (findPreference<PreferenceScreen>("notificationActions") == null) {
                category.addPreference(notificationActions)
            }
            if (findPreference<Preference>("openSystemSettings") == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                category.addPreference(systemSettingsPreference)
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                if (findPreference<Preference>("notificationRingtone") == null) {
                    category.addPreference(ringtonePreference)
                }
                if (findPreference<ListPreference>("notificationVibrate") == null) {
                    category.addPreference(vibratePreference)
                }
            }
        } else {
            category.removePreference(notificationPodcasts)
            category.removePreference(ringtonePreference)
            category.removePreference(vibratePreference)
            category.removePreference(notificationActions)
            category.removePreference(systemSettingsPreference)
        }
    }

    private fun changePodcastsSummary() {
        lifecycleScope.launch(Dispatchers.Default) {
            podcastManager.findSubscribedFlow().collect { podcasts ->
                val podcastCount = podcasts.size
                val notificationCount = podcasts.count { it.isShowNotifications }

                val summary = when {
                    notificationCount == 0 -> resources.getString(LR.string.settings_podcasts_selected_zero)
                    notificationCount == 1 -> resources.getString(LR.string.settings_podcasts_selected_one)
                    notificationCount >= podcastCount -> resources.getString(LR.string.settings_podcasts_selected_all)
                    else -> resources.getString(
                        LR.string.settings_podcasts_selected_x,
                        notificationCount,
                    )
                }
                launch(Dispatchers.Main) {
                    notificationPodcasts?.summary = summary
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupEnabledNotifications()
        setupNotificationVibrate()
        setupHidePlaybackNotifications()
        setupPlayOverNotifications()
    }

    private fun changeVibrateSummary() {
        vibratePreference?.summary = getString(settings.notificationVibrate.value.summary)
    }

    private fun getRingtoneValue(ringtonePath: String): String {
        if (ringtonePath.isNullOrBlank()) {
            return getString(LR.string.settings_notification_silent)
        }
        val ringtone = RingtoneManager.getRingtone(activity, Uri.parse(ringtonePath))

        return when (ringtone) {
            null -> ""
            else -> {
                val title = ringtone.getTitle(activity)
                if (title == NotificationSound.defaultPath) {
                    getString(LR.string.settings_notification_default_sound)
                } else {
                    title
                }
            }
        }
    }

    private fun changeNotificationSoundSummary() {
        ringtonePreference?.let {
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                preference.summary = getRingtoneValue(newValue as String)
                true
            }
            settings.notificationSound.value.path.let { notificationSoundPath ->
                it.setDefaultValue(notificationSoundPath)
                it.summary = getRingtoneValue(notificationSoundPath)
            }
        }
    }

    private fun setupEnabledNotifications() {
        launch(Dispatchers.Default) {
            val enabled = settings.notifyRefreshPodcast.flow.value

            launch(Dispatchers.Main) {
                enabledPreference?.isChecked = enabled
                enabledPreferences(enabled)

                enabledPreference?.setOnPreferenceChangeListener { _, newValue ->
                    val checked = newValue as Boolean
                    settings.notifyRefreshPodcast.set(checked, updateModifiedAt = true)

                    analyticsTracker.track(
                        AnalyticsEvent.SETTINGS_NOTIFICATIONS_NEW_EPISODES_TOGGLED,
                        mapOf("enabled" to checked),
                    )

                    lifecycleScope.launch {
                        podcastManager.updateAllShowNotifications(checked)
                        // Don't change the podcasts summary until after the podcasts have been updated
                        changePodcastsSummary()
                    }
                    if (checked) {
                        settings.setNotificationLastSeenToNow()
                    }
                    enabledPreferences(checked)

                    true
                }

                notificationPodcasts?.setOnPreferenceClickListener {
                    openSelectPodcasts()
                    true
                }

                changePodcastsSummary()
                changeVibrateSummary()
                changeNotificationSoundSummary()
                setupActions()
            }
        }
    }

    private fun setupNotificationVibrate() {
        vibratePreference?.let {
            val options = arrayOf(
                NotificationVibrateSetting.NewEpisodes,
                NotificationVibrateSetting.OnlyWhenSilent,
                NotificationVibrateSetting.Never,
            )
            it.entries = options
                .map { getString(it.summary) }
                .toTypedArray()
            it.entryValues = options.map {
                it.intValue.toString()
            }.toTypedArray()
            it.value = settings.notificationVibrate.value.intValue.toString()
        }
    }

    private fun setupHidePlaybackNotifications() {
        hidePlaybackNotificationsPreference?.isChecked = settings.hideNotificationOnPause.value
    }

    private fun setupPlayOverNotifications() {
        playOverNotificationPreference?.apply {
            val options = listOf(
                PlayOverNotificationSetting.NEVER,
                PlayOverNotificationSetting.DUCK,
                PlayOverNotificationSetting.ALWAYS,
            )
            entries = options.map { getString(it.titleRes) }.toTypedArray()
            entryValues = options.map { it.preferenceInt.toString() }.toTypedArray()
            value = settings.playOverNotification.value.preferenceInt.toString()
        }
        changePlayOverNotificationSummary()
    }

    private fun changePlayOverNotificationSummary() {
        playOverNotificationPreference?.summary = getString(settings.playOverNotification.value.titleRes)
    }

    override fun getBackstackCount(): Int {
        return childFragmentManager.backStackEntryCount
    }
}
