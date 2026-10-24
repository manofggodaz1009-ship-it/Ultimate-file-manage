package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BackupEntry
import com.example.data.model.Note
import com.example.ui.theme.ThemeMode
import com.example.util.BiometricAuthManager
import com.example.util.FileManagerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ScreenState {
  HOME,
  READER,
  EDITOR,
  BACKUP
}

data class NoteDraft(
  val id: Long = 0,
  val title: String = "",
  val content: String = "",
  val fileExtension: String = "txt",
  val customFileName: String = "",
  val isBiometricLocked: Boolean = false,
  val isPinned: Boolean = false,
  val tags: String = "",
  val externalUri: String? = null
)

class NotepadViewModel(application: Application) : AndroidViewModel(application) {

  private val db = AppDatabase.getDatabase(application)
  private val noteDao = db.noteDao()
  private val prefs = application.getSharedPreferences("secure_notepad_prefs", Context.MODE_PRIVATE)

  // App Lock State (100% Fingerprint Biometric)
  private val _isAppUnlocked = MutableStateFlow(false)
  val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

  // Theme Mode
  private val _themeMode = MutableStateFlow(loadThemePreference())
  val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

  // Navigation State
  private val _currentScreen = MutableStateFlow(ScreenState.HOME)
  val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

  // Active Note for Reader or Editor
  private val _activeNote = MutableStateFlow<Note?>(null)
  val activeNote: StateFlow<Note?> = _activeNote.asStateFlow()

  // Current Edit Draft
  private val _editDraft = MutableStateFlow(NoteDraft())
  val editDraft: StateFlow<NoteDraft> = _editDraft.asStateFlow()

  // Search and Filter
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedFilterExtension = MutableStateFlow<String?>(null) // null = all
  val selectedFilterExtension: StateFlow<String?> = _selectedFilterExtension.asStateFlow()

  // Toast / Status Message
  private val _userMessage = MutableStateFlow<String?>(null)
  val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

