package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolder
import kotlin.random.Random
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.Folder as SuggestedFolderModel

fun List<SuggestedFolder>.toFolders(): List<SuggestedFolderModel> {
    val grouped = this.groupBy { it.name }

    return grouped.map { (folderName, folderItems) ->
        SuggestedFolderModel(
            name = folderName,
            podcasts = folderItems.map { it.podcastUuid },
            color = Random.nextInt(0, 12),
        )
    }
}
