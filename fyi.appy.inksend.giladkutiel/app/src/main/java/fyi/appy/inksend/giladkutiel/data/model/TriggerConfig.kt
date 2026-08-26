package fyi.appy.inksend.giladkutiel.data.model

/**
 * Text-length bounds that gate when the floating overlay button appears. Below [minTextLength]
 * there is nothing worth styling; above [maxTextLength] the message is too long to fit the
 * 512×512 image at a legible size, so the button is hidden entirely rather than rendering a
 * wall of tiny text (todo.txt: "if the text is too long hide the button entirely").
 */
data class TriggerConfig(
    val minTextLength: Int = 3,
    val maxTextLength: Int = 240,
)
