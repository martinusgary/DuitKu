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
    var selectedFileName by remember { mutableStateOf("") }
    var importJsonContent by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0: Security/Language, 1: Export, 2: Import

    // Pre-generate JSON on opening for backup
    LaunchedEffect(Unit) {
        backupJson = viewModel.getBackupJson()
    }

    // Launcher for exporting/downloading a file
    val exportLauncher = rememberLauncherForActivityResult(
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
                    if (content.trim().startsWith("{") || content.trim().startsWith("[")) {
                        importJsonContent = content
                        selectedFileName = fileName
                        Toast.makeText(context, if (isId) "File berkas berhasil dimuat!" else "File loaded successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, if (isId) "Struktur berkas JSON tidak sesuai!" else "Invalid JSON file structure!", Toast.LENGTH_LONG).show()
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
                                            text = "v1.2",
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
                    // EXPORT TAB - Full multi-lingual backup
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
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
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isId) "Aman dan Praktis" else "Safe and Easy",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (isId) {
                                        "Seluruh transaksi, akun dompet, tagihan aktif, serta data hutang akan dikompilasi menjadi satu berkas cadangan privat. Ketuk tombol di bawah untuk menyimpannya."
                                    } else {
                                        "Your transactions, accounts, active bills, and debts are compiled into a single file. Tap below to download it securely onto your device."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                exportLauncher.launch("duitku_backup.json")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Download File"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isId) "Unduh Berkas Cadangan" else "Download Backup File", fontWeight = FontWeight.Bold)
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
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
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
                                        text = if (isId) "Akan Menimpa Data Saat Ini" else "Overwrites Current Data",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Text(
                                    text = if (isId) {
                                        "Mengimpor berkas cadangan akan menimpa dan menggantikan seluruh data keuangan saat ini di aplikasi Anda secara permanen. Tindakan ini tidak dapat dibatalkan!"
                                    } else {
                                        "Selecting and importing a backup file will fully replace all financial records in your app. This operation cannot be undone."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
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
                                    if (isId) "Pilih Berkas Cadangan" else "Select Backup File"
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
                                    .background(Color(0xFFE8F5E9))
                                    .border(1.dp, Color(0xFF81C784), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = Color(0xFF2E7D32)
                                )
                                Column {
                                    Text(
                                        text = if (isId) "Siap Diimpor:" else "Ready to Import:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF1B5E20),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        selectedFileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.importBackupJson(importJsonContent) { success ->
                                        if (success) {
                                            Toast.makeText(context, if (isId) "Data berhasil dipulihkan!" else "Data successfully restored!", Toast.LENGTH_LONG).show()
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, if (isId) "Gagal memulihkan data. Periksa keselarasan berkas!" else "Failed to restore. Please check file compliance!", Toast.LENGTH_LONG).show()
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
                                Text(if (isId) "Pulihkan Data Sekarang" else "Restore Data Now", fontWeight = FontWeight.Black)
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
