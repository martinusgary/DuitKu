package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    wallet: Wallet?,
    targetWallet: Wallet? = null,
    category: Category? = null,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    title: String? = null
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    var currentTx by remember(transaction) { mutableStateOf(transaction) }
    var showEditDialog by remember { mutableStateOf(false) }

    val allWallets by viewModel.wallets.collectAsState(emptyList())
    val allCategories by viewModel.categories.collectAsState(emptyList())

    val activeWallet = remember(allWallets, currentTx.walletId, wallet) {
        allWallets.firstOrNull { it.id == currentTx.walletId } ?: wallet
    }
    val activeTargetWallet = remember(allWallets, currentTx.targetWalletId, targetWallet) {
        currentTx.targetWalletId?.let { tid -> allWallets.firstOrNull { it.id == tid } } ?: targetWallet
    }
    val activeCategory = remember(allCategories, currentTx.categoryId, category) {
        allCategories.firstOrNull { it.id == currentTx.categoryId } ?: category
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val dialogWidth = if (screenWidth < 600) (screenWidth * 0.94).dp else 480.dp

    val formattedExactDate = remember(currentTx.date, isId) {
        try {
            val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", if (isId) Locale("id", "ID") else Locale.getDefault())
            sdf.format(Date(currentTx.date))
        } catch (_: Exception) {
            viewModel.formatDate(currentTx.date)
        }
    }

    // Preserve color accents for EXPENSE, INCOME, and TRANSFER banner
    val (bannerBgColor, accentColor, typeLabel) = when (currentTx.type) {
        "EXPENSE" -> Triple(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
            Color(0xFFC62828),
            if (isId) "Nominal Pengeluaran" else "Expense Amount"
        )
        "INCOME" -> Triple(
            Color(0xFF2E7D32).copy(alpha = 0.12f),
            Color(0xFF2E7D32),
            if (isId) "Nominal Pemasukan" else "Income Amount"
        )
        else -> Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.primary,
            if (isId) "Nominal Transfer" else "Transfer Amount"
        )
    }

    val totalDisplayAmount = if (currentTx.type == "EXPENSE" || currentTx.type == "TRANSFER") {
        currentTx.amount + currentTx.adminFee
    } else {
        currentTx.amount
    }

    val installmentText = when {
        currentTx.installmentNumber != null -> {
            if (isId) "Pembayaran / Cicilan ke-${currentTx.installmentNumber}" else "Installment / Payment #${currentTx.installmentNumber}"
        }
        currentTx.note.contains("ke-", ignoreCase = true) -> {
            val part = currentTx.note.substringAfter("ke-").substringBefore(":")
            if (isId) "Pembayaran ke-$part" else "Payment #$part"
        }
        else -> null
    }

    if (showEditDialog) {
        EditTransactionDialog(
            transaction = currentTx,
            viewModel = viewModel,
            onDismiss = { showEditDialog = false },
            onUpdated = { updated ->
                currentTx = updated
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .width(dialogWidth)
                .heightIn(max = (screenHeight * 0.88).dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title ?: (if (isId) "Rincian Transaksi" else "Transaction Details"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Amount Highlight Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = bannerBgColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = viewModel.formatRupiah(totalDisplayAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                    }
                }

                // Detail Rows - Icons now styled to match theme
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Transaction Date
                    DetailRowItem(
                        icon = Icons.Default.CalendarToday,
                        label = if (isId) "Tanggal Transaksi" else "Transaction Date",
                        value = formattedExactDate
                    )

                    // 2. Category (Only for non-transfer and if category exists)
                    if (currentTx.type != "TRANSFER" && activeCategory != null) {
                        DetailRowItem(
                            icon = Icons.Default.Category,
                            label = if (isId) "Kategori" else "Category",
                            value = activeCategory.name
                        )
                    }

                    // 3. Wallet / Method
                    if (currentTx.type == "TRANSFER") {
                        val sourceName = activeWallet?.name ?: (if (isId) "Dompet Asal" else "Source Wallet")
                        val targetName = activeTargetWallet?.name ?: (if (isId) "Dompet Tujuan" else "Destination Wallet")
                        DetailRowItem(
                            icon = Icons.Default.SwapHoriz,
                            label = if (isId) "Transfer Antar Dompet" else "Wallet Transfer",
                            value = "$sourceName → $targetName"
                        )
                    } else {
                        DetailRowItem(
                            icon = Icons.Default.AccountBalanceWallet,
                            label = if (isId) "Metode Dompet" else "Wallet Account",
                            value = activeWallet?.name ?: (if (isId) "Dompet Utama" else "Primary Wallet")
                        )
                    }

                    // 4. Admin Fee (if any)
                    if (currentTx.adminFee > 0.0) {
                        DetailRowItem(
                            icon = Icons.Default.Receipt,
                            label = if (isId) "Biaya Admin" else "Admin Fee",
                            value = "${viewModel.formatRupiah(currentTx.adminFee)} (Pokok: ${viewModel.formatRupiah(currentTx.amount)})"
                        )
                    }

                    // 4b. Daily Budget Status
                    if (currentTx.type == "EXPENSE") {
                        DetailRowItem(
                            icon = Icons.Default.CalendarToday,
                            label = if (isId) "Anggaran Harian" else "Daily Budget",
                            value = if (currentTx.isDailyBudget) {
                                if (isId) "Termasuk (Variabel)" else "Included (Variable)"
                            } else {
                                if (isId) "Dikecualikan (Manual)" else "Excluded (Manual)"
                            }
                        )
                    }

                    // 5. Installment Info (if linked)
                    if (installmentText != null) {
                        DetailRowItem(
                            icon = Icons.Default.Repeat,
                            label = if (isId) "Urutan Cicilan" else "Installment Info",
                            value = installmentText
                        )
                    }

                    // 6. Notes / Description
                    if (currentTx.note.isNotBlank()) {
                        DetailRowItem(
                            icon = Icons.Default.Notes,
                            label = if (isId) "Catatan Transaksi" else "Transaction Note",
                            value = currentTx.note
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Buttons: Delete (optional), Edit, and Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isId) "Hapus" else "Delete", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isId) "Edit" else "Edit", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(if (isId) "Tutup" else "Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRowItem(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconBgColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = iconTint
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
