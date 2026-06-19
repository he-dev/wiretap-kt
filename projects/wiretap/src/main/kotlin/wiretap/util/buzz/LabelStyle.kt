package wiretap.util.buzz

import java.util.Locale
import wiretap.util.PropertyName

fun interface LabelStyle {
    fun applyTo(name: PropertyName, label: String): String

    data object AsIs : LabelStyle {
        override fun applyTo(name: PropertyName, label: String): String =
            label
    }

    data object Capital : LabelStyle {
        override fun applyTo(name: PropertyName, label: String): String =
            label.replaceFirstChar { it.titlecase() }
    }

    data object Camel : LabelStyle {
        override fun applyTo(name: PropertyName, label: String): String =
            label.toWords().mapIndexed { index, word ->
                val normalized = word.lowercase(Locale.ROOT)
                if (index == 0) normalized else normalized.replaceFirstChar { it.titlecase() }
            }.joinToString("")
    }

    data object Upper : LabelStyle {
        override fun applyTo(name: PropertyName, label: String): String =
            label.uppercase(Locale.ROOT)
    }

    data object Lower : LabelStyle {
        override fun applyTo(name: PropertyName, label: String): String =
            label.lowercase(Locale.ROOT)
    }

    data object Kebab : LabelStyle {
        override fun applyTo(name: PropertyName, label: String): String =
            label.toWords().joinToString("-") { it.lowercase(Locale.ROOT) }
    }

    data object Pascal : LabelStyle {
        override fun applyTo(name: PropertyName, label: String): String =
            label.toWords().joinToString("") { word ->
                word.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase() }
            }
    }

    data object Snake : LabelStyle {
        override fun applyTo(name: PropertyName, label: String): String =
            label.toWords().joinToString("_") { it.lowercase(Locale.ROOT) }
    }

    data object Title : LabelStyle {
        override fun applyTo(name: PropertyName, label: String): String =
            label.toWords().joinToString(" ") { word ->
                word.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase() }
            }
    }

    companion object {
        const val AS_IS = "as-is"
        const val CAPITAL = "capital"
        const val CAMEL = "camel"
        const val UPPER = "upper"
        const val LOWER = "lower"
        const val KEBAB = "kebab"
        const val PASCAL = "pascal"
        const val SNAKE = "snake"
        const val TITLE = "title"
    }
}

private val lowerToUpperBoundary = Regex("([\\p{Ll}\\p{N}])([\\p{Lu}])")
private val acronymBoundary = Regex("([\\p{Lu}]+)([\\p{Lu}][\\p{Ll}])")
private val wordSeparator = Regex("[^\\p{L}\\p{N}]+")

private fun String.toWords(): List<String> =
    replace(acronymBoundary, "$1 $2")
        .replace(lowerToUpperBoundary, "$1 $2")
        .split(wordSeparator)
        .filter(String::isNotEmpty)
