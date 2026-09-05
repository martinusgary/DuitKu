package com.example.ui.screens

import androidx.compose.ui.res.painterResource
import com.example.R
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.ui.components.TransactionItemRow
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.util.PdfExporter
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Vibrant palette for pie & chart segments
private val ChartColorPalette = listOf(
    Color(0xFFE53935), // Crimson Red
    Color(0xFFFB8C00), // Orange
    Color(0xFF1E88E5), // Blue
    Color(0xFF43A047), // Green
    Color(0xFF8E24AA), // Purple
    Color(0xFF00ACC1), // Cyan
    Color(0xFFFDD835), // Yellow
    Color(0xFFD81B60), // Pink
    Color(0xFF5E35B1), // Deep Purple
    Color(0xFF3949AB), // Indigo
    Color(0xFF00897B), // Teal
    Color(0xFF7CB342), // Light Green
    Color(0xFF6D4C41), // Brown
    Color(0xFF546E7A)  // Blue Grey
)

@Composable
fun AnalyticsScreen(viewModel: FinanceViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"
    val uiStyle by viewModel.uiStyle.collectAsState()
    val isFresh = uiStyle == "FRESH"

    var selectedTransactionForDetail by remember { mutableStateOf<Transaction?>(null) }
    var selectedCategoryForTransactions by remember { mutableStateOf<Category?>(null) }

    // Calculate analytics metrics
    val expenses = remember(transactions) { transactions.filter { it.type == "EXPENSE" } }
    val incomes = remember(transactions) { transactions.filter { it.type == "INCOME" } }

    val totalExpenseAmount = remember(expenses) { expenses.sumOf { it.amount } }
    val totalIncomeAmount = remember(incomes) { incomes.sumOf { it.amount } }

    // Group expenses by category
    val expenseByCategory = remember(expenses, categories) {
        expenses.groupBy { it.categoryId }
            .mapNotNull { (catId, txList) ->
                val cat = categories.firstOrNull { it.id == catId } ?: return@mapNotNull null
                val sum = txList.sumOf { it.amount }
                CategorySpend(category = cat, totalSpend = sum)
            }
            .sortedByDescending { it.totalSpend }
    }

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
            item {
                Text(
                    text = if (isId) "Arus Kas & Analisis" else "Cash Flow & Analytics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 1. Cashflow Overview Card
            item {
                val cashflowShape = RoundedCornerShape(24.dp)
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isFresh) Modifier.border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                cashflowShape
                            ) else Modifier
                        ),
                    shape = cashflowShape,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isId) "Rasio Arus Kas Keseluruhan" else "Overall Cash Flow Ratio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ShowChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isId) "Ikhtisar" else "Summary",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress representation of Expense over Income
                        val ratio = if (totalIncomeAmount > 0) {
                            (totalExpenseAmount / totalIncomeAmount).coerceIn(0.0, 1.0)
                        } else if (totalExpenseAmount > 0) {
                            1.0
                        } else {
                            0.0
                        }

                        LinearProgressIndicator(
                            progress = { ratio.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = MaterialTheme.colorScheme.error,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    if (isId) "Pemasukan (Total)" else "Income (Total)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    viewModel.formatRupiah(totalIncomeAmount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (isId) "Pengeluaran (Total)" else "Expense (Total)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    viewModel.formatRupiah(totalExpenseAmount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom warning or advice based on financial health! Dynamic dynamic logic.
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (ratio > 0.8) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (ratio > 0.8) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        totalIncomeAmount == 0.0 && totalExpenseAmount > 0.0 -> if (isId) "Jangan lupa untuk mencatat sumber pemasukan Anda!" else "Remember to record your sources of income!"
                                        ratio > 0.8 -> if (isId) "Pengeluaran Anda hampir melebihi pemasukan. Kurangi pengeluaran non-esensial Anda." else "Your expenses are almost exceeding your income. Reduce your non-essential spending."
                                        ratio > 0.5 -> if (isId) "Pengeluaran Anda berada di zona aman, tetap jaga keseimbangannya." else "Your expenses are in a safe zone, but keep them balanced."
                                        else -> if (isId) "Keuangan Anda sehat! Teruslah menabung dan berinvestasi dengan bijak." else "Your finances are healthy! Continue saving and investing wisely."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (ratio > 0.8) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 2. Savings Overview Card (Purely savings pocket total in analytics)
            item {
                SavingsOverviewCard(
                    wallets = wallets,
                    viewModel = viewModel,
                    isFresh = isFresh,
                    isId = isId
                )
            }

            // 3. Category Spending Storage Bar (Substituted spending donut with phone storage style bar)
            if (expenseByCategory.isNotEmpty()) {
                item {
                    CategorySpendingStorageBarCard(
                        expenseByCategory = expenseByCategory,
                        totalExpense = totalExpenseAmount,
                        viewModel = viewModel,
                        isFresh = isFresh,
                        isId = isId,
                        onCategoryClick = { category ->
                            selectedCategoryForTransactions = category
                        }
                    )
                }
            }

            // 3. Monthly Trend Chart (6 Months Cashflow Bar Comparison)
            item {
                MonthlyCashFlowBarChartCard(
                    transactions = transactions,
                    viewModel = viewModel,
                    isFresh = isFresh,
                    isId = isId
                )
            }

            // 4. Laporan PDF & Mutasi Card
            item {
                var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
                var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
                var monthMenuExpanded by remember { mutableStateOf(false) }
                var yearMenuExpanded by remember { mutableStateOf(false) }

                val monthNames = if (isId) listOf(
                    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                ) else listOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
                )

                val yearsList = remember(transactions) {
                    if (transactions.isEmpty()) {
                        listOf(Calendar.getInstance().get(Calendar.YEAR))
                    } else {
                        val years = transactions.map {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = it.date
                            cal.get(Calendar.YEAR)
                        }.distinct().sorted()
                        if (years.isEmpty()) listOf(Calendar.getInstance().get(Calendar.YEAR)) else years
                    }
                }

                // Adjust selectedYear if it's no longer inside yearsList (e.g. on clean/database restore)
                LaunchedEffect(yearsList) {
                    if (!yearsList.contains(selectedYear)) {
                        selectedYear = yearsList.lastOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                    }
                }

                val trendsShape = RoundedCornerShape(24.dp)
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isFresh) Modifier.border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                trendsShape
                            ) else Modifier
                        ),
                    shape = trendsShape,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_pdf_custom),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (isId) "Laporan Bulanan & Mutasi (PDF)" else "Monthly Report & Statement (PDF)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isId) "Ekspor mutasi keuangan lengkap serta ringkasan bulanan Anda langsung ke dokumen PDF standar A4." else "Export complete financial statements and your monthly summaries directly into standard A4 PDF document.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Month Dropdown
                            Box(modifier = Modifier.weight(1.2f)) {
                                OutlinedButton(
                                    onClick = { monthMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = monthNames[selectedMonth],
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = monthMenuExpanded,
                                    onDismissRequest = { monthMenuExpanded = false },
                                    modifier = Modifier.widthIn(min = 120.dp, max = 220.dp)
                                ) {
                                    monthNames.forEachIndexed { idx, name ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                selectedMonth = idx
                                                monthMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Year Dropdown
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { yearMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = selectedYear.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = yearMenuExpanded,
                                    onDismissRequest = { yearMenuExpanded = false },
                                    modifier = Modifier.widthIn(min = 120.dp, max = 220.dp)
                                ) {
                                    yearsList.forEach { yr ->
                                        DropdownMenuItem(
                                            text = { Text(yr.toString()) },
                                            onClick = {
                                                selectedYear = yr
                                                yearMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val context = LocalContext.current

                        val pdfLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.CreateDocument("application/pdf")
                        ) { uri ->
                            uri?.let {
                                try {
                                    context.contentResolver.openOutputStream(it)?.use { stream ->
                                        PdfExporter.generateMonthlyPdfReport(
                                            context = context,
                                            outputStream = stream,
                                            month = selectedMonth,
                                            year = selectedYear,
                                            transactions = transactions,
                                            wallets = wallets,
                                            categories = categories,
                                            viewModel = viewModel
                                        )
                                    }
                                    Toast.makeText(context, if (isId) "Laporan PDF berhasil diunduh!" else "PDF Report downloaded successfully!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, if (isId) "Gagal mengunduh PDF: ${e.message}" else "Failed to download PDF: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val defaultFilename = "Mutasi_${monthNames[selectedMonth]}_$selectedYear.pdf"
                                pdfLauncher.launch(defaultFilename)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(painterResource(id = R.drawable.ic_pdf_custom), contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isId) "Unduh Laporan Mutasi PDF" else "Download Statement PDF Report", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- Dialog 2: Transaction Details ---
        if (selectedTransactionForDetail != null) {
            val tx = selectedTransactionForDetail!!
            val category = categories.firstOrNull { it.id == tx.categoryId }
            val wallet = wallets.firstOrNull { it.id == tx.walletId }
            val targetWallet = tx.targetWalletId?.let { targetId ->
                wallets.firstOrNull { it.id == targetId }
            }

            AlertDialog(
                onDismissRequest = { selectedTransactionForDetail = null },
                title = {
                    Text(
                        text = if (isId) "Detail Transaksi" else "Transaction Details",
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
                            color = when (tx.type) {
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
                                    text = when (tx.type) {
                                        "EXPENSE" -> if (isId) "Pengeluaran" else "Expense"
                                        "INCOME" -> if (isId) "Pemasukan" else "Income"
                                        else -> "Transfer"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when (tx.type) {
                                        "EXPENSE" -> Color(0xFFC62828)
                                        "INCOME" -> Color(0xFF2E7D32)
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = viewModel.formatRupiah(tx.amount),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = when (tx.type) {
                                        "EXPENSE" -> Color(0xFFC62828)
                                        "INCOME" -> Color(0xFF2E7D32)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DetailRow(label = if (isId) "Tanggal" else "Date", value = viewModel.formatDate(tx.date))

                            if (category != null) {
                                DetailRow(label = if (isId) "Kategori" else "Category", value = category.name)
                            }

                            if (tx.type == "TRANSFER" && targetWallet != null) {
                                DetailRow(label = if (isId) "Dari Dompet" else "From Wallet", value = wallet?.name ?: "Unknown")
                                DetailRow(label = if (isId) "Ke Dompet" else "To Wallet", value = targetWallet.name)
                            } else {
                                DetailRow(label = if (isId) "Dompet" else "Wallet", value = wallet?.name ?: "Unknown")
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
                                    text = tx.note.ifBlank { if (isId) "Tidak ada catatan." else "No description added." },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedTransactionForDetail = null }) {
                        Text(if (isId) "Tutup" else "Close", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }

        // --- Category Specific Transactions Full Screen Drill-down ---
        if (selectedCategoryForTransactions != null) {
            val currentCategory = selectedCategoryForTransactions!!
            val categoryTxs = remember(transactions, currentCategory) {
                transactions.filter { it.categoryId == currentCategory.id }
                    .sortedByDescending { it.date }
            }
            val totalCategorySpend = remember(categoryTxs) {
                categoryTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            }

            BackHandler {
                selectedCategoryForTransactions = null
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar with Back Button
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { selectedCategoryForTransactions = null }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = if (isId) "Kembali ke Analytics" else "Back to Analytics",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentCategory.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isId) "Total: ${viewModel.formatRupiah(totalCategorySpend)} (${categoryTxs.size} transaksi)" else "Total: ${viewModel.formatRupiah(totalCategorySpend)} (${categoryTxs.size} transactions)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (categoryTxs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (isId) "Belum ada transaksi untuk kategori ini." else "No transactions found for this category.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categoryTxs, key = { it.id }) { tx ->
                                val wallet = wallets.firstOrNull { it.id == tx.walletId }
                                val targetWallet = wallets.firstOrNull { it.id == tx.targetWalletId }
                                TransactionItemRow(
                                    transaction = tx,
                                    wallet = wallet,
                                    targetWallet = targetWallet,
                                    category = currentCategory,
                                    viewModel = viewModel,
                                    onDelete = {}
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CategorySpend(
    val category: Category,
    val totalSpend: Double
)

@Composable
fun SavingsOverviewCard(
    wallets: List<Wallet>,
    viewModel: FinanceViewModel,
    isFresh: Boolean,
    isId: Boolean
) {
    val cardShape = RoundedCornerShape(24.dp)
    var showSavingsListDialog by remember { mutableStateOf(false) }

    val savingsWallets = remember(wallets) {
        wallets.filter { it.icon == "savings" || it.targetLimit != null }
    }
    val totalSavingsInPockets = remember(savingsWallets) { savingsWallets.sumOf { it.balance } }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSavingsListDialog = true }
            .then(
                if (isFresh) Modifier.border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    cardShape
                ) else Modifier
            ),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF43A047).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wallet_type_savings),
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isId) "Total Saku Tabungan" else "Savings Pockets Total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (savingsWallets.isNotEmpty()) {
                                if (isId) "${savingsWallets.size} Saku Terdaftar" else "${savingsWallets.size} Pockets Registered"
                            } else {
                                if (isId) "Belum ada saku" else "No pockets yet"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF43A047).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isId) "Lihat Daftar" else "View List",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Big Prominent Savings Balance Display
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF43A047).copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color(0xFF43A047).copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isId) "Total Saldo Tabungan" else "Total Savings Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = viewModel.formatRupiah(totalSavingsInPockets),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF43A047).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wallet_type_savings),
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Preview of top 3 savings pockets if available
            if (savingsWallets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    savingsWallets.take(3).forEach { wallet ->
                        val target = wallet.targetLimit
                        val progress = if (target != null && target > 0) (wallet.balance / target).coerceIn(0.0, 1.0).toFloat() else 1f

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF43A047))
                                    )
                                    Text(
                                        text = wallet.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = if (target != null && target > 0) {
                                        "${viewModel.formatRupiah(wallet.balance)} / ${viewModel.formatRupiah(target)}"
                                    } else {
                                        viewModel.formatRupiah(wallet.balance)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (target != null && target > 0) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF43A047),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }

                    if (savingsWallets.size > 3) {
                        Text(
                            text = if (isId) "+${savingsWallets.size - 3} saku lainnya (ketuk untuk rincian)" else "+${savingsWallets.size - 3} more pockets (tap for details)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isId) "Belum ada saku tabungan dibuat di menu Dompet." else "No savings pockets created yet in Wallets menu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    if (showSavingsListDialog) {
        SavingsPocketsListDialog(
            savingsWallets = savingsWallets,
            totalSavings = totalSavingsInPockets,
            viewModel = viewModel,
            isId = isId,
            onDismiss = { showSavingsListDialog = false }
        )
    }
}

@Composable
fun SavingsPocketsListDialog(
    savingsWallets: List<Wallet>,
    totalSavings: Double,
    viewModel: FinanceViewModel,
    isId: Boolean,
    onDismiss: () -> Unit
) {
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF43A047).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_wallet_type_savings),
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isId) "Daftar Saku Tabungan" else "Savings Pockets List",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${savingsWallets.size} " + if (isId) "saku terdaftar di dompet" else "pockets in wallet",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Summary Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF43A047).copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, Color(0xFF43A047).copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isId) "Total Dana Tabungan" else "Total Savings Balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = viewModel.formatRupiah(totalSavings),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                if (savingsWallets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_wallet_type_savings),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isId) "Belum ada saku tabungan." else "No savings pockets found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isId) "Tambahkan saku bertipe Tabungan di menu Dompet." else "Add a Savings pocket in the Wallets tab.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(savingsWallets, key = { it.id }) { wallet ->
                            val target = wallet.targetLimit
                            val hasTarget = target != null && target > 0
                            val progress = if (hasTarget) (wallet.balance / target!!).coerceIn(0.0, 1.0).toFloat() else 1f
                            val pct = if (hasTarget) ((wallet.balance / target!!) * 100).toInt() else 0
                            val isAchieved = hasTarget && wallet.balance >= target!!

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF43A047).copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_wallet_type_savings),
                                                    contentDescription = null,
                                                    tint = Color(0xFF2E7D32),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = wallet.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = viewModel.formatRupiah(wallet.balance),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }

                                    if (hasTarget) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            LinearProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(7.dp)
                                                    .clip(RoundedCornerShape(3.5.dp)),
                                                color = if (isAchieved) Color(0xFF2E7D32) else Color(0xFF43A047),
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isAchieved) {
                                                        if (isId) "🎉 Target Tercapai!" else "🎉 Target Achieved!"
                                                    } else {
                                                        if (isId) "Kurang ${viewModel.formatRupiah(target!! - wallet.balance)}" else "${viewModel.formatRupiah(target!! - wallet.balance)} remaining"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = if (isAchieved) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = if (isAchieved) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = "$pct% • Target ${viewModel.formatRupiah(target!!)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.align(Alignment.Start)
                                        ) {
                                            Text(
                                                text = if (isId) "Tabungan Bebas (Tanpa Target Batas)" else "Open-ended (No Target Cap)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (isId) "Tutup" else "Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CategorySpendingStorageBarCard(
    expenseByCategory: List<CategorySpend>,
    totalExpense: Double,
    viewModel: FinanceViewModel,
    isFresh: Boolean,
    isId: Boolean,
    onCategoryClick: (Category) -> Unit
) {
    val cardShape = RoundedCornerShape(24.dp)
    var showAllCategories by remember { mutableStateOf(false) }

    val displayedCategories = if (showAllCategories || expenseByCategory.size <= 6) {
        expenseByCategory
    } else {
        expenseByCategory.take(6)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isFresh) Modifier.border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    cardShape
                ) else Modifier
            ),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (isId) "Komposisi Pengeluaran" else "Spending Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = viewModel.formatRupiah(totalExpense),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Storage-style Segmented Bar (like mobile phone storage indicator)
            val barShape = RoundedCornerShape(8.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(barShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                if (totalExpense > 0) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        val topItems = expenseByCategory.take(8)
                        val otherItems = expenseByCategory.drop(8)
                        val otherTotal = otherItems.sumOf { it.totalSpend }

                        topItems.forEachIndexed { index, item ->
                            val weight = (item.totalSpend / totalExpense).toFloat().coerceAtLeast(0.005f)
                            val color = ChartColorPalette[index % ChartColorPalette.size]
                            Box(
                                modifier = Modifier
                                    .weight(weight)
                                    .fillMaxHeight()
                                    .background(color)
                                    .clickable {
                                        onCategoryClick(item.category)
                                    }
                            )
                            if (index < topItems.lastIndex || otherTotal > 0) {
                                Spacer(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.surface)
                                )
                            }
                        }
                        if (otherTotal > 0) {
                            val otherWeight = (otherTotal / totalExpense).toFloat().coerceAtLeast(0.005f)
                            Box(
                                modifier = Modifier
                                    .weight(otherWeight)
                                    .fillMaxHeight()
                                    .background(Color(0xFF78909C))
                                    .clickable {
                                        if (otherItems.isNotEmpty()) {
                                            onCategoryClick(otherItems.first().category)
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            // Sub-bar labels (Used vs Total Category Count)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isId) "Total Beban: ${viewModel.formatRupiah(totalExpense)}" else "Total Used: ${viewModel.formatRupiah(totalExpense)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${expenseByCategory.size} ${if (isId) "Kategori" else "Categories"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Category rows like phone storage items (Color dot, Name, Amount, Chevron)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                displayedCategories.forEachIndexed { index, item ->
                    val color = ChartColorPalette[index % ChartColorPalette.size]
                    val pct = if (totalExpense > 0) ((item.totalSpend / totalExpense) * 100).toInt() else 0

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCategoryClick(item.category)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = item.category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = viewModel.formatRupiah(item.totalSpend),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = color.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "$pct%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Show More / Show Less toggle if more than 6 categories
            if (expenseByCategory.size > 6) {
                TextButton(
                    onClick = { showAllCategories = !showAllCategories },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (showAllCategories) {
                            if (isId) "Tampilkan Lebih Sedikit" else "Show Less"
                        } else {
                            if (isId) "Lihat Semua (${expenseByCategory.size} Kategori)" else "View All (${expenseByCategory.size} Categories)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

data class MonthlyBarData(
    val monthName: String,
    val income: Double,
    val expense: Double
)

@Composable
fun MonthlyCashFlowBarChartCard(
    transactions: List<Transaction>,
    viewModel: FinanceViewModel,
    isFresh: Boolean,
    isId: Boolean
) {
    val cardShape = RoundedCornerShape(24.dp)
    val monthShortNames = if (isId) {
        listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des")
    } else {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }

    // Compute last 6 months metrics
    val last6MonthsData = remember(transactions) {
        val result = mutableListOf<MonthlyBarData>()
        val cal = Calendar.getInstance()
        // Start from 5 months ago to current month
        for (i in 5 downTo 0) {
            val targetCal = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.MONTH, -i)
            }
            val targetMonth = targetCal.get(Calendar.MONTH)
            val targetYear = targetCal.get(Calendar.YEAR)

            val monthTx = transactions.filter { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                txCal.get(Calendar.MONTH) == targetMonth && txCal.get(Calendar.YEAR) == targetYear
            }

            val inc = monthTx.filter { it.type == "INCOME" }.sumOf { it.amount }
            val exp = monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            result.add(
                MonthlyBarData(
                    monthName = monthShortNames[targetMonth],
                    income = inc,
                    expense = exp
                )
            )
        }
        result
    }

    val maxAmount = remember(last6MonthsData) {
        val maxVal = last6MonthsData.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0
        if (maxVal <= 0.0) 1.0 else maxVal
    }

    var selectedBarMonth by remember { mutableStateOf<MonthlyBarData?>(null) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isFresh) Modifier.border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    cardShape
                ) else Modifier
            ),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (isId) "Tren Arus Kas (6 Bulan)" else "Cash Flow Trend (6 Months)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Legend indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                    Text(if (isId) "Pemasukan" else "Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFC62828)))
                    Text(if (isId) "Pengeluaran" else "Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bar Chart Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                last6MonthsData.forEach { data ->
                    val isSelected = selectedBarMonth?.monthName == data.monthName
                    val incomeRatio = (data.income / maxAmount).coerceIn(0.0, 1.0).toFloat()
                    val expenseRatio = (data.expense / maxAmount).coerceIn(0.0, 1.0).toFloat()

                    val animatedIncomeHeight by animateFloatAsState(
                        targetValue = incomeRatio,
                        animationSpec = tween(600),
                        label = "inc_bar"
                    )
                    val animatedExpenseHeight by animateFloatAsState(
                        targetValue = expenseRatio,
                        animationSpec = tween(600),
                        label = "exp_bar"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedBarMonth = if (isSelected) null else data
                            }
                            .padding(horizontal = 2.dp)
                    ) {
                        // Bars container
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                // Income bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(fraction = if (animatedIncomeHeight > 0.05f) animatedIncomeHeight else if (data.income > 0) 0.06f else 0.01f)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            if (isSelected) Color(0xFF1B5E20) else Color(0xFF4CAF50)
                                        )
                                )

                                // Expense bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(fraction = if (animatedExpenseHeight > 0.05f) animatedExpenseHeight else if (data.expense > 0) 0.06f else 0.01f)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            if (isSelected) Color(0xFFB71C1C) else Color(0xFFEF5350)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = data.monthName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Info popup banner for clicked bar
            if (selectedBarMonth != null) {
                Spacer(modifier = Modifier.height(14.dp))
                val activeData = selectedBarMonth!!
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Bulan ${activeData.monthName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Masuk: ${viewModel.formatRupiah(activeData.income)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Keluar: ${viewModel.formatRupiah(activeData.expense)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFC62828)
                            )
                            val diff = activeData.income - activeData.expense
                            Text(
                                text = (if (diff >= 0) "Surplus: +" else "Defisit: ") + viewModel.formatRupiah(diff),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (diff >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
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

@Composable
fun CategoryTransactionsFullScreen(
    category: Category,
    totalSpend: Double,
    transactions: List<Transaction>,
    wallets: List<Wallet>,
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    onTransactionClick: (Transaction) -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Safe navigation or top-bar row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = if (isId) "Kembali" else "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Hero card displaying the total spend
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (isId) "Total Pengeluaran" else "Total Spending",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = viewModel.formatRupiah(totalSpend),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isId) "Arsip Transaksi" else "Transactions Archive",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        val expenses = remember(transactions) { transactions.filter { it.type == "EXPENSE" } }

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isId) "Tidak ada transaksi dalam kategori ini." else "No transactions found in this category.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(expenses, key = { it.id }) { tx ->
                    val walletLabel = wallets.firstOrNull { it.id == tx.walletId }?.name ?: "Unknown Wallet"
                    ElevatedCard(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .clickable { onTransactionClick(tx) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = tx.note.ifBlank { if (isId) "Tidak ada catatan" else "No description" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = viewModel.formatRupiah(tx.amount),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFC62828)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = viewModel.formatDate(tx.date),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = walletLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    modifier = Modifier.height(26.dp),
                                    colors = AssistChipDefaults.assistChipColors(
                                        labelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
