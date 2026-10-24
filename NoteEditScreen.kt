package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExtCustomColor
import com.example.ui.theme.ExtDocColor
import com.example.ui.theme.ExtJsonColor
import com.example.ui.theme.ExtMdColor
import com.example.ui.theme.ExtPdfColor
import com.example.ui.theme.ExtTxtColor
import com.example.ui.viewmodel.NotepadViewModel
import com.example.ui.viewmodel.ScreenState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditScreen(
  viewModel: NotepadViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val draft by viewModel.editDraft.collectAsState()

  BackHandler {
    viewModel.navigateTo(ScreenState.HOME)
  }

  // Quick preset extension chips
  val commonExtensions = listOf("txt", "pdf", "md", "json", "doc", "log", "csv", "xml", "py")

  // SAF Export Launcher for directly saving in File Manager with exact user extension
  val createDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("*/*")
  ) { uri: Uri? ->
    if (uri != null) {
      // First save internal draft
      viewModel.saveDraftToInternal()
      // Then write to chosen external URI
      viewModel.exportCurrentNoteToSaf(context, uri)
    }
  }

  val wordCount = if (draft.content.isBlank()) 0 else draft.content.trim().split("\\s+".toRegex()).size
  val charCount = draft.content.length

  Scaffold(
    modifier = modifier.fillMaxSize().imePadding(),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (draft.id == 0L) "புதிய குறிப்பு" else "குறிப்பைத் திருத்து",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
          )
        },
        navigationIcon = {
          IconButton(
            onClick = { viewModel.navigateTo(ScreenState.HOME) },
            modifier = Modifier.testTag("edit_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "பின்செல்"
            )
          }
        },
        actions = {
          // Direct Save to Internal Notepad Button
          IconButton(
            onClick = { viewModel.saveDraftToInternal() },
            modifier = Modifier.testTag("save_internal_button")
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "சேமி",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        )
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Info Card regarding NO AUTO-EXTENSION
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Extension,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "விருப்ப நீட்சி: ஆட்டோ எக்ஸ்டென்ஷன்ஸ் வலுக்கட்டாயமாகச் சேர்க்கப்படாது. நீங்கள் குறிப்பிடும் வடிவத்திலேயே (.pdf, .txt அல்லது எந்த நீட்சியிலும்) நேரடியாகச் சேமிக்கப்படும்.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 18.sp
          )
        }
      }

      // Title TextField
      OutlinedTextField(
        value = draft.title,
        onValueChange = { newTitle ->
          viewModel.updateDraft(
            title = newTitle,
            customFileName = if (draft.customFileName.isBlank() || draft.customFileName.startsWith(draft.title)) {
              val cleanExt = draft.fileExtension.trim().removePrefix(".")
              "${newTitle.trim().replace(" ", "_")}.${cleanExt}"
            } else draft.customFileName
          )
        },
        label = { Text("தலைப்பு (Title)") },
        placeholder = { Text("குறிப்பின் தலைப்பை உள்ளிடவும்...") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("note_title_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface,
          focusedBorderColor = MaterialTheme.colorScheme.primary
        )
      )

      // Custom File Extension & Filename Box
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "கோப்பு வடிவம் & நீட்சி (File Name & Extension):",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )

          // Custom File Name text input (User can write any filename + any extension)
          OutlinedTextField(
            value = draft.customFileName,
            onValueChange = { newName ->
              val dotIdx = newName.lastIndexOf('.')
              val ext = if (dotIdx != -1 && dotIdx < newName.length - 1) {
                newName.substring(dotIdx + 1)
              } else {
                draft.fileExtension
              }
              viewModel.updateDraft(customFileName = newName, fileExtension = ext)
            },
            label = { Text("முழு கோப்புப் பெயர் (உதாரணம்: diary.pdf, notes.txt, data.json)") },
            placeholder = { Text("my_secret_notes.pdf") },
            singleLine = true,
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
              )
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("custom_filename_input")
          )

          // Quick Extension Selector Chips
          Text(
            text = "விரைவு நீட்சி தேர்வு (Quick Presets):",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            commonExtensions.forEach { ext ->
              val isSelected = draft.fileExtension.equals(ext, ignoreCase = true)
              FilterChip(
                selected = isSelected,
                onClick = {
                  val base = draft.title.trim().ifBlank { "Note" }.replace(" ", "_")
                  viewModel.updateDraft(
                    fileExtension = ext,
                    customFileName = "$base.$ext"
                  )
                },
                label = { Text(".$ext", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.primary
                )
              )
            }
          }
        }
      }

      // Main Note Content TextField
      OutlinedTextField(
        value = draft.content,
        onValueChange = { viewModel.updateDraft(content = it) },
        label = { Text("நோட்ஸ் விவரங்கள் (Content)") },
        placeholder = { Text("உங்கள் குறிப்புகளை இங்கே எழுதவும்...") },
        modifier = Modifier
          .fillMaxWidth()
          .height(280.dp)
          .testTag("note_content_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface,
          focusedBorderColor = MaterialTheme.colorScheme.primary
        )
      )

      // Character & Word Counter
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "$wordCount வார்த்தைகள் • $charCount எழுத்துகள்",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "சேமிக்கப்படும் வடிவம்: .${draft.fileExtension}",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      // Security & Pin Toggles
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Biometric Lock Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "கைரேகை பாதுகாப்பு (Fingerprint Lock)",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "இந்தக் குறிப்பைத் திறக்க கைரேகை கட்டாயம் தேவை",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Switch(
              checked = draft.isBiometricLocked,
              onCheckedChange = { viewModel.updateDraft(isBiometricLocked = it) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
              ),
              modifier = Modifier.testTag("biometric_lock_switch")
            )
          }

          // Pin to Top Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "முக்கியமானதாக பின் செய் (Pin to Top)",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "பட்டியலின் மேலே எப்போதும் தெரியும்",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Switch(
              checked = draft.isPinned,
              onCheckedChange = { viewModel.updateDraft(isPinned = it) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
              )
            )
          }
        }
      }

      // Action Buttons Row: Save to File Manager (SAF) & Save to Notepad
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // 1. Direct Save to File Manager (SAF)
        Button(
          onClick = {
            val effName = if (draft.customFileName.isNotBlank()) {
              draft.customFileName.trim()
            } else {
              val titleClean = draft.title.trim().ifBlank { "Untitled" }.replace(" ", "_")
              val extClean = draft.fileExtension.trim().removePrefix(".")
              "$titleClean.$extClean"
            }
            createDocumentLauncher.launch(effName)
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("save_to_file_manager_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          )
        ) {
          Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "ஃபைல் மேனேஜரில் நேரடியாக சேமி",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }

        // 2. If opened from File Manager, allow saving directly back to original Uri
        if (draft.externalUri != null) {
          OutlinedButton(
            onClick = {
              viewModel.saveBackToExternalUri(context)
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("save_back_to_original_file_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(
              imageVector = Icons.Default.UploadFile,
              contentDescription = null,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "அசல் கோப்பில் சேமிக்க (Save to Original)",
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        // 3. Save inside Secure Notepad
        OutlinedButton(
          onClick = { viewModel.saveDraftToInternal() },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("save_to_notepad_button"),
          shape = RoundedCornerShape(14.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Save,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "நோட்பேடில் சேமி (Save in Notepad)",
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(40.dp))
    }
  }
}
