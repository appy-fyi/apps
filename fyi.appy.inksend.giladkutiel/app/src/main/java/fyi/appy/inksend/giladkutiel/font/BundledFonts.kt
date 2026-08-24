package fyi.appy.inksend.giladkutiel.font

/**
 * The 8 bundled `.ttf` assets under `app/src/main/assets/fonts/`, keyed by
 * the id stored in [fyi.appy.inksend.giladkutiel.data.db.StylePresetEntity.fontFamily].
 * A `StylePresetEntity.fontFamily` that isn't one of these ids is instead a
 * [fyi.appy.inksend.giladkutiel.data.db.HandwritingFontEntity.filePath] for a
 * personal handwriting font.
 */
enum class BundledFont(val id: String, val displayName: String, val assetPath: String) {
    PACIFICO("pacifico", "Pacifico", "fonts/pacifico.ttf"),
    CAVEAT("caveat", "Caveat", "fonts/caveat.ttf"),
    PLAYFAIR_DISPLAY("playfair_display", "Playfair Display", "fonts/playfair_display.ttf"),
    BEBAS_NEUE("bebas_neue", "Bebas Neue", "fonts/bebas_neue.ttf"),
    COURIER_PRIME("courier_prime", "Courier Prime", "fonts/courier_prime.ttf"),
    DANCING_SCRIPT("dancing_script", "Dancing Script", "fonts/dancing_script.ttf"),
    OSWALD("oswald", "Oswald", "fonts/oswald.ttf"),
    COMFORTAA("comfortaa", "Comfortaa", "fonts/comfortaa.ttf"),
    ;

    companion object {
        fun byId(id: String): BundledFont? = entries.find { it.id == id }
    }
}
