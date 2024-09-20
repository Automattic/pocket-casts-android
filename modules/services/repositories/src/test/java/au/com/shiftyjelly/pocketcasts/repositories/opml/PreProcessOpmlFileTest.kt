package au.com.shiftyjelly.pocketcasts.repositories.opml

import au.com.shiftyjelly.pocketcasts.utils.extensions.removeNewLines
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Scanner
import org.junit.Assert.assertEquals
import org.junit.Test

class PreProcessOpmlFileTest {
    @Test
    fun preProcessValidOpmlFileWithoutEspecialCharacters() {
        val xmlString = "<outline xmlUrl=\"https://feeds.simplecast.com\" type=\"rss\" text=\"valid text\" />"
        val inputStream: InputStream = ByteArrayInputStream(xmlString.toByteArray(StandardCharsets.UTF_8))
        val processedStream: InputStream = PreProcessOpmlFile().replaceInvalidXmlCharacter(inputStream)

        val processedString = Scanner(processedStream).useDelimiter("\\A").next()
        processedStream.close()

        assertEquals(xmlString, processedString.toString().removeNewLines())
    }

    @Test
    fun preProcessOpmlFileWithInvalidXmlCharacterInElementText() {
        val xmlString = "<outline xmlUrl=\"https://feeds.simplecast.com\" type=\"rss\" text=\"This has <special> characters\" />"
        val inputStream: InputStream = ByteArrayInputStream(xmlString.toByteArray(StandardCharsets.UTF_8))
        val processedStream: InputStream = PreProcessOpmlFile().replaceInvalidXmlCharacter(inputStream)

        val processedString = Scanner(processedStream).useDelimiter("\\A").next()
        processedStream.close()

        val expected = "<outline xmlUrl=\"https://feeds.simplecast.com\" type=\"rss\" text=\"This has &lt;special&gt; characters\" />"
        assertEquals(expected, processedString.removeNewLines())
    }

    @Test
    fun preProcessOpmlFileWithInvalidXmlCharacterInElementTextWhenElementXmlUrlIsLocatedAtTheEnd() {
        val xmlString = "<outline type=\"rss\" text=\"This has <special> characters\" xmlUrl=\"https://feeds.simplecast.com\" />"
        val inputStream: InputStream = ByteArrayInputStream(xmlString.toByteArray(StandardCharsets.UTF_8))
        val processedStream: InputStream = PreProcessOpmlFile().replaceInvalidXmlCharacter(inputStream)

        val processedString = Scanner(processedStream).useDelimiter("\\A").next()
        processedStream.close()

        val expected = "<outline type=\"rss\" text=\"This has &lt;special&gt; characters\" xmlUrl=\"https://feeds.simplecast.com\" />"
        assertEquals(expected, processedString.removeNewLines())
    }

    @Test
    fun preProcessOpmlFileWithInvalidXmlCharacterInElementXmlUrl() {
        val xmlString = "<outline xmlUrl=\"https://feeds.simplecast.com/sz&W8tJ16\" type=\"rss\" text=\"Text\" />"
        val inputStream: InputStream = ByteArrayInputStream(xmlString.toByteArray(StandardCharsets.UTF_8))
        val processedStream: InputStream = PreProcessOpmlFile().replaceInvalidXmlCharacter(inputStream)

        val processedString = Scanner(processedStream).useDelimiter("\\A").next()
        processedStream.close()

        val expected = "<outline xmlUrl=\"https://feeds.simplecast.com/sz&amp;W8tJ16\" type=\"rss\" text=\"Text\" />"
        assertEquals(expected, processedString.removeNewLines())
    }

    @Test
    fun preProcessOpmlFileWithMultipleInvalidXmlCharacterInElementText() {
        val xmlString =
            "<outline xmlUrl=\"https://feeds.simplecast.com/\" type=\"rss\" text=\"<special>\" /> <outline xmlUrl=\"https://feeds.simplecast.com/szW8tJ16\" type=\"rss\" text=\"CNBC’s\" />"
        val inputStream: InputStream = ByteArrayInputStream(xmlString.toByteArray(StandardCharsets.UTF_8))
        val processedStream: InputStream = PreProcessOpmlFile().replaceInvalidXmlCharacter(inputStream)

        val processedString = Scanner(processedStream).useDelimiter("\\A").next()
        processedStream.close()

        val expected = "<outline xmlUrl=\"https://feeds.simplecast.com/\" type=\"rss\" text=\"&lt;special&gt;\" /> <outline xmlUrl=\"https://feeds.simplecast.com/szW8tJ16\" type=\"rss\" text=\"CNBC&apos;s\" />"
        assertEquals(expected, processedString.removeNewLines())
    }

    @Test
    fun preProcessAnAlreadyPreProcessedOpmlFile() {
        val xmlString = "<outline xmlUrl=\"https://feeds.simplecast.com\" type=\"rss\" text=\"This has &lt;special&gt; characters\" />"
        val inputStream: InputStream = ByteArrayInputStream(xmlString.toByteArray(StandardCharsets.UTF_8))
        val processedStream: InputStream = PreProcessOpmlFile().replaceInvalidXmlCharacter(inputStream)

        val processedString = Scanner(processedStream).useDelimiter("\\A").next()
        processedStream.close()

        val expected = "<outline xmlUrl=\"https://feeds.simplecast.com\" type=\"rss\" text=\"This has &lt;special&gt; characters\" />"
        assertEquals(expected, processedString.removeNewLines())
    }
}
