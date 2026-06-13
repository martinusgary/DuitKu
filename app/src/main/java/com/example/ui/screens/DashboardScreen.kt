package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
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

    var showAddDialog by remember { mutableStateOf(false) }

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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToWallets() }
                        .testTag("total_balance_card"),
                    shape = RoundedCornerShape(28.dp),
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
                                text = "MY TOTAL BALANCE",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallet",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = viewModel.formatRupiah(totalBalance),
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
                                text = "${wallets.size} Connected accounts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "View Details →",
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
                                    "Income",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                viewModel.formatRupiah(monthlyIncome),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                "This month",
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
                                    "Expense",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                viewModel.formatRupiah(monthlyExpense),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                            Text(
                                "This month",
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
                        text = "Last 5 Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            val last5Transactions = transactions.take(5)

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
                                "No transactions recorded yet.",
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
                        onDelete = { viewModel.deleteTransaction(txn) }
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
                Text("New Transaction", fontWeight = FontWeight.Bold)
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
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDeleteConfirm = true },
        shape = RoundedCornerShape(24.dp),
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
                        else -> category?.name ?: "Uncategorized"
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
                        text = "${viewModel.formatDate(transaction.date)} • ${wallet?.name ?: "Wallet"}",
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
                text = "$prefix ${viewModel.formatRupiah(transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                color = priceColor
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction record? The wallet balance will be adjusted accordingly.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
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
    var amountStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // "INCOME", "EXPENSE", "TRANSFER"
    var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: 0) }
    var selectedTargetWalletId by remember { mutableStateOf(wallets.getOrNull(1)?.id ?: wallets.firstOrNull()?.id ?: 0) }
    var selectedCategoryId by remember { mutableStateOf(0) }
    var note by remember { mutableStateOf("") }

    // Group categories
    val incomeCategoryList = categories.filter { it.type == "INCOME" }
    val expenseCategoryList = categories.filter { it.type == "EXPENSE" }

    // Keep state of selected category ID matched with type
    LaunchedEffect(selectedType, categories) {
        if (selectedType == "INCOME") {
            selectedCategoryId = incomeCategoryList.firstOrNull()?.id ?: 0
        } else if (selectedType == "EXPENSE") {
            selectedCategoryId = expenseCategoryList.firstOrNull()?.id ?: 0
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add New Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // 1. Selector Tab
                TabRow(
                    selectedTabIndex = when (selectedType) {
                        "EXPENSE" -> 0
                        "INCOME" -> 1
                        else -> 2 // TRANSFER
                    }
                ) {
                    Tab(
                        selected = selectedType == "EXPENSE",
                        onClick = { selectedType = "EXPENSE" },
                        text = { Text("Expense") }
                    )
                    Tab(
                        selected = selectedType == "INCOME",
                        onClick = { selectedType = "INCOME" },
                        text = { Text("Income") }
                    )
                    Tab(
                        selected = selectedType == "TRANSFER",
                        onClick = { selectedType = "TRANSFER" },
                        text = { Text("Transfer") }
                    )
                }

                // Scan Receipt Feature with Gemini AI
                var isScanning by remember { mutableStateOf(false) }
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()

                val photoPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        isScanning = true
                        coroutineScope.launch {
                            try {
                                val result = GeminiClient.scanReceipt(context, uri)
                                if (result != null) {
                                    amountStr = result.amount.toInt().toString()
                                    if (result.type == "EXPENSE" || result.type == "INCOME") {
                                        selectedType = result.type
                                    }
                                    note = result.note
                                    Toast.makeText(context, "Selesai memindai nota belanja!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal memindai foto: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isScanning = false
                            }
                        }
                    }
                }

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
                                "Gemini AI sedang membaca foto nota...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pindai Foto Nota dengan Gemini AI", fontWeight = FontWeight.Bold)
                    }
                }

                // 2. Amount Input
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountStr = it },
                    label = { Text("Amount (Money)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("0") }
                )

                // 3. Wallets Selection (From / Source Wallet)
                Text("Source Wallet:", style = MaterialTheme.typography.labelMedium)
                if (wallets.isEmpty()) {
                    Text("No wallets found. Please add a wallet first in the Wallets tab.", color = MaterialTheme.colorScheme.error)
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

                // 4. Specific to Transfer Destination Wallet
                if (selectedType == "TRANSFER") {
                    Text("Destination Wallet:", style = MaterialTheme.typography.labelMedium)
                    ScrollableTabRow(
                        selectedTabIndex = wallets.indexOfFirst { it.id == selectedTargetWalletId }.coerceAtLeast(0),
                        edgePadding = 0.dp
                    ) {
                        wallets.forEach { w ->
                            Tab(
                                selected = selectedTargetWalletId == w.id,
                                onClick = { selectedTargetWalletId = w.id },
                                text = { Text(w.name) }
                            )
                        }
                    }
                }

                // 5. Category Selection (Only for Income and Expense)
                if (selectedType != "TRANSFER") {
                    Text("Category:", style = MaterialTheme.typography.labelMedium)
                    val listToShow = if (selectedType == "INCOME") incomeCategoryList else expenseCategoryList
                    
                    if (listToShow.isEmpty()) {
                        Text("No categories found.")
                    } else {
                        ScrollableTabRow(
                            selectedTabIndex = listToShow.indexOfFirst { it.id == selectedCategoryId }.coerceAtLeast(0),
                            edgePadding = 0.dp
                        ) {
                            listToShow.forEach { cat ->
                                Tab(
                                    selected = selectedCategoryId == cat.id,
                                    onClick = { selectedCategoryId = cat.id },
                                    text = { Text(cat.name) }
                                )
                            }
                        }
                    }
                }

                // 6. Notes Input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Additional Note") },
                    placeholder = { Text("e.g., Grocery store, salary bonus, coffee, etc.") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amountVal = amountStr.toDoubleOrNull() ?: 0.0
                            if (amountVal <= 0.0) {
                                // Invalid amount
                                return@Button
                            }
                            if (selectedWalletId == 0) return@Button

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
                            onDismiss()
                        },
                        enabled = amountStr.isNotEmpty() && wallets.isNotEmpty()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
