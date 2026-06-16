package wiretap.util.buzz

fun interface PushStateItem {
    operator fun invoke(key: String, value: Any?)

    operator fun invoke(key: PropertyName, value: Any?) =
        invoke(key.toString(), value)
}

fun interface PushMessagePart {
    operator fun invoke(message: String?, vararg args: Any?)
}

fun interface GetStateItem {
    operator fun invoke(key: String): Any?

    operator fun invoke(key: PropertyName): Any? =
        invoke(key.toString())
}

interface StateItemFeed {
    fun stateItems(name: PropertyName, push: PushStateItem)
}

interface MessagePartFeed {
    fun messageParts(root: PropertyName, get: GetStateItem, push: PushMessagePart)
}
