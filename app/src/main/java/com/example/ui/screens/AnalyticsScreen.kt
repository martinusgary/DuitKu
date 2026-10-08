package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.ui.components.CategoryVisuals
import com.example.ui.theme.*
import com.example.ui.util.PdfExporter
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

private val SegmentColors = listOf(
    AccentOrange,
    AccentBlue,
    AccentPurple,
    AccentTeal,
    Color(0xFFEAB308), // Yellow
    Color(0xFFEC4899), // Pink
    Color(0xFF6366F1), // Indigo
    Color(0xFF14B8A6), // Teal
    Color(0xFFF43F5E), // Rose
    Color(0xFF8B5CF6)  // Violet
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    // Metrics calculations
    val totalIncome = remember(transactions) {
        transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount + it.adminFee }
    }

    // Savings pockets calculations (wallets with icon "savings" or bank accounts)
    val savingsWallets = remember(wallets) {
        val filtered = wallets.filter { it.icon == "savings" }
        if (filtered.isNotEmpty()) filtered else wallets
    }
    val totalSavings = remember(savingsWallets) {
        savingsWallets.sumOf { it.balance }
    }

    // Category spending breakdown calculations
    val expenseTransactions = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }
    }
    val categoryBreakdown = remember(expenseTransactions, categories, totalExpense) {
        val grouped = expenseTransactions.groupBy { it.categoryId }
        grouped.map { (catId, txList) ->
            val cat = categories.firstOrNull { it.id == catId }
            val amount = txList.sumOf { it.amount + it.adminFee }
            val percentage = if (totalExpense > 0.0) (amount / totalExpense) * 100.0 else 0.0
            CategoryExpenseItem(
                categoryName = cat?.name ?: (if (isId) "Lain-lain" else "Other"),
                amount = amount,
                percentage = percentage
            )
        }.sortedByDescending { it.amount }
    }

    // PDF Export Month & Year state
    val calendar = remember { Calendar.getInstance() }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    val months = remember(isId) {
        if (isId) listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
        else listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    }
    val years = remember { listOf(2024, 2025, 2026, 2027) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    PdfExporter.generateMonthlyPdfReport(
                        context = context,
                        outputStream = outputStream,
                        month = selectedMonth,
                        year = selectedYear,
                        transactions = transactions,
                        wallets = wallets,
                        categories = categories,
                        viewModel = viewModel
                    )
                    Toast.makeText(context, if (isId) "Laporan PDF berhasil diunduh!" else "PDF report successfully exported!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error export PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

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
            // Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isId) "Analisis Finansial" else "Financial Analytics",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = if (isId) "Pantau arus kas, tabungan, dan pengeluaran" else "Track cash flow, savings, and expense breakdown",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // 1. Overall Cash Flow Ratio Card
            item {
                CashFlowRatioCard(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    formatRupiah = { viewModel.formatRupiah(it) },
                    isId = isId
                )
            }

            // 2. Savings Pockets Total Widget
            item {
                SavingsPocketsWidget(
                    totalSavings = totalSavings,
                    wallets = savingsWallets,
                    formatRupiah = { viewModel.formatRupiah(it) },
                    isId = isId
                )
            }

            // 3. Spending Breakdown (Proportional Segmented Bar)
            item {
                SpendingBreakdownCard(
                    breakdown = categoryBreakdown,
                    totalExpense = totalExpense,
                    formatRupiah = { viewModel.formatRupiah(it) },
                    isId = isId
                )
            }

            // 4. Export PDF Card
            item {
                ExportPdfCard(
                    months = months,
                    years = years,
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear,
                    onMonthSelected = { selectedMonth = it },
                    onYearSelected = { selectedYear = it },
                    onExportClick = {
                        val fileName = "Laporan_Keuangan_${months[selectedMonth]}_$selectedYear.pdf"
                        try {
                            pdfLauncher.launch(fileName)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Tidak dapat membuka pemilih berkas", Toast.LENGTH_SHORT).show()
                        }
                    },
                    isId = isId
                )
            }
        }
    }
}

data class CategoryExpenseItem(
    val categoryName: String,
    val amount: Double,
    val percentage: Double
)

/**
 * 1. Overall Cash Flow Ratio Card:
 * Visualisasi progress bar rasio pemasukan vs pengeluaran.
 * Angka Total Income vs Total Expense.
 * Banner Peringatan Keuangan (Warning Box warna merah/rose transparan) jika pengeluaran mendekati pemasukan.
 */
