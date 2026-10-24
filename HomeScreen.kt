package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.model.Note
import com.example.ui.theme.ExtCustomColor
import com.example.ui.theme.ExtDocColor
import com.example.ui.theme.ExtJsonColor
import com.example.ui.theme.ExtMdColor
import com.example.ui.theme.ExtPdfColor
import com.example.ui.theme.ExtTxtColor
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.NotepadViewModel
import com.example.ui.viewmodel.ScreenState
import com.example.util.BiometricAuthManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  viewModel: NotepadViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activity = context as? FragmentActivity
  val notes by viewModel.notes.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val selectedFilter by viewModel.selectedFilterExtension.collectAsState()
  val themeMode by viewModel.themeMode.collectAsState()

  // SAF File Open Launcher (File Manager Integration)
  val openDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.loadExternalFile(context, uri)
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "பாதுகாப்பான நோட்பேட்",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Secure Notepad • கைரேகை பாதுகாப்பு",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        },
        actions = {
          // Theme Switcher (Dark / Light)
          IconButton(
            onClick = { viewModel.toggleThemeMode() },
            modifier = Modifier.testTag("theme_toggle_button")
          ) {
            Icon(
              imageVector = if (themeMode == ThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
              contentDescription = "தீம் மாற்று",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          // Backup & ZIP Archive Button
          IconButton(
            onClick = { viewModel.navigateTo(ScreenState.BACKUP) },
            modifier = Modifier.testTag("backup_zip_button")
          ) {
            Icon(
              imageVector = Icons.Default.Archive,
              contentDescription = "பேக்கப் & ZIP",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }

          // Immediate Lock App Button
          IconButton(
            onClick = { viewModel.lockApp() },
            modifier = Modifier.testTag("lock_app_button")
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "பூட்டு",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        )
      )
    },
    floatingActionButton = {
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Extended FAB: Open File from Android File Manager
        ExtendedFloatingActionButton(
          onClick = {
            openDocumentLauncher.launch(arrayOf("*/*"))
          },
          icon = {
            Icon(
              imageVector = Icons.Default.FileOpen,
              contentDescription = "கோப்பை திற"
            )
          },
          text = {
            Text(
              text = "கோப்பை திறக்க",
              fontWeight = FontWeight.Medium
            )
          },
          containerColor = MaterialTheme.colorScheme.surfaceVariant,
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.testTag("open_file_fab")
        )

        // Primary FAB: New Secure Note
        FloatingActionButton(
          onClick = { viewModel.startNewNote() },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          shape = CircleShape,
          modifier = Modifier
            .size(56.dp)
            .testTag("new_note_fab")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "புதிய குறிப்பு",
            modifier = Modifier.size(28.dp)
          )
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { viewModel.setSearchQuery(it) },
        placeholder = { Text("குறிப்புகளைத் தேடுக... (தலைப்பு, உள்ளடக்கம்)") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { viewModel.setSearchQuery("") }) {
              Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear"
              )
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface,
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .testTag("search_text_field")
      )

      // Extension & Category Filter Chips
      val filterOptions = listOf(
        null to "அனைத்தும்",
        "PINNED" to "முக்கியமானது",
        "LOCKED" to "கைரேகை பூட்டு",
        "txt" to ".txt",
        "pdf" to ".pdf",
        "md" to ".md",
        "json" to ".json",
        "doc" to ".doc"
      )

      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(filterOptions) { (key, label) ->
          val isSelected = selectedFilter == key
          FilterChip(
            selected = isSelected,
            onClick = { viewModel.setFilterExtension(key) },
            label = { Text(label, fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(10.dp)
          )
        }
      }

      // Notes List or Empty State
      if (notes.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.Fingerprint,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
              modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = if (searchQuery.isNotBlank()) "பொருந்தும் குறிப்புகள் இல்லை" else "குறிப்புகள் ஏதுமில்லை",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "புதிய குறிப்பை உருவாக்க கீழேயுள்ள '+' பட்டனை அழுத்தவும் அல்லது சாதனக் கோப்பைத் திறக்கவும்.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(notes, key = { it.id }) { note ->
            NoteCardItem(
              note = note,
              onClick = {
                if (note.isBiometricLocked && activity != null) {
                  BiometricAuthManager.authenticateWithFingerprint(
                    activity = activity,
                    title = "ரகசியக் குறிப்பைத் திறக்க",
                    subtitle = note.title,
                    onSuccess = { viewModel.openNoteForReading(note) },
                    onError = { _, err -> viewModel.showUserMessage(err.toString()) }
                  )
                } else {
                  viewModel.openNoteForReading(note)
                }
              },
              onTogglePin = { viewModel.togglePin(note) },
              onToggleLock = { viewModel.toggleBiometricLock(note) },
              onDelete = { viewModel.deleteNote(note) }
            )
          }
        }
      }
    }
  }
}

