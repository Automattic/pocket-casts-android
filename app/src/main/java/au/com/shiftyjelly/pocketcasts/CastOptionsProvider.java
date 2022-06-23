package au.com.shiftyjelly.pocketcasts;

import android.content.Context;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.MediaIntentReceiver;
import com.google.android.gms.cast.framework.media.NotificationOptions;

import java.util.ArrayList;
import java.util.List;

import au.com.shiftyjelly.pocketcasts.preferences.Settings;
import au.com.shiftyjelly.pocketcasts.ui.MainActivity;

public class CastOptionsProvider implements OptionsProvider {

    @Override
    public CastOptions getCastOptions(Context context) {
        PocketcastsApplication application = (PocketcastsApplication) context;

        List<String> buttonActions = new ArrayList<>();
        buttonActions.add(MediaIntentReceiver.ACTION_REWIND);
        buttonActions.add(MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK);
        buttonActions.add(MediaIntentReceiver.ACTION_FORWARD);
        buttonActions.add(MediaIntentReceiver.ACTION_STOP_CASTING);

        int[] compatButtonActionsIndices = new int[]{ 0, 1, 2 };

        NotificationOptions notificationOptions = new NotificationOptions.Builder()
                .setActions(buttonActions, compatButtonActionsIndices)
                .setSkipStepMs(application.settings.getSkipForwardInMs())
                .setTargetActivityClassName(MainActivity.class.getName())
                .build();

        CastMediaOptions mediaOptions = new CastMediaOptions.Builder()
                .setNotificationOptions(notificationOptions)
                .build();
        return new CastOptions.Builder()
                .setReceiverApplicationId(Settings.CHROME_CAST_APP_ID)
                .setCastMediaOptions(mediaOptions)
                .build();
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context appContext) {
        return null;
    }

}
