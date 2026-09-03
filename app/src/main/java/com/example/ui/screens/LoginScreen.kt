package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.util.BiometricHelper
import com.example.ui.util.Localization
import com.example.ui.viewmodel.FinanceViewModel

/**
 * Pure Local Authentication Screen.
 * Relies entirely on local credentials (PIN & Biometrics) stored on-device.
 * Zero external email/password dependencies, email verification, or cloud accounts required.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: FinanceViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("security_settings", Context.MODE_PRIVATE) }
    val isRegistered = remember { prefs.getBoolean("is_registered", false) }
    val savedPin = remember { prefs.getString("password", "") ?: "" }
    val isBiometricEnabled = remember { prefs.getBoolean("biometric_enabled", false) }

    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"
    val greetingName by viewModel.userGreetingName.collectAsState()

    var pinInput by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Quick PIN setup state if app is opened in unconfigured state
    var isSettingUpPin by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }

    val activity = context as? androidx.fragment.app.FragmentActivity

    fun triggerBiometric() {
        if (isBiometricEnabled && activity != null && BiometricHelper.isBiometricAvailable(context)) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = Localization.getString("sec_biometric_prompt", isId),
                subtitle = Localization.getString("sec_biometric_desc", isId),
                negativeButtonText = Localization.getString("close", isId),
                onSuccess = {
                    Toast.makeText(context, Localization.getString("login_access_granted", isId), Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                },
                onError = {
                    // Fallback to manual PIN entry
                }
            )
        }
    }

    var showEnterTransition by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showEnterTransition = true
        if (isRegistered && savedPin.isNotEmpty() && isBiometricEnabled) {
            triggerBiometric()
        }
    }

    fun submitPin(pin: String) {
        if (pin.length != 6) {
            errorMessage = if (isId) "PIN harus 6 digit angka" else "PIN must be 6 numeric digits"
            return
        }
        if (pin == savedPin) {
            errorMessage = null
            Toast.makeText(context, Localization.getString("login_access_granted", isId), Toast.LENGTH_SHORT).show()
            onLoginSuccess()
        } else {
            errorMessage = Localization.getString("login_incorrect_pin", isId)
            pinInput = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = showEnterTransition,
            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                animationSpec = tween(450, easing = EaseOutBack),
                initialOffsetY = { 80 }
            )
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .padding(20.dp)
                    .testTag("login_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Security Shield Header
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Local Security",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // App Title & Local User Session
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DuitKu",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = if (isId) "Akun Lokal: $greetingName" else "Local User: $greetingName",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    if (isRegistered && savedPin.isNotEmpty() && !isSettingUpPin) {
                        // Standard registered PIN Login
                        Text(
                            text = Localization.getString("login_desc", isId),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 6) {
                                    pinInput = input
                                    errorMessage = null
                                    if (input.length == 6) {
                                        submitPin(input)
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
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { submitPin(pinInput) }),
                            singleLine = true,
                            isError = errorMessage != null,
                            supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Explicit Submit Button
                        Button(
                            onClick = { submitPin(pinInput) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("login_submit_btn"),
                            shape = RoundedCornerShape(14.dp),
                            enabled = pinInput.length == 6
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.getString("login_btn_submit", isId),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Biometric Unlock Option
                        if (isBiometricEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { triggerBiometric() }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Biometric Login",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isId) "Buka dengan Sidik Jari / Biometrik" else "Unlock with Fingerprint / Biometric",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (isSettingUpPin) {
                        // Quick Local PIN setup
                        Text(
                            text = if (isId) "Atur PIN 6 digit untuk mengamankan data lokal Anda:" else "Set a 6-digit PIN to secure your local data:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 6) newPinInput = it },
                            label = { Text(if (isId) "PIN Baru (6 digit)" else "New PIN (6 digits)") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = confirmPinInput,
                            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 6) confirmPinInput = it },
                            label = { Text(if (isId) "Konfirmasi PIN Baru" else "Confirm New PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Button(
                            onClick = {
                                if (newPinInput.length != 6) {
                                    Toast.makeText(context, if (isId) "PIN harus 6 digit angka!" else "PIN must be 6 digits!", Toast.LENGTH_SHORT).show()
                                } else if (newPinInput != confirmPinInput) {
                                    Toast.makeText(context, if (isId) "Konfirmasi PIN tidak cocok!" else "PIN confirmation does not match!", Toast.LENGTH_SHORT).show()
                                } else {
                                    prefs.edit()
                                        .putBoolean("is_registered", true)
                                        .putString("password", newPinInput)
                                        .apply()
                                    Toast.makeText(context, if (isId) "PIN Keamanan Lokal Disimpan!" else "Local PIN Secured!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = newPinInput.length == 6 && confirmPinInput.length == 6
                        ) {
                            Text(if (isId) "Simpan PIN & Buka Aplikasi" else "Save PIN & Open App", fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = { isSettingUpPin = false }) {
                            Text(if (isId) "Kembali" else "Back")
                        }
                    } else {
                        // Unconfigured security state: Direct Local Login or Setup PIN
                        Text(
                            text = if (isId) "Selamat datang! Aplikasi saat ini beroperasi dengan mode penyimpanan offline lokal." else "Welcome! The app is operating in safe local offline mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = onLoginSuccess,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("local_login_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isId) "Masuk ke Beranda Lokal" else "Enter Local Workspace",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { isSettingUpPin = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isId) "Pasang Kunci PIN Lokal" else "Configure Local PIN Lock",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Local Security Guarantee footer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = Localization.getString("login_footer_secured", isId),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
