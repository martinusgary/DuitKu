package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Bill
import com.example.data.model.Debt
import com.example.data.model.Wallet
import com.example.ui.components.AddDebtLoanModal
import com.example.ui.components.AddRecurringBillModal
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsBillsScreen(
    viewModel: FinanceViewModel,
    showArchivedDebtsDialog: Boolean = false,
    onDismissArchivedDebtsDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    val debts by viewModel.debts.collectAsState()
    val activeDebts by viewModel.activeDebts.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    // 0 = Debts & Loans, 1 = Bills
    var selectedSubtab by remember { mutableStateOf(0) }

    var showAddDebtModal by remember { mutableStateOf(false) }
    var showAddBillModal by remember { mutableStateOf(false) }

    // Pay Bill selection wallet dialog
    var selectedBillToPay by remember { mutableStateOf<Bill?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Segmented Control Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tab: Debts & Loans
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedSubtab == 0) AccentBlue else Color.Transparent)
                        .clickable { selectedSubtab = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isId) "Utang & Pinjaman" else "Debts & Loans",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (selectedSubtab == 0) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (selectedSubtab == 0) Color.White else TextSecondary
                    )
                }

                // Tab: Bills
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedSubtab == 1) AccentBlue else Color.Transparent)
                        .clickable { selectedSubtab = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isId) "Tagihan Rutin" else "Bills",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (selectedSubtab == 1) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (selectedSubtab == 1) Color.White else TextSecondary
                    )
                }
            }

            // Content Container with crossfade
            Crossfade(targetState = selectedSubtab, label = "DebtsBillsSubtabTransition") { tabIndex ->
                if (tabIndex == 0) {
                    // 3. Subtab Debts & Loans View
                    DebtsAndLoansView(
                        debts = activeDebts,
                        viewModel = viewModel,
                        onAddDebtClick = { showAddDebtModal = true },
                        isId = isId
                    )
                } else {
                    // 2. Subtab Bills View
                    BillsView(
                        bills = bills,
                        viewModel = viewModel,
                        onAddBillClick = { showAddBillModal = true },
                        onPayBillClick = { bill -> selectedBillToPay = bill },
                        isId = isId
                    )
                }
            }
        }
    }

    if (showAddDebtModal) {
        AddDebtLoanModal(
            viewModel = viewModel,
            onDismiss = { showAddDebtModal = false }
        )
    }

    if (showAddBillModal) {
        AddRecurringBillModal(
            viewModel = viewModel,
            onDismiss = { showAddBillModal = false }
        )
    }

    // Pay Bill Wallet Selection Dialog
    selectedBillToPay?.let { bill ->
        AlertDialog(
            onDismissRequest = { selectedBillToPay = null },
            containerColor = CardBg,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (isId) "Bayar Tagihan: ${bill.name}" else "Pay Bill: ${bill.name}",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isId) "Pilih dompet sumber pembayaran:" else "Select payment source wallet:",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    wallets.forEach { w ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBg)
                                .clickable {
                                    viewModel.payBill(bill, w.id)
                                    selectedBillToPay = null
                                    Toast.makeText(context, if (isId) "Tagihan ${bill.name} berhasil dibayar!" else "Bill ${bill.name} marked paid!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(w.name, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(viewModel.formatRupiah(w.balance), color = AccentTeal, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedBillToPay = null }) {
                    Text(if (isId) "Batal" else "Cancel", color = TextSecondary)
                }
            }
        )
    }
}

/**
 * Subtab Bills View:
 * Grid Card: Unpaid Bills (Orange) & Paid Bills (Blue).
 * List Tagihan Berulang (Recurring Bills List) dengan tombol aksi "Mark Paid" / "Reset" dan ikon hapus.
 */
