package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.ui.components.CategoryVisuals
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
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
    var selectedWalletFilter by remember { mutableStateOf<Int?>(null) } // null for all wallets
    var selectedDatePreset by remember { mutableStateOf("ALL_TIME") } // ALL_TIME, TODAY, LAST_7, THIS_MONTH, CUSTOM
    var customStartDateMillis by remember { mutableStateOf<Long?>(null) }
    var customEndDateMillis by remember { mutableStateOf<Long?>(null) }
    var sortBy by remember { mutableStateOf("DATE_DESC") } // DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC

    // Filter Sheet Modal state
    var showFilterSheet by remember { mutableStateOf(false) }

    // Bulk Edit State
    val selectedTxIds = remember { mutableStateListOf<Int>() }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    // Clicked transaction details state
    var selectedDetailTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteConfirmForSingle by remember { mutableStateOf(false) }

    // Filter and Sort calculations
    val filteredTransactions = remember(
        transactions, searchQuery, selectedTypeFilter, selectedCategoryFilter, selectedWalletFilter,
        selectedDatePreset, customStartDateMillis, customEndDateMillis, sortBy
    ) {
        var result = transactions.asSequence()

        // 1. Filter by Search Query
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            result = result.filter {
                it.note.contains(q, ignoreCase = true)
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

        // 4. Filter by Wallet
        if (selectedWalletFilter != null) {
            result = result.filter {
                it.walletId == selectedWalletFilter || it.targetWalletId == selectedWalletFilter
            }
        }

        // 5. Filter by Date range preset
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
            "CUSTOM" -> {
                val start = customStartDateMillis
                val end = customEndDateMillis
                if (start != null) {
                    result = result.filter { it.date >= start }
                }
                if (end != null) {
                    val endOfDay = Calendar.getInstance().apply {
                        timeInMillis = end
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    result = result.filter { it.date <= endOfDay }
                }
            }
        }

        // 6. Sorting
        result = when (sortBy) {
            "DATE_DESC" -> result.sortedByDescending { it.date }
            "DATE_ASC" -> result.sortedBy { it.date }
            "AMOUNT_DESC" -> result.sortedByDescending { it.amount + it.adminFee }
            "AMOUNT_ASC" -> result.sortedBy { it.amount + it.adminFee }
            else -> result.sortedByDescending { it.date }
        }

        result.toList()
    }

    // Active filter count
    val activeFilterCount = remember(
        selectedTypeFilter, selectedCategoryFilter, selectedWalletFilter, selectedDatePreset, sortBy
    ) {
        var count = 0
        if (selectedTypeFilter != "ALL") count++
        if (selectedCategoryFilter != null) count++
        if (selectedWalletFilter != null) count++
        if (selectedDatePreset != "ALL_TIME") count++
        if (sortBy != "DATE_DESC") count++
        count
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

            // =========================================================================
            // SKETCH TOP ROW:
            // [ Long Search Box ] + [ Rounded Filter Menu Box ]
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Long Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isId) "Cari catatan transaksi..." else "Search transactions note...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search_custom),
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close_custom),
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )

                // 2. Rounded Filter Box (Sketch: box kecil tidak lancip / rounded square)
                val filterButtonShape = RoundedCornerShape(16.dp)
                Surface(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(filterButtonShape),
                    shape = filterButtonShape,
                    color = if (activeFilterCount > 0) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(
                        width = if (activeFilterCount > 0) 1.5.dp else 1.dp,
                        color = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    shadowElevation = if (activeFilterCount > 0) 2.dp else 0.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_filter_custom),
                            contentDescription = if (isId) "Menu Filter & Urutan" else "Filter & Sort Menu",
                            tint = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )

                        // Badge counter if active filters exist
                        if (activeFilterCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeFilterCount.toString(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // SKETCH DIVIDER LINE (garis di bawah box search & box filter)
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Active Filter Chips Summary (if any filters applied, allow fast 1-tap clear)
            if (activeFilterCount > 0) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Surface(
                            onClick = {
                                selectedTypeFilter = "ALL"
                                selectedCategoryFilter = null
                                selectedWalletFilter = null
                                selectedDatePreset = "ALL_TIME"
                                customStartDateMillis = null
                                customEndDateMillis = null
                                sortBy = "DATE_DESC"
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isId) "Reset Semua" else "Reset All",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (selectedTypeFilter != "ALL") {
                        item {
                            ActiveFilterChipTag(
                                label = when (selectedTypeFilter) {
                                    "EXPENSE" -> if (isId) "Pengeluaran" else "Expense"
                                    "INCOME" -> if (isId) "Pemasukan" else "Income"
                                    "TRANSFER" -> if (isId) "Transfer" else "Transfer"
                                    else -> selectedTypeFilter
                                },
                                onClear = { selectedTypeFilter = "ALL" }
                            )
                        }
                    }

                    if (selectedDatePreset != "ALL_TIME") {
                        item {
                            ActiveFilterChipTag(
                                label = when (selectedDatePreset) {
                                    "TODAY" -> if (isId) "Hari Ini" else "Today"
                                    "LAST_7" -> if (isId) "7 Hari Terakhir" else "Last 7 Days"
                                    "THIS_MONTH" -> if (isId) "Bulan Ini" else "This Month"
                                    "CUSTOM" -> if (isId) "Rentang Kustom" else "Custom Range"
                                    else -> selectedDatePreset
                                },
                                onClear = {
                                    selectedDatePreset = "ALL_TIME"
                                    customStartDateMillis = null
                                    customEndDateMillis = null
                                }
                            )
                        }
                    }

                    if (selectedCategoryFilter != null) {
                        val cat = categories.firstOrNull { it.id == selectedCategoryFilter }
                        if (cat != null) {
                            item {
                                ActiveFilterChipTag(
                                    label = "${if (isId) "Kategori" else "Category"}: ${cat.name}",
                                    onClear = { selectedCategoryFilter = null }
                                )
                            }
                        }
                    }

                    if (selectedWalletFilter != null) {
                        val w = wallets.firstOrNull { it.id == selectedWalletFilter }
                        if (w != null) {
                            item {
                                ActiveFilterChipTag(
                                    label = "${if (isId) "Dompet" else "Wallet"}: ${w.name}",
                                    onClear = { selectedWalletFilter = null }
                                )
                            }
                        }
                    }

                    if (sortBy != "DATE_DESC") {
                        item {
                            ActiveFilterChipTag(
                                label = when (sortBy) {
                                    "DATE_ASC" -> if (isId) "Terlama" else "Oldest"
                                    "AMOUNT_DESC" -> if (isId) "Nominal Tertinggi" else "Highest Amount"
                                    "AMOUNT_ASC" -> if (isId) "Nominal Terendah" else "Lowest Amount"
                                    else -> sortBy
                                },
                                onClear = { sortBy = "DATE_DESC" }
                            )
                        }
                    }
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
                            painter = painterResource(id = R.drawable.ic_receipt_custom),
                            contentDescription = null,
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
                                    val visualInfo = CategoryVisuals.getVisualInfo(
                                        categoryName = categoryOfTx?.name,
                                        transactionType = txn.type,
                                        note = txn.note
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(if (isFresh) RoundedCornerShape(12.dp) else RoundedCornerShape(10.dp))
                                            .background(visualInfo.backgroundColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = visualInfo.iconRes),
                                            contentDescription = categoryOfTx?.name ?: txn.type,
                                            tint = visualInfo.iconColor,
                                            modifier = Modifier.size(20.dp)
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
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = viewModel.formatDate(txn.date),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                            )
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
                                val listDisplayAmount = if (txn.type == "EXPENSE" || txn.type == "TRANSFER") {
                                    txn.amount + txn.adminFee
                                } else {
                                    txn.amount
                                }

                                Text(
                                    text = "$formatSign ${viewModel.formatRupiah(listDisplayAmount)}",
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
                            "Pilih cara hapus ${selectedTxIds.size} transaksi terpilih:" 
                        else "Delete ${selectedTxIds.size} selected transactions:",
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
                            Toast.makeText(context, if (isId) "${toDelete.size} catatan dihapus!" else "${toDelete.size} logs deleted!", Toast.LENGTH_SHORT).show()
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
                                    "Hapus catatan riwayat saja. Saldo dompet tidak berubah."
                                else "Removes records only. Wallet balances unchanged.",
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
                            Toast.makeText(context, if (isId) "${toDelete.size} transaksi dibatalkan & dana kembali!" else "${toDelete.size} transactions cancelled & restored!", Toast.LENGTH_SHORT).show()
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
                                    "Batalkan transaksi dan pulihkan saldo masing-masing dompet."
                                else "Cancel transactions and restore funds to wallets.",
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

        TransactionDetailDialog(
            transaction = txn,
            wallet = walletOfTx,
            targetWallet = targetWalletOfTx,
            category = categoryOfTx,
            viewModel = viewModel,
            onDismiss = { selectedDetailTransaction = null },
            onDelete = {
                showDeleteConfirmForSingle = true
            }
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
                            "Pilih cara hapus transaksi:" 
                        else "Choose deletion method:",
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
                TextButton(onClick = { showDeleteConfirmForSingle = false }) {
                    Text(if (isId) "Batal" else "Cancel")
                }
            }
        )
    }

    // =========================================================================
    // MODAL BOTTOM SHEET / FILTER & SORT MENU (BOX KECIL TIDAK LANCIP DARI SKETSA)
    // Berisi: Tipe Transaksi, Tanggal, Pengurutan, Kategori, & Kategori Dompet
    // =========================================================================
    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Sheet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_filter_custom),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (isId) "Filter & Urutan Transaksi" else "Filter & Sort Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isId) "Sesuaikan tampilan riwayat transaksi" else "Refine your transaction ledger view",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (activeFilterCount > 0) {
                        TextButton(
                            onClick = {
                                selectedTypeFilter = "ALL"
                                selectedCategoryFilter = null
                                selectedWalletFilter = null
                                selectedDatePreset = "ALL_TIME"
                                customStartDateMillis = null
                                customEndDateMillis = null
                                sortBy = "DATE_DESC"
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isId) "Reset" else "Reset",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 0.8.dp
                )

                // Tipe Transaksi
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isId) "Tipe Transaksi" else "Transaction Type",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val types = listOf(
                                "ALL" to (if (isId) "Semua" else "All"),
                                "EXPENSE" to (if (isId) "Pengeluaran" else "Expense"),
                                "INCOME" to (if (isId) "Pemasukan" else "Income"),
                                "TRANSFER" to (if (isId) "Transfer" else "Transfer")
                            )
                            types.forEach { (typeKey, label) ->
                                val isSelected = selectedTypeFilter == typeKey
                                Surface(
                                    onClick = { selectedTypeFilter = typeKey },
                                    modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tanggal Transaksi
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isId) "Tanggal Transaksi" else "Transaction Date",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (selectedDatePreset == "CUSTOM" && customStartDateMillis != null) {
                            val sdf = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }
                            val startStr = sdf.format(Date(customStartDateMillis!!))
                            val endStr = customEndDateMillis?.let { sdf.format(Date(it)) } ?: startStr
                            Text(
                                text = "$startStr - $endStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val datePresets = listOf(
                        "ALL_TIME" to (if (isId) "Semua Waktu" else "All Time"),
                        "TODAY" to (if (isId) "Hari Ini" else "Today"),
                        "LAST_7" to (if (isId) "7 Hari Terakhir" else "Last 7 Days"),
                        "THIS_MONTH" to (if (isId) "Bulan Ini" else "This Month"),
                        "CUSTOM" to (if (isId) "Rentang Kustom..." else "Custom Range...")
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(datePresets) { (presetKey, label) ->
                            val isSelected = selectedDatePreset == presetKey
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedDatePreset = presetKey
                                    if (presetKey == "CUSTOM" && customStartDateMillis == null) {
                                        val cal = Calendar.getInstance()
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                cal.set(y, m, d, 0, 0, 0)
                                                customStartDateMillis = cal.timeInMillis
                                                if (customEndDateMillis == null) {
                                                    customEndDateMillis = System.currentTimeMillis()
                                                }
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                leadingIcon = if (presetKey == "CUSTOM") {
                                    {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_calendar_custom),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            )
                        }
                    }

                    // If custom date range selected, show start & end date selector inputs
                    if (selectedDatePreset == "CUSTOM") {
                        val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Start Date
                            Surface(
                                onClick = {
                                    val cal = Calendar.getInstance().apply {
                                        customStartDateMillis?.let { timeInMillis = it }
                                    }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            cal.set(y, m, d, 0, 0, 0)
                                            customStartDateMillis = cal.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_calendar_custom),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = customStartDateMillis?.let { sdf.format(Date(it)) } ?: (if (isId) "Dari Tanggal" else "Start Date"),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // End Date
                            Surface(
                                onClick = {
                                    val cal = Calendar.getInstance().apply {
                                        customEndDateMillis?.let { timeInMillis = it }
                                    }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            cal.set(y, m, d, 23, 59, 59)
                                            customEndDateMillis = cal.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_calendar_custom),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = customEndDateMillis?.let { sdf.format(Date(it)) } ?: (if (isId) "Sampai Tanggal" else "End Date"),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Urutkan Berdasarkan
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isId) "Urutkan Berdasarkan" else "Sort Order",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val sortOptions = listOf(
                        "DATE_DESC" to (if (isId) "Tanggal Terbaru" else "Newest First"),
                        "DATE_ASC" to (if (isId) "Tanggal Terlama" else "Oldest First"),
                        "AMOUNT_DESC" to (if (isId) "Nominal Tertinggi" else "Highest Amount"),
                        "AMOUNT_ASC" to (if (isId) "Nominal Terendah" else "Lowest Amount")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sortOptions.take(2).forEach { (sortKey, label) ->
                            val isSelected = sortBy == sortKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { sortBy = sortKey },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
                                leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sortOptions.drop(2).forEach { (sortKey, label) ->
                            val isSelected = sortBy == sortKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { sortBy = sortKey },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
                                leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Kategori Transaksi
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isId) "Kategori Transaksi" else "Categories",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (selectedCategoryFilter != null) {
                            TextButton(
                                onClick = { selectedCategoryFilter = null },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text(if (isId) "Reset Kategori" else "Reset Category", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    val filteredCatList = when (selectedTypeFilter) {
                        "EXPENSE" -> categories.filter { it.type == "EXPENSE" }
                        "INCOME" -> categories.filter { it.type == "INCOME" }
                        else -> categories
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null },
                                label = { Text(if (isId) "Semua Kategori" else "All Categories", style = MaterialTheme.typography.labelMedium) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            )
                        }
                        items(filteredCatList, key = { it.id }) { cat ->
                            val isSelected = selectedCategoryFilter == cat.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategoryFilter = if (isSelected) null else cat.id
                                },
                                label = { Text(cat.name, style = MaterialTheme.typography.labelMedium) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            )
                        }
                    }
                }

                // Kategori Berdasarkan Dompet
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isId) "Dompet (Wallet)" else "Wallet",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (selectedWalletFilter != null) {
                            TextButton(
                                onClick = { selectedWalletFilter = null },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text(if (isId) "Reset Dompet" else "Reset Wallet", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedWalletFilter == null,
                                onClick = { selectedWalletFilter = null },
                                label = { Text(if (isId) "Semua Dompet" else "All Wallets", style = MaterialTheme.typography.labelMedium) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_wallet_custom),
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            )
                        }
                        items(wallets, key = { it.id }) { wallet ->
                            val isSelected = selectedWalletFilter == wallet.id
                            val walletIcon = when (wallet.icon) {
                                "bank" -> R.drawable.ic_wallet_type_bank
                                "wallet" -> R.drawable.ic_wallet_type_wallet
                                "savings" -> R.drawable.ic_wallet_type_savings
                                else -> R.drawable.ic_wallet_type_cash
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedWalletFilter = if (isSelected) null else wallet.id
                                },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(wallet.name, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                        Text(
                                            text = "(${viewModel.formatRupiah(wallet.balance)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = walletIcon),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Apply button
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isId) "Terapkan Filter (${filteredTransactions.size} Transaksi)" else "Apply Filter (${filteredTransactions.size} Items)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun ActiveFilterChipTag(
    label: String,
    onClear: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hapus filter",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
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
