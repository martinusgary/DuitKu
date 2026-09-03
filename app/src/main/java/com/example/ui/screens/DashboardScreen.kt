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
import com.example.ui.components.TransactionItemRow
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
        if (!isFresh && !showAddDialog) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .testTag("add_transaction_fab"),
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(id = R.drawable.ic_add_custom), contentDescription = "Add", modifier = Modifier.size(20.dp))
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
        ScanReceiptOptionsDialog(
            context = context,
            isId = isId,
            cameraLauncher = dashboardCameraLauncher,
            permissionLauncher = dashboardRequestPermissionLauncher,
            photoPickerLauncher = dashboardPhotoPickerLauncher,
            onSetTempPhotoUri = { tempPhotoUri = it },
            onDismiss = { showScanOptionsDialog = false }
        )
    }

    if (isScanningReceipt) {
        ScanningProgressDialog(isId = isId)
    }

    if (showSavingsSimDialog) {
        SavingsSimulationDialog(
            isId = isId,
            viewModel = viewModel,
            onDismiss = { showSavingsSimDialog = false }
        )
    }

    if (showDashboardCategoryDialog) {
        CategoryManagementDialog(
            viewModel = viewModel,
            onDismiss = { showDashboardCategoryDialog = false }
        )
    }

    if (showTipsDialog) {
        DashboardTipsDialog(
            isId = isId,
            onDismiss = { showTipsDialog = false }
        )
    }

    if (showUpdateDialog) {
        val update = updateResult as? UpdateResult.NewUpdate
        if (update != null) {
            DashboardUpdateDialog(
                isId = isId,
                currentVersion = viewModel.getAppVersionName(),
                update = update,
                context = context,
                onDismiss = { showUpdateDialog = false }
            )
        }
    }
}
