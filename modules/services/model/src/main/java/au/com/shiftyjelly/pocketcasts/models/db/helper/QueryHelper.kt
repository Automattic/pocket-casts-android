package au.com.shiftyjelly.pocketcasts.models.db.helper

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase

object QueryHelper {

    fun firstRowArray(query: String, params: Array<String>?, appDatabase: AppDatabase): Array<String?>? {
        appDatabase.query(query, params ?: arrayOf()).use { cursor ->
            if (cursor.moveToNext()) {
                val result = arrayOfNulls<String>(cursor.columnCount)
                for (i in 0 until cursor.columnCount) {
                    result[i] = cursor.getString(i)
                }
                return result
            } else {
                return null
            }
        }
    }

    fun findAll(query: String, params: Array<String>?, db: SQLiteDatabase, rowParser: (Cursor) -> Boolean) {
        db.rawQuery(query, params ?: arrayOf()).use { cursor ->
            if (cursor.moveToFirst()) {
                var keepGoing: Boolean
                do {
                    keepGoing = rowParser(cursor)
                } while (keepGoing && cursor.moveToNext())
            }
        }
    }

    /**
     * From value array to ('one','two','three')
     */
    fun convertStringArrayToInStatement(values: Array<String>): String {
        return values.joinToString(separator = ",", prefix = "(", postfix = ")") { "'$it'" }
    }
}
