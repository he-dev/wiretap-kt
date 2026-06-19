package wiretap.util.buzz

import java.util.concurrent.ConcurrentHashMap
import wiretap.util.Configuration
import wiretap.util.warnAboutNonPublicAnnotatedProperty
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.cast
import kotlin.reflect.full.memberProperties

object FindAnnotatedProperties {
    private val cache = ConcurrentHashMap<Key, List<AnnotatedProperty<out Annotation>>>()

    inline fun <reified A : Annotation> on(source: Any): Sequence<AnnotatedProperty<A>> =
        on(source::class, A::class)

    @Suppress("UNCHECKED_CAST")
    fun <A : Annotation> on(type: KClass<*>, annotationType: KClass<A>): Sequence<AnnotatedProperty<A>> =
        // meta: Reflection is cached per source type and annotation type because log paths can be hot.
        (cache.getOrPut(Key(type, annotationType)) {
            discover(type, annotationType)
        } as List<AnnotatedProperty<A>>).asSequence()

    private fun <A : Annotation> discover(
        type: KClass<*>,
        annotationType: KClass<A>,
    ): List<AnnotatedProperty<A>> =
        type.memberProperties.mapNotNull { property ->
            val annotation = property.findAnnotation(annotationType) ?: return@mapNotNull null
            if (property.visibility != KVisibility.PUBLIC) {
                Configuration.diagnosticLogger.warnAboutNonPublicAnnotatedProperty(annotationType, type, property)
                return@mapNotNull null
            }
            AnnotatedProperty(annotation, property)
        }

    private data class Key(
        val type: KClass<*>,
        val annotationType: KClass<out Annotation>,
    )
}

data class AnnotatedProperty<A : Annotation>(
    val annotation: A,
    val property: KProperty1<out Any, *>,
) {
    val name: String
        get() = property.name

    fun value(source: Any): Any? =
        property.getter.call(source)
}

private fun <A : Annotation> KAnnotatedElement.findAnnotation(annotationType: KClass<A>): A? =
    annotations.firstOrNull { annotationType.isInstance(it) }?.let(annotationType::cast)
