package au.com.shiftyjelly.pocketcasts.utils;

import android.os.Build;
import android.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import timber.log.Timber;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Timber only supports a stack level of greater than 5, so when in threads Timber throws an exception.
 */
public class TimberDebugTree extends Timber.Tree {
    private static final int MAX_LOG_LENGTH = 4000;
    private static final int MAX_TAG_LENGTH = 23;
    private static final int CALL_STACK_INDEX = 4;
    private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");

    @Nullable
    protected String createStackElementTag(@NotNull StackTraceElement element) {
        String tag = element.getClassName();
        Matcher m = ANONYMOUS_CLASS.matcher(tag);
        if (m.find()) {
            tag = m.replaceAll("");
        }
        tag = tag.substring(tag.lastIndexOf('.') + 1);
        // Tag length limit was removed in API 24.
        if (tag.length() <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return tag;
        }
        return tag.substring(0, MAX_TAG_LENGTH);
    }

    String getRealTag(String tag) {
        if (tag != null) {
            return tag;
        }

        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length == 0) {
            throw new IllegalStateException("Synthetic stacktrace didn't have enough elements: are you using proguard?");
        }
        return createStackElementTag(stackTrace[CALL_STACK_INDEX]);
    }

    /**
     * Break up {@code message} into maximum-length chunks (if needed) and send to either
     * {@link Log#println(int, String, String) Log.println()} or
     * {@link Log#wtf(String, String) Log.wtf()} for logging.
     *
     * {@inheritDoc}
     */
    @Override protected void log(int priority, String tag, @NotNull String message, Throwable t) {
        String realTag = getRealTag(tag);
        if (message.length() < MAX_LOG_LENGTH) {
            if (priority == Log.ASSERT) {
                Log.wtf(realTag, message);
            } else {
                Log.println(priority, realTag, message);
            }
            return;
        }

        // Split by line, then ensure each line can fit into Log's maximum length.
        for (int i = 0, length = message.length(); i < length; i++) {
            int newline = message.indexOf('\n', i);
            newline = newline != -1 ? newline : length;
            do {
                int end = Math.min(newline, i + MAX_LOG_LENGTH);
                String part = message.substring(i, end);
                if (priority == Log.ASSERT) {
                    Log.wtf(realTag, part);
                } else {
                    Log.println(priority, realTag, part);
                }
                i = end;
            } while (i < newline);
        }
    }
}