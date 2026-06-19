package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import androidx.compose.material.icons.filled.ArrowBack
import com.example.ui.util.PdfExporter
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

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
                        Text(
                            text = if (isId) "Rasio Arus Kas Keseluruhan" else "Overall Cash Flow Ratio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
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

            // 2. Laporan PDF & Mutasi Card
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
                                Icons.Default.PictureAsPdf,
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
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isId) "Unduh Laporan Mutasi PDF" else "Download Statement PDF Report", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Spending Breakdown Title
            item {
                Text(
                    text = if (isId) "Distribusi Pengeluaran Kategori" else "Category Expense Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (expenseByCategory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PieChart,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (isId) "Belum ada transaksi pengeluaran yang tercatat." else "No expense transactions recorded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(expenseByCategory, key = { it.category.id }) { spend ->
                    val pct = if (totalExpenseAmount > 0) spend.totalSpend / totalExpenseAmount else 0.0
                    CategorySpendProgressRow(
                        spend = spend,
                        percentage = pct,
                        viewModel = viewModel,
                        modifier = Modifier.animateItem()
                    )
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
    }
}

data class CategorySpend(
    val category: Category,
    val totalSpend: Double
)

@Composable
fun CategorySpendProgressRow(
    spend: CategorySpend,
    percentage: Double,
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val numPercent = (percentage * 100).toInt()
    
    // Choose beautiful color shades according to category spend amount
    val progressColor = when {
        numPercent > 50 -> Color(0xFFC62828)   // Red
        numPercent > 20 -> Color(0xFFEF6C00)   // Orange
        numPercent > 10 -> Color(0xFF1976D2)   // Blue
        else -> Color(0xFF43A047)               // Green
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = spend.category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "$numPercent%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { percentage.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Total: ${viewModel.formatRupiah(spend.totalSpend)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
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
