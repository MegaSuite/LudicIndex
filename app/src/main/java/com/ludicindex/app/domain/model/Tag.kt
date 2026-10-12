package com.ludicindex.app.domain.model

data class Tag(
    val id: Long,
    val name: String,
    val isPreset: Boolean,
    val colorArgb: Int,
)
