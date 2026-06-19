package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.ui.viewmodel.FinanceViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    isBulkMode: Boolean = false,
    onBulkModeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"
    val uiStyle by viewModel.uiStyle.collectAsState()
    val isFresh = uiStyle == "FRESH"

    // Filter & Sort State
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // ALL, EXPENSE, INCOME, TRANSFER
    var selectedCategoryFilter by remember { mutableStateOf<Int?>(null) } // null for all
    var selectedDatePreset by remember { mutableStateOf("ALL_TIME") } // ALL_TIME, TODAY, LAST_7, THIS_MONTH
    var sortBy by remember { mutableStateOf("DATE_DESC") } // DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC

    // Bulk Edit State
    val selectedTxIds = remember { mutableStateListOf<Int>() }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    // Clicked transaction details state
    var selectedDetailTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteConfirmForSingle by remember { mutableStateOf(false) }

    // Filter and Sort calculations
    val filteredTransactions = remember(
        transactions, searchQuery, selectedTypeFilter, selectedCategoryFilter, selectedDatePreset, sortBy
    ) {
        var result = transactions.asSequence()

        // 1. Filter by Search Query
        if (searchQuery.isNotBlank()) {
            result = result.filter {
                it.note.contains(searchQuery, ignoreCase = true)
            }
        }

        // 2. Filter by Type
        if (selectedTypeFilter != "ALL") {
            result = result.filter { it.type == selectedTypeFilter }
        }

        // 3. Filter by Category
        if (selectedCategoryFilter != null) {
            result = result.filter { it.categoryId == selectedCategoryFilter }
        }

        // 4. Filter by Date range preset
        val now = System.currentTimeMillis()
        when (selectedDatePreset) {
            "TODAY" -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfToday = cal.timeInMillis
                result = result.filter { it.date >= startOfToday }
            }
            "LAST_7" -> {
                val sevenDaysAgo = now - (7L * 24L * 60L * 60L * 1000L)
                result = result.filter { it.date >= sevenDaysAgo }
            }
            "THIS_MONTH" -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfMonth = cal.timeInMillis
                result = result.filter { it.date >= startOfMonth }
            }
        }

        // 5. Sorting
        result = when (sortBy) {
            "DATE_DESC" -> result.sortedByDescending { it.date }
            "DATE_ASC" -> result.sortedBy { it.date }
            "AMOUNT_DESC" -> result.sortedByDescending { it.amount }
            "AMOUNT_ASC" -> result.sortedBy { it.amount }
            else -> result.sortedByDescending { it.date }
        }

        result.toList()
    }

    // Reset selected items if we turn off bulk mode
    LaunchedEffect(isBulkMode) {
        if (!isBulkMode) {
            selectedTxIds.clear()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- HEADER WITH BULK MODE TOGGLE ---
            AnimatedContent(
                targetState = isBulkMode,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(250)) + expandVertically()) togetherWith
                            (fadeOut(animationSpec = tween(200)) + shrinkVertically())
                },
                label = "BulkModeHeaderTransition"
            ) { targetBulkMode ->
                if (targetBulkMode) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color(0xFF8C1D18), // Rich Dark Red matching Image 2
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onBulkModeChange(false) }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Cancel bulk select",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isId) "${selectedTxIds.size} Terpilih" else "${selectedTxIds.size} Selected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Button(
                                onClick = {
                                    if (selectedTxIds.isEmpty()) {
                                        Toast.makeText(context, if (isId) "Pilih transaksi terlebih dahulu" else "Select transactions first", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showBulkDeleteConfirm = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFDAD6), // Soft warm light pink
                                    contentColor = Color(0xFF410002)   // Very dark maroon style
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Selected",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF410002)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isId) "Hapus Terpilih" else "Delete Checked",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isId) "Buku Kas Transaksi" else "Transactions Ledger",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (isId) "Lihat dan kelola seluruh transaksi" else "View and manage all operations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // --- FILTERS EXPANSE ---
            // 1. Search note
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(if (isId) "Cari catatan transaksi..." else "Search transactions note...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            var showTypeMenu by remember { mutableStateOf(false) }
            var showPresetMenu by remember { mutableStateOf(false) }
            var showSortMenu by remember { mutableStateOf(false) }

            // Condensed scrollable single filter row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pin 1: Date Filter
                item {
                    Box {
                        FilterChip(
                            selected = selectedDatePreset != "ALL_TIME",
                            onClick = { showPresetMenu = true },
                            label = {
                                Text(
                                    when (selectedDatePreset) {
                                        "TODAY" -> if (isId) "Hari Ini" else "Today"
                                        "LAST_7" -> if (isId) "7 Hari" else "7 Days"
                                        "THIS_MONTH" -> if (isId) "Bulan Ini" else "This Month"
                                        else -> if (isId) "Semua Tanggal" else "All Dates"
                                    }
                                )
                            },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenu(
                            expanded = showPresetMenu,
                            onDismissRequest = { showPresetMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text(if (isId) "Semua Tanggal" else "All Dates") }, onClick = { selectedDatePreset = "ALL_TIME"; showPresetMenu = false })
                            DropdownMenuItem(text = { Text(if (isId) "Hari Ini" else "Today") }, onClick = { selectedDatePreset = "TODAY"; showPresetMenu = false })
                            DropdownMenuItem(text = { Text(if (isId) "7 Hari Terakhir" else "Last 7 Days") }, onClick = { selectedDatePreset = "LAST_7"; showPresetMenu = false })
                            DropdownMenuItem(text = { Text(if (isId) "Bulan Ini" else "This Month") }, onClick = { selectedDatePreset = "THIS_MONTH"; showPresetMenu = false })
                        }
                    }
                }

                // Pin 2: Type Filter
                item {
                    Box {
                        FilterChip(
                            selected = selectedTypeFilter != "ALL",
                            onClick = { showTypeMenu = true },
                            label = {
                                Text(
                                    when (selectedTypeFilter) {
                                        "EXPENSE" -> if (isId) "Pengeluaran" else "Expenses"
                                        "INCOME" -> if (isId) "Pemasukan" else "Incomes"
                                        "TRANSFER" -> if (isId) "Transfer" else "Transfers"
                                        else -> if (isId) "Semua Jenis" else "All Types"
                                    }
                                )
                            },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenu(
                            expanded = showTypeMenu,
                            onDismissRequest = { showTypeMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text(if (isId) "Semua Jenis" else "All Types") }, onClick = { selectedTypeFilter = "ALL"; showTypeMenu = false })
                            DropdownMenuItem(text = { Text(if (isId) "Pengeluaran" else "Expenses") }, onClick = { selectedTypeFilter = "EXPENSE"; showTypeMenu = false })
                            DropdownMenuItem(text = { Text(if (isId) "Pemasukan" else "Incomes") }, onClick = { selectedTypeFilter = "INCOME"; showTypeMenu = false })
                            DropdownMenuItem(text = { Text(if (isId) "Transfer Uang" else "Transfers") }, onClick = { selectedTypeFilter = "TRANSFER"; showTypeMenu = false })
                        }
                    }
                }

                // Pin 3: Sort Filter
                item {
                    Box {
                        FilterChip(
                            selected = sortBy != "DATE_DESC",
                            onClick = { showSortMenu = true },
                            label = {
                                Text(
                                    when (sortBy) {
                                        "DATE_ASC" -> if (isId) "Terlama" else "Oldest first"
                                        "AMOUNT_DESC" -> if (isId) "Tertinggi" else "Highest"
                                        "AMOUNT_ASC" -> if (isId) "Terendah" else "Lowest"
                                        else -> if (isId) "Terbaru" else "Newest first"
                                    }
                                )
                            },
                            trailingIcon = { Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text(if (isId) "Tanggal Terbaru" else "Newest Date") }, onClick = { sortBy = "DATE_DESC"; showSortMenu = false })
                            DropdownMenuItem(text = { Text(if (isId) "Tanggal Terlama" else "Oldest Date") }, onClick = { sortBy = "DATE_ASC"; showSortMenu = false })
                            DropdownMenuItem(text = { Text(if (isId) "Jumlah Tertinggi" else "Highest Amount") }, onClick = { sortBy = "AMOUNT_DESC"; showSortMenu = false })
                            DropdownMenuItem(text = { Text(if (isId) "Jumlah Terendah" else "Lowest Amount") }, onClick = { sortBy = "AMOUNT_ASC"; showSortMenu = false })
                        }
                    }
                }

                // Simple spacer divider line
                item {
                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    )
                }

                // Pin 4: Category Selectors
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text(if (isId) "Semua Kategori" else "All Categories") }
                    )
                }

                items(categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = selectedCategoryFilter == category.id,
                        onClick = { selectedCategoryFilter = category.id },
                        label = { Text(category.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // --- MAIN LIST EXPANSE ---
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (isId) "Tidak ada transaksi yang sesuai filter" else "No transactions fit the search filters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isId) "Cobalah mengubah filter pencarian Anda" else "Try altering search parameters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { txn ->
                        val walletOfTx = wallets.firstOrNull { it.id == txn.walletId }
                        val targetWalletOfTx = wallets.firstOrNull { it.id == txn.targetWalletId }
                        val categoryOfTx = categories.firstOrNull { it.id == txn.categoryId }

                        val isSelected = selectedTxIds.contains(txn.id)

                        val cardShape = RoundedCornerShape(20.dp)
                        ElevatedCard(
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .clip(cardShape)
                                .clickable {
                                    if (isBulkMode) {
                                        if (isSelected) {
                                            selectedTxIds.remove(txn.id)
                                        } else {
                                            selectedTxIds.add(txn.id)
                                        }
                                    } else {
                                        selectedDetailTransaction = txn
                                    }
                                }
                                .then(
                                    if (isFresh) {
                                        Modifier.border(
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                            cardShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            shape = cardShape,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimatedVisibility(
                                    visible = isBulkMode,
                                    enter = expandHorizontally() + fadeIn(),
                                    exit = shrinkHorizontally() + fadeOut()
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked == true) {
                                                selectedTxIds.add(txn.id)
                                            } else {
                                                selectedTxIds.remove(txn.id)
                                            }
                                        },
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Typology design elements
                                    val iconBg = when (txn.type) {
                                        "INCOME" -> Color(0xFFE8F5E9)
                                        "EXPENSE" -> Color(0xFFFFEBEE)
                                        else -> Color(0xFFE3F2FD)
                                    }
                                    val iconColor = when (txn.type) {
                                        "INCOME" -> Color(0xFF2E7D32)
                                        "EXPENSE" -> Color(0xFFC62828)
                                        else -> Color(0xFF1565C0)
                                    }
                                    val iconToUse = when (txn.type) {
                                        "INCOME" -> Icons.Default.Add
                                        "EXPENSE" -> Icons.Default.Remove
                                        else -> Icons.Default.CompareArrows
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(if (isFresh) RoundedCornerShape(10.dp) else CircleShape)
                                            .background(iconBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            iconToUse,
                                            contentDescription = null,
                                            tint = iconColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        val labelText = when (txn.type) {
                                            "TRANSFER" -> {
                                                val fromName = walletOfTx?.name ?: "???"
                                                val toName = targetWalletOfTx?.name ?: "???"
                                                "Transfer: $fromName → $toName"
                                            }
                                            else -> categoryOfTx?.name ?: (if (isId) "Tanpa Kategori" else "Uncategorized")
                                        }
                                        Text(
                                            text = labelText,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (txn.note.isNotEmpty()) {
                                            Text(
                                                text = txn.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = viewModel.formatDate(txn.date),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${walletOfTx?.name ?: (if (isId) "Dompet" else "Wallet")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                            )
                                        }
                                    }
                                }

                                val priceColor = when (txn.type) {
                                    "INCOME" -> Color(0xFF2E7D32)
                                    "EXPENSE" -> Color(0xFFC62828)
                                    else -> Color(0xFF1565C0)
                                }
                                val formatSign = when (txn.type) {
                                    "INCOME" -> "+"
                                    "EXPENSE" -> "-"
                                    else -> "⇄"
                                }

                                Text(
                                    text = "$formatSign ${viewModel.formatRupiah(txn.amount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                    color = priceColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG 1: BULK DELETE CONFIRM ---
    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = {
                Text(
                    text = if (isId) "Hapus Massal" else "Bulk Deletion",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isId) 
                            "Pilih bagaimana Anda ingin menghapus ${selectedTxIds.size} transaksi terpilih:" 
                        else "Choose how you want to delete the ${selectedTxIds.size} selected transactions:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Option 1: Only history
                    Surface(
                        onClick = {
                            val toDelete = transactions.filter { selectedTxIds.contains(it.id) }
                            viewModel.deleteTransactionsBulk(toDelete, refund = false)
                            onBulkModeChange(false)
                            showBulkDeleteConfirm = false
                            Toast.makeText(context, if (isId) "${toDelete.size} catatan sejarah berhasil dihapus!" else "${toDelete.size} history logs deleted!", Toast.LENGTH_SHORT).show()
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
                                    "Hanya menghapus catatan. Saldo seluruh dompet tidak akan diubah/dikembalikan."
                                else "Removes records only. Wallet balances will NOT be modified.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Option 2: Refund
                    Surface(
                        onClick = {
                            val toDelete = transactions.filter { selectedTxIds.contains(it.id) }
                            viewModel.deleteTransactionsBulk(toDelete, refund = true)
                            onBulkModeChange(false)
                            showBulkDeleteConfirm = false
                            Toast.makeText(context, if (isId) "${toDelete.size} transaksi dibatalkan & saldo dipulihkan!" else "${toDelete.size} transactions cancelled & balances restored!", Toast.LENGTH_SHORT).show()
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
                                    text = if (isId) "Batalkan & Refund Saldo" else "Cancel & Refund Wallets",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isId)
                                    "Membatalkan seluruh transaksi terpilih dan mengembalikan dana ke masing-masing dompet."
                                else "Cancels selected transactions and restores/reverts funds in respective wallets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text(if (isId) "Batal" else "Cancel")
                }
            }
        )
    }

    // --- DIALOG 2: TRANSACTION DETAILS (SINGLE VIEW) ---
    if (selectedDetailTransaction != null) {
        val txn = selectedDetailTransaction!!
        val categoryOfTx = categories.firstOrNull { it.id == txn.categoryId }
        val walletOfTx = wallets.firstOrNull { it.id == txn.walletId }
        val targetWalletOfTx = txn.targetWalletId?.let { targetId ->
            wallets.firstOrNull { it.id == targetId }
        }

        AlertDialog(
            onDismissRequest = { selectedDetailTransaction = null },
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
                        color = when (txn.type) {
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
                                text = when (txn.type) {
                                    "EXPENSE" -> if (isId) "Pengeluaran" else "Expense"
                                    "INCOME" -> if (isId) "Pemasukan" else "Income"
                                    else -> "Transfer"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (txn.type) {
                                    "EXPENSE" -> Color(0xFFC62828)
                                    "INCOME" -> Color(0xFF2E7D32)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.formatRupiah(txn.amount),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = when (txn.type) {
                                    "EXPENSE" -> Color(0xFFC62828)
                                    "INCOME" -> Color(0xFF2E7D32)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TransactionRowItemDetail(label = if (isId) "Tanggal" else "Date", value = viewModel.formatDate(txn.date))

                        if (categoryOfTx != null) {
                            TransactionRowItemDetail(label = if (isId) "Kategori" else "Category", value = categoryOfTx.name)
                        }

                        if (txn.type == "TRANSFER" && targetWalletOfTx != null) {
                            TransactionRowItemDetail(label = if (isId) "Dari Dompet" else "From Wallet", value = walletOfTx?.name ?: "Unknown")
                            TransactionRowItemDetail(label = if (isId) "Ke Dompet" else "To Wallet", value = targetWalletOfTx.name)
                        } else {
                            TransactionRowItemDetail(label = if (isId) "Dompet" else "Wallet", value = walletOfTx?.name ?: "Unknown")
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
                                text = txn.note.ifBlank { if (isId) "Tidak ada catatan." else "No description added." },
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
                        onClick = { showDeleteConfirmForSingle = true }
                    ) {
                        Text(if (isId) "Hapus" else "Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { selectedDetailTransaction = null }) {
                        Text(if (isId) "Tutup" else "Close", fontWeight = FontWeight.Bold)
                    }
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    // --- DIALOG 3: SINGLE DELETE CONFIRM ---
    if (showDeleteConfirmForSingle) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmForSingle = false },
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
                            val txn = selectedDetailTransaction
                            if (txn != null) {
                                viewModel.deleteTransaction(txn, refund = false)
                                Toast.makeText(context, if (isId) "Riwayat dihapus (saldo tetap)!" else "History deleted (balance kept)!", Toast.LENGTH_SHORT).show()
                            }
                            showDeleteConfirmForSingle = false
                            selectedDetailTransaction = null
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
                            val txn = selectedDetailTransaction
                            if (txn != null) {
                                viewModel.deleteTransaction(txn, refund = true)
                                Toast.makeText(context, if (isId) "Transaksi dibatalkan & saldo dipulihkan!" else "Transaction cancelled & balance restored!", Toast.LENGTH_SHORT).show()
                            }
                            showDeleteConfirmForSingle = false
                            selectedDetailTransaction = null
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
                TextButton(onClick = { showDeleteConfirmForSingle = false }) {
                    Text(if (isId) "Batal" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun ScrollableCategoryRow(
    categories: List<Category>,
    selectedCategoryId: Int?,
    isId: Boolean = false,
    onSelect: (Int?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onSelect(null) },
                label = { Text(if (isId) "Semua Kategori" else "All Categories") }
            )
        }
        items(categories, key = { it.id }) { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onSelect(category.id) },
                label = { Text(category.name) }
            )
        }
    }
}

@Composable
fun TransactionRowItemDetail(label: String, value: String) {
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
