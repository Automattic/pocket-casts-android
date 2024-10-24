package au.com.shiftyjelly.pocketcasts.models.to

data class RatingStats(
    private val ones: Int,
    private val twos: Int,
    private val threes: Int,
    private val fours: Int,
    private val fives: Int,
) {
    private val max = maxOf(ones, twos, threes, fours, fives)

    fun count() = ones + twos + threes + fours + fives

    fun count(rating: Rating) = when (rating) {
        Rating.One -> ones
        Rating.Two -> twos
        Rating.Three -> threes
        Rating.Four -> fours
        Rating.Five -> fives
    }

    fun max() = when (max) {
        fives -> Rating.Five
        fours -> Rating.Four
        threes -> Rating.Three
        twos -> Rating.Two
        ones -> Rating.One
        else -> error("Unexpected max value: $max")
    } to max

    fun relativeToMax(rating: Rating) = when (rating) {
        Rating.One -> relativeToMax(ones)
        Rating.Two -> relativeToMax(twos)
        Rating.Three -> relativeToMax(threes)
        Rating.Four -> relativeToMax(fours)
        Rating.Five -> relativeToMax(fives)
    }

    private fun relativeToMax(value: Int) = if (max == 0) 0f else (value.toFloat() / max)
}

enum class Rating(
    val numericalValue: Int,
) {
    One(1),
    Two(2),
    Three(3),
    Four(4),
    Five(5),
}
