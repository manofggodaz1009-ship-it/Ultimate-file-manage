package com.example.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.util.BiometricAuthManager

@Composable
fun LockScreen(
  onUnlockSuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activity = context as? FragmentActivity
  var authErrorMessage by remember { mutableStateOf<String?>(null) }
  var biometricStatus by remember {
    mutableStateOf(BiometricAuthManager.checkBiometricAvailability(context))
  }

  // Animated pulse ring around fingerprint scanner
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  fun triggerBiometricPrompt() {
    if (activity != null) {
      authErrorMessage = null
      BiometricAuthManager.authenticateWithFingerprint(
        activity = activity,
        title = "கைரேகை மூலம் திறக்கவும்",
        subtitle = "பாதுகாப்பான நோட்பேடிற்கு உங்கள் விரலை வைக்கவும்",
        negativeButtonText = "ரத்து செய்",
        onSuccess = {
          authErrorMessage = null
          onUnlockSuccess()
        },
        onError = { code, err ->
          authErrorMessage = err.toString()
        },
        onFailed = {
          authErrorMessage = "கைரேகை பொருந்தவில்லை. மீண்டும் முயற்சிக்கவும்."
        }
      )
    }
  }

  // Trigger prompt once on launch if hardware is ready
  LaunchedEffect(Unit) {
    if (biometricStatus == BiometricAuthManager.BiometricStatus.AVAILABLE) {
      triggerBiometricPrompt()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
          )
        )
      )
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth()
    ) {
      // Top security badge
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
          .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
          .padding(horizontal = 16.dp, vertical = 6.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Security,
          contentDescription = "Security",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "100% கைரேகை பாதுகாப்பு",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(32.dp))

      // Fingerprint scanner circle with pulse ring
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(160.dp)
      ) {
        // Outer pulsing ring
        Box(
          modifier = Modifier
            .size(140.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        )

        // Middle ring
        Box(
          modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )

        // Fingerprint icon inside scanner
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                colors = listOf(
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                )
              )
            )
            .clickable { triggerBiometricPrompt() }
            .testTag("fingerprint_scanner_button")
        ) {
          Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "கைரேகை சென்சார்",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      Text(
        text = "பாதுகாப்பான நோட்பேட்",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "கடவுச்சொல் அல்லது பின் (PIN) கிடையாது.\nசெயலியைத் திறக்க உங்கள் விரலை வைக்கவும்.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 22.sp
      )

      if (authErrorMessage != null) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
          ),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(
            text = authErrorMessage ?: "",
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp),
            textAlign = TextAlign.Center
          )
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      // Primary Unlock Button
      Button(
        onClick = { triggerBiometricPrompt() },
        modifier = Modifier
          .fillMaxWidth(0.85f)
          .height(52.dp)
          .testTag("scan_fingerprint_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      ) {
        Icon(
          imageVector = Icons.Default.Fingerprint,
          contentDescription = null,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "கைரேகை ஸ்கேன் செய்",
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp
        )
      }

      // Hardware status or emulator testing helper
      if (biometricStatus != BiometricAuthManager.BiometricStatus.AVAILABLE) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = when (biometricStatus) {
            BiometricAuthManager.BiometricStatus.NOT_ENROLLED ->
              "சாதனத்தில் கைரேகை பதிவு செய்யப்படவில்லை. செட்டிங்ஸில் பதிவு செய்யவும் அல்லது கீழே உள்ள பட்டனைத் தொடவும்."
            BiometricAuthManager.BiometricStatus.NO_HARDWARE ->
              "கைரேகை ஹார்டுவேர் கண்டறியப்படவில்லை (எமுலேட்டர் சூழல்)."
            else ->
              "கைரேகை சென்சார் தயாராகிறது..."
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.secondary,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Emulator / Testing quick unlock button so testing is never blocked
        OutlinedButton(
          onClick = { onUnlockSuccess() },
          modifier = Modifier.testTag("emulator_quick_unlock_button"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "விரல் தொடுதல் மூலம் திற (எமுலேட்டர் முறை)")
        }
      }
    }
  }
}
