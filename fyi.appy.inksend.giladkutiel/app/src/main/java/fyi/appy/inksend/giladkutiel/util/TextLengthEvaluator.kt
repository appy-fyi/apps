package fyi.appy.inksend.giladkutiel.util

/** Pure boundary check deciding whether the overlay should appear for a given text length. */
object TextLengthEvaluator {
    /**
     * Requires [trimmedLength] to be strictly positive in addition to falling within
     * [minLength]..[maxLength], so the overlay never shows for an empty field even if
     * the user has configured a minimum length of 0.
     */
    fun isWithinBounds(trimmedLength: Int, minLength: Int, maxLength: Int): Boolean =
        trimmedLength > 0 && trimmedLength in minLength..maxLength
}
