package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.ui.components.NewTransactionModal
import com.example.ui.components.TransactionItemRow
import com.example.ui.theme.*
import com.example.ui.util.GeminiClient
import com.example.ui.util.UpdateResult
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch
import java.io.File

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
    val userGreetingName by viewModel.userGreetingName.collectAsState()
    val updateResult by viewModel.updateResult.collectAsState()
    val todayRemainingDailyBudget by viewModel.todayRemainingDailyBudget.collectAsState(initial = 0.0)
    val todayStartOfDayBudget by viewModel.todayStartOfDayBudget.collectAsState(initial = 0.0)
    val todayVariableExpenseSum by viewModel.todayVariableExpenseSum.collectAsState(initial = 0.0)
    val monthlyVariableBudget by viewModel.monthlyVariableBudget.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var initialScannedReceiptsForDialog by remember { mutableStateOf<List<GeminiClient.ScanResult>>(emptyList()) }
    var showScanOptionsDialog by remember { mutableStateOf(false) }
    var isScanningReceipt by remember { mutableStateOf(false) }
    var showTipsDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

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
                val expenseCategoryNames = categories.filter { it.type == "EXPENSE" }.map { it.name }
                coroutineScope.launch {
                    try {
                        val results = GeminiClient.scanMultipleReceipts(context, listOf(uri), expenseCategoryNames)
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
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            } catch (e: Exception) {
                null
            }
            if (uri != null) {
                tempPhotoUri = uri
                dashboardCameraLauncher.launch(uri)
            }
        }
    }

    val dashboardPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isScanningReceipt = true
            val expenseCategoryNames = categories.filter { it.type == "EXPENSE" }.map { it.name }
            coroutineScope.launch {
                try {
                    val results = GeminiClient.scanMultipleReceipts(context, uris, expenseCategoryNames)
                    if (results.isNotEmpty()) {
                        initialScannedReceiptsForDialog = results
                        showAddDialog = true
                        Toast.makeText(context, if (isId) "Pendeteksian multi-nota selesai!" else "Multi-receipt detection completed!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, (if (isId) "Gagal memindai struk: " else "Failed to scan receipts: ") + e.message, Toast.LENGTH_LONG).show()
                } finally {
                    isScanningReceipt = false
                }
            }
        }
    }

    val last5Transactions = remember(transactions) { transactions.take(5) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Welcome Banner
            item {
                WelcomeBanner(
                    userName = userGreetingName,
                    isId = isId
                )
            }

            // 2. Total Balance Hero Card
            item {
                TotalBalanceHeroCard(
                    totalBalance = totalBalance,
                    connectedAccountsCount = wallets.size,
                    isHidden = isHidden,
                    onToggleVisibility = { viewModel.toggleHideAmounts() },
                    onViewDetails = { onNavigateToTab(1) },
                    formatRupiah = { viewModel.formatRupiah(it) },
                    isId = isId
                )
            }

            // 3. Today's Limit Progress Card
            item {
                TodayLimitProgressCard(
                    todayLimit = if (todayStartOfDayBudget > 0.0) todayStartOfDayBudget else (monthlyVariableBudget / 30.0),
                    todaySpent = todayVariableExpenseSum,
                    remaining = todayRemainingDailyBudget,
                    isBudgetConfigured = monthlyVariableBudget > 0.0 || todayStartOfDayBudget > 0.0,
                    formatRupiah = { viewModel.formatRupiah(it) },
                    isId = isId
                )
            }

            // 4. Summary Income & Expense (2 Grid Column)
            item {
                SummaryIncomeExpenseGrid(
                    income = monthlyIncome,
                    expense = monthlyExpense,
                    isHidden = isHidden,
                    formatRupiah = { viewModel.formatRupiah(it) },
                    isId = isId
                )
            }

            // Quick Actions Bar (Scan Receipt & Add Note)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showScanOptionsDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isId) "Pindai Nota" else "Scan Receipt",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateToTab(1) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_wallet_custom),
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isId) "Dompet Saya" else "My Wallets",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // 5. Last 5 Transactions List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isId) "5 Transaksi Terakhir" else "Last 5 Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = if (isId) "Lihat Semua >" else "View All >",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = AccentBlue
                        ),
                        modifier = Modifier.clickable { onNavigateToTab(2) }
                    )
                }
            }

            // Last 5 Transactions Items
            if (last5Transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = TextSecondary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isId) "Belum ada transaksi tercatat." else "No transactions recorded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
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
    }

    if (showAddDialog) {
        NewTransactionModal(
            viewModel = viewModel,
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

    if (showTipsDialog) {
        DashboardTipsDialog(
            isId = isId,
            onDismiss = { showTipsDialog = false }
        )
    }

    if (showCategoryDialog) {
        CategoryManagementDialog(
            viewModel = viewModel,
            onDismiss = { showCategoryDialog = false }
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

/**
 * 1. Welcome Banner:
 * Teks ucapan "Hello, Sobat Duit! 👋" dan status kesehatan keuangan dengan badge hijau berdenyut ("Stable").
 */
@Composable
fun WelcomeBanner(
    userName: String,
    isId: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Hello, Sobat Duit! 👋",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                ),
                color = TextPrimary
            )
            Text(
                text = if (isId) "Kesehatan keuangan terkendali" else "Financial status monitored",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Green Pulsing Badge ("Stable")
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AccentGreen.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentGreen.copy(alpha = pulseAlpha))
                )
                Text(
                    text = "Stable",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AccentGreen
                )
            }
        }
    }
}

