package au.shiftyjelly.pocketcasts.transcripts

internal data class TranscriptEntry(
    val text: String,
    val style: Style,
) {
    enum class Style {
        Simple, Speaker,
    }

    companion object {
        val PreviewList = listOf(
            TranscriptEntry("Lorem ipsum odor amet, consectetuer adipiscing elit.", Style.Simple),
            TranscriptEntry("Speaker 1", Style.Speaker),
            TranscriptEntry("Sodales sem fusce elementum commodo risus purus auctor neque.", Style.Simple),
            TranscriptEntry("Tempus leo eu aenean sed diam urna tempor. Pulvinar vivamus fringilla lacus nec metus bibendum egestas. Iaculis massa nisl malesuada lacinia integer nunc posuere.", Style.Simple),
            TranscriptEntry("Speaker 2", Style.Speaker),
            TranscriptEntry("Duis elementum condimentum interdum. Vivamus sollicitudin blandit luctus. In vulputate ipsum dolor, vitae lacinia augue sollicitudin vel. Phasellus eget augue odio. Cras pharetra libero et lorem laoreet varius. Mauris libero massa, dictum eu dapibus at, condimentum nec eros. Morbi varius lobortis odio a fermentum.", Style.Simple),
            TranscriptEntry("Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi.", Style.Simple),
            TranscriptEntry("Integer non vestibulum enim, nec accumsan lacus. Suspendisse consequat nec ex ac volutpat. Ut iaculis nec odio in elementum. Proin tincidunt est lectus, et posuere magna mollis at. Integer vitae ornare quam. Vivamus lobortis tortor et nunc feugiat molestie. Maecenas vel nulla consequat, porttitor elit ut, tristique diam. Aenean id lectus nec augue finibus consequat at id lorem.", Style.Simple),
            TranscriptEntry("Pulvinar vivamus fringilla lacus nec metus bibendum egestas. Iaculis massa nisl malesuada lacinia integer nunc posuere. Ut hendrerit semper vel class aptent taciti sociosqu. Ad litora torquent per conubia nostra inceptos himenaeos.", Style.Simple),
            TranscriptEntry("Quisque faucibus ex sapien vitae pellentesque sem placerat. In id cursus mi pretium tellus duis convallis.", Style.Simple),
        )
    }
}
