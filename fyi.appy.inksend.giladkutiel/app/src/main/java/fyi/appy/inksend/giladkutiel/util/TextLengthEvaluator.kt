package fyi.appy.inksend.giladkutiel.util

/** Pure boundary check deciding whether the overlay should appear for a given text length. */
object TextLengthEvaluator {
    fun isWithinBounds(trimmedLength: Int, minLength: Int, maxLength: Int): Boolean =
        trimmedLength in minLength..maxLength
}
