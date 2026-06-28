package wiretap.util

import wiretap.util.buzz.ComposeMessage
import wiretap.util.buzz.addActivityDuration
import wiretap.util.buzz.addActivity
import wiretap.util.buzz.composeMessage
import java.util.concurrent.ConcurrentHashMap

object Configuration {
    data class Variant(
        val root: DottedName = DottedName().wiretap,
        val composeMessage: ComposeMessage = composeMessage {
            remarks {
                addActivity()
                addActivityDuration()
            }
            arrange {
                add(root.activity.name)
                add(root.activity.durationMs)
                addRemaining()
            }
            join {
                joinToString("; ") { it }
            }
        },
    )

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Use(
        val variant: String,
    )

    private sealed interface Key {
        data object Default : Key
        data class Named(val value: String) : Key
    }

    private val variants = ConcurrentHashMap<Key, Variant>().apply {
        this[Key.Default] = Variant()
    }
    private val resolved = ConcurrentHashMap<Class<out Activity>, Variant>()

    @Volatile
    internal var diagnosticLogger: ActivityLogger = ActivityLogger.Noop
        private set

    val default: Variant
        get() = variants.getValue(Key.Default)

    operator fun get(name: String): Variant? =
        variants[Key.Named(name)]

    fun logDiagnosticsWith(logger: ActivityLogger): Configuration = apply {
        diagnosticLogger = logger
    }

    fun setDefault(
        variant: () -> Variant,
    ): Configuration = apply {
        variants[Key.Default] = variant()
        resolved.clear()
    }

    fun addNamed(
        name: String,
        variant: () -> Variant,
    ): Configuration = apply {
        check(variants.putIfAbsent(Key.Named(name), variant()) == null) {
            "Configuration variant '$name' already exists."
        }
        resolved.clear()
    }

    fun resolve(activity: Activity): Variant =
        resolved.computeIfAbsent(activity.javaClass) { activityType ->
            val name = activityType.getAnnotation(Use::class.java)?.variant
                ?: return@computeIfAbsent default

            variants[Key.Named(name)] ?: default.also {
                diagnosticLogger.warnAboutMissingConfigurationVariant(name, activityType)
            }
        }
}
