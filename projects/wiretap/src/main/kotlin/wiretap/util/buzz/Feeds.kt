package wiretap.util.buzz

import wiretap.util.MessagePartOptions
import wiretap.util.PropertyName

interface AddLogProperty {
    fun localOnly(key: PropertyName, value: Any?)
    //fun localOnly(key: String, value: Any?) = localOnly(PropertyName(key), value)

    fun cascading(key: PropertyName, value: Any?)
    //fun cascading(key: String, value: Any?) = cascading(PropertyName(key), value)
}

fun interface AddMessagePart {
    fun push(
        name: PropertyName,
        value: Any?,
        options: MessagePartOptions,
    )

    operator fun invoke(
        name: String,
        value: Any?,
        options: MessagePartOptions = MessagePartOptions(),
    ) = push(PropertyName(name), value, options)

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
    fun AddLogProperty.logProperties(root: PropertyName)
}

interface MessagePartSource {
    fun messageParts(root: PropertyName, get: GetLogProperty, add: AddMessagePart)
}
