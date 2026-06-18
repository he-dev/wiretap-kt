package wiretap.util.buzz

fun interface AddLogProperty {
    operator fun invoke(key: String, value: Any?)
    operator fun invoke(key: PropertyName, value: Any?) = invoke(key.toString(), value)
}

data class MessagePartOptions(
    val label: String? = null,
    val separator: String = ": ",
    val format: String? = null,
)

fun interface AddMessagePart {
    fun push(name: String, value: Any?, options: MessagePartOptions)

    fun push(
        name: PropertyName,
        value: Any?,
        options: MessagePartOptions = MessagePartOptions(),
    ) = push(name.toString(), value, options)

    operator fun invoke(
        name: String,
        value: Any?,
        options: MessagePartOptions = MessagePartOptions(),
    ) = push(name, value, options)

    operator fun invoke(
        name: PropertyName,
        value: Any?,
        options: MessagePartOptions = MessagePartOptions(),
    ) = push(name, value, options)
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
