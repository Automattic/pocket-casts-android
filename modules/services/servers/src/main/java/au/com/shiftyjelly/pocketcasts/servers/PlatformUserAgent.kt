package au.com.shiftyjelly.pocketcasts.servers

import android.os.Build

internal object PlatformUserAgent {
    private const val DEFAULT_VM_VERSION = "2.1.0"
    const val FALLBACK = "Dalvik/$DEFAULT_VM_VERSION (Linux; U; Android)"

    val value = create(
        vmVersion = System.getProperty("java.vm.version"),
        androidRelease = Build.VERSION.RELEASE,
    )

    fun create(vmVersion: String?, androidRelease: String?): String {
        val vm = vmVersion?.sanitized()?.takeIf { it.isNotEmpty() } ?: DEFAULT_VM_VERSION
        val release = androidRelease?.sanitized()?.takeIf { it.isNotEmpty() }
        return if (release != null) "Dalvik/$vm (Linux; U; Android $release)" else "Dalvik/$vm (Linux; U; Android)"
    }

    private fun String.sanitized() = filter { it in '!'..'~' && it != '(' && it != ')' && it != ';' }
}
