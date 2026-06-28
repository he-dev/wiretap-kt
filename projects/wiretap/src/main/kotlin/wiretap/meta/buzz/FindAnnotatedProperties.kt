package wiretap.meta.buzz

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.cast
import kotlin.reflect.full.memberProperties
import wiretap.util.Configuration
import wiretap.util.warnAboutNonPublicAnnotatedProperty

private val annotatedPropertyCache =
    ConcurrentHashMap<AnnotatedPropertyKey, List<AnnotatedProperty<out Annotation>>>()

internal inline fun <reified A : Annotation> findAnnotatedProperties(
    source: Any,
): Sequence<AnnotatedProperty<A>> =
    findAnnotatedProperties(source::class, A::class)

// meta: The reified overload keeps call sites clean, while this worker keeps cached reflection in one place.
@Suppress("UNCHECKED_CAST")
private fun <A : Annotation> findAnnotatedProperties(
    type: KClass<*>,
    annotationType: KClass<A>,
): Sequence<AnnotatedProperty<A>> =
    // meta: Reflection is cached per source type and annotation type because log paths can be hot.
    (annotatedPropertyCache.getOrPut(AnnotatedPropertyKey(type, annotationType)) {
        type.memberProperties.mapNotNull { property ->
            val annotation = property.findAnnotation(annotationType) ?: return@mapNotNull null
            if (property.visibility == KVisibility.PUBLIC) {
                AnnotatedProperty(annotation, property)
            } else {
                Configuration.diagnosticLogger.warnAboutNonPublicAnnotatedProperty(annotationType, type, property)
                null
            }
        }
    } as List<AnnotatedProperty<A>>).asSequence()

internal data class AnnotatedProperty<A : Annotation>(
    val annotation: A,
    val property: KProperty1<out Any, *>,
) {
    val name: String
        get() = property.name

    fun value(source: Any): Any? =
        property.getter.call(source)
}

private data class AnnotatedPropertyKey(
    val type: KClass<*>,
    val annotationType: KClass<out Annotation>,
)

private fun <A : Annotation> KAnnotatedElement.findAnnotation(annotationType: KClass<A>): A? =
    annotations.firstOrNull { annotationType.isInstance(it) }?.let(annotationType::cast)
