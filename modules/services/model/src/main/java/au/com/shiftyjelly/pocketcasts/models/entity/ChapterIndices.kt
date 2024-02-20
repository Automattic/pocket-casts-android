package au.com.shiftyjelly.pocketcasts.models.entity

data class ChapterIndices(private val delegate: List<Int> = emptyList()) : List<Int> by delegate {
    companion object {
        fun fromString(value: String?): ChapterIndices {
            val list = value?.split(",")?.map { it.toInt() } ?: emptyList()
            return ChapterIndices(list)
        }

        fun toString(indices: ChapterIndices) = indices.takeIf { it.isNotEmpty() }?.joinToString(",")
    }
}
