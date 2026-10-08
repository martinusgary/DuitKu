package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Bill
import com.example.data.model.Category
import com.example.data.model.Debt
import com.example.data.model.Wallet
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel

/**
 * 1. Modal New Transaction:
 * Input Jenis (Expense/Income), Title, Amount, Category, Wallet Dropdown, Note.
 * Exactly matches Screenshot_20261008_090416.jpg.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionModal(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val wallets by viewModel.wallets.collectAsState()

    var selectedType by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

    var selectedCategoryId by remember(filteredCategories) {
        mutableStateOf(filteredCategories.firstOrNull()?.id ?: 1)
    }

    var selectedWalletId by remember(wallets) {
        mutableStateOf(wallets.firstOrNull()?.id ?: 1)
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var walletDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add New Transaction",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary
                            )
                        }
                    }

                    // Type Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF090D16))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Expense Button
                        Button(
                            onClick = { selectedType = "EXPENSE" },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedType == "EXPENSE") AccentRed else Color.Transparent,
                                contentColor = if (selectedType == "EXPENSE") Color.White else TextSecondary
                            ),
                            elevation = null
                        ) {
                            Text("Expense", fontWeight = FontWeight.Bold)
                        }

                        // Income Button
                        Button(
                            onClick = { selectedType = "INCOME" },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedType == "INCOME") AccentBlue else Color.Transparent,
                                contentColor = if (selectedType == "INCOME") Color.White else TextSecondary
                            ),
                            elevation = null
                        ) {
                            Text("Income", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Title Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Title",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("e.g. Belanja Harian", color = TextSecondary.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // Amount Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Amount (Rp)",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { if (it.all { char -> char.isDigit() }) amountStr = it },
                            placeholder = { Text("e.g. 50000", color = TextSecondary.copy(alpha = 0.5f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // Category & Wallet 2-Column Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Category Dropdown
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Category", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                val currentCatName = filteredCategories.find { it.id == selectedCategoryId }?.name ?: "Category"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(DarkBg)
                                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                                        .clickable { categoryDropdownExpanded = true }
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = currentCatName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                                }
                                DropdownMenu(
                                    expanded = categoryDropdownExpanded,
                                    onDismissRequest = { categoryDropdownExpanded = false },
                                    modifier = Modifier.background(CardBg).border(1.dp, CardBorder)
                                ) {
                                    filteredCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.name, color = TextPrimary) },
                                            onClick = {
                                                selectedCategoryId = cat.id
                                                categoryDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Wallet Dropdown
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Wallet", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                val currentWalletName = wallets.find { it.id == selectedWalletId }?.name ?: "Wallet"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(DarkBg)
                                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                                        .clickable { walletDropdownExpanded = true }
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = currentWalletName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                                }
                                DropdownMenu(
                                    expanded = walletDropdownExpanded,
                                    onDismissRequest = { walletDropdownExpanded = false },
                                    modifier = Modifier.background(CardBg).border(1.dp, CardBorder)
                                ) {
                                    wallets.forEach { w ->
                                        DropdownMenuItem(
                                            text = { Text(w.name, color = TextPrimary) },
                                            onClick = {
                                                selectedWalletId = w.id
                                                walletDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Note / Description Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Note / Description",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            placeholder = { Text("e.g. Jajan Indomaret", color = TextSecondary.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Submit Button
                    val isValid = (amountStr.toDoubleOrNull() ?: 0.0) > 0.0
                    Button(
                        onClick = {
                            val parsedAmount = amountStr.toDoubleOrNull() ?: 0.0
                            if (parsedAmount > 0.0) {
                                val txNote = if (note.isNotBlank()) note else (if (title.isNotBlank()) title else "Transaksi")
                                viewModel.addTransaction(
                                    amount = parsedAmount,
                                    type = selectedType,
                                    walletId = selectedWalletId,
                                    categoryId = selectedCategoryId,
                                    note = txNote,
                                    date = System.currentTimeMillis()
                                )
                                onDismiss()
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor = Color.White,
                            disabledContainerColor = AccentBlue.copy(alpha = 0.4f),
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        )
                    ) {
                        Text("Save Transaction", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

/**
 * 2. Modal Filter & Sort:
 * Filter type (All, Income, Expense, Transfer) & Sort (Newest, Oldest, Highest, Lowest).
 */
