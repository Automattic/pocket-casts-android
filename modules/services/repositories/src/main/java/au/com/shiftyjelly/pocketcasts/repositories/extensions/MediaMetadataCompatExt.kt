package au.com.shiftyjelly.pocketcasts.repositories.extensions

import android.support.v4.media.MediaMetadataCompat

inline val MediaMetadataCompat.id get() = getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) ?: ""
