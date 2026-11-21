package com.pinfold.app.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class BookmarkWithTags(
    @Embedded val bookmark: Bookmark,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BookmarkTag::class,
            parentColumn = "bookmarkId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<Tag>,
)
