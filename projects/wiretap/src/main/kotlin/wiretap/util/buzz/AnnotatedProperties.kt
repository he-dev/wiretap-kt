package wiretap.util.buzz

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

internal inline fun <reified A : Annotation> annotatedProperties(
    source: Any,
): Sequence<AnnotatedProperty<A>> =
    annotatedProperties(source::class, A::class)

@Suppress("UNCHECKED_CAST")
private fun <A : Annotation> annotatedProperties(
    type: KClass<*>,
    annotationType: KClass<A>,
): Sequence<AnnotatedProperty<A>> =
    // meta: Reflection is cached per source type and annotation type because log paths can be hot.
    (annotatedPropertyCache.getOrPut(AnnotatedPropertyKey(type, annotationType)) {
        type.memberProperties.mapNotNull { property ->
            val annotation = property.findAnnotation(annotationType) ?: return@mapNotNull null
            if (property.visibility != KVisibility.PUBLIC) {
                Configuration.diagnosticLogger.warnAboutNonPublicAnnotatedProperty(annotationType, type, property)
                return@mapNotNull null
            }
            AnnotatedProperty(annotation, property)
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
