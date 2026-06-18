package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.util.Localization
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: FinanceViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("security_settings", Context.MODE_PRIVATE) }
    val savedPin = remember { prefs.getString("password", "") ?: "" }
    val isBiometricEnabled = remember { prefs.getBoolean("biometric_enabled", false) }

    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    var pinInput by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }

    val activity = context as? androidx.fragment.app.FragmentActivity

    fun triggerBiometric() {
        if (isBiometricEnabled && activity != null && com.example.ui.util.BiometricHelper.isBiometricAvailable(context)) {
            com.example.ui.util.BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = Localization.getString("sec_biometric_prompt", isId),
                subtitle = Localization.getString("sec_biometric_desc", isId),
                negativeButtonText = Localization.getString("close", isId),
                onSuccess = {
                    Toast.makeText(context, Localization.getString("login_access_granted", isId), Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                },
                onError = { err ->
                    // user can enter PIN manually
                }
            )
        }
    }

    var showEnterTransition by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showEnterTransition = true
        triggerBiometric()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = showEnterTransition,
            enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                animationSpec = tween(500, easing = EaseOutBack),
                initialOffsetY = { 100 }
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .testTag("login_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(28.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Shield security icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Security Screen",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DuitKu",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = Localization.getString("login_desc", isId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Numerical PIN input with keyboard lock and strictly digits check
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { input ->
                            // Enforce digits-only rule and max 6 characters length
                            if (input.all { it.isDigit() } && input.length <= 6) {
                                pinInput = input
                                if (input.length == 6) {
                                    if (input == savedPin) {
                                        Toast.makeText(context, Localization.getString("login_access_granted", isId), Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    } else {
                                        Toast.makeText(context, Localization.getString("login_incorrect_pin", isId), Toast.LENGTH_SHORT).show()
                                        pinInput = ""
                                    }
                                }
                            }
                        },
                        label = { Text(Localization.getString("login_label_pin", isId)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPinVisible = !isPinVisible }) {
                                Icon(
                                    imageVector = if (isPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isPinVisible) "Hide PIN" else "Show PIN"
                                )
                            }
                        },
                        visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (isBiometricEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        IconButton(
                            onClick = { triggerBiometric() },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security Biometric Login",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = if (isId) "Ketuk untuk Sidik Jari" else "Tap for Biometric",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { triggerBiometric() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Localization.getString("login_footer_secured", isId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
