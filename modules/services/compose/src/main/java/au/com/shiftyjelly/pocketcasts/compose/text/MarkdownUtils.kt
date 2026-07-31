package au.com.shiftyjelly.pocketcasts.compose.text

import androidx.core.text.htmlEncode

fun markdownToHtml(markdown: String): String {
    return markdown.lines()
        .joinToString("\n") { line ->
            when {
                line.startsWith("### ") -> "<h3>${line.removePrefix("### ").htmlEncode()}</h3>"
                line.startsWith("## ") -> "<h2>${line.removePrefix("## ").htmlEncode()}</h2>"
                line.startsWith("# ") -> "<h1>${line.removePrefix("# ").htmlEncode()}</h1>"
                line.startsWith("- ") -> "&#8226; ${line.removePrefix("- ").htmlEncode()}<br>"
                line.startsWith("* ") -> "&#8226; ${line.removePrefix("* ").htmlEncode()}<br>"
                line.isBlank() -> "<br>"
                else -> "${line.htmlEncode()}<br>"
            }
        }
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")
}
