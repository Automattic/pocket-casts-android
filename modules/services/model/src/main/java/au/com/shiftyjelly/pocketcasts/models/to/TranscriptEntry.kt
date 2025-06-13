package au.com.shiftyjelly.pocketcasts.models.to

sealed interface TranscriptEntry {
    data class Text(
        val value: String,
    ) : TranscriptEntry

    data class Speaker(
        val name: String,
    ) : TranscriptEntry

    companion object {
        val PreviewList = listOf(
            Text("Lorem ipsum odor amet, lorem consectetuer adipiscing elit."),
            Speaker("Speaker Lorem 1"),
            Text("Sodales sem fusce elementum commodo risus purus auctor neque."),
            Text("Tempus leo eu aenean lorem sed diam urna tempor. Pulvinar vivamus fringilla lacus nec metus bibendum egestas. Iaculis massa nisl malesuada lacinia integer nunc posuere."),
            Speaker("Speaker 2"),
            Text("Duis elementum condimentum interdum. Vivamus sollicitudin blandit luctus. In vulputate ipsum dolor, vitae lacinia augue sollicitudin vel. Phasellus eget augue odio. Cras pharetra libero et lorem laoreet varius. Mauris libero massa, dictum eu dapibus at, condimentum nec eros. Morbi varius lobortis odio a fermentum."),
            Text("Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi."),
            Text("Integer non vestibulum enim, nec accumsan lacus. Suspendisse consequat nec ex ac volutpat. Ut iaculis nec odio in elementum. Proin tincidunt est lectus, et posuere magna mollis at. Integer vitae ornare quam. Vivamus lobortis tortor et nunc feugiat molestie. Maecenas vel nulla consequat, porttitor elit ut, tristique diam. Aenean id lectus nec augue finibus consequat at id lorem."),
            Text("Pulvinar vivamus fringilla lacus nec metus bibendum egestas. Iaculis massa nisl malesuada lacinia integer nunc posuere. Ut hendrerit semper vel class aptent taciti sociosqu. Ad litora torquent per conubia nostra inceptos himenaeos."),
            Text("Quisque faucibus ex sapien vitae pellentesque sem placerat. In id cursus mi pretium tellus duis convallis."),
        )
    }
}
