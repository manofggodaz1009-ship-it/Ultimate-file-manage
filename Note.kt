package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val title: String,
  val content: String,
  val fileExtension: String = "txt", // Custom extension chosen by user, e.g. "pdf", "txt", "md", "json", "doc"
  val customFileName: String = "",   // Full custom filename, e.g. "secret_diary.pdf"
  val isBiometricLocked: Boolean = false, // Require fingerprint authentication for this note
  val isPinned: Boolean = false,
  val tags: String = "",             // Optional categories or tags
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val externalUri: String? = null    // Original SAF Uri if opened from Android File Manager
) {
  /**
   * Returns clean display filename respecting user's exact specification.
   * If customFileName is set by user, returns that verbatim without forced suffix.
   */
  fun getEffectiveFileName(): String {
    if (customFileName.isNotBlank()) {
      return customFileName
    }
    val safeTitle = title.trim().ifBlank { "Untitled_Note" }.replace(" ", "_")
    val cleanExt = fileExtension.trim().removePrefix(".")
    return if (cleanExt.isNotBlank()) "$safeTitle.$cleanExt" else safeTitle
  }
}
