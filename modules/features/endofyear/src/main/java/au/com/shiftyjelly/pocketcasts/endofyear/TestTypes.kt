package au.com.shiftyjelly.pocketcasts.endofyear

public class PublicWithTests {
    fun print() = "Test"
}

public class PublicWithoutTests {
    fun print() = "Test"
}

internal class InternalWithTests {
    fun print() = "Test"
}

internal class InternalWithoutTests {
    fun print() = "Test"
}

private class PrivateType {
    fun print() = "Test"
}