  // Notes List with Search and Filter combine
  val notes: StateFlow<List<Note>> = combine(
    noteDao.getAllNotes(),
    _searchQuery,
    _selectedFilterExtension
  ) { allNotes, query, extFilter ->
    var filtered = if (query.isBlank()) {
      allNotes
    } else {
      allNotes.filter {
        it.title.contains(query, ignoreCase = true) ||
          it.content.contains(query, ignoreCase = true) ||
          it.tags.contains(query, ignoreCase = true) ||
          it.customFileName.contains(query, ignoreCase = true)
      }
    }

    if (extFilter != null) {
      filtered = when (extFilter) {
        "PINNED" -> filtered.filter { it.isPinned }
        "LOCKED" -> filtered.filter { it.isBiometricLocked }
        else -> filtered.filter { it.fileExtension.equals(extFilter, ignoreCase = true) }
      }
    }

    filtered
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Backups Flow
  val backups: StateFlow<List<BackupEntry>> = noteDao.getAllBackups()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  init {
    seedInitialWelcomeNoteIfEmpty()
  }

  private fun loadThemePreference(): ThemeMode {
    val saved = prefs.getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
    return try {
      ThemeMode.valueOf(saved)
    } catch (e: Exception) {
      ThemeMode.DARK
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    _themeMode.value = mode
    prefs.edit().putString("theme_mode", mode.name).apply()
  }

  fun toggleThemeMode() {
    val next = when (_themeMode.value) {
      ThemeMode.DARK -> ThemeMode.LIGHT
      ThemeMode.LIGHT -> ThemeMode.DARK
      ThemeMode.SYSTEM -> ThemeMode.DARK
    }
    setThemeMode(next)
  }

  fun setAppUnlocked(unlocked: Boolean) {
    _isAppUnlocked.value = unlocked
  }

  fun lockApp() {
    _isAppUnlocked.value = false
    _activeNote.value = null
    _currentScreen.value = ScreenState.HOME
    showUserMessage("செயலி கைரேகையால் பூட்டப்பட்டது")
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setFilterExtension(ext: String?) {
    _selectedFilterExtension.value = if (_selectedFilterExtension.value == ext) null else ext
  }

  fun clearUserMessage() {
    _userMessage.value = null
  }

  fun showUserMessage(message: String) {
    _userMessage.value = message
  }

  fun navigateTo(screen: ScreenState) {
    _currentScreen.value = screen
  }

  fun openNoteForReading(note: Note) {
    _activeNote.value = note
    _currentScreen.value = ScreenState.READER
  }

  fun startNewNote() {
    _editDraft.value = NoteDraft(
      fileExtension = "txt"
    )
    _activeNote.value = null
    _currentScreen.value = ScreenState.EDITOR
  }

  fun startEditingNote(note: Note) {
    _activeNote.value = note
    _editDraft.value = NoteDraft(
      id = note.id,
      title = note.title,
      content = note.content,
      fileExtension = note.fileExtension,
      customFileName = note.customFileName,
      isBiometricLocked = note.isBiometricLocked,
      isPinned = note.isPinned,
      tags = note.tags,
      externalUri = note.externalUri
    )
    _currentScreen.value = ScreenState.EDITOR
  }

  fun updateDraft(
    title: String? = null,
    content: String? = null,
    fileExtension: String? = null,
    customFileName: String? = null,
    isBiometricLocked: Boolean? = null,
    isPinned: Boolean? = null,
    tags: String? = null
  ) {
    val current = _editDraft.value
    _editDraft.value = current.copy(
      title = title ?: current.title,
      content = content ?: current.content,
      fileExtension = fileExtension ?: current.fileExtension,
      customFileName = customFileName ?: current.customFileName,
      isBiometricLocked = isBiometricLocked ?: current.isBiometricLocked,
      isPinned = isPinned ?: current.isPinned,
      tags = tags ?: current.tags
    )
  }

  fun saveDraftToInternal(): Long {
    val draft = _editDraft.value
    var savedId = 0L
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      val note = Note(
        id = draft.id,
        title = draft.title.trim().ifBlank { "Untitled Note" },
        content = draft.content,
        fileExtension = draft.fileExtension.trim().removePrefix(".").ifBlank { "txt" },
        customFileName = draft.customFileName.trim(),
        isBiometricLocked = draft.isBiometricLocked,
        isPinned = draft.isPinned,
        tags = draft.tags.trim(),
        updatedAt = now,
        createdAt = if (draft.id == 0L) now else (_activeNote.value?.createdAt ?: now),
        externalUri = draft.externalUri
      )

      savedId = noteDao.insertNote(note)
      val updatedNote = note.copy(id = if (draft.id == 0L) savedId else draft.id)
      _activeNote.value = updatedNote
      _editDraft.value = draft.copy(id = updatedNote.id)

      // Trigger automatic backup snapshot
      triggerAutoBackup("தானியங்கி பேக்கப் - சேமிக்கப்பட்டது")

      showUserMessage("நோட் வெற்றிகரமாக சேமிக்கப்பட்டது (.${updatedNote.fileExtension})")
      _currentScreen.value = ScreenState.READER
    }
    return savedId
  }

  fun togglePin(note: Note) {
    viewModelScope.launch {
      val updated = note.copy(isPinned = !note.isPinned)
      noteDao.updateNote(updated)
      if (_activeNote.value?.id == note.id) {
        _activeNote.value = updated
      }
    }
  }

  fun toggleBiometricLock(note: Note) {
    viewModelScope.launch {
      val updated = note.copy(isBiometricLocked = !note.isBiometricLocked)
      noteDao.updateNote(updated)
      if (_activeNote.value?.id == note.id) {
        _activeNote.value = updated
      }
      val msg = if (updated.isBiometricLocked) {
        "இந்தக் குறிப்பிற்கு கைரேகை பூட்டு இயக்கப்பட்டது"
      } else {
        "கைரேகை பூட்டு நீக்கப்பட்டது"
      }
      showUserMessage(msg)
    }
  }

  fun deleteNote(note: Note) {
    viewModelScope.launch {
      noteDao.deleteNote(note)
      if (_activeNote.value?.id == note.id) {
        _activeNote.value = null
        _currentScreen.value = ScreenState.HOME
      }
      showUserMessage("குறிப்பு நீக்கப்பட்டது")
    }
  }

  /**
   * Save changes directly back to an external file opened from File Manager
   */
  fun saveBackToExternalUri(context: Context): Boolean {
    val draft = _editDraft.value
    val uriString = draft.externalUri ?: return false
    val uri = Uri.parse(uriString)
    val success = FileManagerHelper.writeTextToUri(context, uri, draft.content)
    if (success) {
      showUserMessage("ஃபைல் மேனேஜர் கோப்பில் நேரடியாக மாற்றங்கள் சேமிக்கப்பட்டது")
      // Also update in internal DB if present
      saveDraftToInternal()
    } else {
      showUserMessage("கோப்பை சேமிப்பதில் பிழை ஏற்பட்டது")
    }
    return success
  }

  /**
   * Open an external file from Android File Manager via SAF Uri
   */
  fun loadExternalFile(context: Context, uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val fileName = FileManagerHelper.getFileNameFromUri(context, uri)
        val content = FileManagerHelper.readTextFromUri(context, uri)

        // Parse title and extension from file name without forcing auto-extension
        val dotIndex = fileName.lastIndexOf('.')
        val ext = if (dotIndex != -1 && dotIndex < fileName.length - 1) {
          fileName.substring(dotIndex + 1)
        } else {
          "txt"
        }
        val cleanTitle = if (dotIndex != -1) fileName.substring(0, dotIndex) else fileName

        withContext(Dispatchers.Main) {
          _editDraft.value = NoteDraft(
            title = cleanTitle,
            content = content,
            fileExtension = ext,
            customFileName = fileName,
            externalUri = uri.toString()
          )
          _activeNote.value = null
          _currentScreen.value = ScreenState.EDITOR
          showUserMessage("ஃபைல் மேனேஜரிலிருந்து கோப்பு திறக்கப்பட்டது: $fileName")
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          showUserMessage("கோப்பை திறப்பதில் பிழை: ${e.localizedMessage}")
        }
      }
    }
  }

