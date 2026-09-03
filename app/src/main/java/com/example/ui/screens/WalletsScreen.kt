package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.model.Wallet
import com.example.ui.viewmodel.FinanceViewModel
import androidx.compose.ui.res.painterResource
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletsScreen(viewModel: FinanceViewModel) {
    val wallets by viewModel.wallets.collectAsState()
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"
    
    var showAddWalletDialog by remember { mutableStateOf(false) }
    var selectedWalletForDetail by remember { mutableStateOf<Wallet?>(null) }
    var selectedWalletForEdit by remember { mutableStateOf<Wallet?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (wallets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_wallet_custom),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (isId) "Dompet tidak ditemukan." else "No wallets found.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isId) "Buat dompet baru untuk mencatat saldo awal Anda." else "Create a wallet to record your initial balance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showAddWalletDialog = true },
                        modifier = Modifier.testTag("create_wallet_btn")
                    ) {
                        Icon(painterResource(id = R.drawable.ic_add_custom), contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isId) "Tambah Dompet Baru" else "Add New Wallet")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isId) "Dompet & Akun Saya" else "My Wallets & Accounts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isId) "Ketuk dompet untuk melihat rincian saldo atau mengeditnya." else "Tap a wallet to view balance details or edit it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(wallets, key = { it.id }) { wallet ->
                        WalletGridCard(
                            wallet = wallet,
                            viewModel = viewModel,
                            onClick = { selectedWalletForDetail = wallet },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            // Floating Fab to Add Wallet when list is not empty
            if (!showAddWalletDialog && selectedWalletForDetail == null) {
                FloatingActionButton(
                    onClick = { showAddWalletDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                        .testTag("add_wallet_fab"),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(painterResource(id = R.drawable.ic_add_custom), contentDescription = if (isId) "Tambah Dompet" else "Add Wallet")
                }
            }
        }
    }

    if (showAddWalletDialog) {
        AddWalletDialog(
            viewModel = viewModel,
            onDismiss = { showAddWalletDialog = false }
        )
    }

    selectedWalletForDetail?.let { wallet ->
        WalletDetailDialog(
            wallet = wallet,
            viewModel = viewModel,
            onDismiss = { selectedWalletForDetail = null },
            onEditRequest = {
                selectedWalletForDetail = null
                selectedWalletForEdit = wallet
            }
        )
    }

    selectedWalletForEdit?.let { wallet ->
        EditWalletDialog(
            wallet = wallet,
            viewModel = viewModel,
            onDismiss = { selectedWalletForEdit = null }
        )
    }
}

@Composable
fun WalletDetailDialog(
    wallet: Wallet,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onEditRequest: () -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val dialogWidth = if (screenWidth < 600) (screenWidth * 0.94).dp else 500.dp

    val iconPainter = when (wallet.icon) {
        "bank" -> painterResource(id = R.drawable.ic_wallet_type_bank)
        "wallet" -> painterResource(id = R.drawable.ic_wallet_type_wallet)
        "savings" -> painterResource(id = R.drawable.ic_wallet_type_savings)
        else -> painterResource(id = R.drawable.ic_wallet_type_cash)
    }

    val typeLabel = when (wallet.icon) {
        "bank" -> "Bank"
        "wallet" -> "E-Money"
        "savings" -> if (isId) "Tabungan" else "Savings"
        else -> if (isId) "Tunai / Cash" else "Cash"
    }

    val gradientBrush = when (wallet.icon) {
        "bank" -> Brush.verticalGradient(listOf(Color(0xFF1E88E5), Color(0xFF1565C0)))
        "wallet" -> Brush.verticalGradient(listOf(Color(0xFF8E24AA), Color(0xFF5E35B1)))
        "savings" -> Brush.verticalGradient(listOf(Color(0xFF43A047), Color(0xFF2E7D32)))
        else -> Brush.verticalGradient(listOf(Color(0xFFFFA000), Color(0xFFF57C00)))
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(if (isId) "Hapus Dompet: ${wallet.name}?" else "Delete Wallet: ${wallet.name}?") },
            text = { Text(if (isId) "Apakah Anda yakin ingin menghapus dompet ini? Riwayat transaksi lama tetap tersimpan." else "Are you sure you want to delete this wallet? Previous transaction records will remain.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWallet(wallet)
                        showDeleteConfirm = false
                        onDismiss()
                    }
                ) {
                    Text(if (isId) "Ya, Hapus" else "Yes, Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(if (isId) "Batal" else "Cancel")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .width(dialogWidth)
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp)
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
                        text = if (isId) "Informasi Dompet" else "Wallet Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Visual Hero Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(gradientBrush)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        iconPainter,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = typeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = wallet.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = viewModel.formatRupiah(wallet.balance),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Balance summary description
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wallet_custom),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isId) 
                                "Saldo aktif dapat digunakan langsung untuk pencatatan transaksi masuk, keluar, atau transfer."
                            else 
                                "Active balance is ready for tracking income, expense, and transfer transactions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Actions Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Single Delete Button
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete_custom),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isId) "Hapus" else "Delete")
                    }

                    // Edit Button
                    Button(
                        onClick = onEditRequest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit_custom),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isId) "Edit Dompet" else "Edit Wallet")
                    }
                }
            }
        }
    }
}

