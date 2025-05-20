package au.com.shiftyjelly.pocketcasts.settings.util

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Immutable
sealed interface TextResource {
    @Immutable
    data class Text(val text: String) : TextResource

    @Immutable
    data class StringId(@StringRes val id: Int, val args: ImmutableList<Any> = emptyList<Any>().toImmutableList()) : TextResource

    @Immutable
    data class PluralId(@PluralsRes val id: Int, val count: Int, val args: ImmutableList<Any> = emptyList<Any>().toImmutableList()) : TextResource

    @Suppress("SpreadOperator")
    @Composable
    @ReadOnlyComposable
    fun asString() = when (this) {
        is Text -> text
        is StringId -> stringResource(id, *args.toTypedArray())
        is PluralId -> pluralStringResource(id, count, *args.toTypedArray())
    }

    @Suppress("SpreadOperator")
    fun asString(context: Context) = when (this) {
        is Text -> text
        is StringId -> context.getString(id, *args.toTypedArray())
        is PluralId -> context.resources.getQuantityString(id, count, *args.toTypedArray())
    }

    companion object {
        @JvmName("fromNullableText")
        fun fromText(text: String?) = text?.let { fromText(it) }
        fun fromText(text: String) = Text(text)
        fun fromStringId(@StringRes id: Int, vararg args: Any) = StringId(id, args.toImmutableList())
        fun fromPluralId(@PluralsRes id: Int, count: Int, vararg args: Any) = PluralId(id, count, args.toImmutableList())
    }
}
