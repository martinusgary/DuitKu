package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Bill
import com.example.data.model.Debt
import com.example.data.model.Wallet
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun DebtsBillsScreen(viewModel: FinanceViewModel) {
    var activeSubTab by remember { mutableStateOf(0) } // 0: Hutang, 1: Tagihan
    val debts by viewModel.debts.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val wallets by viewModel.wallets.collectAsState()

    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showAddBillDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sub-navigation tab row
        TabRow(selectedTabIndex = activeSubTab) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Debts & Loans")
                    }
                }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bills")
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (activeSubTab == 0) {
                // TAB HUTANG-PIUTANG
                DebtsTabContent(
                    debts = debts,
                    wallets = wallets,
                    viewModel = viewModel,
                    onAddClick = { showAddDebtDialog = true }
                )
            } else {
                // TAB TAGIHAN
                BillsTabContent(
                    bills = bills,
                    wallets = wallets,
                    viewModel = viewModel,
                    onAddClick = { showAddBillDialog = true }
                )
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
    val totalHutang = debts.filter { it.type == "HUTANG" }.sumOf { it.remainingAmount }
    val totalPiutang = debts.filter { it.type == "PIUTANG" }.sumOf { it.remainingAmount }

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
                    Text("My Debts (Owed to Others)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        viewModel.formatRupiah(totalHutang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text("To Pay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
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
                    Text("My Loans (Owed to Me)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        viewModel.formatRupiah(totalPiutang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text("To Collect", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Active Debt & Loan List", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Note")
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
                    Text("No debt/loan records found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        onDeleteClick = { selectedDebtForDelete = debt }
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
            title = { Text("Delete Debt Record?") },
            text = { Text("Are you sure you want to delete this record from your device? The current balance will not be affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDebt(selectedDebtForDelete!!)
                        selectedDebtForDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDebtForDelete = null }) {
                    Text("Cancel")
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
    onDeleteClick: () -> Unit
) {
    val isHutang = debt.type == "HUTANG"
    val colorAccent = if (isHutang) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
    val percentageFinished = if (debt.totalAmount > 0) {
        ((debt.totalAmount - debt.remainingAmount) / debt.totalAmount).coerceIn(0.0, 1.0)
    } else {
        1.0
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
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
                            text = if (isHutang) "My Debt" else "My Loan",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Remaining Amount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        viewModel.formatRupiah(debt.remainingAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = colorAccent
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Due Date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    text = "Note: ${debt.notes}",
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
                    "Paid: ${(percentageFinished * 100).toInt()}%",
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
                        Text("Record Installment", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                } else {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("PAID", fontWeight = FontWeight.Black) },
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
    var personName by remember { mutableStateOf("") }
    var totalAmountStr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("HUTANG") } // HUTANG / PIUTANG
    var notes by remember { mutableStateOf("") }
    var daysToDue by remember { mutableStateOf("7") } // Calculate due date as CurrentTime + items * 1 day

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Debt/Loan", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector
                TabRow(selectedTabIndex = if (type == "HUTANG") 0 else 1) {
                    Tab(
                        selected = type == "HUTANG",
                        onClick = { type = "HUTANG" },
                        text = { Text("I Owe (Expense)") }
                    )
                    Tab(
                        selected = type == "PIUTANG",
                        onClick = { type = "PIUTANG" },
                        text = { Text("Owed to Me (Income)") }
                    )
                }

                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("Person / Institution Name") },
                    placeholder = { Text("e.g., John Doe, Bank") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = totalAmountStr,
                    onValueChange = { if (it.all { char -> char.isDigit() }) totalAmountStr = it },
                    label = { Text("Total Amount") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = daysToDue,
                    onValueChange = { if (it.all { char -> char.isDigit() }) daysToDue = it },
                    label = { Text("Days until Due Date") },
                    suffix = { Text("Days left") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Additional Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PayDebtInstallmentDialog(
    debt: Debt,
    wallets: List<Wallet>,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    var amountPaidStr by remember { mutableStateOf("") }
    var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: 0) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment for ${debt.personName}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Remaining Debt: ${viewModel.formatRupiah(debt.remainingAmount)}", fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = amountPaidStr,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountPaidStr = it },
                    label = { Text("Payment Amount") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Select Payment Wallet/Account:", style = MaterialTheme.typography.labelMedium)
                if (wallets.isEmpty()) {
                    Text("No wallets found.", color = MaterialTheme.colorScheme.error)
                } else {
                    ScrollableTabRow(
                        selectedTabIndex = wallets.indexOfFirst { it.id == selectedWalletId }.coerceAtLeast(0),
                        edgePadding = 0.dp
                    ) {
                        wallets.forEach { w ->
                            Tab(
                                selected = selectedWalletId == w.id,
                                onClick = { selectedWalletId = w.id },
                                text = { Text(w.name) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Notes / Installment number") },
                    placeholder = { Text("e.g., Part payment 1, full repayment") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
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
                Text("Save Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
            Text("Recurring Bills List", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Bill")
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
                    Text("No recurring bills registered yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        modifier = Modifier.fillMaxWidth(),
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
                                    text = "Due Date: ${bill.dueDateValue}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row {
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
                                            Text("RECORD PAYMENT", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    IconButton(onClick = { selectedBillForDelete = bill }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
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
            title = { Text("Delete Bill?") },
            text = { Text("Are you sure you want to delete recurring bill '${selectedBillForDelete!!.name}' from the system?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBill(selectedBillForDelete!!)
                        selectedBillForDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBillForDelete = null }) {
                    Text("Cancel")
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
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var dueDateValue by remember { mutableStateOf("Every 10th") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Bill", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bill Name") },
                    placeholder = { Text("e.g., WiFi, Electricity, Health Insurance") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountStr = it },
                    label = { Text("Monthly Amount") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dueDateValue,
                    onValueChange = { dueDateValue = it },
                    label = { Text("Due Date Statement") },
                    placeholder = { Text("e.g., Every 15th of the month") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PayBillDialog(
    bill: Bill,
    wallets: List<Wallet>,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay Bill: ${bill.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Bill Amount: ${viewModel.formatRupiah(bill.amount)}")
                Text("Select the source wallet/account for payment:")
                
                if (wallets.isEmpty()) {
                    Text("No wallets found.", color = MaterialTheme.colorScheme.error)
                } else {
                    ScrollableTabRow(
                        selectedTabIndex = wallets.indexOfFirst { it.id == selectedWalletId }.coerceAtLeast(0),
                        edgePadding = 0.dp
                    ) {
                        wallets.forEach { w ->
                            Tab(
                                selected = selectedWalletId == w.id,
                                onClick = { selectedWalletId = w.id },
                                text = { Text(w.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedWalletId != 0) {
                        viewModel.payBill(bill, selectedWalletId)
                        onDismiss()
                    }
                },
                enabled = wallets.isNotEmpty()
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
