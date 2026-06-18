package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.util.Localization
import com.example.ui.util.UpdateResult
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val prefs = remember { context.getSharedPreferences("security_settings", Context.MODE_PRIVATE) }
    var isRegistered by remember { mutableStateOf(prefs.getBoolean("is_registered", false)) }
    var savedPassword by remember { mutableStateOf(prefs.getString("password", "") ?: "") }

    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }

    var backupJson by remember { mutableStateOf("") }
    var backupEncrypted by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf("") }
    var importJsonContent by remember { mutableStateOf("") }
    var isEncryptedFile by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0: Security/Language, 1: Export, 2: Import

    // Pre-generate JSON on opening for backup
    LaunchedEffect(Unit) {
        try {
            backupJson = viewModel.getBackupJson()
            backupEncrypted = viewModel.getEncryptedBackup()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Launcher for exporting/downloading JSON file
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(backupJson.toByteArray())
                }
                Toast.makeText(context, if (isId) "Ekspor data berhasil diunduh!" else "Backup downloaded successfully!", Toast.LENGTH_LONG).show()
                onDismiss()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Launcher for exporting/downloading .duitku encrypted file
    val exportDuitkuLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(backupEncrypted.toByteArray())
                }
                Toast.makeText(context, if (isId) "Ekspor berkas terenkripsi (.duitku) berhasil diunduh!" else "Encrypted Backup (.duitku) downloaded successfully!", Toast.LENGTH_LONG).show()
                onDismiss()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Launcher for importing/uploading a file
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                var fileName = "duitku_backup.json"
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }

                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val content = inputStream.bufferedReader().use { reader -> reader.readText() }
                    val contentTrimmed = content.trim()
                    
                    if (contentTrimmed.startsWith("{") || contentTrimmed.startsWith("[")) {
                        importJsonContent = contentTrimmed
                        selectedFileName = fileName
                        isEncryptedFile = false
                        Toast.makeText(context, if (isId) "Berkas standard JSON berhasil dimuat!" else "Standard JSON file loaded successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Let's attempt decryption to see if it qualifies as a decrypted .duitku backup
                        val decrypted = com.example.ui.util.CryptoHelper.decrypt(contentTrimmed)
                        val decryptedTrimmed = decrypted.trim()
                        if (decryptedTrimmed.startsWith("{") || decryptedTrimmed.startsWith("[")) {
                            importJsonContent = contentTrimmed
                            selectedFileName = fileName
                            isEncryptedFile = true
                            Toast.makeText(context, if (isId) "Berkas enkripsi (.duitku) berhasil didekripsi & dimuat!" else "Encrypted backup file (.duitku) successfully decrypted and loaded!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, if (isId) "Struktur berkas tidak valid atau kunci dekripsi tidak sesuai!" else "Invalid file structure or decryption key mismatch!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val dialogWidth = if (screenWidth < 600) (screenWidth * 0.94).dp else 520.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .width(dialogWidth)
                .heightIn(max = (screenHeight * 0.85).dp)
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = Localization.getString("settings_title", isId),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                Text(
                    text = Localization.getString("settings_desc", isId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Navigation Tabs (Modern Segmented Control)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val segments = listOf(
                        0 to Localization.getString("tab_security", isId),
                        1 to Localization.getString("tab_backup", isId),
                        2 to Localization.getString("tab_restore", isId)
                    )
                    segments.forEach { (index, label) ->
                        val isSelected = activeTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { activeTab = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (activeTab == 0) {
                    // SECURITY & LANGUAGE SELECTION
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Language Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = Localization.getString("lang_card_title", isId),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = Localization.getString("lang_card_subtitle", isId),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // English option
                                    val isEnSelected = !isId
                                    val enBgColor by animateColorAsState(
                                        targetValue = if (isEnSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        label = "enBgColor"
                                    )
                                    val enContentColor by animateColorAsState(
                                        targetValue = if (isEnSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        label = "enContentColor"
                                    )
                                    val buttonShape = RoundedCornerShape(12.dp)

                                    Surface(
                                        onClick = { viewModel.setLanguage("en") },
                                        shape = buttonShape,
                                        color = enBgColor,
                                        contentColor = enContentColor,
                                        border = if (isEnSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (isEnSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = Localization.getString("lang_en", isId),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isEnSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Bahasa Indonesia option
                                    val isIdSelected = isId
                                    val idBgColor by animateColorAsState(
                                        targetValue = if (isIdSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        label = "idBgColor"
                                    )
                                    val idContentColor by animateColorAsState(
                                        targetValue = if (isIdSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        label = "idContentColor"
                                    )

                                    Surface(
                                        onClick = { viewModel.setLanguage("id") },
                                        shape = buttonShape,
                                        color = idBgColor,
                                        contentColor = idContentColor,
                                        border = if (isIdSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (isIdSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = Localization.getString("lang_id", isId),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isIdSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. PIN Configuration Area
                        if (isRegistered) {
                            // PIN lock is active, show state and disable button
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = Localization.getString("sec_pin_active", isId),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = Localization.getString("sec_pin_active_desc", isId),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Biometric Configuration Toggle (Only available if PIN is configured)
                            val isBiometricSupported = remember { com.example.ui.util.BiometricHelper.isBiometricAvailable(context) }
                            var isBiometricEnabled by remember { mutableStateOf(prefs.getBoolean("biometric_enabled", false)) }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = Localization.getString("sec_biometric_title", isId),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = Localization.getString("sec_biometric_desc", isId),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Switch(
                                        checked = isBiometricEnabled,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (isBiometricSupported) {
                                                    val activity = context as? androidx.fragment.app.FragmentActivity
                                                    if (activity != null) {
                                                        com.example.ui.util.BiometricHelper.showBiometricPrompt(
                                                            activity = activity,
                                                            title = Localization.getString("sec_biometric_prompt", isId),
                                                            subtitle = Localization.getString("sec_biometric_desc", isId),
                                                            negativeButtonText = Localization.getString("close", isId),
                                                            onSuccess = {
                                                                prefs.edit().putBoolean("biometric_enabled", true).apply()
                                                                isBiometricEnabled = true
                                                                Toast.makeText(context, Localization.getString("sec_biometric_success", isId), Toast.LENGTH_LONG).show()
                                                            },
                                                            onError = { err ->
                                                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                            }
                                                        )
                                                    }
                                                } else {
                                                    Toast.makeText(context, Localization.getString("sec_biometric_error_setup", isId), Toast.LENGTH_LONG).show()
                                                }
                                            } else {
                                                prefs.edit().putBoolean("biometric_enabled", false).apply()
                                                isBiometricEnabled = false
                                                Toast.makeText(context, Localization.getString("sec_biometric_disabled", isId), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    prefs.edit().clear().apply()
                                    isRegistered = false
                                    savedPassword = ""
                                    passwordInput = ""
                                    confirmPasswordInput = ""
                                    Toast.makeText(context, Localization.getString("sec_pin_disabled", isId), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Disable PIN lock",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Localization.getString("sec_disable_lock", isId), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Not yet configured, show Setup Input fields
                            Text(
                                text = Localization.getString("sec_unregistered_title", isId),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() } && input.length <= 6) {
                                        passwordInput = input
                                    }
                                },
                                label = { Text(Localization.getString("sec_label_pin", isId)) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = confirmPasswordInput,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() } && input.length <= 6) {
                                        confirmPasswordInput = input
                                    }
                                },
                                label = { Text(Localization.getString("sec_label_confirm", isId)) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Button(
                                onClick = {
                                    if (passwordInput.trim().isEmpty()) {
                                        Toast.makeText(context, Localization.getString("sec_pin_empty", isId), Toast.LENGTH_SHORT).show()
                                    } else if (!passwordInput.all { it.isDigit() }) {
                                        Toast.makeText(context, Localization.getString("sec_pin_numeric", isId), Toast.LENGTH_SHORT).show()
                                    } else if (passwordInput.length != 6) {
                                        Toast.makeText(context, Localization.getString("sec_pin_length_invalid", isId), Toast.LENGTH_SHORT).show()
                                    } else if (passwordInput != confirmPasswordInput) {
                                        Toast.makeText(context, Localization.getString("sec_pin_mismatch", isId), Toast.LENGTH_SHORT).show()
                                    } else {
                                        prefs.edit()
                                            .putBoolean("is_registered", true)
                                            .putString("username", "Pengguna") // compatibility placeholder for internal logic
                                            .putString("password", passwordInput)
                                            .putBoolean("biometric_enabled", false) // removed
                                            .apply()
                                        
                                        isRegistered = true
                                        savedPassword = passwordInput
                                        Toast.makeText(context, Localization.getString("sec_pin_success", isId), Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Localization.getString("sec_btn_create", isId), fontWeight = FontWeight.Bold)
                            }
                        }

                        // 3. App Version & Update Checker Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isId) "Versi Aplikasi" else "App Version",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = "v${viewModel.getAppVersionName()}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isId) "Tekan untuk memeriksa rilis baru" else "Tap to check for new releases",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    val updateResult by viewModel.updateResult.collectAsState()
                                    var checkingUpdate by remember { mutableStateOf(false) }

                                    LaunchedEffect(updateResult) {
                                        if (checkingUpdate && updateResult != null) {
                                            checkingUpdate = false
                                            when (updateResult) {
                                                is UpdateResult.NoUpdate -> {
                                                    Toast.makeText(context, if (isId) "Aplikasi Anda sudah versi terbaru!" else "Your app is already up to date!", Toast.LENGTH_SHORT).show()
                                                }
                                                is UpdateResult.NewUpdate -> {
                                                    Toast.makeText(context, if (isId) "Pembaruan ditemukan!" else "Update found!", Toast.LENGTH_SHORT).show()
                                                }
                                                is UpdateResult.Error -> {
                                                    Toast.makeText(context, (if (isId) "Gagal memeriksa pembaruan: " else "Failed to check update: ") + (updateResult as UpdateResult.Error).message, Toast.LENGTH_LONG).show()
                                                }
                                                else -> {}
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (!checkingUpdate) {
                                                checkingUpdate = true
                                                viewModel.checkForAppUpdates()
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        if (checkingUpdate) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(if (isId) "Periksa" else "Check", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (activeTab == 1) {
                    // EXPORT / BACKUP TAB - High grade encrypted local files + Google Drive Custom Sync
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Section 1: Local Backup Files
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (isId) "1. Berkas Cadangan Lokal" else "1. Local Backup Files",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = if (isId) {
                                        "Semua data keuangan dienkripsi secara aman dengan algoritma AES-128 menjadi file khusus .duitku untuk transfer privat antar perangkat."
                                    } else {
                                        "All financial metrics are encrypted using AES-128 algorithms into a custom .duitku file for secure offline transfer."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Button A: Private Encrypted Backup (.duitku)
                                Button(
                                    onClick = {
                                        exportDuitkuLauncher.launch("cadangan_duitku.duitku")
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isId) "Ekspor Terenkripsi (.duitku)" else "Encrypted Export (.duitku)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Button B: Standard Unencrypted Backup (.json)
                                OutlinedButton(
                                    onClick = {
                                        exportJsonLauncher.launch("duitku_backup.json")
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isId) "Ekspor Standar JSON (.json)" else "Standard JSON Export (.json)",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // Section 2: Google Drive Auto-Sync Engine
                        var googleAccount by remember { mutableStateOf(viewModel.getGDriveAccount()) }
                        var lastSyncTime by remember { mutableStateOf(viewModel.getGDriveLastSync()) }
                        val gdriveSyncState by viewModel.gdriveSyncState.collectAsState()

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (isId) "2. Sinkronisasi Google Drive" else "2. Google Drive Sync",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = if (isId) {
                                        "Koneksi sandboxed langsung ke appDataFolder Google Drive Anda tanpa pihak ketiga agar privasi Anda terjaga 100%."
                                    } else {
                                        "Sync to your Google Drive's isolated appDataFolder with 100% cloud privacy absolute assurance."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (googleAccount != null) {
                                    // Connected Account State
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Connected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = googleAccount ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (lastSyncTime != null) {
                                                    (if (isId) "Sinkron terakhir: " else "Last sync: ") + lastSyncTime
                                                } else {
                                                    if (isId) "Belum pernah disinkronisasi" else "Never synced"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        TextButton(onClick = {
                                            viewModel.disconnectGDrive()
                                            googleAccount = null
                                            lastSyncTime = null
                                        }) {
                                            Text(if (isId) "Hapus" else "Unlink", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.syncGDriveNow { success ->
                                                if (success) {
                                                    lastSyncTime = viewModel.getGDriveLastSync()
                                                    Toast.makeText(context, if (isId) "Sinkronisasi Google Drive Berhasil!" else "Google Drive Synced Successfully!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (gdriveSyncState == "SYNCING") {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(if (isId) "Sinkronkan Sekarang" else "Sync Now", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    // Not Connected state - Personalized linking
                                    Button(
                                        onClick = {
                                            // Secure personalized link
                                            viewModel.connectGDrive("martinus.gary1@gmail.com")
                                            googleAccount = "martinus.gary1@gmail.com"
                                            Toast.makeText(context, if (isId) "Berhasil menautkan Akun Google Drive!" else "Linked GDrive successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isId) "Tautkan martinus.gary1@gmail.com" else "Link martinus.gary1@gmail.com", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Section 3: Android system Auto-Backup Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "System Auto Backup",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isId) "Auto-Backup Android Aktif" else "Android Auto-Backup Engaged",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isId) {
                                            "Sistem Android Anda secara default menyinkronkan data database Room privat DuitKu ini secara aman ke cadangan cloud akun Google pribadi Anda ketika perangkat sedang dicas di malam hari lewat Wi-Fi."
                                        } else {
                                            "Our custom system backup rules guarantee persistent system sync of the Room databases to your personal GDrive storage when the phone charges overnight on Wi-Fi."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else if (activeTab == 2) {
                    // RESTORE TAB
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = if (isId) "Menimpa Data yang Ada" else "Destructive Overwrite",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    text = if (isId) {
                                        "Mengimpor berkas cadangan (baik .json maupun terenkripsi .duitku) akan menimpa seluruh status keuangan data lokal Anda saat ini. Tindakan ini tidak dapat dibatalkan!"
                                    } else {
                                        "Restoring database documents completely replaces existing states with the backup inputs. This operation cannot be undone."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(100)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Upload File"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedFileName.isEmpty()) {
                                    if (isId) "Pilih Berkas Cadangan (.json/.duitku)" else "Select Backup File (.json/.duitku)"
                                } else {
                                    if (isId) "Ganti Berkas Cadangan" else "Change Backup File"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (selectedFileName.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    if (isEncryptedFile) Icons.Default.Lock else Icons.Default.CheckCircle,
                                    contentDescription = "Status",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isId) {
                                            if (isEncryptedFile) "Siap Diimpor (Cadangan Terenkripsi):" else "Siap Diimpor (Cadangan Standar):"
                                        } else {
                                            if (isEncryptedFile) "Ready: (Encrypted Backup)" else "Ready: (Standard JSON Backup)"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        selectedFileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (isEncryptedFile) {
                                        viewModel.importEncryptedBackup(importJsonContent) { success ->
                                            if (success) {
                                                Toast.makeText(context, if (isId) "Cadangan Terenkripsi Berhasil Dipulihkan!" else "Encrypted Backup successfully restored!", Toast.LENGTH_LONG).show()
                                                onDismiss()
                                            } else {
                                                Toast.makeText(context, if (isId) "Gagal mendekripsi atau memulihkan berkas!" else "Failed to decrypt/restore backup code!", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        viewModel.importBackupJson(importJsonContent) { success ->
                                            if (success) {
                                                Toast.makeText(context, if (isId) "Data berhasil dipulihkan!" else "Data successfully restored!", Toast.LENGTH_LONG).show()
                                                onDismiss()
                                            } else {
                                                Toast.makeText(context, if (isId) "Gagal memulihkan data. Periksa keselarasan berkas!" else "Failed to restore. Please check file compliance!", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text(if (isId) "Pulihkan Sekarang" else "Restore Now", fontWeight = FontWeight.Black)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isId) "Belum ada berkas cadangan terpilih." else "No backup file selected yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                }
            }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Localization.getString("close", isId), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
