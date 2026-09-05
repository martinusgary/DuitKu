package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebtBillPaymentDetailDialog(
    transaction: Transaction,
    wallet: Wallet?,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    title: String? = null
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val dialogWidth = if (screenWidth < 600) (screenWidth * 0.94).dp else 480.dp

    val fullDateFormatter = SimpleDateFormat("dd MMMM yyyy, HH:mm", if (isId) Locale("id", "ID") else Locale.getDefault())
    val formattedExactDate = fullDateFormatter.format(Date(transaction.date))

    val installmentText = when {
        transaction.installmentNumber != null -> {
            if (isId) "Pembayaran / Cicilan ke-${transaction.installmentNumber}" else "Installment / Payment #${transaction.installmentNumber}"
        }
        transaction.note.contains("ke-", ignoreCase = true) -> {
            val part = transaction.note.substringAfter("ke-").substringBefore(":")
            if (isId) "Pembayaran ke-$part" else "Payment #$part"
        }
        else -> {
            if (isId) "Pembayaran Tunggal / Rutin" else "Standard / Single Payment"
        }
    }

    val walletName = wallet?.name ?: (if (isId) "Dompet Utama" else "Primary Wallet")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .width(dialogWidth)
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
                        text = title ?: (if (isId) "Rincian Transaksi Pembayaran" else "Payment Transaction Details"),
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
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isId) "Nominal Dibayar" else "Amount Paid",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = viewModel.formatRupiah(transaction.amount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Detail Rows
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Payment Date
                    DetailItem(
                        icon = Icons.Default.CalendarToday,
                        label = if (isId) "Tanggal Pembayaran" else "Payment Date",
                        value = formattedExactDate
                    )

                    // 2. Payment Method (Via)
                    DetailItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = if (isId) "Metode Pembayaran (Via)" else "Payment Method (Via)",
                        value = walletName
                    )

                    // 3. Installment Info
                    DetailItem(
                        icon = Icons.Default.Repeat,
                        label = if (isId) "Urutan Cicilan / Frekuensi" else "Installment / Frequency Info",
                        value = installmentText
                    )

                    // 4. Notes / Description
                    if (transaction.note.isNotBlank()) {
                        DetailItem(
                            icon = Icons.Default.Notes,
                            label = if (isId) "Catatan Transaksi" else "Transaction Note",
                            value = transaction.note
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isId) "Tutup" else "Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
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
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
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
