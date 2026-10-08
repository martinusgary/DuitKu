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
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun TransactionItemRow(
    transaction: Transaction,
    wallet: Wallet?,
    targetWallet: Wallet?,
    category: Category?,
    viewModel: FinanceViewModel,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    forceShowAmount: Boolean = false
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    val isHidden by viewModel.isAmountsHidden.collectAsState()
    val shouldHide = isHidden && !forceShowAmount

    var showDetailsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val cardShape = RoundedCornerShape(20.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable { showDetailsDialog = true },
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = CardBg
        ),
        border = BorderStroke(1.dp, CardBorder)
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
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(visualInfo.backgroundColor.copy(alpha = 0.25f)),
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
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (transaction.note.isNotEmpty()) {
                        Text(
                            text = transaction.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
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
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            val priceColor = when (transaction.type) {
                "INCOME" -> AccentGreen
                "EXPENSE" -> AccentRed
                else -> AccentBlue
            }
            val prefix = when (transaction.type) {
                "INCOME" -> "+"
                "EXPENSE" -> "-"
                else -> "⇄"
            }
            val listDisplayAmount = if (transaction.type == "EXPENSE" || transaction.type == "TRANSFER") {
                transaction.amount + transaction.adminFee
            } else {
                transaction.amount
            }

            Text(
                text = if (shouldHide) "$prefix Rp ••••••" else "$prefix ${viewModel.formatRupiah(listDisplayAmount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                color = priceColor
            )
        }
    }

    if (showDetailsDialog) {
        TransactionDetailDialog(
            transaction = transaction,
            wallet = wallet,
            targetWallet = targetWallet,
            category = category,
            viewModel = viewModel,
            onDismiss = { showDetailsDialog = false },
            onDelete = {
                showDetailsDialog = false
                showDeleteConfirm = true
            }
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
                            "Pilih cara hapus transaksi:" 
                        else "Choose deletion method:",
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
                                    text = if (isId) "Hapus Riwayat Saja" else "Delete History Only",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isId)
                                    "Hapus catatan riwayat saja. Saldo dompet tetap."
                                else "Removes record only. Wallet balance unchanged.",
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
                                    "Batalkan transaksi dan kembalikan dana ke saldo dompet."
                                else "Cancels transaction and restores funds to wallet.",
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