@Composable
fun CashFlowRatioCard(
    totalIncome: Double,
    totalExpense: Double,
    formatRupiah: (Double) -> String,
    isId: Boolean,
    modifier: Modifier = Modifier
) {
    val totalCashFlow = totalIncome + totalExpense
    val incomeRatio = if (totalCashFlow > 0.0) (totalIncome / totalCashFlow).toFloat() else 0.5f
    val expenseRatio = if (totalCashFlow > 0.0) (totalExpense / totalCashFlow).toFloat() else 0.5f

    val isExpenseNearIncome = totalIncome > 0.0 && totalExpense >= (totalIncome * 0.8)
    val isDeficit = totalExpense > totalIncome && totalIncome > 0.0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isId) "Rasio Arus Kas Keseluruhan" else "Overall Cash Flow Ratio",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            // Numbers: Income vs Expense
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentGreen))
                        Text(if (isId) "Total Pemasukan" else "Total Income", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Text(
                        text = formatRupiah(totalIncome),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp),
                        color = AccentGreen
                    )
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentRed))
                        Text(if (isId) "Total Pengeluaran" else "Total Expense", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Text(
                        text = formatRupiah(totalExpense),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp),
                        color = AccentRed
                    )
                }
            }

            // Ratio Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF1E293B))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(incomeRatio.coerceAtLeast(0.01f))
                            .background(AccentGreen)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(expenseRatio.coerceAtLeast(0.01f))
                            .background(AccentRed)
                    )
                }
            }

            // Warning Box jika pengeluaran mendekati pemasukan
            if (isExpenseNearIncome) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AccentRed.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = AccentRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isDeficit) {
                                    if (isId) "Peringatan: Pengeluaran Melebihi Pemasukan!" else "Warning: Expenses Exceed Income!"
                                } else {
                                    if (isId) "Peringatan: Pengeluaran Mendekati Pemasukan" else "Warning: Expenses Near Income Level"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = AccentRed
                            )
                            Text(
                                text = if (isId) {
                                    "Kendalikan pengeluaran harian Anda agar tabungan tetap terjaga stabil."
                                } else {
                                    "Moderate your daily expenses to keep your savings cushion stable."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFCA5A5)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. Savings Pockets Total Widget:
 * Ringkasan total tabungan dan rincian kantong tabungan.
 */
@Composable
fun SavingsPocketsWidget(
    totalSavings: Double,
    wallets: List<Wallet>,
    formatRupiah: (Double) -> String,
    isId: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isId) "Kantong Tabungan Saya" else "Savings Pockets Total",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Text(
                    text = formatRupiah(totalSavings),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentTeal
                    )
                )
            }

            // Wallet pockets breakdown
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                wallets.take(4).forEach { wallet ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBg)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AccentTeal.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_wallet_type_savings),
                                    contentDescription = null,
                                    tint = AccentTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = wallet.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = formatRupiah(wallet.balance),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. Spending Breakdown (Proportional Segmented Bar):
 * Segmented progress bar multi-warna mewakili persentase pengeluaran per kategori.
 * List rincian kategori.
 */
@Composable
fun SpendingBreakdownCard(
    breakdown: List<CategoryExpenseItem>,
    totalExpense: Double,
    formatRupiah: (Double) -> String,
    isId: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isId) "Rincian Pengeluaran per Kategori" else "Spending Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            if (breakdown.isEmpty() || totalExpense <= 0.0) {
                Text(
                    text = if (isId) "Belum ada pengeluaran tercatat." else "No expense recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                // Multi-Color Segmented Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        breakdown.forEachIndexed { index, item ->
                            val color = SegmentColors[index % SegmentColors.size]
                            val weight = item.percentage.toFloat().coerceAtLeast(0.5f)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(weight)
                                    .background(color)
                            )
                            if (index < breakdown.size - 1) {
                                Spacer(modifier = Modifier.width(1.5.dp))
                            }
                        }
                    }
                }

                // Category List Items
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    breakdown.forEachIndexed { index, item ->
                        val color = SegmentColors[index % SegmentColors.size]
                        val formattedPercent = String.format("%.0f%%", item.percentage)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Text(
                                    text = item.categoryName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formattedPercent,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = color
                                )
                                Text(
                                    text = formatRupiah(item.amount),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. Export PDF Card:
 * Dropdown pemilih bulan dan tahun.
 * Tombol download laporan keuangan format PDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportPdfCard(
    months: List<String>,
    years: List<Int>,
    selectedMonth: Int,
    selectedYear: Int,
    onMonthSelected: (Int) -> Unit,
    onYearSelected: (Int) -> Unit,
    onExportClick: () -> Unit,
    isId: Boolean,
    modifier: Modifier = Modifier
) {
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var yearMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = if (isId) "Unduh Laporan PDF" else "Export PDF Report",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = if (isId) "Pilih periode bulan dan tahun pembukuan" else "Choose ledger period month & year",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Month and Year dropdowns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Month selector
                Box(modifier = Modifier.weight(1.5f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBg)
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            .clickable { monthMenuExpanded = true }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = months[selectedMonth],
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                    }
                    DropdownMenu(
                        expanded = monthMenuExpanded,
                        onDismissRequest = { monthMenuExpanded = false }
                    ) {
                        months.forEachIndexed { index, mName ->
                            DropdownMenuItem(
                                text = { Text(mName) },
                                onClick = {
                                    onMonthSelected(index)
                                    monthMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Year selector
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBg)
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            .clickable { yearMenuExpanded = true }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedYear.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                    }
                    DropdownMenu(
                        expanded = yearMenuExpanded,
                        onDismissRequest = { yearMenuExpanded = false }
                    ) {
                        years.forEach { y ->
                            DropdownMenuItem(
                                text = { Text(y.toString()) },
                                onClick = {
                                    onYearSelected(y)
                                    yearMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Download Button
            Button(
                onClick = onExportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isId) "Unduh Laporan Keuangan" else "Download Financial Report",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
