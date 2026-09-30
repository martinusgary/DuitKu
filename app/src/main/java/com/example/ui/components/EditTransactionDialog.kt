package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onUpdated: ((Transaction) -> Unit)? = null
) {
    val context = LocalContext.current
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    val wallets by viewModel.wallets.collectAsState(emptyList())
    val categories by viewModel.categories.collectAsState(emptyList())

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val dialogWidth = if (screenWidth < 600) (screenWidth * 0.94).dp else 480.dp

    // Form states initialized with existing transaction values
    var selectedType by remember { mutableStateOf(transaction.type) } // "EXPENSE", "INCOME", "TRANSFER"
    var amountStr by remember {
        mutableStateOf(
            if (transaction.amount % 1.0 == 0.0) transaction.amount.toLong().toString()
            else transaction.amount.toString()
        )
    }
    var selectedWalletId by remember { mutableStateOf(transaction.walletId) }
    var selectedTargetWalletId by remember { mutableStateOf(transaction.targetWalletId ?: 0) }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var note by remember { mutableStateOf(transaction.note) }
    var selectedDateMillis by remember { mutableStateOf(transaction.date) }
    var isDailyBudget by remember { mutableStateOf(transaction.isDailyBudget) }
    var enableAdminFee by remember { mutableStateOf(transaction.adminFee > 0.0) }
    var adminFeeStr by remember {
        mutableStateOf(
            if (transaction.adminFee > 0.0) {
                if (transaction.adminFee % 1.0 == 0.0) transaction.adminFee.toLong().toString()
                else transaction.adminFee.toString()
            } else ""
        )
    }

    // Filter categories based on selectedType
    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

    // Reset category if not in filtered list when type switches
    LaunchedEffect(selectedType) {
        if (selectedType == "TRANSFER") {
            selectedCategoryId = 0
            if (selectedTargetWalletId == 0) {
                selectedTargetWalletId = wallets.firstOrNull { it.id != selectedWalletId }?.id ?: 0
            }
        } else {
            val exists = filteredCategories.any { it.id == selectedCategoryId }
            if (!exists) {
                selectedCategoryId = filteredCategories.firstOrNull()?.id ?: 0
            }
        }
    }

    val formattedDateText = remember(selectedDateMillis, isId) {
        try {
            val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", if (isId) Locale("id", "ID") else Locale.getDefault())
            sdf.format(Date(selectedDateMillis))
        } catch (_: Exception) {
            viewModel.formatDate(selectedDateMillis)
        }
    }

    val openDatePicker = {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)
                TimePickerDialog(
                    context,
                    { _, hour, min ->
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, min)
                        selectedDateMillis = cal.timeInMillis
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .width(dialogWidth)
                .heightIn(max = (screenHeight * 0.90).dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { openDatePicker() }
                        ) {
                            Text(
                                text = if (isId) "Edit Transaksi" else "Edit Transaction",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = formattedDateText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { openDatePicker() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = if (isId) "Ubah Tanggal" else "Change Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // 1. Transaction Type Selector Tabs
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Triple("EXPENSE", if (isId) "Pengeluaran" else "Expense", MaterialTheme.colorScheme.error),
                            Triple("INCOME", if (isId) "Pemasukan" else "Income", Color(0xFF2E7D32)),
                            Triple("TRANSFER", if (isId) "Transfer" else "Transfer", MaterialTheme.colorScheme.primary)
                        ).forEach { (typeKey, label, activeColor) ->
                            val isSelected = selectedType == typeKey
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedType = typeKey },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isSelected) 2.dp else 0.dp
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Amount Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isId) "Nominal Transaksi" else "Transaction Amount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { input ->
                            val sanitized = input.filter { it.isDigit() }
                            amountStr = sanitized
                        },
                        modifier = Modifier.fillMaxWidth(),
                        prefix = {
                            Text(
                                text = "Rp ",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick amount adjust chips (+10k, +50k, +100k, +500k)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10_000L, 50_000L, 100_000L, 500_000L).forEach { addVal ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val currentVal = amountStr.toLongOrNull() ?: 0L
                                        amountStr = (currentVal + addVal).toString()
                                    }
                            ) {
                                Text(
                                    text = "+${addVal / 1000}k",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Include in Daily Budget Toggle (only for EXPENSE)
                    if (selectedType == "EXPENSE") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Include in Daily Budget",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = isDailyBudget,
                                onCheckedChange = { isDailyBudget = it }
                            )
                        }
                    }
                }

                // 3. Wallet Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (selectedType == "TRANSFER") {
                            if (isId) "Dompet Asal" else "Source Wallet"
                        } else {
                            if (isId) "Metode / Dompet" else "Wallet Account"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(wallets) { w ->
                            val isSelected = selectedWalletId == w.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedWalletId = w.id
                                        if (selectedType == "TRANSFER" && selectedTargetWalletId == w.id) {
                                            selectedTargetWalletId = wallets.firstOrNull { it.id != w.id }?.id ?: 0
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = w.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Target Wallet (Only for TRANSFER)
                if (selectedType == "TRANSFER") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isId) "Dompet Tujuan" else "Destination Wallet",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val destinationWallets = remember(wallets, selectedWalletId) {
                            wallets.filter { it.id != selectedWalletId }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(destinationWallets) { w ->
                                val isSelected = selectedTargetWalletId == w.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedTargetWalletId = w.id }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.SwapHoriz,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = w.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Category Selection (Only for EXPENSE and INCOME)
                if (selectedType != "TRANSFER") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isId) "Kategori" else "Category",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filteredCategories.forEach { cat ->
                                val isSelected = selectedCategoryId == cat.id
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedCategoryId = cat.id }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Category,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. Admin Fee Toggle
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { enableAdminFee = !enableAdminFee },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isId) "Ada Biaya Admin?" else "Include Admin Fee?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = enableAdminFee,
                            onCheckedChange = { enableAdminFee = it }
                        )
                    }

                    if (enableAdminFee) {
                        OutlinedTextField(
                            value = adminFeeStr,
                            onValueChange = { input ->
                                val sanitized = input.filter { it.isDigit() }
                                adminFeeStr = sanitized
                            },
                            modifier = Modifier.fillMaxWidth(),
                            prefix = {
                                Text(
                                    text = "Rp ",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // 8. Note / Description Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isId) "Catatan Transaksi" else "Transaction Note",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (isId) "Tambahkan catatan..." else "Add note...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Buttons: Batal & Simpan Perubahan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(if (isId) "Batal" else "Cancel", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val parsedAmount = amountStr.toDoubleOrNull() ?: 0.0
                            if (parsedAmount <= 0.0) {
                                Toast.makeText(
                                    context,
                                    if (isId) "Nominal harus lebih dari 0" else "Amount must be greater than 0",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            if (selectedType == "TRANSFER" && selectedWalletId == selectedTargetWalletId) {
                                Toast.makeText(
                                    context,
                                    if (isId) "Dompet asal dan tujuan tidak boleh sama" else "Source and target wallets must be different",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            val parsedAdminFee = if (enableAdminFee) (adminFeeStr.toDoubleOrNull() ?: 0.0) else 0.0

                            val updatedTransaction = transaction.copy(
                                amount = parsedAmount,
                                type = selectedType,
                                walletId = selectedWalletId,
                                categoryId = if (selectedType == "TRANSFER") 0 else selectedCategoryId,
                                note = note.trim(),
                                date = selectedDateMillis,
                                targetWalletId = if (selectedType == "TRANSFER") selectedTargetWalletId else null,
                                adminFee = parsedAdminFee,
                                isDailyBudget = if (selectedType == "EXPENSE") isDailyBudget else true
                            )

                            viewModel.updateTransaction(updatedTransaction)
                            Toast.makeText(
                                context,
                                if (isId) "Perubahan transaksi berhasil disimpan" else "Transaction updated successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            onUpdated?.invoke(updatedTransaction)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(if (isId) "Simpan" else "Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
