package au.com.shiftyjelly.pocketcasts.endofyear

public class PublicWithTests {
    fun print() = "Test"
}

public class PublicWithoutTests {
    fun print() = "Test"
}

internal class InternalWithTests {
    fun print() = "Test"
}

internal class InternalWithoutTests {
    fun print() = "Test"
}

private class PrivateType {
    fun print() = "Test"
}

enum class MyStateClass {
    Done,
    DoneGoToDiscover,
    DoneShowPlusPromotion,
    DoneShowWelcomeInReferralFlow,
}

data class MyOtherData(
    @DrawableRes val imageResId: Int,
    @StringRes val contentDescriptionResourceId: Int,
    @StringRes val titleResourceId: Int,
    @StringRes val descriptionResourceId: Int,
    val imageHeight: Dp,
    val imageTopPadding: Dp,
)
