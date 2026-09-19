package app.thdev.glassnavlab.core.navigation.assertions

internal fun assertEquals(
    expected: Any?,
    actual: Any?,
    label: String,
) {
    if (expected != actual) {
        fail("Expected $label <$expected>, but was <$actual>.")
    }
}

internal fun fail(message: String): Nothing {
    throw AssertionError(message)
}
