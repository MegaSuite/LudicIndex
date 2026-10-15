package com.ludicindex.app.data.local

/** Preset tags. Empty today: the platform is a per-entry field, not a tag. */
object PresetTags {

    data class Preset(val name: String, val colorArgb: Int)

    val ALL: List<Preset> = emptyList()
}
