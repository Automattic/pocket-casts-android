package au.com.shiftyjelly.pocketcasts.repositories.opml

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class PreProcessOpmlFile {
    fun replaceInvalidXmlCharacter(inputStream: InputStream): InputStream {
        val tempFile = File.createTempFile("output", ".xml")
        tempFile.deleteOnExit()

        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            BufferedWriter(OutputStreamWriter(FileOutputStream(tempFile), Charsets.UTF_8)).use { writer ->
                reader.lineSequence().forEach { line ->
                    val modifiedLine = line
                        .replace("<outline", "\n<outline")
                        .replace("&(?!amp;|quot;|gt;|lt;)".toRegex(), "&amp;")
                        .replace("’", "&apos;")

                    val textRegex = """(?<=text=")(.*?)(?="\s*(?:/>|\s+xmlUrl|\s+type))""".toRegex()
                    val finalLine = textRegex.replace(modifiedLine) { matchResult ->
                        val text = matchResult.groupValues[1]
                        val fixedText = text
                            .replace("\"", "&quot;")
                            .replace(">", "&gt;")
                            .replace("<", "&lt;")
                        matchResult.value.replace(text, fixedText)
                    }

                    writer.write(finalLine)
                    writer.newLine()
                }
            }
        }

        return FileInputStream(tempFile)
    }
}
