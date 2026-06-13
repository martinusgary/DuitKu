package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import com.example.ui.util.PdfExporter
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

@Composable
fun AnalyticsScreen(viewModel: FinanceViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    // Calculate analytics metrics
    val expenses = transactions.filter { it.type == "EXPENSE" }
    val incomes = transactions.filter { it.type == "INCOME" }

    val totalExpenseAmount = expenses.sumOf { it.amount }
    val totalIncomeAmount = incomes.sumOf { it.amount }

    // Group expenses by category
    val expenseByCategory = remember(transactions, categories) {
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
                    text = "Cash Flow & Analytics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 1. Cashflow Overview Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Overall Cash Flow Ratio",
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
                                    "Income (Total)",
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
                                    "Expense (Total)",
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
                                        totalIncomeAmount == 0.0 && totalExpenseAmount > 0.0 -> "Remember to record your sources of income!"
                                        ratio > 0.8 -> "Your expenses are almost exceeding your income. Reduce your non-essential spending."
                                        ratio > 0.5 -> "Your expenses are in a safe zone, but keep them balanced."
                                        else -> "Your finances are healthy! Continue saving and investing wisely."
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

                val monthNames = listOf(
                    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
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

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
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
                                text = "Laporan Bulanan & Mutasi (PDF)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ekspor mutasi keuangan lengkap serta ringkasan bulanan Anda langsung ke dokumen PDF standar A4.",
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
                                    onDismissRequest = { monthMenuExpanded = false }
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
                                    onDismissRequest = { yearMenuExpanded = false }
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
                        val wallets by viewModel.wallets.collectAsState()

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
                                    Toast.makeText(context, "Laporan PDF berhasil diunduh!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal mengunduh PDF: ${e.message}", Toast.LENGTH_LONG).show()
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
                            Text("Unduh Laporan Mutasi PDF", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Spending Breakdown Title
            item {
                Text(
                    text = "Category Expense Distribution",
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
                                "No expense transactions recorded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(expenseByCategory) { spend ->
                    val pct = if (totalExpenseAmount > 0) spend.totalSpend / totalExpenseAmount else 0.0
                    CategorySpendProgressRow(
                        spend = spend,
                        percentage = pct,
                        viewModel = viewModel
                    )
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
fun CategorySpendProgressRow(
    spend: CategorySpend,
    percentage: Double,
    viewModel: FinanceViewModel
) {
    val numPercent = (percentage * 100).toInt()
    
    // Choose beautiful color shades according to category spend amount
    val progressColor = when {
        numPercent > 50 -> Color(0xFFC62828)   // Red
        numPercent > 25 -> Color(0xFFEF6C00)   // Orange
        numPercent > 10 -> Color(0xFF1976D2)   // Blue
        else -> Color(0xFF43A047)               // Green
    }

    ElevatedCard(
        modifier = Modifier
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
