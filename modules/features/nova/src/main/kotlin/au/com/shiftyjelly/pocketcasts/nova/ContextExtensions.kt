package au.com.shiftyjelly.pocketcasts.nova

import android.content.Context

internal val Context.isNovaLauncherInstalled get() = runCatching {
    packageManager.getPackageInfo("com.teslacoilsw.launcher", 0)
}.isSuccess
