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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.model.Wallet
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletsScreen(viewModel: FinanceViewModel) {
    val wallets by viewModel.wallets.collectAsState()
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"
    
    var showAddWalletDialog by remember { mutableStateOf(false) }
    var selectedWalletToInteract by remember { mutableStateOf<Wallet?>(null) }

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
                        Icons.Default.AccountBalanceWallet,
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
                    Button(onClick = { showAddWalletDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
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
                    text = if (isId) "Ketuk dompet untuk menghapusnya atau sesuaikan saldo." else "Tap a wallet to delete it or adjust its balance.",
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
                            onClick = { selectedWalletToInteract = wallet },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            // Small Floating Fab to Add Wallet when list is not empty
            FloatingActionButton(
                onClick = { showAddWalletDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_wallet_fab"),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = if (isId) "Tambah Dompet" else "Add Wallet")
            }
        }
    }

    if (showAddWalletDialog) {
        AddWalletDialog(
            viewModel = viewModel,
            onDismiss = { showAddWalletDialog = false }
        )
    }

    selectedWalletToInteract?.let { wallet ->
        AlertDialog(
            onDismissRequest = { selectedWalletToInteract = null },
            title = { Text(if (isId) "Hapus Dompet: ${wallet.name}?" else "Delete Wallet: ${wallet.name}?") },
            text = { Text(if (isId) "Apakah Anda yakin ingin menghapus dompet ini? Menghapusnya tidak akan menghapus riwayat transaksi otomatis, tetapi tidak akan tersedia lagi sebagai pilihan." else "Are you sure you want to delete this wallet? Deleting it will not automatically delete transaction histories, but it will no longer be available as a choice.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWallet(wallet)
                        selectedWalletToInteract = null
                    }
                ) {
                    Text(if (isId) "Hapus Dompet" else "Delete Wallet", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedWalletToInteract = null }) {
                    Text(if (isId) "Batal" else "Cancel")
                }
            }
        )
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

    val iconVector = when (wallet.icon) {
        "bank" -> Icons.Default.AccountBalance
        "wallet" -> Icons.Default.CreditCard
        "savings" -> Icons.Default.Savings
        else -> Icons.Default.Payments // CASH
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
                            iconVector,
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
                                Triple("cash", Icons.Default.Payments, if (isId) "Tunai" else "Cash"),
                                Triple("bank", Icons.Default.AccountBalance, "Bank"),
                                Triple("wallet", Icons.Default.CreditCard, "E-Money"),
                                Triple("savings", Icons.Default.Savings, if (isId) "Tabungan" else "Savings")
                            )

                            icons.forEach { (key, icon, label) ->
                                val isSelected = selectedIcon == key
                                OutlinedIconContainerButton(
                                    icon = icon,
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
                        enabled = walletName.trim().isNotEmpty()
                    ) {
                        Text(if (isId) "Simpan" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun OutlinedIconContainerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                icon,
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
