package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class TranscriptSanitizationTest {
    class GeneralBehavior {
        @Test
        fun `do not modify clean transcript`() {
            val input = buildTranscript {
                speaker("Speaker 1")
                text("Text? And more text text.")
                text("Text!")
                speaker("Sepaker 2")
                text("Text…")
            }

            val output = input.sanitize()

            assertEquals(input, output)
        }

        @Test
        fun `trim white space`() {
            val input = buildTranscript {
                speaker("\n\t Speaker \t\n")
                text("\n\t Text. \t\n")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    speaker("Speaker")
                    text("Text.")
                },
                output,
            )
        }

        @Test
        fun `compact inner white space`() {
            val input = buildTranscript {
                speaker("Speaker  with\n\n\twhite \n space")
                text("Text  with     multiple\t empty\t\tspaces.")
                text("Text\n\n\nwith\n\n\n\n\nmultiple new lines.")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    speaker("Speaker with white space")
                    text("Text with multiple empty spaces.")
                    text("Text\n\nwith\n\nmultiple new lines.")
                },
                output,
            )
        }

        @Test
        fun `remove empty entries`() {
            val input = buildTranscript {
                text("Text 1.")
                text("")
                speaker("Speaker")
                speaker("")
                text("Text 2.")
                text("")
                text("")
                speaker("")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("Text 1.")
                    speaker("Speaker")
                    text("Text 2.")
                },
                output,
            )
        }

        @Test
        fun `join consecutive speakers`() {
            val input = buildTranscript {
                speaker("Speaker 1")
                speaker("Speaker 2")
                speaker("Speaker 3")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    speaker("Speaker 1, Speaker 2, Speaker 3")
                },
                output,
            )
        }

        @Test
        fun `filter out duplicate speakers`() {
            val input = buildTranscript {
                speaker("Speaker 1, Speaker 2")
                speaker("Speaker 3, Speaker 3")
                speaker("Speaker 1, Speaker 2, Speaker 4")
                speaker("Speaker 3")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    speaker("Speaker 1, Speaker 2, Speaker 3, Speaker 4")
                },
                output,
            )
        }

        @Test
        fun `sort speakers`() {
            val input = buildTranscript {
                speaker("C, B")
                speaker("D")
                speaker("A")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    speaker("A, B, C, D")
                },
                output,
            )
        }

        @Test
        fun `remove repeated speakers`() {
            val input = buildTranscript {
                speaker("Speaker 1")
                text("Text 1.")
                speaker("Speaker 1")
                text("Text 2.")
                speaker("Speaker 1")
                text("Text 3.")
                text("Text 4.")
                speaker("Speaker 2")
                text("Text 5.")
                speaker("Speaker 1")
                text("Text 6.")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    speaker("Speaker 1")
                    text("Text 1.")
                    text("Text 2.")
                    text("Text 3.")
                    text("Text 4.")
                    speaker("Speaker 2")
                    text("Text 5.")
                    speaker("Speaker 1")
                    text("Text 6.")
                },
                output,
            )
        }

        @Test
        fun `join split texts`() {
            val input = buildTranscript {
                text("Text 1")
                text("Text 2")
                text("Text 3")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("Text 1 Text 2 Text 3")
                },
                output,
            )
        }

        @Test
        fun `move unfinished sentence to next text`() {
            val input = buildTranscript {
                text("Period. Unfinished")
                text("sentence. And now")
                text("next, unfinished")
                text("sentence.")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("Period.")
                    text("Unfinished sentence.")
                    text("And now next, unfinished sentence.")
                },
                output,
            )
        }
    }

    @RunWith(Parameterized::class)
    class FullSentenceDetection(
        private val pattern: String,
        patternDescription: String,
    ) {
        @Test
        fun `detect sentence break`() {
            val input = buildTranscript {
                text("Some text")
                text("end$pattern")
                text("more text")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("Some text end$pattern")
                    text("more text")
                },
                output,
            )
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{1}")
            fun params() = listOf(
                arrayOf(".", "dot"),
                arrayOf("!", "exclamation mark"),
                arrayOf("?", "question mark"),
                arrayOf("…", "ellipsis"),
                arrayOf("-", "hyphen"),
                arrayOf(")", "parenthesis"),
                arrayOf("]", "square bracket"),
                arrayOf(">", "diamond bracket"),
                arrayOf("}", "curly bracket"),
                arrayOf("\"", "double quote"),
                arrayOf("”", "double styled quote"),
                arrayOf("'", "single quote"),
                arrayOf("’", "single styled quote"),
            )
        }
    }

    @RunWith(Parameterized::class)
    class MidSentenceDetectedPattern(
        private val pattern: String,
        patternDescription: String,
    ) {
        @Test
        fun `detect mid-sentence break`() {
            val input = buildTranscript {
                text("Some text$pattern and now")
                text("more text")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("Some text$pattern")
                    text("and now more text")
                },
                output,
            )
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{1}")
            fun params() = listOf(
                arrayOf(".", "dot"),
                arrayOf("!", "exclamation mark"),
                arrayOf("?", "question mark"),
                arrayOf("…", "ellipsis"),
                arrayOf(".\"", "dot double quote"),
                arrayOf("!\"", "exclamation mark double quote"),
                arrayOf("?\"", "question mark double quote"),
                arrayOf("…\"", "ellipsis double quote"),
                arrayOf(".”", "dot with double styled quote"),
                arrayOf("!”", "exclamation mark with double styled quote"),
                arrayOf("?”", "question mark with double styled quote"),
                arrayOf("…”", "ellipsis with double styled quote"),
                arrayOf(".'", "dot with single quote"),
                arrayOf("!'", "exclamation mark with single quote"),
                arrayOf("?'", "question mark with single quote"),
                arrayOf("…'", "ellipsis with single quote"),
                arrayOf(".’", "dot with single styled quote"),
                arrayOf("!’", "exclamation mark with single styled quote"),
                arrayOf("?’", "question mark with single styled quote"),
                arrayOf("…’", "ellipsis with single styled quote"),
            )
        }
    }

    @RunWith(Parameterized::class)
    class MidSentenceIgnoredPatterns(
        private val pattern: String,
        patternDescription: String,
    ) {
        @Test
        fun `detect mid-sentence break`() {
            val input = buildTranscript {
                text("Some text$pattern and now")
                text("more text")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("Some text$pattern and now more text")
                },
                output,
            )
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{1}")
            fun params() = listOf(
                arrayOf("-", "hyphen"),
                arrayOf(")", "parenthesis"),
                arrayOf("]", "square bracket"),
                arrayOf(">", "diamond bracket"),
                arrayOf("}", "curly bracket"),
                arrayOf("\"", "double quote"),
                arrayOf("”", "double styled quote"),
                arrayOf("'", "single quote"),
                arrayOf("’", "single styled quote"),
            )
        }
    }
}

private fun buildTranscript(block: TranscriptBuilder.() -> Unit): List<TranscriptEntry> {
    return TranscriptBuilder().apply(block).build()
}

private class TranscriptBuilder {
    private val entries = mutableListOf<TranscriptEntry>()

    fun text(value: String) {
        entries += TranscriptEntry.Text(value)
    }

    fun speaker(value: String) {
        entries += TranscriptEntry.Speaker(value)
    }

    fun build() = entries
}
