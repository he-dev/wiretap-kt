package wiretap.util

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
