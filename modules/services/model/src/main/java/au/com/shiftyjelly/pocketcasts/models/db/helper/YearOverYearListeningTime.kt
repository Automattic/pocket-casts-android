package au.com.shiftyjelly.pocketcasts.models.db.helper

data class YearOverYearListeningTime(
    val totalPlayedTimeLastYear: Long,
    val totalPlayedTimeThisYear: Long,
) {
    private val nonRoundedPercentage: Double = if (totalPlayedTimeLastYear == 0L) {
        Double.POSITIVE_INFINITY
    } else {
        ((100.0 * totalPlayedTimeThisYear) / totalPlayedTimeLastYear) - 100
    }
    val percentage: Double = ((nonRoundedPercentage * 100) / 100)
    val formattedPercentage = String.format("%.0f", percentage)
}
