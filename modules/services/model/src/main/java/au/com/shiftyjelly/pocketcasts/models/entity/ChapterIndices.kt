package au.com.shiftyjelly.pocketcasts.models.entity

data class ChapterIndices(private val delegate: List<Int> = emptyList()) : List<Int> by delegate
