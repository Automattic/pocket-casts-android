package au.com.shiftyjelly.pocketcasts.repositories.playback;

import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;

import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager;
import timber.log.Timber;

public class ShiftyCustomAudio {
    private static final int LOUDNESS_TARGET_GAIN = 1000;

    private boolean boostVolume;
    private float playbackSpeed = 0;

    private LoudnessEnhancer enhancer;
    private Equalizer equalizer;
    private final StatsManager statsManager;

    public ShiftyCustomAudio(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    public void setPlaybackSpeed(float playbackSpeed) {
        this.playbackSpeed = playbackSpeed;
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setBoostVolume(boolean boostVolume) {
        this.boostVolume = boostVolume;

        if (enhancer != null) {
            enhancer.setEnabled(boostVolume);
        }
        if (equalizer != null) {
            equalizer.setEnabled(boostVolume);
        }
    }

    public void setupVolumeBoost(int audioSessionId) {
        try {
            enhancer = new LoudnessEnhancer(audioSessionId);
            enhancer.setTargetGain(LOUDNESS_TARGET_GAIN);
            enhancer.setEnabled(boostVolume);

        }
        catch (Exception e) {
            //some devices don't support the loudness enhancer, they'll throw an exception when you try and create one
            //Samsung S5 and LG G3 both do this, seems to work on the Nexus devices mainly
        }

        //as a fallback, if we can't create a loudness enhancer, create an equilizer which boosts voice
        if (enhancer == null) {
            try {
                equalizer = new Equalizer(0, audioSessionId);
                short bands = equalizer.getNumberOfBands();

                for (short band = 0; band < bands; band++) {
                    int frequency = equalizer.getCenterFreq(band) / 1000;

                    if (frequency < 100) {
                        equalizer.setBandLevel(band, (short)(-5 * 100));
                    }
                    else if (frequency < 250) {
                        equalizer.setBandLevel(band, (short)0);
                    }
                    else if (frequency < 1000) {
                        equalizer.setBandLevel(band, (short)(10 * 100));
                    }
                    else if (frequency < 2000) {
                        equalizer.setBandLevel(band, (short)(12 * 100));
                    }
                    else if (frequency < 10000) {
                        equalizer.setBandLevel(band, (short)(8 * 100));
                    }
                    else {
                        equalizer.setBandLevel(band, (short)0);
                    }
                }

                equalizer.setEnabled(boostVolume);
            }
            catch (Exception e) {
                //some devices don't support the equilizer, they'll throw an exception when you try and create one
                Timber.e(e);
            }
        }
    }

    public void addSilenceSkippedTime(long timeUs) {
        statsManager.addTimeSavedSilenceRemoval(timeUs / 1000);
    }

    public void addVariableSpeedTime(long timeUs, float speed) {
        statsManager.addTimeSavedVariableSpeed((long) (((timeUs * speed) - timeUs) / 1000));
    }
}
