package au.com.shiftyjelly.pocketcasts.discover

import androidx.compose.runtime.saveable.listSaver

data class TvOpenedCategory(val id: Int, val name: String, val source: String)

val TvOpenedCategorySaver = listSaver<TvOpenedCategory?, String>(
    save = { category -> category?.let { listOf(it.id.toString(), it.name, it.source) }.orEmpty() },
    restore = { saved ->
        saved.takeIf { it.size == 3 }?.let { (id, name, source) ->
            id.toIntOrNull()?.let { TvOpenedCategory(it, name, source) }
        }
    },
)
