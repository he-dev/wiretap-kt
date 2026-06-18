package wiretap.util.buzz

fun interface AddLogProperty {
    operator fun invoke(key: String, value: Any?)
    operator fun invoke(key: PropertyName, value: Any?) = invoke(key.toString(), value)
}

fun interface AddMessagePart {
    operator fun invoke(message: String?, vararg args: Any?)
}

fun interface GetLogProperty {
    operator fun invoke(key: String): Any?

    operator fun invoke(key: PropertyName): Any? =
        invoke(key.toString())
}

interface LogPropertySource {
    fun logProperties(root: PropertyName, add: AddLogProperty)
}

interface MessagePartSource {
    fun messageParts(root: PropertyName, get: GetLogProperty, add: AddMessagePart)
}
