package com.ludicindex.app.domain.model

/** A developer / publisher with the IGDB company id, so its games can be browsed. */
data class CompanyRef(
    val id: Long,
    val name: String,
)

/** The role a company had on a game; decides which games its page lists. */
enum class CompanyRole(val label: String) {
    DEVELOPER("开发商"),
    PUBLISHER("发行商"),
    ;

    companion object {
        fun fromKey(key: String): CompanyRole = entries.firstOrNull { it.name == key } ?: DEVELOPER
    }
}
