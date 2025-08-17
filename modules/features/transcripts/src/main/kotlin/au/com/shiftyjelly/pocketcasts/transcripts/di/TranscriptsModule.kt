package au.com.shiftyjelly.pocketcasts.transcripts.di

import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.transcripts.TranscriptSharingClient
import au.com.shiftyjelly.pocketcasts.transcripts.asTranscriptClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object TranscriptsModule {
    @Provides
    fun transcriptSharingClient(client: SharingClient): TranscriptSharingClient = client.asTranscriptClient()
}
