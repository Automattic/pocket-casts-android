package au.com.shiftyjelly.pocketcasts.nova

import android.content.Context
import au.com.shiftyjelly.pocketcasts.utils.getPackageInfo

internal val Context.isNovaLauncherInstalled get() = getPackageInfo("com.teslacoilsw.launcher") != null
