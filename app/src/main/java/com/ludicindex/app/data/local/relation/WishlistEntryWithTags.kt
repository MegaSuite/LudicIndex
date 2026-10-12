package com.ludicindex.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.ludicindex.app.data.local.entity.EntryTagCrossRef
import com.ludicindex.app.data.local.entity.TagEntity
import com.ludicindex.app.data.local.entity.WishlistEntryEntity

data class WishlistEntryWithTags(
    @Embedded
    val entry: WishlistEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EntryTagCrossRef::class,
            parentColumn = "entry_id",
            entityColumn = "tag_id",
        ),
    )
    val tags: List<TagEntity>,
)
