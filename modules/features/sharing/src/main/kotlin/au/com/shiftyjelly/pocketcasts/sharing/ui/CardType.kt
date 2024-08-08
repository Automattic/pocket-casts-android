package au.com.shiftyjelly.pocketcasts.sharing.ui

sealed interface CardType {
    data object Vertical : VisualCardType
    data object Horizontal : VisualCardType
    data object Square : VisualCardType
    data object Audio : CardType

    companion object {
        val entires by lazy(LazyThreadSafetyMode.NONE) {
            listOf(
                CardType.Vertical,
                CardType.Horizontal,
                CardType.Square,
                CardType.Audio,
            )
        }

        val visualEntries by lazy(LazyThreadSafetyMode.NONE) {
            listOf(
                CardType.Vertical,
                CardType.Horizontal,
                CardType.Square,
            )
        }
    }
}

sealed interface VisualCardType : CardType
