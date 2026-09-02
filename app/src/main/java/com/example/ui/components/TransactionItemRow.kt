package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.ui.viewmodel.FinanceViewModel

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
