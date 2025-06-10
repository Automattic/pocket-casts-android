package au.shiftyjelly.pocketcasts.transcripts.data

sealed interface TranscriptEntry {
    data class Text(
        val value: String,
    ) : TranscriptEntry

    data class Speaker(
        val name: String,
    ) : TranscriptEntry

    companion object {
        val PreviewList = listOf(
            TranscriptEntry.Text("Lorem ipsum odor amet, consectetuer adipiscing elit."),
            TranscriptEntry.Speaker("Speaker 1"),
            TranscriptEntry.Text("Sodales sem fusce elementum commodo risus purus auctor neque."),
            TranscriptEntry.Text("Tempus leo eu aenean sed diam urna tempor. Pulvinar vivamus fringilla lacus nec metus bibendum egestas. Iaculis massa nisl malesuada lacinia integer nunc posuere."),
            TranscriptEntry.Speaker("Speaker 2"),
            TranscriptEntry.Text("Duis elementum condimentum interdum. Vivamus sollicitudin blandit luctus. In vulputate ipsum dolor, vitae lacinia augue sollicitudin vel. Phasellus eget augue odio. Cras pharetra libero et lorem laoreet varius. Mauris libero massa, dictum eu dapibus at, condimentum nec eros. Morbi varius lobortis odio a fermentum."),
            TranscriptEntry.Text("Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi."),
            TranscriptEntry.Text("Integer non vestibulum enim, nec accumsan lacus. Suspendisse consequat nec ex ac volutpat. Ut iaculis nec odio in elementum. Proin tincidunt est lectus, et posuere magna mollis at. Integer vitae ornare quam. Vivamus lobortis tortor et nunc feugiat molestie. Maecenas vel nulla consequat, porttitor elit ut, tristique diam. Aenean id lectus nec augue finibus consequat at id lorem."),
            TranscriptEntry.Text("Pulvinar vivamus fringilla lacus nec metus bibendum egestas. Iaculis massa nisl malesuada lacinia integer nunc posuere. Ut hendrerit semper vel class aptent taciti sociosqu. Ad litora torquent per conubia nostra inceptos himenaeos."),
            TranscriptEntry.Text("Quisque faucibus ex sapien vitae pellentesque sem placerat. In id cursus mi pretium tellus duis convallis."),
        )
    }
}
