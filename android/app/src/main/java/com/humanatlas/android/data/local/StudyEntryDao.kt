package com.humanatlas.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyEntryDao {

    @Query("SELECT * FROM study_entries ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<StudyEntryEntity>>

    @Query("SELECT * FROM study_entries WHERE structure_id = :structureId LIMIT 1")
    fun observeById(structureId: String): Flow<StudyEntryEntity?>

    @Query("SELECT * FROM study_entries WHERE structure_id = :structureId LIMIT 1")
    suspend fun getById(structureId: String): StudyEntryEntity?

    @Query("SELECT COUNT(*) FROM study_entries")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: StudyEntryEntity)

    @Query("DELETE FROM study_entries WHERE structure_id = :structureId")
    suspend fun deleteById(structureId: String)
}
