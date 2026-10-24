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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.ui.theme.ExtCustomColor
import com.example.ui.theme.ExtDocColor
import com.example.ui.theme.ExtJsonColor
import com.example.ui.theme.ExtMdColor
import com.example.ui.theme.ExtPdfColor
import com.example.ui.theme.ExtTxtColor
import com.example.ui.viewmodel.NotepadViewModel
import com.example.ui.viewmodel.ScreenState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteReaderScreen(
  viewModel: NotepadViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activeNote by viewModel.activeNote.collectAsState()

  BackHandler {
    viewModel.navigateTo(ScreenState.HOME)
  }

  if (activeNote == null) {
    viewModel.navigateTo(ScreenState.HOME)
    return
  }

  val note = activeNote!!
  var fontSizeSp by remember { mutableFloatStateOf(16f) }
  var showFontSlider by remember { mutableStateOf(false) }

  // SAF Export Launcher for saving directly to File Manager with user's specified extension
  val createDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("*/*")
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.exportCurrentNoteToSaf(context, uri)
    }
  }

  val extUpper = note.fileExtension.uppercase().removePrefix(".")
  val extColor = when (extUpper) {
    "TXT" -> ExtTxtColor
    "PDF" -> ExtPdfColor
    "MD" -> ExtMdColor
    "JSON" -> ExtJsonColor
    "DOC", "DOCX" -> ExtDocColor
    else -> ExtCustomColor
  }

  val wordCount = if (note.content.isBlank()) 0 else note.content.trim().split("\\s+".toRegex()).size
  val charCount = note.content.length
  val readingMinutes = (wordCount / 180).coerceAtLeast(1)

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "வாசிப்பு முறை (Reader)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
          )
        },
        navigationIcon = {
          IconButton(
            onClick = { viewModel.navigateTo(ScreenState.HOME) },
            modifier = Modifier.testTag("reader_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "பின்செல்"
            )
          }
        },
        actions = {
          // Toggle font size controls
          IconButton(
            onClick = { showFontSlider = !showFontSlider },
            modifier = Modifier.testTag("font_size_button")
          ) {
            Icon(
              imageVector = Icons.Default.FormatSize,
              contentDescription = "எழுத்து அளவு",
              tint = if (showFontSlider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
          }

          // Toggle Pin
          IconButton(onClick = { viewModel.togglePin(note) }) {
            Icon(
              imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
              contentDescription = "பின் செய்",
              tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
          }

          // Toggle Biometric Lock
          IconButton(onClick = { viewModel.toggleBiometricLock(note) }) {
            Icon(
              imageVector = if (note.isBiometricLocked) Icons.Default.Fingerprint else Icons.Default.LockOpen,
              contentDescription = "கைரேகை பூட்டு",
              tint = if (note.isBiometricLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
          }

          // Export to Android File Manager
          IconButton(
            onClick = {
              createDocumentLauncher.launch(note.getEffectiveFileName())
            },
            modifier = Modifier.testTag("reader_export_file_manager_button")
          ) {
            Icon(
              imageVector = Icons.Default.SaveAlt,
              contentDescription = "ஃபைல் மேனேஜரில் சேமி",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          // Share
          IconButton(
            onClick = {
              val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, note.title)
                putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
                type = "text/plain"
              }
              context.startActivity(Intent.createChooser(sendIntent, "பகிர் (Share Note)"))
            }
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "பகிர்"
            )
          }

          // Edit Note
          IconButton(
            onClick = { viewModel.startEditingNote(note) },
            modifier = Modifier.testTag("reader_edit_button")
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "திருத்து",
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
    ) {
      // Font size slider panel
      if (showFontSlider) {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("எழுத்து: ${fontSizeSp.toInt()}sp", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.width(12.dp))
            Slider(
              value = fontSizeSp,
              onValueChange = { fontSizeSp = it },
              valueRange = 12f..32f,
              modifier = Modifier.weight(1f),
              colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
              )
            )
          }
        }
      }

      // Scrollable Reader Content
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp)
      ) {
        // Metadata Row: Extension Badge, Filename, Biometric Status
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(extColor.copy(alpha = 0.15f))
              .border(1.dp, extColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
              .padding(horizontal = 10.dp, vertical = 3.dp)
          ) {
            Text(
              text = ".$extUpper",
              color = extColor,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }

          Text(
            text = note.getEffectiveFileName(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
          )

          Spacer(modifier = Modifier.weight(1f))

          if (note.isBiometricLocked) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Fingerprint,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "கைரேகை பாதுகாப்பு",
                  color = MaterialTheme.colorScheme.primary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Note Title
        Text(
          text = note.title.ifBlank { "Untitled Note" },
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Stats: Word count, character count, date
        val dateFormatted = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(note.updatedAt))
        Text(
          text = "$dateFormatted • $wordCount வார்த்தைகள் • $charCount எழுத்துகள் • ~$readingMinutes நிமிடம் வாசிப்பு",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal divider
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Main Note Body
        Text(
          text = note.content.ifBlank { "(இந்தக் குறிப்பில் உள்ளடக்கம் ஏதுமில்லை)" },
          fontSize = fontSizeSp.sp,
          lineHeight = (fontSizeSp * 1.55f).sp,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("reader_content_text")
        )

        Spacer(modifier = Modifier.height(60.dp))
      }
    }
  }
}
