package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.util.Localization
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val prefs = remember { context.getSharedPreferences("security_settings", Context.MODE_PRIVATE) }
    var isRegistered by remember { mutableStateOf(prefs.getBoolean("is_registered", false)) }
    var savedPassword by remember { mutableStateOf(prefs.getString("password", "") ?: "") }

    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"
    val greetingName by viewModel.userGreetingName.collectAsState()
    var nameInputState by remember(greetingName) { mutableStateOf(greetingName) }

    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }

    var backupJson by remember { mutableStateOf("") }
    var backupEncrypted by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf("") }
    var importJsonContent by remember { mutableStateOf("") }
    var isEncryptedFile by remember { mutableStateOf(false) }

    // Google Drive dynamic sync states
    var googleAccount by remember { mutableStateOf(viewModel.getGDriveAccount()) }
    var lastSyncTime by remember { mutableStateOf(viewModel.getGDriveLastSync()) }
    val gdriveSyncState by viewModel.gdriveSyncState.collectAsState()
    var showGoogleDialog by remember { mutableStateOf(false) }
    var googleEmailInput by remember { mutableStateOf("") }
    var showCloudRestorePrompt by remember { mutableStateOf(false) }
    var cloudBackupTimeFound by remember { mutableStateOf<String?>(null) }
    var cloudBackupDataFound by remember { mutableStateOf<String?>(null) }

    // Biometrics support
    val isBiometricSupported = remember {
        val biometricManager = androidx.biometric.BiometricManager.from(context)
        biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }
    var isBiometricEnabled by remember {
        mutableStateOf(prefs.getBoolean("biometric_enabled", false))
    }

    // Active Category View state (null = Main Menu, 1 = Profile & Backup, 2 = Visuals & Themes, 3 = Lock & Security)
    var activeCategory by remember { mutableStateOf<Int?>(null) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (activeCategory == null) {
            // ==========================================
            // MAIN SETTINGS LIST (WHATSAPP STYLE STYLE)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Scrollable main menu content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                        Text(
                            text = if (isId) "Pengaturan" else "Settings",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (isId) "Kelola profil sapaan, keamanan PIN, visual, dan cadangan data." else "Manage custom greetings, security locks, visual styles, and data storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // A. PROFILE ACCOUNT CARD (At the very top) - Clicking goes to Profile & Backup category
                    Card(
                        onClick = { activeCategory = 1 },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Initials Avatar generated from name
                            val initials = greetingName.split(" ")
                                .filter { it.isNotEmpty() }
                                .map { it.first().uppercase() }
                                .take(2)
                                .joinToString("")
                                .let { if (it.isEmpty()) "U" else it }

                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = greetingName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                
                                // Dynamic email follow from GDrive email list if any
                                if (!googleAccount.isNullOrEmpty()) {
                                    Text(
                                        text = googleAccount!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Edit Profile & Backup",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // B. CATEGORIES ITEMS LIST
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            
                            // Item 1: Visuals & Themes
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { activeCategory = 2 }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isId) "Tampilan & Tema" else "Visuals & Themes",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isId) "Bahasa aplikasi, skema warna dinamis tema, & gaya antarmuka." else "App language, dynamic Material theme colors, & UI layout styles.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Divider(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // Item 2: Lock & Security
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { activeCategory = 3 }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isId) "Keamanan & PIN Kunci" else "PIN Lock & Security",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isId) "Amankan data anggaran keuangan Anda dengan sandi PIN & biometric." else "Secure financial lockers via safe numeric PINs & biometrics.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Info footer at the absolute bottom corner of the layout
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Copyright 2026",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isId) "Seluruh data Anda terenkripsi dan disimpan lokal secara aman." else "All records are securely encrypted and retained offline in sandbox storage.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            // ==========================================
            // DETAIL SUB-SCREENS WRAPPED WITH BACK BUTTON
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Return Bar header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { activeCategory = null },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text(
                            text = when (activeCategory) {
                                1 -> if (isId) "Profil & Cadangan Data" else "Profile & Data Backup"
                                2 -> if (isId) "Tampilan & Tema" else "Visuals & Themes"
                                3 -> if (isId) "Keamanan & Kunci PIN" else "PIN Lock & Security"
                                else -> ""
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = when (activeCategory) {
                                1 -> if (isId) "Ubah nama sapaan, sinkronisasi Google Drive, atau ekspor-impor data." else "Customize greeting name, sync Google Drive, or export/import files."
                                2 -> if (isId) "Pilih preferensi bahasa, palet warna, dan gaya tampilan visual." else "Choose language preferences, custom color themes, and visual interface styles."
                                3 -> if (isId) "Konfigurasi pengunci PIN enam digit dan verifikasi keamanan biometrik." else "Configure the six-digit safety passcode and biometrics verification lock."
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Detail display panels
                when (activeCategory) {
                    1 -> {
                        // ----------------------------------------------------
                        // CATEGORY 1: PROFILE & BACKUP (PROFIL & CADANGAN DATA)
                        // ----------------------------------------------------
                        
                        // A. Greeting Name Edit Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (isId) "Ubah Nama Sapaan" else "Change Greeting Name",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isId) "Nama ini akan muncul di bagian sambutan di dasbor utama." else "This name will appear on the greeting message at your central console.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = nameInputState,
                                        onValueChange = { nameInputState = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        placeholder = { Text(if (isId) "Tulis nama sapaan..." else "Enter greeting name...") },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Button(
                                        onClick = {
                                            val trimmed = nameInputState.trim()
                                            if (trimmed.isNotEmpty()) {
                                                viewModel.setUserGreetingName(trimmed)
                                                Toast.makeText(context, if (isId) "Nama sapaan diperbarui!" else "Greeting name updated!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(56.dp)
                                    ) {
                                        Text(if (isId) "Simpan" else "Save")
                                    }
                                }
                            }
                        }

                        // B. Google Drive Cloud synchronization
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (isId) "Google Drive - Sinkronisasi Cloud" else "Google Drive - Cloud Synchronization",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Text(
                                    text = if (isId) {
                                        "Amankan seluruh saku, anggaran, transaksi, dan target keuangan Anda secara berkala ke cloud Google Drive pribadi."
                                    } else {
                                        "Securely persist all pockets, spending budgets, transaction records, and goals regularly to your personal Google Drive account."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (googleAccount != null) {
                                    // Connected State
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(googleAccount ?: "", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                            Text(
                                                text = if (lastSyncTime != null) {
                                                    (if (isId) "Sinkronisasi terakhir: " else "Last synced: ") + lastSyncTime
                                                } else {
                                                    if (isId) "Belum sinkron" else "Not synced yet"
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
                                            Text(if (isId) "Putus" else "Unlink", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
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
                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(if (isId) "Sinkronkan Sekarang" else "Sync Now", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    // Unconnected Google sync state
                                    Button(
                                        onClick = {
                                            googleEmailInput = ""
                                            showGoogleDialog = true
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isId) "Hubungkan Akun Google Drive" else "Link Google Drive Account", fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Auto-sync notice
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Sync",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
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
                        }

                        // C. Export Data
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (isId) "Cadangkan Data (Ekspor)" else "Backup Financial Data (Export)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isId) {
                                        "Unduh salinan riwayat transaksi, anggaran belanja, saku rekening, dan utang piutang Anda dalam bentuk berkas luring."
                                    } else {
                                        "Download a portable file bundle containing all of your local ledger records, wallets, budgeting parameters, and debt histories."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { exportJsonLauncher.launch("duitku_backup.json") },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Standard JSON", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { exportDuitkuLauncher.launch("duitku_backup.duitku") },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Secure .duitku", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // D. Import Data
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (isId) "Pulihkan Data (Impor)" else "Restore/Import Backup File",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = if (isId) "Menimpa Data Aktif" else "Destructive Overwrite Warning",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = if (isId) {
                                                    "Mengimpor database akan mengganti seluruh transaksi Anda saat ini. Tindakan ini tidak dapat dibatalkan!"
                                                } else {
                                                    "Restoring replaces target documents completely with the backup. This operation cannot be undone."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = "Upload UI"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedFileName.isEmpty()) {
                                            if (isId) "Pilih Berkas (.json/.duitku)" else "Select Backup file (.json/.duitku)"
                                        } else {
                                            if (isId) "Ganti Berkas Terpilih" else "Change Selected File"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                if (selectedFileName.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            if (isEncryptedFile) Icons.Default.Lock else Icons.Default.CheckCircle,
                                            contentDescription = "Status",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isId) {
                                                    if (isEncryptedFile) "Siap Impor (Terenkripsi):" else "Ready Standard Backup:"
                                                } else {
                                                    if (isEncryptedFile) "Ready: (Encrypted .duitku)" else "Ready: (Standard JSON)"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                selectedFileName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (isEncryptedFile) {
                                                viewModel.importEncryptedBackup(importJsonContent) { success ->
                                                    if (success) {
                                                        selectedFileName = ""
                                                        importJsonContent = ""
                                                        Toast.makeText(context, if (isId) "Cadangan Terenkripsi Berhasil Dipulihkan!" else "Encrypted Backup successfully restored!", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        Toast.makeText(context, if (isId) "Gagal mendekripsi atau memulihkan berkas!" else "Failed to decrypt/restore backup file!", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            } else {
                                                viewModel.importBackupJson(importJsonContent) { success ->
                                                    if (success) {
                                                        selectedFileName = ""
                                                        importJsonContent = ""
                                                        Toast.makeText(context, if (isId) "Data berhasil dipulihkan!" else "Data successfully restored!", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        Toast.makeText(context, if (isId) "Gagal memulihkan data. Periksa keselarasan berkas!" else "Failed to restore. Please check file compliance!", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (isId) "Pulihkan Sekarang" else "Restore Now", fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // ----------------------------------------------------
                        // CATEGORY 2: VISUALS & THEMES (TAMPILAN & TEMA)
                        // ----------------------------------------------------
                        
                        // A. Interface Language
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    val isEnglish = !isId
                                    
                                    // English option
                                    Surface(
                                        onClick = { viewModel.setLanguage("en") },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isEnglish) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        contentColor = if (isEnglish) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        border = if (isEnglish) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (isEnglish) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text("English (US)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Indonesian option
                                    Surface(
                                        onClick = { viewModel.setLanguage("id") },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        contentColor = if (isId) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        border = if (isId) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (isId) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text("Bahasa Indonesia", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // B. Theme Selection Card
                        val currentTheme by viewModel.appTheme.collectAsState()
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = Localization.getString("theme_card_title", isId),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = Localization.getString("theme_card_subtitle", isId),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val themes = listOf(
                                    Triple("CLASSIC", "theme_classic", listOf(Color(0xFF6750A4), Color(0xFF625B71), Color(0xFFFDF8FF))),
                                    Triple("DYNAMIC", "theme_dynamic", listOf(Color(0xFF4A8CA6), Color(0xFF5D9663), Color(0xFFEBEFFC))),
                                    Triple("MINT", "theme_mint", listOf(Color(0xFF00B1A9), Color(0xFF059669), Color(0xFFF4FBF9))),
                                    Triple("OCEAN", "theme_ocean", listOf(Color(0xFF0564CA), Color(0xFF0EA5E9), Color(0xFFF3F7FC))),
                                    Triple("SUNSET", "theme_sunset", listOf(Color(0xFFFF5E14), Color(0xFFEAB308), Color(0xFFFFF9F6))),
                                    Triple("SAKURA", "theme_sakura", listOf(Color(0xFFD05090), Color(0xFF9C27B0), Color(0xFFFFF6FB)))
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    themes.forEach { (themeId, labelKey, colorsList) ->
                                        val isSelected = currentTheme == themeId
                                        val itemBgColor by animateColorAsState(
                                            targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            label = "themeBg_$themeId"
                                        )
                                        val itemContentColor by animateColorAsState(
                                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            label = "themeContent_$themeId"
                                        )

                                        Surface(
                                            onClick = { viewModel.setAppTheme(themeId) },
                                            shape = RoundedCornerShape(12.dp),
                                            color = itemBgColor,
                                            contentColor = itemContentColor,
                                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .border(
                                                                width = if (isSelected) 0.dp else 1.5.dp,
                                                                color = MaterialTheme.colorScheme.outline,
                                                                shape = CircleShape
                                                            )
                                                            .background(
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                                shape = CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                        }
                                                    }
                                                    
                                                    Column {
                                                        Text(
                                                            text = Localization.getString(labelKey, isId),
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                // Palette Spheres
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    colorsList.forEach { col ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clip(CircleShape)
                                                                .background(col)
                                                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // C. UI Design/Visual style selection
                        val uiStyle by viewModel.uiStyle.collectAsState()
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = Localization.getString("style_card_title", isId),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = Localization.getString("style_card_subtitle", isId),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val isStyleFresh = uiStyle == "FRESH"
                                    
                                    // FRESH style
                                    Surface(
                                        onClick = { viewModel.setUiStyle("FRESH") },
                                        modifier = Modifier.weight(1f).height(64.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isStyleFresh) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        contentColor = if (isStyleFresh) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        border = if (isStyleFresh) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (isStyleFresh) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = if (isId) "Wallet Fresh (Playful)" else "Digital Fresh (Playful)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    // SOLID standard style
                                    Surface(
                                        onClick = { viewModel.setUiStyle("SOLID") },
                                        modifier = Modifier.weight(1f).height(64.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (!isStyleFresh) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        contentColor = if (!isStyleFresh) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        border = if (!isStyleFresh) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (!isStyleFresh) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = if (isId) "Standar Bersih (Classic)" else "Modern Classic (Clean)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // ----------------------------------------------------
                        // CATEGORY 3: LOCK & SECURITY (KUNCI & KEAMANAN)
                        // ----------------------------------------------------
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (isRegistered) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Active",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                text = Localization.getString("sec_pin_active", isId),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = Localization.getString("sec_pin_active_desc", isId),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    // Biometrics option
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
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
                                    // PIN NOT set up yet
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
                                                    .putString("password", passwordInput)
                                                    .apply()
                                                isRegistered = true
                                                savedPassword = passwordInput
                                                passwordInput = ""
                                                confirmPasswordInput = ""
                                                Toast.makeText(context, Localization.getString("sec_pin_success", isId), Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(Localization.getString("sec_btn_create", isId), fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }


    // -----------------------------------------------------------------
    // POP-UP SUB-DIALOGS (Google Drive Link Email accounts selection dialog)
    // -----------------------------------------------------------------
    if (showGoogleDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isId) "Masuk dengan Google" else "Sign In with Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val loggingIn = gdriveSyncState == "LOGGING_IN"

                    if (loggingIn) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = if (isId) "Menghubungkan & memeriksa Drive..." else "Connecting & checking Drive...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (isId) {
                                "Pilih salah satu akun Google yang terdeteksi atau masukkan alamat email Google kustom Anda di bawah."
                            } else {
                                "Choose a detected Google account or enter your custom Google secure email address below."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isId) "Akun Tersimpan di Ponsel:" else "Saved Phone Accounts:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                              )

                            val savedAccounts = viewModel.getAccountHistory()

                            if (savedAccounts.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isId) "Belum ada akun tersimpan di perangkat ini." else "No saved accounts detected on this device.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            } else {
                                savedAccounts.forEach { suggestEmail ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { googleEmailInput = suggestEmail },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (googleEmailInput == suggestEmail) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                        ),
                                        border = if (googleEmailInput == suggestEmail) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.AccountCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = if (googleEmailInput == suggestEmail) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = suggestEmail,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = googleEmailInput,
                            onValueChange = { googleEmailInput = it },
                            label = { Text("Email Google kustom") },
                            placeholder = { Text("email@example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                val loggingIn = gdriveSyncState == "LOGGING_IN"
                if (!loggingIn) {
                    Button(
                        onClick = {
                            val email = googleEmailInput.trim()
                            if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                viewModel.connectGDrive(email)
                                viewModel.fetchCloudBackup(email) { fileTime, fileData ->
                                    showGoogleDialog = false
                                    googleAccount = viewModel.getGDriveAccount()
                                    lastSyncTime = viewModel.getGDriveLastSync()
                                    
                                    if (fileTime != null && fileData != null) {
                                        cloudBackupTimeFound = fileTime
                                        cloudBackupDataFound = fileData
                                        showCloudRestorePrompt = true
                                    } else {
                                        Toast.makeText(context, if (isId) "Akun Drive Terhubung!" else "Drive Account Connected successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, if (isId) "Alamat email tidak valid!" else "Please input a valid Google email address!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(100)
                    ) {
                        Text(if (isId) "Hubungkan" else "Connect", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                val loggingIn = gdriveSyncState == "LOGGING_IN"
                if (!loggingIn) {
                    TextButton(onClick = { showGoogleDialog = false }) {
                        Text(if (isId) "Batal" else "Cancel")
                    }
                }
            }
        )
    }

    if (showCloudRestorePrompt) {
        AlertDialog(
            onDismissRequest = { showCloudRestorePrompt = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isId) "Cadangan Cloud Ditemukan" else "Cloud Backup Located",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isId) {
                            "Akun Google Drive Anda menyimpan berkas sinkronisasi cadangan transaksi DuitKu terdeteksi dari tanggal:"
                        } else {
                            "Your Google Drive contains an archived DuitKu transaction history recorded on:"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = cloudBackupTimeFound ?: "Unknown Date",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isId) {
                            "Apakah Anda ingin memulihkan cadangan cloud ini sekarang? Tindakan ini akan menimpa seluruh status data lokal saku dan rincian transaksi DuitKu sekarang berjalan."
                        } else {
                            "Do you wish to restore this cloud backup now? Doing so will completely replace your current local financial ledger records."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cloudData = cloudBackupDataFound
                        if (cloudData != null) {
                            viewModel.importBackupJson(cloudData) { ok ->
                                showCloudRestorePrompt = false
                                if (ok) {
                                    Toast.makeText(context, if (isId) "Cadangan Cloud Berhasil Dipulihkan!" else "Cloud Backup Successfully Restored!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, if (isId) "Gagal memulihkan cadangan cloud!" else "Cloud restore unsuccessful!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            showCloudRestorePrompt = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(100)
                ) {
                    Text(if (isId) "Pulihkan Sekarang" else "Restore Cloud Data", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloudRestorePrompt = false }) {
                    Text(if (isId) "Lewati (Gunakan Lokal)" else "Keep Local Data")
                }
            }
        )
    }
}
