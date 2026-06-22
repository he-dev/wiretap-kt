package wiretap.util

import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal fun ActivityLogger.warnAboutMissingConfigurationVariant(
    variant: String,
    activityType: Class<out Activity>,
) {
    log(
        LogEntry(
            level = ActivityStatusLevel.Warning,
            message = "Configuration variant '$variant' requested by ${activityType.name} was not found; using the default one.",
            properties = emptyMap(),
        ),
    )
}

internal fun ActivityLogger.warnAboutNonPublicAnnotatedProperty(
    annotationType: KClass<out Annotation>,
    sourceType: KClass<*>,
    property: KProperty1<out Any, *>,
) {
    log(
        LogEntry(
            level = ActivityStatusLevel.Warning,
            message = "@${annotationType.simpleName} on non-public property '${sourceType.qualifiedName}.${property.name}' was ignored; annotated properties must be public.",
            properties = emptyMap(),
        ),
    )
}

internal fun ActivityLogger.warnAboutDuplicateLogProperty(
    sourceType: KClass<*>,
    property: PropertyName,
) {
    log(
        LogEntry(
            level = ActivityStatusLevel.Warning,
            message = "Log-property source '${sourceType.qualifiedName}' pushed '$property' more than once; the first value was kept.",
            properties = emptyMap(),
        ),
    )
}

internal fun ActivityLogger.warnAboutLastStatusOverwrite(
    activity: Activity,
    ignoredStatus: ActivityStatus<*>,
) {
    log(
        LogEntry(
            level = ActivityStatusLevel.Warning,
            message = "${activity.name} status was already set to " +
                "[${activity.status.code.lowercase(Locale.ROOT)}]; ignored later status " +
                "[${ignoredStatus.code.lowercase(Locale.ROOT)}].",
            properties = emptyMap(),
        ),
    )
}
