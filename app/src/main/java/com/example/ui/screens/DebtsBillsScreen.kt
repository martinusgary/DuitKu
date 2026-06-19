package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.model.Bill
import com.example.data.model.Debt
import com.example.data.model.Wallet
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun DebtsBillsScreen(viewModel: FinanceViewModel) {
    val debts by viewModel.debts.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"
    val uiStyle by viewModel.uiStyle.collectAsState()
    val isFresh = uiStyle == "FRESH"

    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showAddBillDialog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp
        val showIcons = screenWidthDp >= 380 || !isId
        val tabTextStyle = if (screenWidthDp < 360 || (isId && screenWidthDp < 400)) {
            MaterialTheme.typography.bodySmall
        } else {
            MaterialTheme.typography.bodyMedium
        }

        // Sub-navigation tab row (Modern Segmented Control)
        val controlShape = RoundedCornerShape(16.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = controlShape
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isDebtsSelected = pagerState.currentPage == 0
            
            // Debts & Loans
            val debtsTabShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isDebtsSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = debtsTabShape
                    )
                    .clip(debtsTabShape)
                    .clickable {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (showIcons) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isDebtsSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isId) "Hutang & Piutang" else "Debts & Loans",
                        color = if (isDebtsSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = tabTextStyle,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Bills
            val billsTabShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (!isDebtsSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = billsTabShape
                    )
                    .clip(billsTabShape)
                    .clickable {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (showIcons) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (!isDebtsSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isId) "Tagihan" else "Bills",
                        color = if (!isDebtsSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = tabTextStyle,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> {
                    DebtsTabContent(
                        debts = debts,
                        wallets = wallets,
                        viewModel = viewModel,
                        onAddClick = { showAddDebtDialog = true }
                    )
                }
                1 -> {
                    BillsTabContent(
                        bills = bills,
                        wallets = wallets,
                        viewModel = viewModel,
                        onAddClick = { showAddBillDialog = true }
                    )
                }
            }
        }
    }

    if (showAddDebtDialog) {
        AddDebtDialog(
            viewModel = viewModel,
            onDismiss = { showAddDebtDialog = false }
        )
    }

    if (showAddBillDialog) {
        AddBillDialog(
            viewModel = viewModel,
            onDismiss = { showAddBillDialog = false }
        )
    }
}

// ==========================================
// 1. HUTANG CONTENT & DIALOGS
// ==========================================

