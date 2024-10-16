package au.com.shiftyjelly.pocketcasts.models.to

data class RatingStats(
    val ones: Int,
    val twos: Int,
    val threes: Int,
    val fours: Int,
    val fives: Int,
) {
    val onesRelative get() = relativeToMax(ones)
    val twosRelative get() = relativeToMax(twos)
    val threesRelative get() = relativeToMax(threes)
    val foursRelative get() = relativeToMax(fours)
    val fivesRelative get() = relativeToMax(fives)

    private val max = maxOf(ones, twos, threes, fours, fives)

    private fun relativeToMax(value: Int) = if (max == 0) 0f else (value.toFloat() / max)
}
