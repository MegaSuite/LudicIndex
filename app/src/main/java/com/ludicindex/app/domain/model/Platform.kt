package com.ludicindex.app.domain.model

/**
 * A store / console platform. Only [XBOX] and [PSN] are used by the subscription catalogs so far;
 * the rest are declared now so owned-account sources can reuse the same library model later.
 */
enum class Platform(val label: String, val storeLabel: String) {
    XBOX("Xbox", "Xbox"),
    PSN("PlayStation", "PS Store"),
    STEAM("Steam", "Steam"),
    EPIC("Epic", "Epic"),
    GOG("GOG", "GOG"),
    NINTENDO("Nintendo", "Nintendo"),
    ;

    companion object {
        fun fromKey(key: String): Platform? = entries.firstOrNull { it.name == key }
    }
}