@Composable
fun DebtsTabContent(
    debts: List<Debt>,
    wallets: List<Wallet>,
    viewModel: FinanceViewModel,
    onAddClick: () -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    val totalHutang = remember(debts) { debts.filter { it.type == "HUTANG" }.sumOf { it.remainingAmount } }
    val totalPiutang = remember(debts) { debts.filter { it.type == "PIUTANG" }.sumOf { it.remainingAmount } }

    var selectedDebtForPay by remember { mutableStateOf<Debt?>(null) }
    var selectedDebtForDelete by remember { mutableStateOf<Debt?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(if (isId) "Hutang Saya (Ke Orang Lain)" else "My Debts (Owed to Others)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        viewModel.formatRupiah(totalHutang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(if (isId) "Untuk Dibayar" else "To Pay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }

            ElevatedCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(if (isId) "Piutang Saya (Tagihan ke Orang)" else "My Loans (Owed to Me)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        viewModel.formatRupiah(totalPiutang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(if (isId) "Untuk Ditagih" else "To Collect", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (isId) "Daftar Hutang & Piutang Aktif" else "Active Debt & Loan List", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isId) "Tambah Catatan" else "Add Note")
            }
        }

        if (debts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (isId) "Belum ada catatan hutang/piutang." else "No debt/loan records found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(debts, key = { it.id }) { debt ->
                    DebtCardRow(
                        debt = debt,
                        viewModel = viewModel,
                        onPayClick = { selectedDebtForPay = debt },
                        onDeleteClick = { selectedDebtForDelete = debt },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (selectedDebtForPay != null) {
        PayDebtInstallmentDialog(
            debt = selectedDebtForPay!!,
            wallets = wallets,
            viewModel = viewModel,
            onDismiss = { selectedDebtForPay = null }
        )
    }

    if (selectedDebtForDelete != null) {
        AlertDialog(
            onDismissRequest = { selectedDebtForDelete = null },
            title = { Text(if (isId) "Hapus Catatan Hutang/Piutang?" else "Delete Debt Record?") },
            text = { Text(if (isId) "Apakah Anda yakin ingin menghapus catatan ini? Saldo dompet saat ini tidak akan terpengaruh secara otomatis." else "Are you sure you want to delete this record from your device? The current balance will not be affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDebt(selectedDebtForDelete!!)
                        selectedDebtForDelete = null
                    }
                ) {
                    Text(if (isId) "Hapus" else "Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDebtForDelete = null }) {
                    Text(if (isId) "Batal" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun DebtCardRow(
    debt: Debt,
    viewModel: FinanceViewModel,
    onPayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    val isHutang = debt.type == "HUTANG"
    val colorAccent = if (isHutang) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
    val percentageFinished = if (debt.totalAmount > 0) {
        ((debt.totalAmount - debt.remainingAmount) / debt.totalAmount).coerceIn(0.0, 1.0)
    } else {
        1.0
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colorAccent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isHutang) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = colorAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = debt.personName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isHutang) {
                                if (isId) "Hutang Saya" else "My Debt"
                            } else {
                                if (isId) "Piutang Saya" else "My Loan"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colorAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = if (isId) "Hapus" else "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(if (isId) "Sisa Saldo" else "Remaining Amount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        viewModel.formatRupiah(debt.remainingAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = colorAccent
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(if (isId) "Jatuh Tempo" else "Due Date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        viewModel.formatDate(debt.dueDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (debt.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isId) "Catatan: ${debt.notes}" else "Note: ${debt.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar and status
            LinearProgressIndicator(
                progress = { percentageFinished.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = colorAccent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isId) "Lunas: ${(percentageFinished * 100).toInt()}%" else "Paid: ${(percentageFinished * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (debt.remainingAmount > 0) {
                    Button(
                        onClick = onPayClick,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorAccent)
                    ) {
                        Text(if (isId) "Bayar Cicilan" else "Record Installment", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                } else {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(if (isId) "LUNAS" else "PAID", fontWeight = FontWeight.Black) },
                        border = null,
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFFE8F5E9),
                            labelColor = Color(0xFF2E7D32)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AddDebtDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    var personName by remember { mutableStateOf("") }
    var totalAmountStr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("HUTANG") } // HUTANG / PIUTANG
    var notes by remember { mutableStateOf("") }
    var daysToDue by remember { mutableStateOf("7") } // Calculate due date as CurrentTime + items * 1 day

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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isId) "Catat Hutang/Piutang" else "Record Debt/Loan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Selector (Modern Segmented Control)
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
                            val isHutang = type == "HUTANG"
                            val dialogTabShape = RoundedCornerShape(8.dp)
                            // HUTANG
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isHutang) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = dialogTabShape
                                    )
                                    .clip(dialogTabShape)
                                    .clickable { type = "HUTANG" }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isId) "Saya Berhutang" else "I Owe",
                                    color = if (isHutang) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // PIUTANG
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (!isHutang) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = dialogTabShape
                                    )
                                    .clip(dialogTabShape)
                                    .clickable { type = "PIUTANG" }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isId) "Piutang Saya" else "Owed to Me",
                                    color = if (!isHutang) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedTextField(
                            value = personName,
                            onValueChange = { personName = it },
                            label = { Text(if (isId) "Nama Orang / Lembaga" else "Person / Institution Name") },
                            placeholder = { Text(if (isId) "misal: John Doe, Bank" else "e.g., John Doe, Bank") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = totalAmountStr,
                            onValueChange = { if (it.all { char -> char.isDigit() }) totalAmountStr = it },
                            label = { Text(if (isId) "Jumlah Total" else "Total Amount") },
                            prefix = { Text("Rp ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("0") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = daysToDue,
                            onValueChange = { if (it.all { char -> char.isDigit() }) daysToDue = it },
                            label = { Text(if (isId) "Hari hingga Jatuh Tempo" else "Days until Due Date") },
                            suffix = { Text(if (isId) "Hari lagi" else "Days left") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(if (isId) "Catatan Tambahan" else "Additional Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

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
                            val amountVal = totalAmountStr.toDoubleOrNull() ?: 0.0
                            val daysVal = daysToDue.toLongOrNull() ?: 7
                            val dueDateCalculated = System.currentTimeMillis() + daysVal * 24 * 60 * 60 * 1000L
                            
                            if (personName.trim().isNotEmpty() && amountVal > 0) {
                                viewModel.addDebt(
                                    personName = personName,
                                    totalAmount = amountVal,
                                    dueDate = dueDateCalculated,
                                    type = type,
                                    notes = notes
                                )
                                onDismiss()
                            }
                        },
                        enabled = personName.trim().isNotEmpty() && totalAmountStr.isNotEmpty()
                    ) {
                        Text(if (isId) "Simpan" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun PayDebtInstallmentDialog(
    debt: Debt,
    wallets: List<Wallet>,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    var amountPaidStr by remember { mutableStateOf("") }
    var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: 0) }
    var comment by remember { mutableStateOf("") }

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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isId) "Catat Pembayaran untuk ${debt.personName}" else "Record Payment for ${debt.personName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text((if (isId) "Sisa Hutang: " else "Remaining Debt: ") + viewModel.formatRupiah(debt.remainingAmount), fontWeight = FontWeight.SemiBold)

                        OutlinedTextField(
                            value = amountPaidStr,
                            onValueChange = { if (it.all { char -> char.isDigit() }) amountPaidStr = it },
                            label = { Text(if (isId) "Jumlah Pembayaran" else "Payment Amount") },
                            prefix = { Text("Rp ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("0") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text(if (isId) "Pilih Dompet Sumber Pembayaran:" else "Select Payment Wallet/Account:", style = MaterialTheme.typography.labelMedium)
                        if (wallets.isEmpty()) {
                            Text(if (isId) "Tidak ada dompet ditemukan." else "No wallets found.", color = MaterialTheme.colorScheme.error)
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

                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            label = { Text(if (isId) "Catatan / Keterangan Cicilan" else "Notes / Installment number") },
                            placeholder = { Text(if (isId) "misal: Cicilan ke-1, pelunasan" else "e.g., Part payment 1, full repayment") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

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
                            val payVal = amountPaidStr.toDoubleOrNull() ?: 0.0
                            if (payVal > 0 && selectedWalletId != 0) {
                                viewModel.payDebtInstallment(
                                    debt = debt,
                                    amountPaid = payVal,
                                    walletId = selectedWalletId,
                                    note = comment
                                )
                                onDismiss()
                            }
                        },
                        enabled = amountPaidStr.isNotEmpty() && wallets.isNotEmpty()
                    ) {
                        Text(if (isId) "Simpan Pembayaran" else "Save Payment")
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. TAGIHAN (BILLS) TAB CONTENT & DIALOGS
// ==========================================

@Composable
fun BillsTabContent(
    bills: List<Bill>,
    wallets: List<Wallet>,
    viewModel: FinanceViewModel,
    onAddClick: () -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    var selectedBillForPay by remember { mutableStateOf<Bill?>(null) }
    var selectedBillForDelete by remember { mutableStateOf<Bill?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (isId) "Daftar Tagihan Rutin" else "Recurring Bills List", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isId) "Tambah Tagihan" else "Add Bill")
            }
        }

        if (bills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (isId) "Belum ada tagihan rutin yang terdaftar." else "No recurring bills registered yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bills, key = { bill -> bill.id }) { bill ->
                    val isLunas = bill.status == "LUNAS"
                    val borderAccent = if (isLunas) {
                        BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f))
                    } else {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    }

                    Card(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        border = borderAccent,
                        colors = CardColors(
                            containerColor = if (isLunas) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isLunas) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isLunas) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = bill.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLunas) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = viewModel.formatRupiah(bill.amount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (isLunas) Color(0xFF2E7D32).copy(alpha = 0.6f) else Color(0xFFC62828)
                                )
                                Text(
                                    text = if (isId) "Jatuh Tempo: ${bill.dueDateValue}" else "Due Date: ${bill.dueDateValue}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isLunas) {
                                        TextButton(onClick = { viewModel.resetBillStatus(bill) }) {
                                            Text("Reset", style = MaterialTheme.typography.bodySmall)
                                        }
                                    } else {
                                        Button(
                                            onClick = { selectedBillForPay = bill },
                                            modifier = Modifier.height(36.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text(if (isId) "BAYAR TAGIHAN" else "RECORD PAYMENT", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    IconButton(onClick = { selectedBillForDelete = bill }) {
                                        Icon(Icons.Default.Delete, contentDescription = if (isId) "Hapus" else "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedBillForPay != null) {
        PayBillDialog(
            bill = selectedBillForPay!!,
            wallets = wallets,
            viewModel = viewModel,
            onDismiss = { selectedBillForPay = null }
        )
    }

    if (selectedBillForDelete != null) {
        AlertDialog(
            onDismissRequest = { selectedBillForDelete = null },
            title = { Text(if (isId) "Hapus Tagihan?" else "Delete Bill?") },
            text = { Text(if (isId) "Apakah Anda yakin ingin menghapus tagihan rutin '${selectedBillForDelete!!.name}' dari sistem?" else "Are you sure you want to delete recurring bill '${selectedBillForDelete!!.name}' from the system?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBill(selectedBillForDelete!!)
                        selectedBillForDelete = null
                    }
                ) {
                    Text(if (isId) "Hapus" else "Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBillForDelete = null }) {
                    Text(if (isId) "Batal" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun AddBillDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var dueDateValue by remember { mutableStateOf("Every 10th") }

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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isId) "Tambah Tagihan Baru" else "Add New Bill",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(if (isId) "Nama Tagihan" else "Bill Name") },
                            placeholder = { Text(if (isId) "misal: WiFi, Listrik, Asuransi Kesehatan" else "e.g., WiFi, Electricity, Health Insurance") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { if (it.all { char -> char.isDigit() }) amountStr = it },
                            label = { Text(if (isId) "Jumlah Bulanan" else "Monthly Amount") },
                            prefix = { Text("Rp ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("0") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = dueDateValue,
                            onValueChange = { dueDateValue = it },
                            label = { Text(if (isId) "Keterangan Jatuh Tempo" else "Due Date Statement") },
                            placeholder = { Text(if (isId) "misal: Setiap tanggal 15 setiap bulan" else "e.g., Every 15th of the month") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

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
                            val amountVal = amountStr.toDoubleOrNull() ?: 0.0
                            if (name.trim().isNotEmpty() && amountVal > 0) {
                                viewModel.addBill(
                                    name = name,
                                    amount = amountVal,
                                    dueDateValue = dueDateValue
                                )
                                onDismiss()
                            }
                        },
                        enabled = name.trim().isNotEmpty() && amountStr.isNotEmpty()
                    ) {
                        Text(if (isId) "Simpan" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun PayBillDialog(
    bill: Bill,
    wallets: List<Wallet>,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: 0) }

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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isId) "Bayar Tagihan: ${bill.name}" else "Pay Bill: ${bill.name}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text((if (isId) "Jumlah Tagihan: " else "Bill Amount: ") + viewModel.formatRupiah(bill.amount))
                        Text(if (isId) "Pilih dompet/akun sumber pembayaran:" else "Select the source wallet/account for payment:")
                        
                        if (wallets.isEmpty()) {
                            Text(if (isId) "Tidak ada dompet ditemukan." else "No wallets found.", color = MaterialTheme.colorScheme.error)
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
                    }
                }

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
                            if (selectedWalletId != 0) {
                                viewModel.payBill(bill, selectedWalletId)
                                onDismiss()
                            }
                        },
                        enabled = wallets.isNotEmpty()
                    ) {
                        Text(if (isId) "Konfirmasi Pembayaran" else "Confirm Payment")
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
