package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BackupEntry
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
  @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
  fun getAllNotes(): Flow<List<Note>>

  @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
  suspend fun getNoteById(id: Long): Note?

  @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY isPinned DESC, updatedAt DESC")
  fun searchNotes(query: String): Flow<List<Note>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNote(note: Note): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNotes(notes: List<Note>)

  @Update
  suspend fun updateNote(note: Note)

  @Delete
  suspend fun deleteNote(note: Note)

  @Query("DELETE FROM notes WHERE id = :id")
  suspend fun deleteNoteById(id: Long)

  @Query("DELETE FROM notes")
  suspend fun clearAllNotes()

  @Query("SELECT COUNT(*) FROM notes")
  suspend fun getNotesCount(): Int

  // Backups
  @Query("SELECT * FROM backups ORDER BY timestamp DESC")
  fun getAllBackups(): Flow<List<BackupEntry>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBackup(backup: BackupEntry): Long

  @Query("DELETE FROM backups WHERE id = :id")
  suspend fun deleteBackupById(id: Long)

  @Query("SELECT * FROM notes")
  suspend fun getAllNotesDirect(): List<Note>
}
