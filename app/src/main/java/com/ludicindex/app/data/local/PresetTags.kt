package com.ludicindex.app.data.local

/** Tags every install starts with; users can delete them or add their own. */
object PresetTags {

    data class Preset(val name: String, val colorArgb: Int)

    val ALL: List<Preset> = listOf(
        Preset("PS5", 0xFF0070D1.toInt()),
        Preset("PS4", 0xFF2E6DB4.toInt()),
        Preset("Xbox", 0xFF107C10.toInt()),
        Preset("Switch", 0xFFE60012.toInt()),
        Preset("PC", 0xFF5C6BC0.toInt()),
        Preset("Steam Deck", 0xFF1B2838.toInt()),
        Preset("Playing", 0xFF2E7D32.toInt()),
    )
}
