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
                output.withoutWords(),
            )
        }

        @Test
        fun `join split texts uses min start time and max end time`() {
            val input = buildTranscript {
                text("Text 1", startTimeMs = 1000, endTimeMs = 2000)
                text("Text 2", startTimeMs = 2000, endTimeMs = 3000)
                text("Text 3", startTimeMs = 3000, endTimeMs = 4000)
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("Text 1 Text 2 Text 3", startTimeMs = 1000, endTimeMs = 4000)
                },
                output.withoutWords(),
            )
        }

        @Test
        fun `join split texts preserves word timings`() {
            val input = buildTranscript {
                text("Text 1", startTimeMs = 1000, endTimeMs = 2000)
                text("Text 2", startTimeMs = 2000, endTimeMs = 3000)
                text("Text 3", startTimeMs = 3000, endTimeMs = 4000)
            }

            val output = input.sanitize()

            val entry = output.single() as TranscriptEntry.Text
            assertEquals(
                listOf(
                    TranscriptEntry.WordTiming("Text 1", 1000, 2000, 0, 6),
                    TranscriptEntry.WordTiming("Text 2", 2000, 3000, 7, 13),
                    TranscriptEntry.WordTiming("Text 3", 3000, 4000, 14, 20),
                ),
                entry.words,
            )
        }

        @Test
        fun `single fragment sentence has no word timings`() {
            val input = buildTranscript {
                text("Complete sentence.", startTimeMs = 0, endTimeMs = 1000)
            }

            val output = input.sanitize()

            val entry = output.single() as TranscriptEntry.Text
            assertEquals(emptyList<TranscriptEntry.WordTiming>(), entry.words)
        }

        @Test
        fun `mid-sentence split preserves timing`() {
            val input = buildTranscript {
                text("Period. Unfinished", startTimeMs = 0, endTimeMs = 1000)
                text("sentence.", startTimeMs = 1000, endTimeMs = 2000)
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("Period.", startTimeMs = 0, endTimeMs = 388)
                    text("Unfinished sentence.", startTimeMs = 388, endTimeMs = 2000)
                },
                output.withoutWords(),
            )
        }

        @Test
        fun `mid-sentence split preserves word timings`() {
            val input = buildTranscript {
                text("Period. Unfinished", startTimeMs = 0, endTimeMs = 1000)
                text("sentence.", startTimeMs = 1000, endTimeMs = 2000)
            }

            val output = input.sanitize()

            val secondEntry = output[1] as TranscriptEntry.Text
            assertEquals(
                listOf(
                    TranscriptEntry.WordTiming("Unfinished", 388, 1000, 0, 10),
                    TranscriptEntry.WordTiming("sentence.", 1000, 2000, 11, 20),
                ),
                secondEntry.words,
            )
        }

        @Test
        fun `residual accumulator flush preserves timing`() {
            val input = buildTranscript {
                text("Finished.", startTimeMs = 0, endTimeMs = 1000)
                text("Unfinished", startTimeMs = 1000, endTimeMs = 2000)
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("Finished.", startTimeMs = 0, endTimeMs = 1000)
                    text("Unfinished", startTimeMs = 1000, endTimeMs = 2000)
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
                output.withoutWords(),
            )
        }

        @Test
        fun `keep CJK sentences as separate entries`() {
            // Regression test for PCDROID-639: Japanese (CJK) sentences end with `。`/`！`/`？`,
            // which were not recognised as sentence terminators, so the whole transcript
            // collapsed into a single entry and the synced auto-scroll had no per-line anchor.
            val input = buildTranscript {
                text("これはペンです。", startTimeMs = 0, endTimeMs = 1000)
                text("今日はいい天気ですね！", startTimeMs = 1000, endTimeMs = 2000)
                text("元気ですか？", startTimeMs = 2000, endTimeMs = 3000)
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("これはペンです。", startTimeMs = 0, endTimeMs = 1000)
                    text("今日はいい天気ですね！", startTimeMs = 1000, endTimeMs = 2000)
                    text("元気ですか？", startTimeMs = 2000, endTimeMs = 3000)
                },
                output.withoutWords(),
            )
        }

        @Test
        fun `split CJK mid-sentence break to next entry`() {
            val input = buildTranscript {
                text("最初の文。続きは")
                text("次の文。")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("最初の文。")
                    // No space is inserted between CJK fragments when they are rejoined.
                    text("続きは次の文。")
                },
                output.withoutWords(),
            )
        }

        @Test
        fun `do not insert spaces when joining CJK fragments`() {
            val input = buildTranscript {
                text("これは")
                text("ペンです。")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("これはペンです。")
                },
                output.withoutWords(),
            )
        }

        @Test
        fun `split CJK mid-sentence preserves timing and word offsets`() {
            val input = buildTranscript {
                text("最初の文。続きは", startTimeMs = 0, endTimeMs = 1000)
                text("次の文。", startTimeMs = 1000, endTimeMs = 2000)
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("最初の文。", startTimeMs = 0, endTimeMs = 625)
                    text("続きは次の文。", startTimeMs = 625, endTimeMs = 2000)
                },
                output.withoutWords(),
            )

            val secondEntry = output[1] as TranscriptEntry.Text
            assertEquals(
                listOf(
                    TranscriptEntry.WordTiming("続きは", 625, 1000, 0, 3),
                    TranscriptEntry.WordTiming("次の文。", 1000, 2000, 3, 7),
                ),
                secondEntry.words,
            )
        }

        @Test
        fun `do not treat a bare CJK closing mark as a sentence end`() {
            // A bare closing 」 wraps a mid-sentence quoted term (これは「AI」について話します。), so a
            // cue ending right after it must keep accumulating instead of splitting mid-sentence.
            val input = buildTranscript {
                text("これは「AI」")
                text("について話します。")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("これは「AI」について話します。")
                },
                output.withoutWords(),
            )
        }

        @Test
        fun `do not insert a space before a CJK closer after a Latin run`() {
            // A Latin token can abut a CJK closer across a cue boundary (cue 1 ends 「AI, cue 2
            // starts 」…); no space should be inserted before the closing bracket.
            val input = buildTranscript {
                text("これは「AI")
                text("」について話します。")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("これは「AI」について話します。")
                },
                output.withoutWords(),
            )
        }

        @Test
        fun `do not insert spaces around supplementary-plane CJK ideographs`() {
            // 𠀋 is a CJK Extension B ideograph encoded as a surrogate pair; it must still count as CJK.
            val input = buildTranscript {
                text("𠀋")
                text("です。")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("𠀋です。")
                },
                output.withoutWords(),
            )
        }

        @Test
        fun `keep Devanagari sentences as separate entries`() {
            // Hindi (and other Devanagari scripts) end sentences with the danda `।`; without it in
            // the terminator list the transcript collapses into a single entry with no
            // auto-scroll anchor — the same failure as PCDROID-639 for CJK.
            val input = buildTranscript {
                text("यह एक कलम है।", startTimeMs = 0, endTimeMs = 1000)
                text("आज मौसम अच्छा है।", startTimeMs = 1000, endTimeMs = 2000)
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("यह एक कलम है।", startTimeMs = 0, endTimeMs = 1000)
                    text("आज मौसम अच्छा है।", startTimeMs = 1000, endTimeMs = 2000)
                },
                output.withoutWords(),
            )
        }

        @Test
        fun `insert spaces when joining Devanagari fragments`() {
            // Devanagari scripts use spaces between words, unlike CJK/Thai.
            val input = buildTranscript {
                text("यह एक")
                text("कलम है।")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("यह एक कलम है।")
                },
                output.withoutWords(),
            )
        }

        @Test
        fun `do not insert spaces when joining Thai fragments`() {
            // Thai is written without spaces between words.
            val input = buildTranscript {
                text("สวัสดี")
                text("ครับ")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("สวัสดีครับ")
                },
                output.withoutWords(),
            )
        }

        @Test
        fun `flush Thai transcript at cue boundaries`() {
            // Thai has no sentence-final punctuation, so no terminator list can ever segment it.
            // The accumulator cap flushes at cue boundaries instead, so the transcript still
            // yields multiple entries for the synced auto-scroll to anchor on.
            val cue = "ก".repeat(80)
            val input = buildTranscript {
                text(cue, startTimeMs = 0, endTimeMs = 1000)
                text(cue, startTimeMs = 1000, endTimeMs = 2000)
                text(cue, startTimeMs = 2000, endTimeMs = 3000)
                text(cue, startTimeMs = 3000, endTimeMs = 4000)
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("ก".repeat(160), startTimeMs = 0, endTimeMs = 2000)
                    text("ก".repeat(160), startTimeMs = 2000, endTimeMs = 4000)
                },
                output.withoutWords(),
            )
        }

        @Test
        fun `flush unpunctuated captions at cue boundaries`() {
            // Auto-generated captions without any punctuation must not collapse into a single
            // entry either; spaces are still inserted between the joined Latin fragments.
            val cue = "a".repeat(80)
            val input = buildTranscript {
                text(cue, startTimeMs = 0, endTimeMs = 1000)
                text(cue, startTimeMs = 1000, endTimeMs = 2000)
                text(cue, startTimeMs = 2000, endTimeMs = 3000)
                text(cue, startTimeMs = 3000, endTimeMs = 4000)
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("$cue $cue", startTimeMs = 0, endTimeMs = 2000)
                    text("$cue $cue", startTimeMs = 2000, endTimeMs = 4000)
                },
                output.withoutWords(),
            )
        }

        @Test
        fun `keep a Latin closing quote attached to a CJK sentence`() {
            // Japanese speech can be wrapped in ASCII / curly quotes; the closing quote must stay
            // with the sentence rather than being detached onto the next entry.
            val input = buildTranscript {
                text("彼は\"はい。\"それから")
                text("帰った。")
            }

            val output = input.sanitize()

            assertEquals(
                buildTranscript {
                    text("彼は\"はい。\"")
                    text("それから帰った。")
                },
                output.withoutWords(),
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
                output.withoutWords(),
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
                arrayOf("。", "ideographic full stop"),
                arrayOf("！", "fullwidth exclamation mark"),
                arrayOf("？", "fullwidth question mark"),
                arrayOf("｡", "halfwidth ideographic full stop"),
                arrayOf("．", "fullwidth full stop"),
                arrayOf("؟", "arabic question mark"),
                arrayOf("۔", "urdu full stop"),
                arrayOf("।", "devanagari danda"),
                arrayOf("॥", "devanagari double danda"),
                arrayOf("။", "burmese section mark"),
                arrayOf("។", "khmer khan"),
                arrayOf("៕", "khmer bariyoosan"),
                arrayOf("።", "ethiopic full stop"),
                arrayOf("։", "armenian full stop"),
                arrayOf("-", "hyphen"),
                arrayOf(")", "parenthesis"),
                arrayOf("]", "square bracket"),
                arrayOf(">", "diamond bracket"),
                arrayOf("}", "curly bracket"),
                // CJK closers end a sentence only as a terminal combo (terminator + closer)
                arrayOf("。」", "ideographic full stop with corner bracket"),
                arrayOf("！」", "fullwidth exclamation mark with corner bracket"),
                arrayOf("？」", "fullwidth question mark with corner bracket"),
                arrayOf("。』", "ideographic full stop with white corner bracket"),
                arrayOf("。）", "ideographic full stop with fullwidth parenthesis"),
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
                output.withoutWords(),
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
                arrayOf("。", "ideographic full stop"),
                arrayOf("！", "fullwidth exclamation mark"),
                arrayOf("？", "fullwidth question mark"),
                arrayOf("｡", "halfwidth ideographic full stop"),
                arrayOf("．", "fullwidth full stop"),
                arrayOf("؟", "arabic question mark"),
                arrayOf("۔", "urdu full stop"),
                arrayOf("।", "devanagari danda"),
                arrayOf("॥", "devanagari double danda"),
                arrayOf("။", "burmese section mark"),
                arrayOf("។", "khmer khan"),
                arrayOf("៕", "khmer bariyoosan"),
                arrayOf("።", "ethiopic full stop"),
                arrayOf("։", "armenian full stop"),
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
                arrayOf("。」", "ideographic full stop with corner bracket"),
                arrayOf("！」", "fullwidth exclamation mark with corner bracket"),
                arrayOf("？」", "fullwidth question mark with corner bracket"),
                arrayOf("。』", "ideographic full stop with white corner bracket"),
                // Mixed-script quoting: CJK terminator with Latin quote, and Latin terminator with CJK bracket
                arrayOf("。\"", "ideographic full stop with double quote"),
                arrayOf("。”", "ideographic full stop with double styled quote"),
                arrayOf(".」", "dot with corner bracket"),
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
                output.withoutWords(),
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
                arrayOf("）", "fullwidth parenthesis"),
                arrayOf("」", "corner bracket"),
                arrayOf("』", "white corner bracket"),
                arrayOf("\"", "double quote"),
                arrayOf("”", "double styled quote"),
                arrayOf("'", "single quote"),
                arrayOf("’", "single styled quote"),
            )
        }
    }
}

private fun List<TranscriptEntry>.withoutWords() = map { entry ->
    if (entry is TranscriptEntry.Text) entry.copy(words = emptyList()) else entry
}

private fun buildTranscript(block: TranscriptBuilder.() -> Unit): List<TranscriptEntry> {
    return TranscriptBuilder().apply(block).build()
}

private class TranscriptBuilder {
    private val entries = mutableListOf<TranscriptEntry>()

    fun text(value: String, startTimeMs: Long = -1L, endTimeMs: Long = -1L) {
        entries += TranscriptEntry.Text(value, startTimeMs = startTimeMs, endTimeMs = endTimeMs)
    }

    fun speaker(value: String) {
        entries += TranscriptEntry.Speaker(value)
    }

    fun build() = entries
}
