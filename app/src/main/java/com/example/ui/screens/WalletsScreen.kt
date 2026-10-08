package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Wallet
import com.example.ui.components.AddWalletModal
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel

// Distinctive luxury gradient themes for cards
private val WalletGradients = listOf(
    listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6)), // Deep Blue -> Electric Blue (Mandiri / Bank)
    listOf(Color(0xFF0F766E), Color(0xFF14B8A6)), // Dark Teal -> Emerald (GoPay / E-Money)
    listOf(Color(0xFF581C87), Color(0xFF8B5CF6)), // Royal Purple -> Violet (Jago Main)
    listOf(Color(0xFFC2410C), Color(0xFFF97316)), // Deep Orange -> Orange (ShopeePay)
    listOf(Color(0xFF065F46), Color(0xFF10B981)), // Dark Green -> Emerald (Cash)
    listOf(Color(0xFF1E293B), Color(0xFF475569)), // Slate -> Steel (Savings)
    listOf(Color(0xFF831843), Color(0xFFEC4899))  // Wine -> Pink (Pocket)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletsScreen(viewModel: FinanceViewModel) {
    val wallets by viewModel.wallets.collectAsState()
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    var showAddWalletModal by remember { mutableStateOf(false) }
    var selectedWalletForDetail by remember { mutableStateOf<Wallet?>(null) }
    var selectedWalletForEdit by remember { mutableStateOf<Wallet?>(null) }

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
            // Header with Add Wallet button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isId) "Dompet & Akun Saya" else "My Wallets & Accounts",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = if (isId) "${wallets.size} dompet terhubung" else "${wallets.size} connected wallets",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = { showAddWalletModal = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isId) "Tambah Dompet" else "Add Wallet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Grid 2 Kolom Card Dompet/Bank
            if (wallets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wallet_custom),
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = TextSecondary.copy(alpha = 0.4f)
                        )
                        Text(
                            text = if (isId) "Belum ada dompet tersimpan." else "No wallets saved yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(wallets, key = { it.id }) { wallet ->
                        val index = wallets.indexOf(wallet)
                        val gradientColors = WalletGradients[index % WalletGradients.size]

                        WalletGridCard(
                            wallet = wallet,
                            gradientColors = gradientColors,
                            viewModel = viewModel,
                            onClick = { selectedWalletForDetail = wallet },
                            isId = isId
                        )
                    }
                }
            }
        }
    }

    if (showAddWalletModal) {
        AddWalletModal(
            viewModel = viewModel,
            onDismiss = { showAddWalletModal = false }
        )
    }

    // Detail Dialog
    selectedWalletForDetail?.let { wallet ->
        WalletDetailDialog(
            wallet = wallet,
            viewModel = viewModel,
            onDismiss = { selectedWalletForDetail = null },
            onEditRequest = {
                selectedWalletForDetail = null
                selectedWalletForEdit = wallet
            },
            isId = isId
        )
    }

    // Edit Dialog
    selectedWalletForEdit?.let { wallet ->
        EditWalletDialog(
            wallet = wallet,
            viewModel = viewModel,
            onDismiss = { selectedWalletForEdit = null },
            isId = isId
        )
    }
}

/**
 * Grid 2 Kolom Card Dompet/Bank:
 * Tiap card memiliki warna gradien unik, badge kategori, ikon spesifik, dan sisa saldo.
 */
@Composable
fun WalletGridCard(
    wallet: Wallet,
    gradientColors: List<Color>,
    viewModel: FinanceViewModel,
    onClick: () -> Unit,
    isId: Boolean,
    modifier: Modifier = Modifier
) {
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
        else -> if (isId) "Tunai" else "Cash"
    }

    val cardShape = RoundedCornerShape(20.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(cardShape)
            .clickable { onClick() },
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Icon and Badge
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
                            painter = iconPainter,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Bottom: Name & Balance
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = wallet.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = viewModel.formatRupiah(wallet.balance),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun WalletDetailDialog(
    wallet: Wallet,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onEditRequest: () -> Unit,
    isId: Boolean
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(wallet.name, fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Saldo: ${viewModel.formatRupiah(wallet.balance)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = AccentTeal
                )
                Text(
                    text = if (isId) "Kategori: ${wallet.icon.uppercase()}" else "Category: ${wallet.icon.uppercase()}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onEditRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Edit")
                }
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isId) "Hapus" else "Delete")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isId) "Tutup" else "Close", color = TextSecondary)
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = CardBg,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(if (isId) "Hapus Dompet?" else "Delete Wallet?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    if (isId) "Apakah Anda yakin ingin menghapus dompet ini?" else "Are you sure you want to delete this wallet?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWallet(wallet)
                        showDeleteConfirm = false
                        onDismiss()
                        Toast.makeText(context, if (isId) "Dompet dihapus" else "Wallet deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text(if (isId) "Hapus" else "Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(if (isId) "Batal" else "Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun EditWalletDialog(
    wallet: Wallet,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    isId: Boolean
) {
    var name by remember { mutableStateOf(wallet.name) }
    var balanceStr by remember { mutableStateOf(wallet.balance.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(if (isId) "Edit Dompet" else "Edit Wallet", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isId) "Nama Dompet" else "Wallet Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { if (it.all { c -> c.isDigit() }) balanceStr = it },
                    label = { Text(if (isId) "Saldo" else "Balance") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
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
        },
        confirmButton = {
            Button(
                onClick = {
                    val newBalance = balanceStr.toDoubleOrNull() ?: wallet.balance
                    viewModel.updateWallet(wallet.copy(name = name.trim(), balance = newBalance))
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isId) "Batal" else "Cancel", color = TextSecondary)
            }
        }
    )
}
