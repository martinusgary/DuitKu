package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    onNavigateToWallets: () -> Unit
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

    var showAddDialog by remember { mutableStateOf(false) }

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
            // 1. Total Balance Card (Clean, modern solid background matching Material You theme)
            item {
                val cardShape = RoundedCornerShape(28.dp)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .clickable { onNavigateToWallets() }
                        .testTag("total_balance_card"),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isId) "TOTAL SALDO" else "TOTAL BALANCE",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                                fontWeight = FontWeight.Bold
                            )
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
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Wallet",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(15.dp))
                        
                        Text(
                            text = if (isHidden) "Rp ••••••" else viewModel.formatRupiah(totalBalance),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isId) "${wallets.size} Akun Terhubung" else "${wallets.size} Connected accounts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isId) "Lihat Rincian →" else "View Details →",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Income and Expense Summary Cards (Row of 2)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Income Card
                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (isHidden) "Rp ••••••" else viewModel.formatRupiah(monthlyIncome),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                if (isId) "Bulan ini" else "This month",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Expense Card
                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (isHidden) "Rp ••••••" else viewModel.formatRupiah(monthlyExpense),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                            Text(
                                if (isId) "Bulan ini" else "This month",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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

        // Floating Action Button
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
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isId) "Transaksi Baru" else "New Transaction", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            viewModel = viewModel,
            wallets = wallets,
            categories = categories,
            onDismiss = { showAddDialog = false }
        )
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
                // Typology specific colored circle icons
                val circleBg = when (transaction.type) {
                    "INCOME" -> Color(0xFFE8F5E9)
                    "EXPENSE" -> Color(0xFFFFEBEE)
                    else -> Color(0xFFE3F2FD) // TRANSFER
                }
                val iconColor = when (transaction.type) {
                    "INCOME" -> Color(0xFF2E7D32)
                    "EXPENSE" -> Color(0xFFC62828)
                    else -> Color(0xFF1565C0)
                }
                val iconToUse = when (transaction.type) {
                    "INCOME" -> Icons.Default.Add
                    "EXPENSE" -> Icons.Default.Remove
                    else -> Icons.Default.CompareArrows
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(circleBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        iconToUse,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

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
                    
                    Text(
                        text = "${viewModel.formatDate(transaction.date)} • ${wallet?.name ?: (if (isId) "Dompet" else "Wallet")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
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
    onDismiss: () -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    var amountStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // "INCOME", "EXPENSE", "TRANSFER"
    var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: 0) }
    var selectedTargetWalletId by remember { mutableStateOf(wallets.getOrNull(1)?.id ?: wallets.firstOrNull()?.id ?: 0) }
    var selectedCategoryId by remember { mutableStateOf(0) }
    var note by remember { mutableStateOf("") }
    var scannedReceipts by remember { mutableStateOf<List<GeminiClient.ScanResult>>(emptyList()) }

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

                // Scan Receipt Feature with Gemini AI (Only shown for EXPENSE)
                var isScanning by remember { mutableStateOf(false) }
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()

                var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

                val cameraLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture()
                ) { success ->
                    if (success) {
                        val uri = tempPhotoUri
                        if (uri != null) {
                            isScanning = true
                            coroutineScope.launch {
                                try {
                                    val results = GeminiClient.scanMultipleReceipts(context, listOf(uri))
                                    if (results.isNotEmpty()) {
                                        scannedReceipts = results
                                        Toast.makeText(context, if (isId) "Pendeteksian struk selesai!" else "Receipt detection completed!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, if (isId) "Gagal mendeteksi rincian dari struk." else "No details detected from the receipt.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, (if (isId) "Gagal memindai struk: " else "Failed to scan receipt: ") + e.message, Toast.LENGTH_LONG).show()
                                } finally {
                                    isScanning = false
                                }
                            }
                        }
                    }
                }

                val photoPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickMultipleVisualMedia()
                ) { uris ->
                    if (uris.isNotEmpty()) {
                        isScanning = true
                        coroutineScope.launch {
                            try {
                                val results = GeminiClient.scanMultipleReceipts(context, uris)
                                if (results.isNotEmpty()) {
                                    scannedReceipts = results
                                    Toast.makeText(context, if (isId) "Pendeteksian multi-nota selesai!" else "Multi-receipt detection completed!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, if (isId) "Gagal mendeteksi rincian dari struk." else "No details detected from the receipts.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, (if (isId) "Gagal memindai struk: " else "Failed to scan receipts: ") + e.message, Toast.LENGTH_LONG).show()
                            } finally {
                                isScanning = false
                            }
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedType == "EXPENSE",
                    enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.tween(250)) + androidx.compose.animation.fadeOut()
                ) {
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        if (isScanning) {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    Text(
                                        if (isId) "Gemini AI sedang membaca struk..." else "Gemini AI is reading the receipt...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Camera Button
                                OutlinedButton(
                                    onClick = {
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
                                            cameraLauncher.launch(uri)
                                        } else {
                                            Toast.makeText(context, if (isId) "Gagal membuat file foto" else "Failed to initialize camera file", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isId) "Kamera" else "Camera",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                // Gallery Button
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isId) "Galeri" else "Gallery",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                if (scannedReceipts.isNotEmpty()) {
                                    IconButton(
                                        onClick = { scannedReceipts = emptyList() },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear scanned receipts",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
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
                                            Icons.Default.Delete,
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

                                viewModel.addTransaction(
                                    amount = amountVal,
                                    type = selectedType,
                                    walletId = selectedWalletId,
                                    categoryId = selectedCategoryId,
                                    note = note,
                                    date = System.currentTimeMillis(),
                                    targetWalletId = if (selectedType == "TRANSFER") selectedTargetWalletId else null
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