@Composable
fun FilterSortModal(
    selectedType: String,
    onTypeSelect: (String) -> Unit,
    sortBy: String,
    onSortSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Filter & Sort Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    // Filter by Type
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Transaction Type", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ALL" to "All", "EXPENSE" to "Expense", "INCOME" to "Income", "TRANSFER" to "Transfer").forEach { (typeKey, typeLabel) ->
                                val isSelected = selectedType == typeKey
                                Surface(
                                    onClick = { onTypeSelect(typeKey) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) AccentBlue else DarkBg,
                                    border = if (isSelected) null else BorderStroke(1.dp, CardBorder),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(typeLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) Color.White else TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // Sort By
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sort Order", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "DATE_DESC" to "Newest",
                                "DATE_ASC" to "Oldest",
                                "AMOUNT_DESC" to "Highest",
                                "AMOUNT_ASC" to "Lowest"
                            ).forEach { (sortKey, sortLabel) ->
                                val isSelected = sortBy == sortKey
                                Surface(
                                    onClick = { onSortSelect(sortKey) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) AccentBlue else DarkBg,
                                    border = if (isSelected) null else BorderStroke(1.dp, CardBorder),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(sortLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) Color.White else TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Apply Filters", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 3. Modal Add Wallet:
 * Nama dompet, Kategori (Cash, E-Money, Savings, Bank), Saldo Awal.
 * Exactly matches Screenshot_20261008_090355.jpg.
 */
@Composable
fun AddWalletModal(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    var walletName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Cash") }
    var initialBalanceStr by remember { mutableStateOf("0") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Cash", "Bank", "E-Money", "Savings")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add New Wallet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    // Wallet Name
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Wallet Name", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        OutlinedTextField(
                            value = walletName,
                            onValueChange = { walletName = it },
                            placeholder = { Text("e.g. BCA Digital / GoPay", color = TextSecondary.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // Account Category Dropdown
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Account Category", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkBg)
                                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                                    .clickable { categoryDropdownExpanded = true }
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(selectedCategory, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                            }
                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false },
                                modifier = Modifier.background(CardBg).border(1.dp, CardBorder)
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, color = TextPrimary) },
                                        onClick = {
                                            selectedCategory = cat
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Initial Balance
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Initial Balance (Rp)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        OutlinedTextField(
                            value = initialBalanceStr,
                            onValueChange = { if (it.all { char -> char.isDigit() }) initialBalanceStr = it },
                            placeholder = { Text("0", color = TextSecondary.copy(alpha = 0.5f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val canCreate = walletName.trim().isNotBlank()
                    Button(
                        onClick = {
                            val balance = initialBalanceStr.toDoubleOrNull() ?: 0.0
                            val iconTag = when (selectedCategory) {
                                "Savings" -> "savings"
                                "Bank" -> "bank"
                                "E-Money" -> "emoney"
                                else -> "cash"
                            }
                            viewModel.addWallet(
                                name = walletName.trim(),
                                balance = balance,
                                icon = iconTag
                            )
                            onDismiss()
                        },
                        enabled = canCreate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Create Wallet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

/**
 * 4. Modal Add Recurring Bill:
 * Provider/Nama Tagihan, Nominal, Tanggal Jatuh Tempo.
 */
@Composable
fun AddRecurringBillModal(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    var billName by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var dueDayStr by remember { mutableStateOf("10") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add Recurring Bill", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Provider / Bill Name", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        OutlinedTextField(
                            value = billName,
                            onValueChange = { billName = it },
                            placeholder = { Text("e.g. WiFi IndiHome / Listrik PLN", color = TextSecondary.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Amount (Rp)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { if (it.all { char -> char.isDigit() }) amountStr = it },
                            placeholder = { Text("e.g. 300000", color = TextSecondary.copy(alpha = 0.5f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Due Date (Day of Month: 1-31)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        OutlinedTextField(
                            value = dueDayStr,
                            onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 2) dueDayStr = it },
                            placeholder = { Text("10", color = TextSecondary.copy(alpha = 0.5f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    val canSave = billName.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0.0
                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            val dueDay = (dueDayStr.toIntOrNull() ?: 10).coerceIn(1, 31)
                            viewModel.addBill(
                                name = billName.trim(),
                                amount = amount,
                                dueDateValue = dueDay.toString()
                            )
                            onDismiss()
                        },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Save Recurring Bill", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

/**
 * 5. Modal Add Debt/Loan:
 * Tipe (Debt/Loan), Penanggung Jawab, Nominal.
 */
@Composable
fun AddDebtLoanModal(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf("HUTANG") } // HUTANG (Debt), PIUTANG (Loan)
    var personName by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add Debt or Loan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    // Switcher Debt vs Loan
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkBg)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { type = "HUTANG" },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "HUTANG") AccentPurple else Color.Transparent,
                                contentColor = if (type == "HUTANG") Color.White else TextSecondary
                            ),
                            elevation = null
                        ) {
                            Text("My Debt (Hutang)", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { type = "PIUTANG" },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "PIUTANG") AccentGreen else Color.Transparent,
                                contentColor = if (type == "PIUTANG") Color.White else TextSecondary
                            ),
                            elevation = null
                        ) {
                            Text("My Loan (Piutang)", fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Person / Counterparty Name", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        OutlinedTextField(
                            value = personName,
                            onValueChange = { personName = it },
                            placeholder = { Text("e.g. Budi / Teman Kantor", color = TextSecondary.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Amount (Rp)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { if (it.all { char -> char.isDigit() }) amountStr = it },
                            placeholder = { Text("e.g. 150000", color = TextSecondary.copy(alpha = 0.5f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Notes / Reason", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("e.g. Talangan makan siang", color = TextSecondary.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    val canSave = personName.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0.0
                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            val due = System.currentTimeMillis() + (30L * 86400000L) // 30 days
                            viewModel.addDebt(
                                personName = personName.trim(),
                                totalAmount = amount,
                                dueDate = due,
                                type = type,
                                notes = notes.trim()
                            )
                            onDismiss()
                        },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (type == "HUTANG") AccentPurple else AccentGreen)
                    ) {
                        Text("Save Record", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
