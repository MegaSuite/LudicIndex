package com.ludicindex.app.ui.wishlist

import com.ludicindex.app.domain.WishlistSort

val WishlistSort.displayLabel: String
    get() = when (this) {
        WishlistSort.ADDED_DESC -> "最近添加"
        WishlistSort.NAME_ASC -> "名称"
        WishlistSort.RELEASE_DATE_DESC -> "发行日期"
        WishlistSort.RATING_DESC -> "评分"
    }

/** Colors offered when the user creates a custom tag. */
val TAG_PALETTE: List<Int> = listOf(
    0xFF5C6BC0.toInt(),
    0xFF0070D1.toInt(),
    0xFF107C10.toInt(),
    0xFFE60012.toInt(),
    0xFF00897B.toInt(),
    0xFFF9A825.toInt(),
    0xFFC62828.toInt(),
    0xFF6D4C41.toInt(),
)