  /**
   * Export note to user chosen file in File Manager (CreateDocument)
   */
  fun exportCurrentNoteToSaf(context: Context, destUri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val draft = _editDraft.value
        val note = _activeNote.value ?: Note(
          title = draft.title,
          content = draft.content,
          fileExtension = draft.fileExtension,
          customFileName = draft.customFileName
        )

        context.contentResolver.openOutputStream(destUri, "wt")?.use { out ->
          FileManagerHelper.writeNoteToStream(note, out)
        }

        withContext(Dispatchers.Main) {
          showUserMessage("ஃபைல் மேனேஜரில் கோப்பு வெற்றிகரமாக சேமிக்கப்பட்டது: ${note.getEffectiveFileName()}")
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          showUserMessage("ஃபைல் மேனேஜரில் சேமிப்பதில் பிழை: ${e.localizedMessage}")
        }
      }
    }
  }

  /**
   * Export all notes as a single ZIP archive to SAF Uri
   */
  fun exportAllNotesToZipSaf(context: Context, destUri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val allNotes = noteDao.getAllNotesDirect()
        val zipBytes = FileManagerHelper.createNotesZip(allNotes)

        context.contentResolver.openOutputStream(destUri, "wt")?.use { out ->
          out.write(zipBytes)
          out.flush()
        }

        withContext(Dispatchers.Main) {
          showUserMessage("அனைத்து ${allNotes.size} குறிப்புகளும் ZIP கோப்பாக பதிவிறக்கப்பட்டது!")
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          showUserMessage("ZIP கோப்பை சேமிப்பதில் தோல்வி: ${e.localizedMessage}")
        }
      }
    }
  }

  /**
   * Auto backup creation
   */
  fun triggerAutoBackup(customName: String? = null) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val allNotes = noteDao.getAllNotesDirect()
        if (allNotes.isEmpty()) return@launch

        val jsonArray = JSONArray()
        for (n in allNotes) {
          val obj = JSONObject()
          obj.put("id", n.id)
          obj.put("title", n.title)
          obj.put("content", n.content)
          obj.put("fileExtension", n.fileExtension)
          obj.put("customFileName", n.customFileName)
          obj.put("isBiometricLocked", n.isBiometricLocked)
          obj.put("isPinned", n.isPinned)
          obj.put("tags", n.tags)
          obj.put("createdAt", n.createdAt)
          obj.put("updatedAt", n.updatedAt)
          jsonArray.put(obj)
        }

        val dateStr = SimpleDateFormat("dd-MMM HH:mm", Locale.getDefault()).format(Date())
        val name = customName ?: "தானியங்கி பேக்கப் ($dateStr)"

        val backup = BackupEntry(
          timestamp = System.currentTimeMillis(),
          noteCount = allNotes.size,
          backupName = name,
          serializedData = jsonArray.toString()
        )
        noteDao.insertBackup(backup)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  fun restoreBackup(backup: BackupEntry) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val jsonArray = JSONArray(backup.serializedData)
        val restoredList = mutableListOf<Note>()
        for (i in 0 until jsonArray.length()) {
          val obj = jsonArray.getJSONObject(i)
          val note = Note(
            title = obj.optString("title", "Restored Note"),
            content = obj.optString("content", ""),
            fileExtension = obj.optString("fileExtension", "txt"),
            customFileName = obj.optString("customFileName", ""),
            isBiometricLocked = obj.optBoolean("isBiometricLocked", false),
            isPinned = obj.optBoolean("isPinned", false),
            tags = obj.optString("tags", ""),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
          )
          restoredList.add(note)
        }

        noteDao.clearAllNotes()
        noteDao.insertNotes(restoredList)

        withContext(Dispatchers.Main) {
          showUserMessage("பேக்கப் மீட்டமைக்கப்பட்டது! (${restoredList.size} குறிப்புகள்)")
          _currentScreen.value = ScreenState.HOME
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          showUserMessage("மீட்டமைப்பதில் பிழை: ${e.localizedMessage}")
        }
      }
    }
  }

  fun deleteBackup(backup: BackupEntry) {
    viewModelScope.launch {
      noteDao.deleteBackupById(backup.id)
      showUserMessage("பேக்கப் நீக்கப்பட்டது")
    }
  }

  private fun seedInitialWelcomeNoteIfEmpty() {
    viewModelScope.launch(Dispatchers.IO) {
      if (noteDao.getNotesCount() == 0) {
        val welcomeNote = Note(
          title = "பாதுகாப்பான நோட்பேடிற்கு நல்வரவு",
          content = """
            பாதுகாப்பான நோட்பேட் (Secure Notepad) செயலிக்கு வரவேற்கிறோம்!

            முக்கிய அம்சங்கள்:
            1. 100% கைரேகை பாதுகாப்பு (Fingerprint Authentication):
               - கடவுச்சொல் அல்லது பின் (PIN) இல்லாமல் உங்கள் கைரேகையை வைத்து மட்டுமே திறக்க முடியும்.
            2. விருப்பப்படி கோப்பு நீட்சி (Custom Extension):
               - .txt, .pdf, .md, .json அல்லது நீங்கள் விரும்பும் எந்தவொரு தனிப்பயன் நீட்சியுடனும் ஃபைல் மேனேஜரில் நேரடியாக சேமிக்கலாம்.
            3. ஃபைல் மேனேஜர் ஒருங்கிணைப்பு:
               - சாதனத்தில் உள்ள கோப்புகளை நேரடியாகத் திறந்து திருத்தி சேமிக்கலாம்.
            4. ஆட்டோ பேக்கப் & ZIP பதிவிறக்கம்:
               - அனைத்து குறிப்புகளையும் ஒரே ZIP கோப்பாக ஏற்றுமதி செய்யலாம்.
            5. டார்க் மற்றும் லைட் மோடு:
               - எந்த நேரத்திலும் உங்கள் விருப்பத்திற்கேற்ப மாற்றிக் கொள்ளலாம்.
          """.trimIndent(),
          fileExtension = "txt",
          customFileName = "Welcome_Tamil.txt",
          isPinned = true,
          isBiometricLocked = false,
          tags = "தகவல்"
        )
        noteDao.insertNote(welcomeNote)
      }
    }
  }
}
