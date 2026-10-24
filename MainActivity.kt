package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.ui.screens.BackupZipScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.NoteEditScreen
import com.example.ui.screens.NoteReaderScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NotepadViewModel
import com.example.ui.viewmodel.ScreenState

class MainActivity : FragmentActivity() {

  private val viewModel: NotepadViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    handleIncomingIntent(intent)

    setContent {
      val themeMode by viewModel.themeMode.collectAsState()
      val isUnlocked by viewModel.isAppUnlocked.collectAsState()
      val currentScreen by viewModel.currentScreen.collectAsState()
      val userMessage by viewModel.userMessage.collectAsState()
      val snackbarHostState = remember { SnackbarHostState() }

      LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
          Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
          viewModel.clearUserMessage()
        }
      }

      MyApplicationTheme(themeMode = themeMode) {
        Box(modifier = Modifier.fillMaxSize()) {
          if (!isUnlocked) {
            LockScreen(
              onUnlockSuccess = {
                viewModel.setAppUnlocked(true)
              }
            )
          } else {
            when (currentScreen) {
              ScreenState.HOME -> HomeScreen(viewModel = viewModel)
              ScreenState.READER -> NoteReaderScreen(viewModel = viewModel)
              ScreenState.EDITOR -> NoteEditScreen(viewModel = viewModel)
              ScreenState.BACKUP -> BackupZipScreen(viewModel = viewModel)
            }
          }
          SnackbarHost(hostState = snackbarHostState)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIncomingIntent(intent)
  }

  private fun handleIncomingIntent(intent: Intent?) {
    if (intent == null) return
    val action = intent.action
    val uri = intent.data
    if ((Intent.ACTION_VIEW == action || Intent.ACTION_EDIT == action) && uri != null) {
      viewModel.loadExternalFile(this, uri)
    }
  }
}
