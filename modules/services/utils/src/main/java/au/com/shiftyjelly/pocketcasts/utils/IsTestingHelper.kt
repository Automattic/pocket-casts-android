package au.com.shiftyjelly.pocketcasts.utils

val IS_RUNNING_UNDER_TEST: Boolean by lazy {
    try {
        Class.forName("androidx.test.espresso.Espresso")
        true
    } catch (e: ClassNotFoundException) {
        false
    }
}
