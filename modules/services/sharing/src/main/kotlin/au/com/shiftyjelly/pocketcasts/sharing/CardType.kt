package au.com.shiftyjelly.pocketcasts.sharing

sealed interface CardType {
    data object Vertical : VisualCardType {
        override val aspectRatio = 1.5f
    }
    data object Horizontal : VisualCardType {
        override val aspectRatio = 0.52f
    }
    data object Square : VisualCardType {
        override val aspectRatio = 1f
    }
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

sealed interface VisualCardType : CardType {
    val aspectRatio: Float
}
