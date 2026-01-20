@file:OptIn(ExperimentalCoroutinesApi::class)

package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.media.AudioManager
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class FocusManagerTest {

    private val context: Context = mock()
    private val playOverNotificationSetting: UserSetting<PlayOverNotificationSetting> = mock {
        on { value } doReturn PlayOverNotificationSetting.NEVER
    }
    private val settings: Settings = mock {
        on { playOverNotification } doReturn playOverNotificationSetting
    }

    @Test
    fun `audio focus regained within 200ms should not pause playback`() = runTest {
        val listener = mock<FocusManager.FocusChangeListener>()
        val focusManager = FocusManager(context, settings, listener, this)

        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

        advanceTimeBy(100)
        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        advanceUntilIdle()

        verify(listener, never()).onFocusLoss(any(), any())
        verify(listener, times(2)).onFocusGain(any())
    }

    @Test
    fun `audio focus lost for more than 200ms should pause playback`() = runTest {
        val listener = mock<FocusManager.FocusChangeListener>()
        val focusManager = FocusManager(context, settings, listener, this)

        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

        advanceTimeBy(250)
        advanceUntilIdle()

        verify(listener, times(1)).onFocusLoss(eq(PlayOverNotificationSetting.NEVER), eq(true))
    }

    @Test
    fun `audio focus lost for exactly 200ms should pause playback`() = runTest {
        val listener = mock<FocusManager.FocusChangeListener>()
        val focusManager = FocusManager(context, settings, listener, this)

        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

        advanceTimeBy(200)
        advanceUntilIdle()

        verify(listener, times(1)).onFocusLoss(eq(PlayOverNotificationSetting.NEVER), eq(true))
    }

    @Test
    fun `multiple rapid focus loss and gain should cancel previous jobs`() = runTest {
        val listener = mock<FocusManager.FocusChangeListener>()
        val focusManager = FocusManager(context, settings, listener, this)

        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        advanceTimeBy(50)

        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        advanceTimeBy(50)

        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        advanceTimeBy(50)

        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        advanceUntilIdle()

        verify(listener, never()).onFocusLoss(any(), any())
        verify(listener, times(3)).onFocusGain(any())
    }

    @Test
    fun `non-duck audio focus loss should pause immediately`() = runTest {
        val listener = mock<FocusManager.FocusChangeListener>()
        val focusManager = FocusManager(context, settings, listener, this)

        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)

        verify(listener, times(1)).onFocusLoss(eq(PlayOverNotificationSetting.NEVER), eq(true))
    }

    @Test
    fun `permanent audio focus loss should pause immediately`() = runTest {
        val listener = mock<FocusManager.FocusChangeListener>()
        val focusManager = FocusManager(context, settings, listener, this)

        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)

        verify(listener, times(1)).onFocusLoss(eq(PlayOverNotificationSetting.NEVER), eq(false))
    }

    @Test
    fun `duck focus loss with duck setting enabled should pass correct value`() = runTest {
        val duckSetting: UserSetting<PlayOverNotificationSetting> = mock {
            on { value } doReturn PlayOverNotificationSetting.DUCK
        }
        val settingsWithDuck: Settings = mock {
            on { playOverNotification } doReturn duckSetting
        }
        val listener = mock<FocusManager.FocusChangeListener>()
        val focusManager = FocusManager(context, settingsWithDuck, listener, this)

        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

        advanceTimeBy(250)
        advanceUntilIdle()

        verify(listener, times(1)).onFocusLoss(eq(PlayOverNotificationSetting.DUCK), eq(true))
    }
}
