package au.com.shiftyjelly.pocketcasts.servers.sync.old

import io.reactivex.Single
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface SyncOldServer {

    @FormUrlEncoded
    @POST("/sync/update")
    fun syncUpdate(@FieldMap fields: Map<String, String>): Single<SyncUpdateResponse>
}
