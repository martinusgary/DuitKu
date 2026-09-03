package com.example.ui.screens

import androidx.compose.ui.res.painterResource
import com.example.R
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.ui.util.UpdateResult
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
    viewModel: FinanceViewModel,
    activeCategory: Int? = null,
    onActiveCategoryChange: (Int?) -> Unit = {}
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

    // Local backup states

    // Biometrics support
    val isBiometricSupported = remember {
        val biometricManager = androidx.biometric.BiometricManager.from(context)
        biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }
    var isBiometricEnabled by remember {
        mutableStateOf(prefs.getBoolean("biometric_enabled", false))
    }

    // Handle back button when within a Settings submenu
    androidx.activity.compose.BackHandler(enabled = activeCategory != null) {
        onActiveCategoryChange(null)
    }

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
                var fileName = "duitku_backup"
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }

                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val rawBytes = inputStream.readBytes()
                    
                    // 1. Try decrypting from raw bytes (handles raw cipher, GZIP, Base64, plain JSON)
                    var decrypted = com.example.ui.util.CryptoHelper.decryptBytes(rawBytes)
                    
                    // 2. If empty, try decrypting from text representation
                    if (decrypted.isEmpty()) {
                        val text = String(rawBytes, Charsets.UTF_8).trim()
                        decrypted = com.example.ui.util.CryptoHelper.decrypt(text)
                    }

                    if (decrypted.isNotEmpty()) {
                        importJsonContent = decrypted
                        selectedFileName = fileName
                        isEncryptedFile = fileName.endsWith(".duitku") || !decrypted.startsWith("{")
                        Toast.makeText(context, if (isId) "Berkas cadangan ($fileName) berhasil dibaca & siap dipulihkan!" else "Backup file ($fileName) loaded successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Fallback: If it's plain text json or unencrypted string
                        val text = String(rawBytes, Charsets.UTF_8).trim()
                        if (text.startsWith("{") || text.startsWith("[")) {
                            importJsonContent = text
                            selectedFileName = fileName
                            isEncryptedFile = false
                            Toast.makeText(context, if (isId) "Berkas JSON ($fileName) berhasil dimuat!" else "JSON backup ($fileName) loaded!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, if (isId) "Gagal membaca berkas cadangan. Pastikan berkas .duitku atau .json valid." else "Could not read backup file. Please ensure valid .duitku or .json file.", Toast.LENGTH_LONG).show()
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
                        onClick = { onActiveCategoryChange(1) },
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
                                
                                Text(
                                    text = if (isId) "Akun Lokal & Keamanan" else "Local Account & Security",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_forward_custom),
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
                                    .clickable { onActiveCategoryChange(2) }
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
                                    painter = painterResource(id = R.drawable.ic_arrow_forward_custom),
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
                                    .clickable { onActiveCategoryChange(3) }
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
                                    painter = painterResource(id = R.drawable.ic_arrow_forward_custom),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Divider(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // Item 4: About & Updates
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onActiveCategoryChange(4) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isId) "Info Aplikasi & Pembaruan" else "App Info & Updates",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isId) "Versi v${viewModel.getAppVersionName()} • Periksa pembaruan rilis GitHub & info database." else "Version v${viewModel.getAppVersionName()} • Check GitHub releases & database status.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_arrow_forward_custom),
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
                        onClick = { onActiveCategoryChange(null) },
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
                                4 -> if (isId) "Info Aplikasi & Pembaruan" else "App Info & Updates"
                                else -> ""
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = when (activeCategory) {
                                1 -> if (isId) "Ubah nama sapaan, atau ekspor-impor cadangan data lokal." else "Customize greeting name, or export/import local backup data."
                                2 -> if (isId) "Pilih preferensi bahasa, palet warna, dan gaya tampilan visual." else "Choose language preferences, custom color themes, and visual interface styles."
                                3 -> if (isId) "Konfigurasi pengunci PIN enam digit dan verifikasi keamanan biometrik." else "Configure the six-digit safety passcode and biometrics verification lock."
                                4 -> if (isId) "Periksa pembaruan rilis terbaru dari GitHub dan perlindungan data lokal." else "Check for the latest GitHub releases and safe local data preservation."
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

                        // B. 100% Local Storage Notice
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
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
                                    Icons.Default.Security,
                                    contentDescription = "Penyimpanan Lokal",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isId) "Penyimpanan 100% Lokal & Privat" else "100% Local & Private Storage",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isId) {
                                            "Seluruh database dompet, transaksi, dan target keuangan Anda tersimpan sepenuhnya secara offline di memori internal perangkat ini. Tidak ada data yang dikirim ke server pihak ketiga. Gunakan fitur Ekspor & Impor di bawah untuk mencadangkan data Anda secara mandiri."
                                        } else {
                                            "All your wallets, transactions, and financial goals are stored completely offline inside this device's internal storage. No data is transmitted to third-party servers. Use the Export & Import tools below to manage your local backups."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                        importLauncher.launch(arrayOf("*/*"))
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
                                            if (isId) "Pilih Berkas (.duitku / .json)" else "Select Backup file (.duitku / .json)"
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
                                                    if (isEncryptedFile) "Siap Dipulihkan (Berkas .duitku):" else "Siap Dipulihkan (Berkas JSON):"
                                                } else {
                                                    if (isEncryptedFile) "Ready to restore (.duitku):" else "Ready to restore (.json):"
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
                                            viewModel.importBackupJson(importJsonContent) { success ->
                                                if (success) {
                                                    selectedFileName = ""
                                                    importJsonContent = ""
                                                    Toast.makeText(context, if (isId) "Data keuangan berhasil dipulihkan secara penuh!" else "Financial data restored successfully!", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, if (isId) "Gagal memulihkan data. Format berkas tidak cocok." else "Failed to restore data. Incompatible format.", Toast.LENGTH_LONG).show()
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
                    4 -> {
                        // ----------------------------------------------------
                        // CATEGORY 4: APP INFO & GITHUB UPDATES
                        // ----------------------------------------------------
                        val currentVer = viewModel.getAppVersionName()
                        val updateState by viewModel.updateResult.collectAsState()

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.SystemUpdate,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "DuitKu Finance",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isId) "Versi Terpasang: v$currentVer" else "Installed Version: v$currentVer",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Status Card
                                when (val result = updateState) {
                                    is UpdateResult.NewUpdate -> {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Download,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (isId) "Pembaruan Tersedia: v${result.latestVersionName} 🚀" else "Update Available: v${result.latestVersionName} 🚀",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                                Text(
                                                    text = result.releaseNotes.ifBlank { if (isId) "Peningkatan kinerja & perbaikan." else "Performance improvements." },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                                )
                                                Button(
                                                    onClick = {
                                                        try {
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.downloadUrl))
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            try {
                                                                val pageIntent = Intent(Intent.ACTION_VIEW, Uri.parse(result.pageUrl))
                                                                context.startActivity(pageIntent)
                                                            } catch (ex: Exception) {
                                                                Toast.makeText(context, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(if (isId) "Unduh Pembaruan (APK)" else "Download Update (APK)")
                                                }
                                            }
                                        }
                                    }
                                    is UpdateResult.NoUpdate -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isId) "Aplikasi Anda sudah versi terbaru!" else "You're running the latest version!",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    is UpdateResult.Error -> {
                                        Text(
                                            text = if (isId) "Gagal memeriksa pembaruan: ${result.message}" else "Could not check update: ${result.message}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    null -> {
                                        Text(
                                            text = if (isId) "Ketuk tombol di bawah untuk memeriksa versi rilis GitHub." else "Tap the button below to check GitHub for new versions.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        Toast.makeText(context, if (isId) "Memeriksa rilis GitHub..." else "Checking GitHub releases...", Toast.LENGTH_SHORT).show()
                                        viewModel.checkForAppUpdates()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isId) "Periksa Pembaruan GitHub" else "Check GitHub Updates", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Data Preservation Notice Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isId) "Perlindungan Data Saat Update" else "Data Preservation Guarantee",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = if (isId) {
                                        "DuitKu menggunakan migrasi database Room terintegrasi (Non-Destructive Migration). Setiap pembaruan atau update APK tidak akan pernah mereset data keuangan, transaksi, maupun anggaran belanja Anda."
                                    } else {
                                        "DuitKu employs non-destructive Room database schemas. Installing application updates or new APK builds safely preserves all your records, budgets, and wallets intact without resets."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }


}
