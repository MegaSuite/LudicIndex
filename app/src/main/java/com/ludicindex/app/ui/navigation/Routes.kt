package com.ludicindex.app.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.ludicindex.app.R
import com.ludicindex.app.domain.model.CompanyRole

object Routes {
    const val HOME = "home"
    const val WISHLIST = "wishlist"
    const val SETTINGS = "settings"

    const val ARG_IGDB_ID = "igdbId"
    const val GAME_DETAIL = "game/{igdbId}"
    fun gameDetail(igdbId: Long): String = "game/${igdbId}"

    /** Paste-many-titles import flow. */
    const val IMPORT = "import"

    /** A developer / publisher's games. */
    const val ARG_COMPANY_ID = "companyId"
    const val ARG_COMPANY_ROLE = "role"
    const val ARG_COMPANY_NAME = "name"
    const val COMPANY = "company/{$ARG_COMPANY_ID}/{$ARG_COMPANY_ROLE}?$ARG_COMPANY_NAME={$ARG_COMPANY_NAME}"

    fun company(companyId: Long, role: CompanyRole, name: String): String =
        "company/$companyId/${role.name}?$ARG_COMPANY_NAME=${Uri.encode(name)}"

    val topLevel = setOf(HOME, WISHLIST, SETTINGS)
}

enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, R.string.tab_home, Icons.Filled.Home),
    WISHLIST(Routes.WISHLIST, R.string.tab_wishlist, Icons.Filled.Favorite),
    SETTINGS(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings),
}
