package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.data.model.Wallet
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletsScreen(viewModel: FinanceViewModel) {
    val wallets by viewModel.wallets.collectAsState()
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
                        "Belum ada tempat menyimpan uang.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Mulai buat dompet Anda untuk mencatat saldo awal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showAddWalletDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tambah Dompet Baru")
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
                    text = "Daftar Dompet / Rekening Anda",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Ketuk kartu dompet untuk menghapus atau mengedit saldo Anda.",
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
                            onClick = { selectedWalletToInteract = wallet }
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
                Icon(Icons.Default.Add, contentDescription = "Tambah Dompet")
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
            title = { Text("Hapus Dompet: ${wallet.name}?") },
            text = { Text("Apakah Anda yakin ingin menghapus dompet ini? Menghapus dompet tidak akan menghapus riwayat transaksi secara otomatis, namun dompet tidak akan muncul lagi di pilihan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWallet(wallet)
                        selectedWalletToInteract = null
                    }
                ) {
                    Text("Hapus Dompet", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedWalletToInteract = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun WalletGridCard(
    wallet: Wallet,
    viewModel: FinanceViewModel,
    onClick: () -> Unit
) {
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
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
                            "savings" -> "Tabungan"
                            else -> "Cash"
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
    var walletName by remember { mutableStateOf("") }
    var initialBalanceStr by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("cash") } // "cash", "bank", "wallet", "savings"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Dompet / Sumber Dana", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = walletName,
                    onValueChange = { walletName = it },
                    label = { Text("Nama Dompet / Rekening") },
                    placeholder = { Text("Misal: Bank Mandiri, Dompet Saku, GoPay") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = initialBalanceStr,
                    onValueChange = { if (it.all { char -> char.isDigit() }) initialBalanceStr = it },
                    label = { Text("Saldo Awal") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Jenis Dompet / Ikon:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val icons = listOf(
                        Triple("cash", Icons.Default.Payments, "Cash"),
                        Triple("bank", Icons.Default.AccountBalance, "Bank"),
                        Triple("wallet", Icons.Default.CreditCard, "E-Money"),
                        Triple("savings", Icons.Default.Savings, "Tabungan")
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
        },
        confirmButton = {
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
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun OutlinedIconContainerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(2.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
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
