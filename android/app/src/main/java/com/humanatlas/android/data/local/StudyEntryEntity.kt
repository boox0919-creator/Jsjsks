package com.humanatlas.android.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_entries",
    indices = [
        Index(value = ["is_bookmarked"]),
        Index(value = ["updated_at"])
    ]
)
data class StudyEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "structure_id")
    val structureId: String,

    @ColumnInfo(name = "structure_name")
    val structureName: String,

    @ColumnInfo(name = "system_id")
    val systemId: String,

    @ColumnInfo(name = "note_text")
    val noteText: String = "",

    @ColumnInfo(name = "is_bookmarked")
    val isBookmarked: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
