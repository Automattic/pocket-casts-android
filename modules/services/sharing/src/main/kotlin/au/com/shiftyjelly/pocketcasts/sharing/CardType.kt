package au.com.shiftyjelly.pocketcasts.sharing

import com.automattic.eventhorizon.ShareActionCardType

sealed interface CardType {
    val eventHorizonValue: ShareActionCardType

    data object Vertical : VisualCardType {
        override val aspectRatio = 1.5f
        override val eventHorizonValue get() = ShareActionCardType.Vertical
    }
    data object Horizontal : VisualCardType {
        override val aspectRatio = 0.52f
        override val eventHorizonValue get() = ShareActionCardType.Horizontal
    }
    data object Square : VisualCardType {
        override val aspectRatio = 1f
        override val eventHorizonValue get() = ShareActionCardType.Square
    }
    data object Audio : CardType {
        override val eventHorizonValue get() = ShareActionCardType.Audio
    }

    companion object {
        val entries by lazy(LazyThreadSafetyMode.NONE) {
            listOf(
                Vertical,
                Horizontal,
                Square,
                Audio,
            )
        }

        val visualEntries by lazy(LazyThreadSafetyMode.NONE) {
            listOf(
                Vertical,
                Horizontal,
                Square,
            )
        }
    }
}

sealed interface VisualCardType : CardType {
    val aspectRatio: Float
}