@Composable
fun EditWalletDialog(
    wallet: Wallet,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    var walletName by remember(wallet) { mutableStateOf(wallet.name) }
    var balanceStr by remember(wallet) { 
        mutableStateOf(if (wallet.balance % 1.0 == 0.0) wallet.balance.toLong().toString() else wallet.balance.toString()) 
    }
    var selectedIcon by remember(wallet) { mutableStateOf(wallet.icon) }

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
                    text = if (isId) "Edit Dompet / Akun" else "Edit Wallet / Account",
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
                            value = walletName,
                            onValueChange = { walletName = it },
                            label = { Text(if (isId) "Nama Dompet / Akun" else "Wallet / Account Name") },
                            placeholder = { Text(if (isId) "misal: Rekening Bank, Tunai, E-wallet" else "e.g. Bank Account, Cash, E-wallet") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = balanceStr,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) balanceStr = it },
                            label = { Text(if (isId) "Saldo Dompet" else "Wallet Balance") },
                            prefix = { Text("Rp ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("0") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text(if (isId) "Tipe / Ikon Dompet:" else "Wallet Type / Icon:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val icons = listOf(
                                Triple("cash", painterResource(id = R.drawable.ic_wallet_type_cash), if (isId) "Tunai" else "Cash"),
                                Triple("bank", painterResource(id = R.drawable.ic_wallet_type_bank), "Bank"),
                                Triple("wallet", painterResource(id = R.drawable.ic_wallet_type_wallet), "E-Money"),
                                Triple("savings", painterResource(id = R.drawable.ic_wallet_type_savings), if (isId) "Tabungan" else "Savings")
                            )

                            icons.forEach { (key, painter, label) ->
                                val isSelected = selectedIcon == key
                                OutlinedIconContainerButton(
                                    painter = painter,
                                    label = label,
                                    isSelected = isSelected,
                                    onClick = { selectedIcon = key },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isId) "Batal" else "Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = {
                            val balanceVal = balanceStr.toDoubleOrNull() ?: 0.0
                            if (walletName.trim().isNotEmpty()) {
                                viewModel.updateWallet(
                                    wallet.copy(
                                        name = walletName.trim(),
                                        balance = balanceVal,
                                        icon = selectedIcon
                                    )
                                )
                                onDismiss()
                            }
                        },
                        enabled = walletName.trim().isNotEmpty(),
                        modifier = Modifier.weight(1.4f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isId) "Simpan Perubahan" else "Save Changes", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun WalletGridCard(
    wallet: Wallet,
    viewModel: FinanceViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    val gradientBrush = when (wallet.icon) {
        "bank" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF1E88E5), Color(0xFF1565C0))
        )
        "wallet" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF8E24AA), Color(0xFF5E35B1))
        )
        "savings" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF43A047), Color(0xFF2E7D32))
        )
        else -> Brush.verticalGradient(
            colors = listOf(Color(0xFFFFA000), Color(0xFFF57C00)) // CASH
        )
    }

    val iconPainter = when (wallet.icon) {
        "bank" -> painterResource(id = R.drawable.ic_wallet_type_bank)
        "wallet" -> painterResource(id = R.drawable.ic_wallet_type_wallet)
        "savings" -> painterResource(id = R.drawable.ic_wallet_type_savings)
        else -> painterResource(id = R.drawable.ic_wallet_type_cash) // CASH
    }

    val uiStyle by viewModel.uiStyle.collectAsState()
    val isFresh = uiStyle == "FRESH"
    val cardShape = RoundedCornerShape(24.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(cardShape)
            .clickable { onClick() }
            .then(
                if (isFresh) {
                    Modifier.border(
                        BorderStroke(1.5.dp, Color.White.copy(alpha = 0.22f)),
                        cardShape
                    )
                } else {
                    Modifier
                }
            ),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFresh) 4.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(16.dp)
        ) {
            // Background canvas digital decoration circle
            if (isFresh) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.12f)
                ) {
                    drawCircle(
                        color = Color.White,
                        radius = size.width * 0.42f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.15f)
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            iconPainter,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Text(
                        text = when (wallet.icon) {
                            "bank" -> "Bank"
                            "wallet" -> "E-Money"
                            "savings" -> if (isId) "Tabungan" else "Savings"
                            else -> if (isId) "Tunai" else "Cash"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text(
                        text = wallet.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = viewModel.formatRupiah(wallet.balance),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun AddWalletDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    var walletName by remember { mutableStateOf("") }
    var initialBalanceStr by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("cash") } // "cash", "bank", "wallet", "savings"

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
                    text = if (isId) "Tambah Dompet / Akun" else "Add Wallet / Account",
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
                            value = walletName,
                            onValueChange = { walletName = it },
                            label = { Text(if (isId) "Nama Dompet / Akun" else "Wallet / Account Name") },
                            placeholder = { Text(if (isId) "misal: Rekening Bank, Tunai, E-wallet" else "e.g. Bank Account, Cash, E-wallet") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = initialBalanceStr,
                            onValueChange = { if (it.all { char -> char.isDigit() }) initialBalanceStr = it },
                            label = { Text(if (isId) "Saldo Awal" else "Starting Balance") },
                            prefix = { Text("Rp ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("0") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text(if (isId) "Tipe / Ikon Dompet:" else "Wallet Type / Icon:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val icons = listOf(
                                Triple("cash", painterResource(id = R.drawable.ic_wallet_type_cash), if (isId) "Tunai" else "Cash"),
                                Triple("bank", painterResource(id = R.drawable.ic_wallet_type_bank), "Bank"),
                                Triple("wallet", painterResource(id = R.drawable.ic_wallet_type_wallet), "E-Money"),
                                Triple("savings", painterResource(id = R.drawable.ic_wallet_type_savings), if (isId) "Tabungan" else "Savings")
                            )

                            icons.forEach { (key, painter, label) ->
                                val isSelected = selectedIcon == key
                                OutlinedIconContainerButton(
                                    painter = painter,
                                    label = label,
                                    isSelected = isSelected,
                                    onClick = { selectedIcon = key },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isId) "Batal" else "Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = {
                            val balanceVal = initialBalanceStr.toDoubleOrNull() ?: 0.0
                            if (walletName.trim().isNotEmpty()) {
                                viewModel.addWallet(
                                    name = walletName,
                                    balance = balanceVal,
                                    icon = selectedIcon
                                )
                                onDismiss()
                            }
                        },
                        enabled = walletName.trim().isNotEmpty(),
                        modifier = Modifier.weight(1.4f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isId) "Tambah Dompet" else "Add Wallet", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun OutlinedIconContainerButton(
    painter: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(12.dp)
    Card(
        modifier = modifier
            .padding(2.dp)
            .clip(cardShape)
            .clickable { onClick() },
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painter,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