@Composable
fun NoteCardItem(
  note: Note,
  onClick: () -> Unit,
  onTogglePin: () -> Unit,
  onToggleLock: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  var menuExpanded by remember { mutableStateOf(false) }
  val extUpper = note.fileExtension.uppercase().removePrefix(".")
  val extColor = when (extUpper) {
    "TXT" -> ExtTxtColor
    "PDF" -> ExtPdfColor
    "MD" -> ExtMdColor
    "JSON" -> ExtJsonColor
    "DOC", "DOCX" -> ExtDocColor
    else -> ExtCustomColor
  }

  val dateString = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(note.updatedAt))

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable { onClick() }
      .testTag("note_item_${note.id}"),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = if (note.isPinned) {
      androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
      androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    },
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      // Top row: Extension tag, Pin icon, Lock icon, Menu
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Custom Extension Badge
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(extColor.copy(alpha = 0.15f))
              .border(1.dp, extColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
              .padding(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text(
              text = if (extUpper.isNotBlank()) ".$extUpper" else ".TXT",
              color = extColor,
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp
            )
          }

          if (note.isBiometricLocked) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Fingerprint,
                  contentDescription = "பூட்டப்பட்டது",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "கைரேகை",
                  color = MaterialTheme.colorScheme.primary,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }

          if (note.isPinned) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Default.PushPin,
              contentDescription = "Pinned",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp)
            )
          }
        }

        Box {
          IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "Options",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
          ) {
            DropdownMenuItem(
              text = { Text(if (note.isPinned) "பின்னை நீக்கு" else "முக்கியமானதாக பின் செய்") },
              leadingIcon = {
                Icon(
                  imageVector = if (note.isPinned) Icons.Outlined.PushPin else Icons.Default.PushPin,
                  contentDescription = null
                )
              },
              onClick = {
                menuExpanded = false
                onTogglePin()
              }
            )
            DropdownMenuItem(
              text = { Text(if (note.isBiometricLocked) "கைரேகை பூட்டை நீக்கு" else "கைரேகை மூலம் பூட்டு") },
              leadingIcon = {
                Icon(
                  imageVector = if (note.isBiometricLocked) Icons.Default.LockOpen else Icons.Default.Fingerprint,
                  contentDescription = null
                )
              },
              onClick = {
                menuExpanded = false
                onToggleLock()
              }
            )
            DropdownMenuItem(
              text = { Text("நீக்கு (Delete)", color = MaterialTheme.colorScheme.error) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error
                )
              },
              onClick = {
                menuExpanded = false
                onDelete()
              }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Title
      Text(
        text = note.title.ifBlank { "Untitled Note" },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Content preview (Hidden if note is biometric locked)
      val previewText = if (note.isBiometricLocked) {
        "🔐 கைரேகை மூலம் பூட்டப்பட்ட ரகசிய குறிப்பு. திறக்க கிளிக் செய்யவும்."
      } else {
        note.content.ifBlank { "உள்ளடக்கம் இல்லை" }
      }

      Text(
        text = previewText,
        style = MaterialTheme.typography.bodySmall,
        color = if (note.isBiometricLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 18.sp
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Bottom Row: Date and Custom Filename
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = dateString,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Text(
          text = note.getEffectiveFileName(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
