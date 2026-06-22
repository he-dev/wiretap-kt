package wiretap.util

import wiretap.util.buzz.AddMessagePart
import wiretap.util.buzz.GetLogProperty
import wiretap.util.buzz.MessagePartSource

class QuickBuzz(
    override val name: String,
    override val message: String? = null,
) : Activity.Buzz(), QuickMessageSource {
    class Okay(
        override val message: String? = null,
    ) : ActivityStatus.Okay<QuickBuzz>(), QuickMessageSource

    class Noop(
        override val message: String? = null,
    ) : ActivityStatus.Noop<QuickBuzz>(), QuickMessageSource

    class Fail(
        override val message: String? = null,
        exception: Throwable? = null,
    ) : ActivityStatus.Fail<QuickBuzz>(exception), QuickMessageSource
}

class QuickItem(
    override val name: String,
    override val message: String? = null,
    val omitStatuses: Set<OmitStatus> = emptySet(),
) : Activity.Item(), QuickMessageSource {
    internal override val resolvedOmitStatuses: Set<OmitStatus>
        get() = omitStatuses

    class Okay(
        override val message: String? = null,
    ) : ActivityStatus.Okay<QuickItem>(), QuickMessageSource

    class Noop(
        override val message: String? = null,
    ) : ActivityStatus.Noop<QuickItem>(), QuickMessageSource

    class Fail(
        override val message: String? = null,
        exception: Throwable? = null,
    ) : ActivityStatus.Fail<QuickItem>(exception), QuickMessageSource
}

class QuickSnap(
    override val name: String,
    override val message: String? = null,
) : Activity.Snap(), QuickMessageSource {
    class Okay(
        override val message: String? = null,
    ) : ActivityStatus.Okay<QuickSnap>(), QuickMessageSource

    class Noop(
        override val message: String? = null,
    ) : ActivityStatus.Noop<QuickSnap>(), QuickMessageSource

    class Fail(
        override val message: String? = null,
        exception: Throwable? = null,
    ) : ActivityStatus.Fail<QuickSnap>(exception), QuickMessageSource
}

class QuickBulk(
    override val name: String,
    override val message: String? = null,
) : Activity.Bulk<QuickItem>(), QuickMessageSource {
    class Okay(
        override val message: String? = null,
    ) : ActivityStatus.Okay<QuickBulk>(), QuickMessageSource

    class Noop(
        override val message: String? = null,
    ) : ActivityStatus.Noop<QuickBulk>(), QuickMessageSource

    class Fail(
        override val message: String? = null,
        exception: Throwable? = null,
    ) : ActivityStatus.Fail<QuickBulk>(exception), QuickMessageSource
}

private interface QuickMessageSource : MessagePartSource {
    val message: String?

    override fun messageParts(root: PropertyName, get: GetLogProperty, add: AddMessagePart) {
        message?.let { add(root.append("message"), it) }
    }
}
