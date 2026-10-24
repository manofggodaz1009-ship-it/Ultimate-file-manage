package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.BackupEntry
import com.example.ui.viewmodel.NotepadViewModel
import com.example.ui.viewmodel.ScreenState
import com.example.util.FileManagerHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupZipScreen(
  viewModel: NotepadViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val backups by viewModel.backups.collectAsState()
  val allNotes by viewModel.notes.collectAsState()

  BackHandler {
    viewModel.navigateTo(ScreenState.HOME)
  }

  var selectedBackupToRestore by remember { mutableStateOf<BackupEntry?>(null) }

  // SAF Zip download launcher
  val createZipLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/zip")
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.exportAllNotesToZipSaf(context, uri)
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "பேக்கப் & ZIP டவுன்லோட்",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
          )
        },
        navigationIcon = {
          IconButton(
            onClick = { viewModel.navigateTo(ScreenState.HOME) },
            modifier = Modifier.testTag("backup_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "பின்செல்"
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        )
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. ZIP Download Banner Card
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
          ),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Archive,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "மொத்த குறிப்புகளையும் ZIP ஆக பதிவிறக்க",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "உங்கள் அனைத்து குறிப்புகளையும் (.txt, .pdf, .md வடிவில்) ஒரே ZIP கோப்பாக மாற்றவும்.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Direct Download Button
              Button(
                onClick = {
                  val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                  createZipLauncher.launch("SecureNotepad_Notes_$dateStamp.zip")
                },
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("download_zip_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary
                )
              ) {
                Icon(
                  imageVector = Icons.Default.Download,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "ZIP டவுன்லோட்",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
              }

              // Share ZIP Button
              OutlinedButton(
                onClick = {
                  try {
                    val zipBytes = FileManagerHelper.createNotesZip(allNotes)
                    val cacheFile = File(context.cacheDir, "SecureNotepad_Backup.zip")
                    cacheFile.writeBytes(zipBytes)
                    val contentUri = FileProvider.getUriForFile(
                      context,
                      "${context.packageName}.fileprovider",
                      cacheFile
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                      type = "application/zip"
                      putExtra(Intent.EXTRA_STREAM, contentUri)
                      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "ZIP கோப்பைப் பகிர்"))
                  } catch (e: Exception) {
                    viewModel.showUserMessage("பகிர்வதில் பிழை: ${e.localizedMessage}")
                  }
                },
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("share_zip_button"),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Share,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "ZIP பகிர்",
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp
                )
              }
            }
          }
        }
      }

      // 2. Auto-Backup Section Card
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Backup,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "தானியங்கி பேக்கப் (Auto Backup)",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "தரவுகள் இழக்காமல் இருக்க குறிப்புகள் சேமிக்கப்படும்போது தானாக பேக்கப் எடுக்கப்படுகிறது.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Button(
              onClick = {
                viewModel.triggerAutoBackup("பயனர் பேக்கப் (Manual Backup)")
                viewModel.showUserMessage("புதிய பேக்கப் வெற்றிகரமாக உருவாக்கப்பட்டது!")
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("create_backup_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
              )
            ) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "இப்போதே பேக்கப் உருவாக்கு (Backup Now)",
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }

      // 3. Existing Backups History Header
      item {
        Text(
          text = "பேக்கப் வரலாறு (Backup History):",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
      }

      if (backups.isEmpty()) {
        item {
          Text(
            text = "பேக்கப் பதிவுகள் ஏதுமில்லை.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        items(backups, key = { it.id }) { backup ->
          val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(backup.timestamp))
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = backup.backupName,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "$dateStr • ${backup.noteCount} குறிப்புகள்",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              Row {
                // Restore button
                IconButton(
                  onClick = { selectedBackupToRestore = backup },
                  modifier = Modifier.testTag("restore_backup_${backup.id}")
                ) {
                  Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "மீட்டமை",
                    tint = MaterialTheme.colorScheme.primary
                  )
                }

                // Delete button
                IconButton(
                  onClick = { viewModel.deleteBackup(backup) }
                ) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "நீக்கு",
                    tint = MaterialTheme.colorScheme.error
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // Confirmation Dialog for Restore
  if (selectedBackupToRestore != null) {
    val b = selectedBackupToRestore!!
    AlertDialog(
      onDismissRequest = { selectedBackupToRestore = null },
      title = { Text("பேக்கப் மீட்டமைக்கவா?") },
      text = {
        Text("இந்த பேக்கப்பில் உள்ள ${b.noteCount} குறிப்புகள் மீட்டமைக்கப்படும். தற்போதைய குறிப்புகள் மாற்றப்படும். தொடரவா?")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.restoreBackup(b)
            selectedBackupToRestore = null
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
          )
        ) {
          Text("மீட்டமை")
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedBackupToRestore = null }) {
          Text("ரத்து செய்")
        }
      }
    )
  }
}
