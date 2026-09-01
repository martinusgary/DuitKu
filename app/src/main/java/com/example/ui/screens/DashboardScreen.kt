package com.example.ui.screens

import android.Manifest
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.util.PdfExporter
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.ui.util.Localization
import com.example.ui.components.CategoryVisuals
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.FileProvider
import java.io.File
import android.net.Uri
import android.content.Intent
import com.example.ui.util.UpdateResult
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.ui.util.GeminiClient
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val totalBalance by viewModel.totalBalance.collectAsState(initial = 0.0)
    val monthlyIncome by viewModel.monthlyIncomeSum.collectAsState(initial = 0.0)
    val monthlyExpense by viewModel.monthlyExpenseSum.collectAsState(initial = 0.0)
    val transactions by viewModel.transactions.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    val isHidden by viewModel.isAmountsHidden.collectAsState()
    val currentStyle by viewModel.uiStyle.collectAsState()
    val currentTheme by viewModel.appTheme.collectAsState()
    val isFresh = currentStyle == "FRESH"
    val userGreetingName by viewModel.userGreetingName.collectAsState()
    val updateResult by viewModel.updateResult.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var initialScannedReceiptsForDialog by remember { mutableStateOf<List<GeminiClient.ScanResult>>(emptyList()) }
    var showScanOptionsDialog by remember { mutableStateOf(false) }
    var isScanningReceipt by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val dashboardCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = tempPhotoUri
            if (uri != null) {
                isScanningReceipt = true
                coroutineScope.launch {
                    try {
                        val results = GeminiClient.scanMultipleReceipts(context, listOf(uri))
                        if (results.isNotEmpty()) {
                            initialScannedReceiptsForDialog = results
                            showAddDialog = true
                            Toast.makeText(context, if (isId) "Pendeteksian struk selesai!" else "Receipt detection completed!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, if (isId) "Gagal mendeteksi rincian dari struk." else "No details detected from the receipt.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, (if (isId) "Gagal memindai struk: " else "Failed to scan receipt: ") + e.message, Toast.LENGTH_LONG).show()
                    } finally {
                        isScanningReceipt = false
                    }
                }
            }
        }
    }

    val dashboardRequestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = try {
                val tempFile = File.createTempFile("receipt_cam_", ".jpg", context.cacheDir).apply {
                    createNewFile()
                    deleteOnExit()
                }
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
            } catch (e: Exception) {
                null
            }
            if (uri != null) {
                tempPhotoUri = uri
                try {
                    dashboardCameraLauncher.launch(uri)
                } catch (e: Exception) {
                    Toast.makeText(context, (if (isId) "Gagal membuka kamera: " else "Failed to open camera: ") + e.message, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, if (isId) "Gagal membuat file foto" else "Failed to initialize camera file", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, if (isId) "Izin kamera diperlukan untuk mengambil foto struk" else "Camera permission is required to take receipt photos", Toast.LENGTH_SHORT).show()
        }
    }

    val dashboardPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isScanningReceipt = true
            coroutineScope.launch {
                try {
                    val results = GeminiClient.scanMultipleReceipts(context, uris)
                    if (results.isNotEmpty()) {
                        initialScannedReceiptsForDialog = results
                        showAddDialog = true
                        Toast.makeText(context, if (isId) "Pendeteksian multi-nota selesai!" else "Multi-receipt detection completed!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, if (isId) "Gagal mendeteksi rincian dari struk." else "No details detected from the receipts.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, (if (isId) "Gagal memindai struk: " else "Failed to scan receipts: ") + e.message, Toast.LENGTH_LONG).show()
                } finally {
                    isScanningReceipt = false
                }
            }
        }
    }

    var showSavingsSimDialog by remember { mutableStateOf(false) }
    var showTipsDialog by remember { mutableStateOf(false) }
    var showDashboardCategoryDialog by remember { mutableStateOf(false) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                     val calendar = java.util.Calendar.getInstance()
                     PdfExporter.generateMonthlyPdfReport(
                         context = context,
                         outputStream = outputStream,
                         month = calendar.get(java.util.Calendar.MONTH),
                         year = calendar.get(java.util.Calendar.YEAR),
                         transactions = transactions,
                         wallets = wallets,
                         categories = categories,
                         viewModel = viewModel
                     )
                     Toast.makeText(context, if (isId) "Laporan PDF berhasil disimpan!" else "PDF Report saved successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkForAppUpdates()
    }

    LaunchedEffect(updateResult) {
        if (updateResult is UpdateResult.NewUpdate) {
            showUpdateDialog = true
        }
    }

    val last5Transactions = remember(transactions) { transactions.take(5) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 0. Greeting Header (Fresh Wallet Style)
            if (isFresh) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isId) "Halo, $userGreetingName! 👋" else "Hello, $userGreetingName! 👋",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (isId) "Kantong keuangan aman terkendali." else "All pocket funds are safe & secure.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { showTipsDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isId) "Halo, $userGreetingName! 👋" else "Hello, $userGreetingName! 👋",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isId) "Semua keuangan aman dan terkendali." else "Financial health is secure and steady.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 0.5. GitHub Update Banner (if available)
            val currentUpdate = updateResult
            if (currentUpdate is UpdateResult.NewUpdate) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showUpdateDialog = true },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isId) "Pembaruan Aplikasi Tersedia! 🚀" else "New App Update Available! 🚀",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (isId) "Versi ${currentUpdate.latestVersionName} telah rilis di GitHub. Ketuk untuk mengunduh." else "Version ${currentUpdate.latestVersionName} is available on GitHub. Tap to update.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            FilledTonalButton(
                                onClick = { showUpdateDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(if (isId) "Lihat" else "View", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // 1. Total Balance Card
            item {
                val isDark = isSystemInDarkTheme()
                val cardShape = RoundedCornerShape(24.dp)
                
                val customCardBg = if (isFresh) {
                    if (isDark) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    } else {
                        when (currentTheme) {
                            "MINT" -> Color(0xFFF0FDFB)
                            "OCEAN" -> Color(0xFFF0F7FF)
                            "SUNSET" -> Color(0xFFFFFBEB)
                            "SAKURA" -> Color(0xFFFFF5F5)
                            else -> Color(0xFFFAF9F6)
                        }
                    }
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }

                val borderStroke = if (isFresh) {
                    BorderStroke(
                        width = 1.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                } else {
                    null
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .testTag("total_balance_card"),
                    shape = cardShape,
                    border = borderStroke,
                    colors = CardDefaults.cardColors(containerColor = customCardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isFresh) 0.dp else 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (isFresh) {
                            Canvas(
                                modifier = Modifier
                                    .matchParentSize()
                                    .alpha(if (isDark) 0.04f else 0.08f)
                            ) {
                                drawCircle(
                                    color = if (isDark) Color.White else Color(0xFFFFCC00),
                                    radius = size.width * 0.32f,
                                    center = androidx.compose.ui.geometry.Offset(size.width * 0.94f, size.height * 0.12f)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isFresh) 20.dp else 24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isFresh) {
                                                    if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFFFECB3)
                                                } else {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (isFresh) {
                                                if (isId) "Kantong Utama" else "Main Pocket"
                                            } else {
                                                if (isId) "TOTAL SALDO" else "TOTAL BALANCE"
                                            },
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isFresh) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = if (isHidden) "Pocket ID: ••••-••••-••••" else "Pocket ID: 1032-8633-9142",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clickable {
                                                            if (isHidden) {
                                                                Toast.makeText(context, if (isId) "Buka sembunyi saku untuk menyalin!" else "Unhide pocket values to copy!", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, if (isId) "Pocket ID disalin!" else "Pocket ID copied!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.toggleHideAmounts() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Balance Visibility",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = if (isHidden) "Rp ••••••" else viewModel.formatRupiah(totalBalance),
                                style = if (isFresh) {
                                    MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 32.sp,
                                        letterSpacing = (-0.5).sp
                                    )
                                } else {
                                    MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                },
                                color = if (isFresh) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                            )

                            if (isFresh) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.White)
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFEEEEEE)
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onNavigateToTab(1) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_wallet_custom),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isId) "Kelola Kantong Keuangan →" else "Manage Financial Pockets →",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isId) "${wallets.size} Akun Terhubung" else "${wallets.size} Connected accounts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isId) "Lihat Rincian →" else "View Details →",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isFresh) {
                // Large double pills side-by-side like "Transfer & Pay" and "Scan QRIS"
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isDark = isSystemInDarkTheme()
                        val pillBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.White
                        val strokeColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFEFEFEF)
                        val pillShape = RoundedCornerShape(24.dp)
                        
                        // Pill 1: Tambah Transaksi (Add Transaction)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clip(pillShape)
                                .clickable { showAddDialog = true },
                            shape = pillShape,
                            border = BorderStroke(1.dp, strokeColor),
                            colors = CardDefaults.cardColors(containerColor = pillBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isId) "Tambah Transaksi" else "Add Transaction",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Pill 2: Pindai Nota (Scan Receipt)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clip(pillShape)
                                .clickable { showScanOptionsDialog = true },
                            shape = pillShape,
                            border = BorderStroke(1.dp, strokeColor),
                            colors = CardDefaults.cardColors(containerColor = pillBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEDE7F6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DocumentScanner,
                                        contentDescription = null,
                                        tint = Color(0xFF673AB7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isId) "Pindai Nota" else "Scan Receipt",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Plan Ahead Banner Section like Jago's
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isId) "Asisten Rencana" else "Plan Ahead",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (isId) "Sembunyikan" else "Hide",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    Toast.makeText(context, if (isId) "Asisten selalu bersamamu!" else "Your plan coach stays active!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.08f) else Color(0xFFFFE0B2)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else Color(0xFFFFF9F2)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFFE0B2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📋", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isId) "Belum Bayar Tagihan Buku?" else "Unpaid items or dues?",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFE65100)
                                    )
                                    Text(
                                        text = if (isId) "Jangan lupa cek & selesaikan cicilan atau pinjaman kamu hari ini." else "Never forget your dues. Check details on the bills screen.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Shortcuts (Fitur Unggulan) Jago/GoPay styled grids
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isId) "Fitur Pilihan & Navigasi" else "Shortcuts & Navigation",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Shortcut 1: Riwayat Transaksi (Tab 2)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(76.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else Color(0xFFF9FAFB)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.06f) else Color(0xFFEEEEEE),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { onNavigateToTab(2) }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_receipt_custom),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (isId) "Transaksi" else "Transactions",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isId) "Buku Kas" else "Ledger",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Shortcut 2: Analisis (Tab 3)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(76.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else Color(0xFFF9FAFB)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.06f) else Color(0xFFEEEEEE),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { onNavigateToTab(3) }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_analytics_custom),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (isId) "Analisis Kas" else "Analytics",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isId) "Arus Keuangan" else "Flow Analysis",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Shortcut 3: Utang & Tagihan (Tab 4)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(76.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else Color(0xFFF9FAFB)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.06f) else Color(0xFFEEEEEE),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { onNavigateToTab(4) }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_debts_custom),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (isId) "Tagihan" else "Bills & Debts",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isId) "Utang Piutang" else "Debts Ledger",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Shortcut 4: Unduh Laporan PDF
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(76.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else Color(0xFFF9FAFB)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.06f) else Color(0xFFEEEEEE),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            try {
                                                pdfLauncher.launch(if (isId) "Laporan_Keuangan_DuitKu" else "Financial_Report_DuitKu")
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error launching file picker", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_pdf_custom),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (isId) "Unduh PDF" else "Export PDF",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isId) "Laporan" else "Report",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Category & Tips Dialogs moved to screen level below LazyColumn
            }

            // 2. Income and Expense Summary Cards (Row of 2 or Unified Card)
            item {
                if (isFresh) {
                    val unifiedShape = RoundedCornerShape(24.dp)
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                unifiedShape
                            ),
                        shape = unifiedShape,
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left side: Income
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE8F5E9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Income",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isId) "Pemasukan" else "Income",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (isHidden) "Rp ••••••" else viewModel.formatRupiah(monthlyIncome),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    if (isId) "Bulan ini" else "This month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1B5E20).copy(alpha = 0.7f)
                                )
                            }

                            // Vertical divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(60.dp)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            )

                            // Right side: Expense
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFEBEE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Expense",
                                            tint = Color(0xFFC62828),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isId) "Pengeluaran" else "Expense",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB71C1C)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (isHidden) "Rp ••••••" else viewModel.formatRupiah(monthlyExpense),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFC62828)
                                )
                                Text(
                                    if (isId) "Bulan ini" else "This month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB71C1C).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Income Card
                        val incomeShape = RoundedCornerShape(24.dp)
                        ElevatedCard(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (isFresh) {
                                        Modifier.border(
                                            BorderStroke(1.5.dp, Color(0xFF2E7D32).copy(alpha = 0.25f)),
                                            incomeShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            shape = incomeShape,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (isFresh) Color(0xFFE8F5E9).copy(alpha = 0.5f) else MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE8F5E9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Income",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isId) "Pemasukan" else "Income",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFresh) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (isHidden) "Rp ••••••" else viewModel.formatRupiah(monthlyIncome),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    if (isId) "Bulan ini" else "This month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isFresh) Color(0xFF1B5E20).copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Expense Card
                        val expenseShape = RoundedCornerShape(24.dp)
                        ElevatedCard(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (isFresh) {
                                        Modifier.border(
                                            BorderStroke(1.5.dp, Color(0xFFC62828).copy(alpha = 0.25f)),
                                            expenseShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            shape = expenseShape,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (isFresh) Color(0xFFFFEBEE).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFEBEE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Expense",
                                            tint = Color(0xFFC62828),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isId) "Pengeluaran" else "Expense",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFresh) Color(0xFFB71C1C) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (isHidden) "Rp ••••••" else viewModel.formatRupiah(monthlyExpense),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFC62828)
                                )
                                Text(
                                    if (isId) "Bulan ini" else "This month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isFresh) Color(0xFFB71C1C).copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Transactions List Section (Last 5 transactions)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isId) "5 Transaksi Terakhir" else "Last 5 Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (last5Transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (isId) "Belum ada transaksi tercatat." else "No transactions recorded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(last5Transactions, key = { it.id }) { txn ->
                    val walletOfTx = wallets.firstOrNull { it.id == txn.walletId }
                    val targetWalletOfTx = wallets.firstOrNull { it.id == txn.targetWalletId }
                    val categoryOfTx = categories.firstOrNull { it.id == txn.categoryId }

                    TransactionItemRow(
                        transaction = txn,
                        wallet = walletOfTx,
                        targetWallet = targetWalletOfTx,
                        category = categoryOfTx,
                        viewModel = viewModel,
                        onDelete = {},
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        // Floating Action Button (Only show if not on FRESH theme)
        if (!isFresh) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_transaction_fab"),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(id = R.drawable.ic_add_custom), contentDescription = "Add")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isId) "Transaksi Baru" else "New Transaction", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            viewModel = viewModel,
            wallets = wallets,
            categories = categories,
            initialScannedReceipts = initialScannedReceiptsForDialog,
            onDismiss = { 
                showAddDialog = false
                initialScannedReceiptsForDialog = emptyList()
            }
        )
    }

    if (showScanOptionsDialog) {
        Dialog(
            onDismissRequest = { showScanOptionsDialog = false }
        ) {
            val dialogWidth = if (LocalConfiguration.current.screenWidthDp < 600) (LocalConfiguration.current.screenWidthDp * 0.94).dp else 400.dp
            Card(
                modifier = Modifier
                    .width(dialogWidth)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isId) "Pindai Nota Belanja" else "Scan Shopping Receipt",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
                        IconButton(onClick = { showScanOptionsDialog = false }) {
                            Icon(painterResource(id = R.drawable.ic_close_custom), contentDescription = "Close")
                        }
                    }

                    Text(
                        text = if (isId) 
                            "Gunakan asisten kecerdasan buatan Gemini AI untuk memindai struk belanja secara otomatis."
                            else "Use Gemini AI assistant to automatically extract details from your shopping receipts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 1: Kamera
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showScanOptionsDialog = false
                                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                
                                if (hasPermission) {
                                    val uri = try {
                                        val tempFile = File.createTempFile("receipt_cam_", ".jpg", context.cacheDir).apply {
                                            createNewFile()
                                            deleteOnExit()
                                        }
                                        FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            tempFile
                                        )
                                    } catch (e: Exception) {
                                        null
                                    }
                                    if (uri != null) {
                                        tempPhotoUri = uri
                                        try {
                                            dashboardCameraLauncher.launch(uri)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, (if (isId) "Gagal membuka kamera: " else "Failed to open camera: ") + e.message, Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, if (isId) "Gagal membuat file foto" else "Failed to initialize camera file", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    dashboardRequestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isId) "Ambil Foto via Kamera" else "Take Photo via Camera",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (isId) "Ambil gambar nota instan" else "Snap an instant receipt photo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Option 2: Galeri
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showScanOptionsDialog = false
                                dashboardPhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isId) "Unggah dari Galeri" else "Upload from Gallery",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = if (isId) "Pilih satu/banyak gambar nota" else "Select single/multiple receipt images",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isScanningReceipt) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Card(
                modifier = Modifier
                    .width(320.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = if (isId) "Menganalisis Nota" else "Analyzing Receipt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isId) "Asisten Kecerdasan Buatan Gemini AI sedang membaca detail rincian belanja Anda..." else "Gemini AI assistant is reading your shopping receipt details...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    if (showSavingsSimDialog) {
        Dialog(
            onDismissRequest = { showSavingsSimDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val dialogWidth = if (LocalConfiguration.current.screenWidthDp < 600) (LocalConfiguration.current.screenWidthDp * 0.94).dp else 520.dp
            Card(
                modifier = Modifier
                    .width(dialogWidth)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isId) "Simulasi Anggaran Cerdas" else "Smart Budget Simulation",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
                        IconButton(onClick = { showSavingsSimDialog = false }) {
                            Icon(painterResource(id = R.drawable.ic_close_custom), contentDescription = "Close")
                        }
                    }

                    Text(
                        text = if (isId) 
                            "Gunakan kalkulator saku cerdas ini untuk memproyeksikan target tabungan bulanan Anda berdasarkan formula budgeting 50/30/20!" 
                            else "Use this budgeting projector to simulate your monthly savings goals based on the balanced 50/30/20 formula!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var monthlyIncomeInput by remember { mutableStateOf("5000000") }
                    OutlinedTextField(
                        value = monthlyIncomeInput,
                        onValueChange = { monthlyIncomeInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isId) "Estimasi Pendapatan Bulanan (Rp)" else "Monthly Income Estimate (Rp)") },
                        leadingIcon = { Text("Rp", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val incomeAmount = monthlyIncomeInput.toDoubleOrNull() ?: 0.0
                    val needs = incomeAmount * 0.5
                    val wants = incomeAmount * 0.3
                    val savings = incomeAmount * 0.2

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isId) "Hasil Formula Jago 50/30/20:" else "Projected 50/30/20 Allocation:",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isId) "Kebutuhan Pokok (50%)" else "Needs & Dues (50%)", style = MaterialTheme.typography.bodySmall)
                                Text(viewModel.formatRupiah(needs), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isId) "Keinginan & Jajan (30%)" else "Wants & Lifestyle (30%)", style = MaterialTheme.typography.bodySmall)
                                Text(viewModel.formatRupiah(wants), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isId) "Masa Depan & Tabungan (20%)" else "Invest & Savings (20%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Text(viewModel.formatRupiah(savings), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Button(
                        onClick = { showSavingsSimDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isId) "Terapkan Rencana Ini" else "Apply Plan Principles")
                    }
                }
            }
        }
    }

    if (showDashboardCategoryDialog) {
        CategoryManagementDialog(
            viewModel = viewModel,
            onDismiss = { showDashboardCategoryDialog = false }
        )
    }

    if (showTipsDialog) {
        val tipsId = listOf(
            "Formula 50/30/20: Taruh 50% untuk Kebutuhan Pokok, 30% Keinginan, dan 20% Tabungan/Utang.",
            "Hemat jajan kopi harian luar! Membuat sendiri di rumah/kantor menghemat puluhan ribu sehari.",
            "Aset terbaik adalah pencatatan keuangan! Rutin catat sekecil apapun pengeluaran Anda di DuitKu.",
            "Membagi kantong (misal Kantong Belanja vs Kantong Tabungan) menjaga saldo Anda tidak gampang boncos.",
            "Disiplin menabung di awal gajian, amankan juga dana darurat terlebih dahulu sebelum konsumtif berlebih."
        )
        val tipsEn = listOf(
            "The 50/30/20 Rule: Set 50% for Needs, 30% for Wants, and 20% for Savings.",
            "Avoid expensive daily takeout coffee! Making your own drink saves massive wallet pocket cash.",
            "Tracking builds security! Consistent financial logging in DuitKu secures early detection of leaks.",
            "Segregate your holdings! Having separate pockets for groceries and savings halts excessive spending.",
            "Pay yourself first! Transfer money to your savings pocket immediately upon getting paid to beat temptation."
        )
        val tipsToUse = if (isId) tipsId else tipsEn
        val randomTip = remember(showTipsDialog) { tipsToUse.random() }

        AlertDialog(
            onDismissRequest = { showTipsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(if (isId) "Tips Dompet Cerdas" else "Smart Saving Tips")
                }
            },
            text = {
                Text(
                    text = randomTip,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showTipsDialog = false }) {
                    Text(if (isId) "Keren, Mengerti!" else "Got it!")
                }
            }
        )
    }

    if (showUpdateDialog) {
        val update = updateResult as? UpdateResult.NewUpdate
        if (update != null) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isId) "Pembaruan Tersedia" else "Update Available",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isId)
                                "Versi baru (${update.latestVersionName}) telah dirilis! Versi Anda saat ini adalah v${viewModel.getAppVersionName()}."
                                else "A new version (${update.latestVersionName}) is available! Your current version is v${viewModel.getAppVersionName()}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (update.releaseNotes.isNotEmpty()) {
                            Text(
                                text = if (isId) "Catatan Rilis:" else "Release Notes:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 150.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = update.releaseNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            showUpdateDialog = false
                        }
                    ) {
                        Text(if (isId) "Unduh & Perbarui" else "Download & Update", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showUpdateDialog = false }
                    ) {
                        Text(if (isId) "Nanti" else "Later")
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionItemRow(
    transaction: Transaction,
    wallet: Wallet?,
    targetWallet: Wallet?,
    category: Category?,
    viewModel: FinanceViewModel,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    val isHidden by viewModel.isAmountsHidden.collectAsState()

    var showDetailsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val cardShape = RoundedCornerShape(24.dp)
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable { showDetailsDialog = true },
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val visualInfo = CategoryVisuals.getVisualInfo(
                    categoryName = category?.name,
                    transactionType = transaction.type,
                    note = transaction.note
                )

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(visualInfo.backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = visualInfo.iconRes),
                        contentDescription = category?.name ?: transaction.type,
                        tint = visualInfo.iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    val labelText = when (transaction.type) {
                        "TRANSFER" -> {
                            val asal = wallet?.name ?: "???"
                            val tujuan = targetWallet?.name ?: "???"
                            "Transfer: $asal → $tujuan"
                        }
                        else -> category?.name ?: (if (isId) "Tanpa Kategori" else "Uncategorized")
                    }
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (transaction.note.isNotEmpty()) {
                        Text(
                            text = transaction.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${viewModel.formatDate(transaction.date)} • ${wallet?.name ?: (if (isId) "Dompet" else "Wallet")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        if (transaction.adminFee > 0.0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Text(
                                    text = "+Admin ${viewModel.formatRupiah(transaction.adminFee)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            val priceColor = when (transaction.type) {
                "INCOME" -> Color(0xFF2E7D32)
                "EXPENSE" -> Color(0xFFC62828)
                else -> Color(0xFF1565C0)
            }
            val prefix = when (transaction.type) {
                "INCOME" -> "+"
                "EXPENSE" -> "-"
                else -> "⇄"
            }

            Text(
                text = if (isHidden) "$prefix Rp ••••••" else "$prefix ${viewModel.formatRupiah(transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                color = priceColor
            )
        }
    }

    if (showDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = {
                Text(
                    text = if (isId) "Rincian Transaksi" else "Transaction Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = when (transaction.type) {
                            "EXPENSE" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            "INCOME" -> Color(0xFFE8F5E9)
                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = when (transaction.type) {
                                    "EXPENSE" -> if (isId) "Pengeluaran" else "Expense"
                                    "INCOME" -> if (isId) "Pemasukan" else "Income"
                                    else -> "Transfer"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (transaction.type) {
                                    "EXPENSE" -> Color(0xFFC62828)
                                    "INCOME" -> Color(0xFF2E7D32)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.formatRupiah(transaction.amount),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = when (transaction.type) {
                                    "EXPENSE" -> Color(0xFFC62828)
                                    "INCOME" -> Color(0xFF2E7D32)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardDetailRow(label = if (isId) "Tanggal" else "Date", value = viewModel.formatDate(transaction.date))

                        if (category != null) {
                            DashboardDetailRow(label = if (isId) "Kategori" else "Category", value = category.name)
                        }

                        if (transaction.type == "TRANSFER" && targetWallet != null) {
                            DashboardDetailRow(label = if (isId) "Dari Dompet" else "From Wallet", value = wallet?.name ?: "Unknown")
                            DashboardDetailRow(label = if (isId) "Ke Dompet" else "To Wallet", value = targetWallet.name)
                        } else {
                            DashboardDetailRow(label = if (isId) "Dompet" else "Wallet", value = wallet?.name ?: "Unknown")
                        }

                        if (transaction.adminFee > 0.0) {
                            DashboardDetailRow(
                                label = if (isId) "Biaya Admin" else "Admin Fee",
                                value = viewModel.formatRupiah(transaction.adminFee)
                            )
                            val totalDeducted = transaction.amount + transaction.adminFee
                            DashboardDetailRow(
                                label = if (isId) "Total Dipotong Dari Dompet" else "Total Deducted From Wallet",
                                value = viewModel.formatRupiah(totalDeducted)
                            )
                        }

                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        )

                        Column {
                            Text(
                                text = if (isId) "Catatan / Deskripsi" else "Note / Description",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = transaction.note.ifBlank { if (isId) "Tidak ada catatan." else "No description added." },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showDeleteConfirm = true }
                    ) {
                        Text(if (isId) "Hapus" else "Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showDetailsDialog = false }) {
                        Text(if (isId) "Tutup" else "Close", fontWeight = FontWeight.Bold)
                    }
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = if (isId) "Hapus Transaksi" else "Delete Transaction",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isId) 
                            "Pilih bagaimana Anda ingin menghapus transaksi ini:" 
                        else "Choose how you want to delete this transaction:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Option 1: Only history
                    Surface(
                        onClick = {
                            viewModel.deleteTransaction(transaction, refund = false)
                            showDeleteConfirm = false
                            showDetailsDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isId) "Hanya Hapus Riwayat" else "Delete History Only",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isId)
                                    "Hanya menghapus catatan. Saldo dompet tidak akan diubah/dikembalikan."
                                else "Removes record only. The wallet balance will NOT be modified.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Option 2: Refund
                    Surface(
                        onClick = {
                            viewModel.deleteTransaction(transaction, refund = true)
                            showDeleteConfirm = false
                            showDetailsDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.SettingsBackupRestore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isId) "Batalkan & Refund Saldo" else "Cancel & Refund Wallet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isId)
                                    "Membatalkan transaksi dan mengembalikan uang ke saldo dompet Anda."
                                else "Cancels the transaction and restores/reverts funds in the wallet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(if (isId) "Batal" else "Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
fun DashboardDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    viewModel: FinanceViewModel,
    wallets: List<Wallet>,
    categories: List<Category>,
    initialScannedReceipts: List<GeminiClient.ScanResult> = emptyList(),
    onDismiss: () -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    var amountStr by remember { mutableStateOf("") }
    var adminFeeStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // "INCOME", "EXPENSE", "TRANSFER"
    var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: 0) }
    var selectedTargetWalletId by remember { mutableStateOf(wallets.getOrNull(1)?.id ?: wallets.firstOrNull()?.id ?: 0) }
    var selectedCategoryId by remember { mutableStateOf(0) }
    var note by remember { mutableStateOf("") }
    var scannedReceipts by remember { mutableStateOf<List<GeminiClient.ScanResult>>(initialScannedReceipts) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isScanningInsideDialog by remember { mutableStateOf(false) }
    var tempPhotoUriInsideDialog by remember { mutableStateOf<Uri?>(null) }

    val dialogCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = tempPhotoUriInsideDialog
            if (uri != null) {
                isScanningInsideDialog = true
                coroutineScope.launch {
                    try {
                        val results = GeminiClient.scanMultipleReceipts(context, listOf(uri))
                        if (results.isNotEmpty()) {
                            if (results.size == 1) {
                                val single = results.first()
                                if (single.amount > 0.0) amountStr = single.amount.toInt().toString()
                                if (single.note.isNotBlank()) note = single.note
                                if (single.type == "INCOME" || single.type == "EXPENSE") {
                                    selectedType = single.type
                                }
                            } else {
                                scannedReceipts = results
                            }
                            Toast.makeText(context, if (isId) "Pendeteksian struk selesai!" else "Receipt detection completed!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, if (isId) "Gagal mendeteksi rincian dari struk." else "No details detected from the receipt.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, (if (isId) "Gagal memindai struk: " else "Failed to scan receipt: ") + (e.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
                    } finally {
                        isScanningInsideDialog = false
                    }
                }
            }
        }
    }

    val dialogRequestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = try {
                val tempFile = File.createTempFile("receipt_cam_", ".jpg", context.cacheDir).apply {
                    createNewFile()
                    deleteOnExit()
                }
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
            } catch (e: Exception) {
                null
            }
            if (uri != null) {
                tempPhotoUriInsideDialog = uri
                try {
                    dialogCameraLauncher.launch(uri)
                } catch (e: Exception) {
                    Toast.makeText(context, (if (isId) "Gagal membuka kamera: " else "Failed to open camera: ") + (e.message ?: ""), Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, if (isId) "Izin kamera diperlukan untuk mengambil foto struk" else "Camera permission is required to take receipt photos", Toast.LENGTH_SHORT).show()
        }
    }

    val dialogPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isScanningInsideDialog = true
            coroutineScope.launch {
                try {
                    val results = GeminiClient.scanMultipleReceipts(context, uris)
                    if (results.isNotEmpty()) {
                        if (results.size == 1) {
                            val single = results.first()
                            if (single.amount > 0.0) amountStr = single.amount.toInt().toString()
                            if (single.note.isNotBlank()) note = single.note
                            if (single.type == "INCOME" || single.type == "EXPENSE") {
                                selectedType = single.type
                            }
                        } else {
                            scannedReceipts = results
                        }
                        Toast.makeText(context, if (isId) "Pendeteksian struk selesai!" else "Receipt detection completed!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, if (isId) "Gagal mendeteksi rincian dari struk." else "No details detected from the receipts.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, (if (isId) "Gagal memindai struk: " else "Failed to scan receipts: ") + (e.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
                } finally {
                    isScanningInsideDialog = false
                }
            }
        }
    }

    // Group categories
    val incomeCategoryList = remember(categories) { categories.filter { it.type == "INCOME" } }
    val expenseCategoryList = remember(categories) { categories.filter { it.type == "EXPENSE" } }

    // Keep state of selected category ID matched with type
    LaunchedEffect(selectedType, categories) {
        if (selectedType == "INCOME") {
            selectedCategoryId = incomeCategoryList.firstOrNull()?.id ?: 0
        } else if (selectedType == "EXPENSE") {
            selectedCategoryId = expenseCategoryList.firstOrNull()?.id ?: 0
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
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isId) "Tambah Transaksi Baru" else "Add New Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // 1. Selector Tab (High-polish Segmented Control)
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
                    val items = listOf(
                        "EXPENSE" to (if (isId) "Pengeluaran" else "Expense"),
                        "INCOME" to (if (isId) "Pemasukan" else "Income"),
                        "TRANSFER" to (if (isId) "Transfer" else "Transfer")
                    )
                    items.forEach { (type, label) ->
                        val isSelected = selectedType == type
                        val tabItemShape = RoundedCornerShape(8.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = tabItemShape
                                )
                                .clip(tabItemShape)
                                .clickable { selectedType = type }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Quick Scan Receipt Section (Camera & Gallery)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = "Scan Struk",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isId) "Pindai Struk / Upload Gambar" else "Scan Receipt / Upload Image",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (isScanningInsideDialog) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            }
                        }
                        Text(
                            text = if (isId) 
                                "Otomatis isi nominal, catatan, dan tipe dari foto struk belanja dengan AI Gemini." 
                            else 
                                "Auto-populate amount, notes, and category from receipt photo via Gemini AI.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    dialogRequestPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                enabled = !isScanningInsideDialog,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Kamera",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isId) "Kamera" else "Camera",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    dialogPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isScanningInsideDialog,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "Galeri",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isId) "Galeri Foto" else "Gallery",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (scannedReceipts.isNotEmpty()) {
                    Text(
                        text = if (isId) "Rincian Transaksi Terdeteksi:" else "Detected Transactions Detail:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    scannedReceipts.forEachIndexed { index, receipt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${if (isId) "Nota" else "Receipt"} #${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = {
                                            scannedReceipts = scannedReceipts.filterIndexed { idx, _ -> idx != index }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_delete_custom),
                                            contentDescription = "Hapus Nota",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = receipt.note,
                                    onValueChange = { newNote ->
                                        scannedReceipts = scannedReceipts.mapIndexed { idx, item ->
                                            if (idx == index) item.copy(note = newNote) else item
                                        }
                                    },
                                    label = { Text(if (isId) "Catatan / Toko" else "Note / Store") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = if (receipt.amount == 0.0) "" else receipt.amount.toInt().toString(),
                                    onValueChange = { newAmtStr ->
                                        val amtDouble = newAmtStr.toDoubleOrNull() ?: 0.0
                                        scannedReceipts = scannedReceipts.mapIndexed { idx, item ->
                                            if (idx == index) item.copy(amount = amtDouble) else item
                                        }
                                    },
                                    label = { Text(if (isId) "Jumlah (Uang)" else "Amount") },
                                    prefix = { Text("Rp ") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                } else {
                    // 2. Regular Single Amount Input
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amountStr = it },
                        label = { Text(if (isId) "Jumlah (Uang)" else "Amount (Money)") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("0") }
                    )

                    // 2b. Admin Fee Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = adminFeeStr,
                            onValueChange = { if (it.all { char -> char.isDigit() }) adminFeeStr = it },
                            label = { Text(if (isId) "Biaya Admin (Opsional)" else "Admin Fee (Optional)") },
                            prefix = { Text("Rp ") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Quick chips for Admin Fee
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("0" to "Rp 0", "1000" to "1.000", "2500" to "2.500", "6500" to "6.500").forEach { (valStr, label) ->
                                val isSelected = adminFeeStr == valStr || (valStr == "0" && (adminFeeStr.isEmpty() || adminFeeStr == "0"))
                                Surface(
                                    onClick = { adminFeeStr = if (valStr == "0") "" else valStr },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        val parsedAmt = amountStr.toDoubleOrNull() ?: 0.0
                        val parsedFee = adminFeeStr.toDoubleOrNull() ?: 0.0

                        if (selectedType == "TRANSFER" && parsedAmt > 0.0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (isId) "Ringkasan Aliran Dana Transfer:" else "Transfer Fund Flow Summary:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isId) 
                                            "• Terpotong dari dompet asal: ${viewModel.formatRupiah(parsedAmt + parsedFee)} (Nominal + Admin)"
                                        else 
                                            "• Deducted from source: ${viewModel.formatRupiah(parsedAmt + parsedFee)} (Amount + Admin)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isId) 
                                            "• Masuk ke dompet tujuan: ${viewModel.formatRupiah(parsedAmt)} (Bersih tanpa admin)"
                                        else 
                                            "• Received in target: ${viewModel.formatRupiah(parsedAmt)} (Net amount)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (parsedFee > 0.0 && parsedAmt > 0.0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text(
                                    text = if (isId) 
                                        "Total saldo terpotong: ${viewModel.formatRupiah(parsedAmt + parsedFee)} (${viewModel.formatRupiah(parsedAmt)} + Admin ${viewModel.formatRupiah(parsedFee)})"
                                    else 
                                        "Total deducted: ${viewModel.formatRupiah(parsedAmt + parsedFee)} (${viewModel.formatRupiah(parsedAmt)} + Fee ${viewModel.formatRupiah(parsedFee)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Wallets Selection (From / Source Wallet)
                Text(if (isId) "Dompet Asal:" else "Source Wallet:", style = MaterialTheme.typography.labelMedium)
                if (wallets.isEmpty()) {
                    Text(if (isId) "Dompet tidak ditemukan. Silakan tambahkan dompet terlebih dahulu di tab Dompet." else "No wallets found. Please add a wallet first in the Wallets tab.", color = MaterialTheme.colorScheme.error)
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(wallets, key = { it.id }) { w ->
                            SimpleCustomChip(
                                text = w.name,
                                isSelected = selectedWalletId == w.id,
                                onClick = { selectedWalletId = w.id }
                            )
                        }
                    }
                }

                // 4. Specific to Transfer Destination Wallet
                if (selectedType == "TRANSFER") {
                    Text(if (isId) "Dompet Tujuan:" else "Destination Wallet:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(wallets, key = { it.id }) { w ->
                            SimpleCustomChip(
                                text = w.name,
                                isSelected = selectedTargetWalletId == w.id,
                                onClick = { selectedTargetWalletId = w.id }
                            )
                        }
                    }
                }

                // 5. Category Selection (Only for Income and Expense)
                if (selectedType != "TRANSFER") {
                    Text(if (isId) "Kategori:" else "Category:", style = MaterialTheme.typography.labelMedium)
                    val listToShow = if (selectedType == "INCOME") incomeCategoryList else expenseCategoryList
                    
                    if (listToShow.isEmpty()) {
                        Text(if (isId) "Kategori tidak ditemukan." else "No categories found.")
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(listToShow, key = { it.id }) { cat ->
                                SimpleCustomChip(
                                    text = cat.name,
                                    isSelected = selectedCategoryId == cat.id,
                                    onClick = { selectedCategoryId = cat.id }
                                )
                            }
                        }
                    }
                }

                // 6. Notes Input (only show if no scanned receipts are active)
                if (scannedReceipts.isEmpty()) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(if (isId) "Catatan Tambahan" else "Additional Note") },
                        placeholder = { Text(if (isId) "misal: Belanja bulanan, bonus gaji, kopi, dll." else "e.g., Grocery store, salary bonus, coffee, etc.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isId) "Batal" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedWalletId == 0) return@Button

                            if (scannedReceipts.isNotEmpty()) {
                                // Save all scanned receipts
                                for (receipt in scannedReceipts) {
                                    if (receipt.amount > 0.0) {
                                        viewModel.addTransaction(
                                            amount = receipt.amount,
                                            type = receipt.type,
                                            walletId = selectedWalletId,
                                            categoryId = selectedCategoryId,
                                            note = receipt.note,
                                            date = System.currentTimeMillis(),
                                            targetWalletId = null
                                        )
                                    }
                                }
                            } else {
                                // Save single manual transaction
                                val amountVal = amountStr.toDoubleOrNull() ?: 0.0
                                if (amountVal <= 0.0) return@Button

                                if (selectedType == "TRANSFER" && selectedWalletId == selectedTargetWalletId) {
                                    // Can't transfer to same wallet
                                    return@Button
                                }

                                val adminFeeVal = adminFeeStr.toDoubleOrNull() ?: 0.0
                                viewModel.addTransaction(
                                    amount = amountVal,
                                    type = selectedType,
                                    walletId = selectedWalletId,
                                    categoryId = selectedCategoryId,
                                    note = note,
                                    date = System.currentTimeMillis(),
                                    targetWalletId = if (selectedType == "TRANSFER") selectedTargetWalletId else null,
                                    adminFee = adminFeeVal
                                )
                            }
                            onDismiss()
                        },
                        enabled = wallets.isNotEmpty() && (
                            scannedReceipts.isNotEmpty() || amountStr.isNotEmpty()
                        )
                    ) {
                        Text(if (isId) "Simpan" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleCustomChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val chipShape = RoundedCornerShape(percent = 50)
    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        label = "chipBgColor"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chipContentColor"
    )
    Surface(
        onClick = onClick,
        shape = chipShape,
        color = animatedBgColor,
        contentColor = animatedContentColor,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
