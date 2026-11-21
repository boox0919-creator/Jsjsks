package com.humanatlas.android.data.repository

import com.humanatlas.android.data.local.StudyEntryDao
import com.humanatlas.android.data.local.StudyEntryEntity
import com.humanatlas.android.model.SelectedStructure
import kotlinx.coroutines.flow.Flow

class StudyRepository(private val dao: StudyEntryDao) {

    val allEntries: Flow<List<StudyEntryEntity>> = dao.observeAll()

    val savedCount: Flow<Int> = dao.observeCount()

    fun observeEntry(structureId: String): Flow<StudyEntryEntity?> =
        dao.observeById(structureId)

    suspend fun saveNote(
        structure: SelectedStructure,
        noteText: String,
        isBookmarked: Boolean
    ) {
        val cleaned = noteText.trim()
        if (cleaned.isEmpty() && !isBookmarked) {
            dao.deleteById(structure.id)
            return
        }
        val existing = dao.getById(structure.id)
        val now = System.currentTimeMillis()
        dao.upsert(
            StudyEntryEntity(
                structureId = structure.id,
                structureName = structure.name,
                systemId = structure.system,
                noteText = cleaned,
                isBookmarked = isBookmarked,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
    }

    suspend fun toggleBookmark(structure: SelectedStructure): Boolean {
        val existing = dao.getById(structure.id)
        val nextBookmarked = !(existing?.isBookmarked ?: false)
        val existingNote = existing?.noteText.orEmpty().trim()
        if (!nextBookmarked && existingNote.isEmpty()) {
            dao.deleteById(structure.id)
        } else {
            val now = System.currentTimeMillis()
            dao.upsert(
                StudyEntryEntity(
                    structureId = structure.id,
                    structureName = structure.name,
                    systemId = structure.system,
                    noteText = existingNote,
                    isBookmarked = nextBookmarked,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
            )
        }
        return nextBookmarked
    }

    suspend fun toggleBookmarkByEntry(entry: StudyEntryEntity): Boolean {
        val nextBookmarked = !entry.isBookmarked
        if (!nextBookmarked && entry.noteText.isBlank()) {
            dao.deleteById(entry.structureId)
        } else {
            dao.upsert(
                entry.copy(
                    isBookmarked = nextBookmarked,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return nextBookmarked
    }

    suspend fun deleteEntry(structureId: String) {
        dao.deleteById(structureId)
    }
}
