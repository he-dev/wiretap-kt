package wiretap.util

import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal fun ActivityLogger.warnAboutMissingConfigurationVariant(
    variant: String,
    activityType: Class<out Activity>,
) {
    val message = "Configuration variant '$variant' requested by ${activityType.name} was not found; using the default one."
    log(ActivityStatusLevel.Warning,emptyMap(), message, null)
}

internal fun ActivityLogger.warnAboutNonPublicAnnotatedProperty(
    annotationType: KClass<out Annotation>,
    sourceType: KClass<*>,
    property: KProperty1<out Any, *>,
) {
    val message = "@${annotationType.simpleName} on non-public property '${sourceType.qualifiedName}.${property.name}' was ignored; annotated properties must be public."
    log(ActivityStatusLevel.Warning,emptyMap(), message, null)
}

internal fun ActivityLogger.warnAboutDuplicateLogProperty(
    sourceType: KClass<*>,
    property: DottedName,
) {
    val message = "Log-property source '${sourceType.qualifiedName}' pushed '$property' more than once; the first value was kept."
    log(ActivityStatusLevel.Warning,emptyMap(), message, null)
}

internal fun ActivityLogger.warnAboutLastStatusOverwrite(
    activity: Activity,
    ignoredStatus: ActivityStatus<*>,
) {
    val message = "${activity.name} status was already set to " +
        "[${activity.status.code.lowercase(Locale.ROOT)}]; ignored later status " +
        "[${ignoredStatus.code.lowercase(Locale.ROOT)}]."
    log(ActivityStatusLevel.Warning,emptyMap(), message, null)
}


