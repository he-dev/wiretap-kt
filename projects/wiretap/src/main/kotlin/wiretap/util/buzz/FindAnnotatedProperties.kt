package wiretap.util.buzz

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.cast
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

object FindAnnotatedProperties {
    private val cache = ConcurrentHashMap<Key, List<AnnotatedProperty<out Annotation>>>()

    inline fun <reified A : Annotation> on(source: Any): List<AnnotatedProperty<A>> =
        on(source::class, A::class)

    @Suppress("UNCHECKED_CAST")
    fun <A : Annotation> on(type: KClass<*>, annotationType: KClass<A>): List<AnnotatedProperty<A>> =
        cache.getOrPut(Key(type, annotationType)) {
            discover(type, annotationType)
        } as List<AnnotatedProperty<A>>

    private fun <A : Annotation> discover(
        type: KClass<*>,
        annotationType: KClass<A>,
    ): List<AnnotatedProperty<A>> =
        type.memberProperties.mapNotNull { property ->
            val annotation = property.findAnnotation(annotationType) ?: return@mapNotNull null
            property.isAccessible = true
            AnnotatedProperty(annotation, property)
        }.orderByConstructor(type) { it.property.name }

    private fun <T> List<T>.orderByConstructor(type: KClass<*>, nameOf: (T) -> String): List<T> {
        val parameterOrder = type.primaryConstructor
            ?.parameters
            ?.mapIndexedNotNull { index, parameter -> parameter.name?.let { it to index } }
            ?.toMap()
            ?: emptyMap()

        return sortedWith(
            compareBy<T> { parameterOrder[nameOf(it)] ?: Int.MAX_VALUE }
                .thenBy(nameOf)
        )
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