@Composable
fun BillsView(
    bills: List<Bill>,
    viewModel: FinanceViewModel,
    onAddBillClick: () -> Unit,
    onPayBillClick: (Bill) -> Unit,
    isId: Boolean
) {
    val unpaidBills = remember(bills) { bills.filter { it.status != "LUNAS" } }
    val paidBills = remember(bills) { bills.filter { it.status == "LUNAS" } }

    val unpaidTotal = remember(unpaidBills) { unpaidBills.sumOf { it.amount } }
    val paidTotal = remember(paidBills) { paidBills.sumOf { it.amount } }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Grid Card: Unpaid Bills (Orange) & Paid Bills (Blue)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Unpaid Bills (Orange)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AccentOrange.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(16.dp))
                        }
                        Text(if (isId) "Belum Dibayar" else "Unpaid Bills", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Text(
                        text = viewModel.formatRupiah(unpaidTotal),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = AccentOrange,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${unpaidBills.size} ${if (isId) "Tagihan" else "Bills"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            // Paid Bills (Blue)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AccentBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                        }
                        Text(if (isId) "Sudah Dibayar" else "Paid Bills", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Text(
                        text = viewModel.formatRupiah(paidTotal),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = AccentBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${paidBills.size} ${if (isId) "Tagihan" else "Bills"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Section Header with Add Bill Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isId) "Daftar Tagihan Berulang" else "Recurring Bills List",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            Button(
                onClick = onAddBillClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isId) "Tambah" else "Add Bill", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // List Tagihan Berulang
        if (bills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(44.dp), tint = TextSecondary.copy(alpha = 0.4f))
                    Text(if (isId) "Belum ada tagihan terdaftar." else "No recurring bills set.", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bills, key = { it.id }) { bill ->
                    val isPaid = bill.status == "LUNAS"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = bill.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = viewModel.formatRupiah(bill.amount),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isPaid) AccentBlue else AccentOrange
                                )
                                Text(
                                    text = "${if (isId) "Jatuh tempo tgl" else "Due on day"} ${bill.dueDateValue}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isPaid) {
                                    OutlinedButton(
                                        onClick = { viewModel.resetBillStatus(bill) },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, CardBorder),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Reset", fontSize = 12.sp, color = TextSecondary)
                                    }
                                } else {
                                    Button(
                                        onClick = { onPayBillClick(bill) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(if (isId) "Bayar" else "Mark Paid", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteBill(bill) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Subtab Debts & Loans View:
 * Grid Card: My Debts (Hutang Saya - Ungu) & My Loans (Piutang Saya - Hijau).
 * List Catatan Hutang/Piutang Aktif (atau Empty State jika kosong).
 */
@Composable
fun DebtsAndLoansView(
    debts: List<Debt>,
    viewModel: FinanceViewModel,
    onAddDebtClick: () -> Unit,
    isId: Boolean
) {
    val myDebts = remember(debts) { debts.filter { it.type == "HUTANG" } }
    val myLoans = remember(debts) { debts.filter { it.type == "PIUTANG" } }

    val myDebtsTotal = remember(myDebts) { myDebts.sumOf { it.remainingAmount.coerceAtLeast(0.0) } }
    val myLoansTotal = remember(myLoans) { myLoans.sumOf { it.remainingAmount.coerceAtLeast(0.0) } }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Grid Card: My Debts (Ungu) & My Loans (Hijau)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // My Debts (Hutang Saya - Ungu)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AccentPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                        }
                        Text(if (isId) "Hutang Saya" else "My Debts", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Text(
                        text = viewModel.formatRupiah(myDebtsTotal),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = AccentPurple,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${myDebts.size} ${if (isId) "Catatan" else "Notes"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            // My Loans (Piutang Saya - Hijau)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AccentGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CallReceived, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                        }
                        Text(if (isId) "Piutang Saya" else "My Loans", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Text(
                        text = viewModel.formatRupiah(myLoansTotal),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = AccentGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${myLoans.size} ${if (isId) "Catatan" else "Notes"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Section Header with Add Debt/Loan Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isId) "Catatan Hutang & Piutang Aktif" else "Active Debts & Loans",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            Button(
                onClick = onAddDebtClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isId) "Tambah" else "Add Entry", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // List Catatan Hutang/Piutang Aktif (atau Empty State jika kosong)
        if (debts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.MoneyOff, contentDescription = null, modifier = Modifier.size(44.dp), tint = TextSecondary.copy(alpha = 0.4f))
                    Text(if (isId) "Tidak ada hutang atau piutang aktif." else "No active debts or loans.", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(debts, key = { it.id }) { debt ->
                    val isHutang = debt.type == "HUTANG"
                    val remaining = debt.remainingAmount.coerceAtLeast(0.0)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = (if (isHutang) AccentPurple else AccentGreen).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (isHutang) (if (isId) "HUTANG" else "DEBT") else (if (isId) "PIUTANG" else "LOAN"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isHutang) AccentPurple else AccentGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = debt.personName,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                }

                                Text(
                                    text = if (isId) "Sisa: ${viewModel.formatRupiah(remaining)}" else "Remaining: ${viewModel.formatRupiah(remaining)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )

                                if (debt.notes.isNotBlank()) {
                                    Text(
                                        text = debt.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.payDebtInstallment(debt, debt.remainingAmount, 1, "Pelunasan")
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(if (isId) "Lunas" else "Settled", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = { viewModel.deleteDebt(debt) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