/**
 * 2. Total Balance Hero Card:
 * Latar belakang gradien dark blue ke indigo dengan glow efek di sudut.
 * Menampilkan angka Total Balance (Rp 908.684) dengan tombol Toggle Eye (Hide/Show balance).
 * Menampilkan status jumlah akun terhubung ("6 Connected accounts") dan tombol "View Details >" ke halaman Wallets.
 */
@Composable
fun TotalBalanceHeroCard(
    totalBalance: Double,
    connectedAccountsCount: Int,
    isHidden: Boolean,
    onToggleVisibility: () -> Unit,
    onViewDetails: () -> Unit,
    formatRupiah: (Double) -> String,
    isId: Boolean,
    modifier: Modifier = Modifier
) {
    val heroShape = RoundedCornerShape(24.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 16.dp, shape = heroShape, spotColor = AccentBlue.copy(alpha = 0.35f))
            .clip(heroShape)
            .testTag("total_balance_card"),
        shape = heroShape,
        border = BorderStroke(1.dp, Color(0x223B82F6))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E3A8A), // Dark blue
                            Color(0xFF312E81)  // Indigo
                        )
                    )
                )
        ) {
            // Subtle Radial Glow Effect in the Top-Right Corner
            Canvas(
                modifier = Modifier
                    .matchParentSize()
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x5560A5FA), // Light blue glow
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.95f, size.height * 0.1f),
                        radius = size.width * 0.45f
                    ),
                    center = Offset(size.width * 0.95f, size.height * 0.1f),
                    radius = size.width * 0.45f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Top Header: Label & Eye Toggle Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isId) "TOTAL SALDO" else "TOTAL BALANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF93C5FD) // Soft blue
                    )

                    IconButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Balance",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Total Balance Numeric Text
                Text(
                    text = if (isHidden) "Rp ••••••" else formatRupiah(totalBalance),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Footer: Connected Accounts & View Details Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { onViewDetails() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isId) "$connectedAccountsCount Akun Terhubung" else "$connectedAccountsCount Connected accounts",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFFE0E7FF)
                    )

                    Text(
                        text = if (isId) "Lihat Rincian >" else "View Details >",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 3. Today's Limit Progress Card:
 * Menampilkan batas harian (Limit: Rp 43.400) dan yang sudah terpakai (Spent: Rp 10.000 / 23%).
 * Indikator LinearProgressIndicator dua warna bergradasi.
 */
@Composable
fun TodayLimitProgressCard(
    todayLimit: Double,
    todaySpent: Double,
    remaining: Double,
    isBudgetConfigured: Boolean,
    formatRupiah: (Double) -> String,
    isId: Boolean,
    modifier: Modifier = Modifier
) {
    val limit = if (todayLimit > 0.0) todayLimit else 50000.0
    val fraction = if (limit > 0.0) (todaySpent / limit).coerceIn(0.0, 1.0).toFloat() else 0f
    val percent = (fraction * 100).toInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isId) "Batas Pengeluaran Hari Ini" else "Today's Spending Limit",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Text(
                    text = "Limit: ${formatRupiah(limit)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = TextSecondary
                )
            }

            // Spent vs Percent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spent: ${formatRupiah(todaySpent)} ($percent%)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (fraction >= 0.85f) AccentRed else AccentOrange
                )

                Text(
                    text = if (isId) "Sisa: ${formatRupiah(remaining.coerceAtLeast(0.0))}" else "Left: ${formatRupiah(remaining.coerceAtLeast(0.0))}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = AccentGreen
                )
            }

            // Two-Color Gradient Linear Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E293B))
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val progressWidth = size.width * fraction
                    if (progressWidth > 0f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    AccentTeal,
                                    if (fraction >= 0.85f) AccentRed else AccentBlue
                                )
                            ),
                            size = androidx.compose.ui.geometry.Size(progressWidth, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4. Summary Income & Expense (2 Grid Column):
 * Card Income (Hijau/Emerald, teks nilai Rp ...).
 * Card Expense (Merah/Rose, teks nilai Rp ...).
 */
@Composable
fun SummaryIncomeExpenseGrid(
    income: Double,
    expense: Double,
    isHidden: Boolean,
    formatRupiah: (Double) -> String,
    isId: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Income Card (Hijau / Emerald)
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AccentGreen.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Income",
                            tint = AccentGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = if (isId) "Pemasukan" else "Income",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextSecondary
                    )
                }

                Text(
                    text = if (isHidden) "Rp ••••••" else formatRupiah(income),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = AccentGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Expense Card (Merah / Rose)
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AccentRed.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Expense",
                            tint = AccentRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = if (isId) "Pengeluaran" else "Expense",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextSecondary
                    )
                }

                Text(
                    text = if (isHidden) "Rp ••••••" else formatRupiah(expense),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = AccentRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
