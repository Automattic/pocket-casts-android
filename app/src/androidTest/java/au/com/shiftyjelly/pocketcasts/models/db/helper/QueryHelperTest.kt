package au.com.shiftyjelly.pocketcasts.models.db.helper

import org.junit.Assert.assertEquals
import org.junit.Test

class QueryHelperTest {

    @Test
    fun convertStringArrayToInStatement() {
        val result = QueryHelper.convertStringArrayToInStatement(arrayOf("one", "two", "three"))
        assertEquals("('one','two','three')", result)
    }
}
