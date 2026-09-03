package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.Category
import com.example.data.model.Wallet
import com.example.ui.util.GeminiClient
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    viewModel: FinanceViewModel,
    wallets: List<Wallet>,
    categories: List<Category>,
    initialScannedReceipts: List<GeminiClient.ScanResult> = emptyList(),
    onDismiss: () -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"

    var amountStr by remember { mutableStateOf("") }
    var enableAdminFee by remember { mutableStateOf(false) }
    var adminFeeStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // "INCOME", "EXPENSE", "TRANSFER"
    var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: 0) }
    var selectedTargetWalletId by remember { mutableStateOf(wallets.getOrNull(1)?.id ?: wallets.firstOrNull()?.id ?: 0) }
    var selectedCategoryId by remember { mutableStateOf(0) }
    var note by remember { mutableStateOf("") }
    var scannedReceipts by remember { mutableStateOf<List<GeminiClient.ScanResult>>(initialScannedReceipts) }

    // Debt / Loan (Talangan & Split) states
    var enableDebtOption by remember { mutableStateOf(false) }
    // 3 debt modes:
    // "FULL_DEBT": Opsi 1 - Hutang 1 transaksi penuh (tidak pakai wallet apapun, masuk ke pengeluaran & tab hutang)
    // "SPLIT_DEBT": Opsi 2 - Bayar sebagian (uang kurang), pakai wallet dan hanya saldo yang dibayar yang terpotong, sisanya masuk hutang
    // "LEND_FRIEND": Opsi 3 - Talangi teman (piutang)
    var debtModeOption by remember { mutableStateOf("FULL_DEBT") }
    var debtPersonName by remember { mutableStateOf("") }
    var debtWalletPaidStr by remember { mutableStateOf("") } // Amount paid from wallet in SPLIT_DEBT
    var debtCustomAmountStr by remember { mutableStateOf("") } // Custom amount for LEND_FRIEND
    var debtDueDays by remember { mutableIntStateOf(7) } // 3, 7, 14, 30 days

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isScanningInsideDialog by remember { mutableStateOf(false) }
    var tempPhotoUriInsideDialog by remember { mutableStateOf<Uri?>(null) }

    val dialogCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = tempPhotoUriInsideDialog
            if (uri != null) {
                isScanningInsideDialog = true
                coroutineScope.launch {
                    try {
                        val results = GeminiClient.scanMultipleReceipts(context, listOf(uri))
                        if (results.isNotEmpty()) {
                            if (results.size == 1) {
                                val single = results.first()
                                if (single.amount > 0.0) amountStr = single.amount.toInt().toString()
                                if (single.note.isNotBlank()) note = single.note
                                if (single.type == "INCOME" || single.type == "EXPENSE") {
                                    selectedType = single.type
                                }
                            } else {
                                scannedReceipts = results
                            }
                            Toast.makeText(context, if (isId) "Pendeteksian struk selesai!" else "Receipt detection completed!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, if (isId) "Gagal mendeteksi rincian dari struk." else "No details detected from the receipt.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, (if (isId) "Gagal memindai struk: " else "Failed to scan receipt: ") + (e.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
                    } finally {
                        isScanningInsideDialog = false
                    }
                }
            }
        }
    }

    val dialogRequestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = try {
                val tempFile = File.createTempFile("receipt_cam_", ".jpg", context.cacheDir).apply {
                    createNewFile()
                    deleteOnExit()
                }
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
            } catch (e: Exception) {
                null
            }
            if (uri != null) {
                tempPhotoUriInsideDialog = uri
                try {
                    dialogCameraLauncher.launch(uri)
                } catch (e: Exception) {
                    Toast.makeText(context, (if (isId) "Gagal membuka kamera: " else "Failed to open camera: ") + (e.message ?: ""), Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, if (isId) "Izin kamera diperlukan untuk mengambil foto struk" else "Camera permission is required to take receipt photos", Toast.LENGTH_SHORT).show()
        }
    }

    val dialogPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isScanningInsideDialog = true
            coroutineScope.launch {
                try {
                    val results = GeminiClient.scanMultipleReceipts(context, uris)
                    if (results.isNotEmpty()) {
                        if (results.size == 1) {
                            val single = results.first()
                            if (single.amount > 0.0) amountStr = single.amount.toInt().toString()
                            if (single.note.isNotBlank()) note = single.note
                            if (single.type == "INCOME" || single.type == "EXPENSE") {
                                selectedType = single.type
                            }
                        } else {
                            scannedReceipts = results
                        }
                        Toast.makeText(context, if (isId) "Pendeteksian struk selesai!" else "Receipt detection completed!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, if (isId) "Gagal mendeteksi rincian dari struk." else "No details detected from the receipts.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, (if (isId) "Gagal memindai struk: " else "Failed to scan receipts: ") + (e.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
                } finally {
                    isScanningInsideDialog = false
                }
            }
        }
    }

    // Group categories
    val incomeCategoryList = remember(categories) { categories.filter { it.type == "INCOME" } }
    val expenseCategoryList = remember(categories) { categories.filter { it.type == "EXPENSE" } }

    // Keep state of selected category ID matched with type
    LaunchedEffect(selectedType, categories) {
        if (selectedType == "INCOME") {
            selectedCategoryId = incomeCategoryList.firstOrNull()?.id ?: 0
        } else if (selectedType == "EXPENSE") {
            selectedCategoryId = expenseCategoryList.firstOrNull()?.id ?: 0
        }
    }

    // Auto calculate default wallet payment when wallet changes or total amount changes in SPLIT_DEBT
    val currentSelectedWallet = wallets.firstOrNull { it.id == selectedWalletId }
    val parsedTotalAmount = amountStr.toDoubleOrNull() ?: 0.0

    LaunchedEffect(selectedWalletId, amountStr, enableDebtOption, debtModeOption) {
        if (enableDebtOption && debtModeOption == "SPLIT_DEBT" && debtWalletPaidStr.isEmpty()) {
            val walletBal = currentSelectedWallet?.balance ?: 0.0
            if (walletBal > 0.0 && parsedTotalAmount > 0.0) {
                val autoPaid = minOf(walletBal, parsedTotalAmount)
                debtWalletPaidStr = autoPaid.toInt().toString()
            }
        }
    }

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
                .fillMaxWidth(0.95f)
                .widthIn(max = 520.dp)
                .heightIn(max = (screenHeight * 0.90).dp)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isId) "Tambah Transaksi Baru" else "Add New Transaction",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // 1. Transaction Type Selector with Vibrant Gradients
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val items = listOf(
                        Triple("EXPENSE", if (isId) "Pengeluaran" else "Expense", listOf(Color(0xFFE53935), Color(0xFFC62828))),
                        Triple("INCOME", if (isId) "Pemasukan" else "Income", listOf(Color(0xFF43A047), Color(0xFF2E7D32))),
                        Triple("TRANSFER", if (isId) "Transfer" else "Transfer", listOf(Color(0xFF1E88E5), Color(0xFF1565C0)))
                    )
                    items.forEach { (type, label, gradientColors) ->
                        val isSelected = selectedType == type
                        val tabItemShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(tabItemShape)
                                .background(
                                    if (isSelected) Brush.horizontalGradient(gradientColors) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .clickable { selectedType = type }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Quick Scan Receipt Section (Camera & Gallery)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DocumentScanner,
                                        contentDescription = "Scan Struk",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = if (isId) "Pindai Struk Belanja (AI)" else "Scan Receipt (AI)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (isScanningInsideDialog) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            }
                        }
                        Text(
                            text = if (isId)
                                "Pindai struk untuk isi otomatis nominal & rincian."
                            else
                                "Scan receipt to auto-fill amount & details.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    dialogRequestPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                enabled = !isScanningInsideDialog,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Kamera",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isId) "Kamera" else "Camera",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    dialogPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isScanningInsideDialog,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "Galeri",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isId) "Galeri Foto" else "Gallery",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (scannedReceipts.isNotEmpty()) {
                    Text(
                        text = if (isId) "Rincian Transaksi Terdeteksi:" else "Detected Transactions Detail:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    scannedReceipts.forEachIndexed { index, receipt ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${if (isId) "Nota" else "Receipt"} #${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = {
                                            scannedReceipts = scannedReceipts.filterIndexed { idx, _ -> idx != index }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_delete_custom),
                                            contentDescription = "Hapus Nota",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = receipt.note,
                                    onValueChange = { newNote ->
                                        scannedReceipts = scannedReceipts.mapIndexed { idx, item ->
                                            if (idx == index) item.copy(note = newNote) else item
                                        }
                                    },
                                    label = { Text(if (isId) "Catatan / Toko" else "Note / Store") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = if (receipt.amount == 0.0) "" else receipt.amount.toInt().toString(),
                                    onValueChange = { newAmtStr ->
                                        val amtDouble = newAmtStr.toDoubleOrNull() ?: 0.0
                                        scannedReceipts = scannedReceipts.mapIndexed { idx, item ->
                                            if (idx == index) item.copy(amount = amtDouble) else item
                                        }
                                    },
                                    label = { Text(if (isId) "Jumlah (Uang)" else "Amount") },
                                    prefix = { Text("Rp ") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                } else {
                    // 2. Regular Single Amount Input
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amountStr = it },
                        label = { Text(if (isId) "Jumlah (Uang)" else "Amount (Money)") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("0") }
                    )

                    // 2b. Admin Fee Section (Modeled after Debt & Split collapsible toggle card)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (enableAdminFee) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header Row toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        enableAdminFee = !enableAdminFee
                                        if (!enableAdminFee) {
                                            adminFeeStr = ""
                                        }
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (enableAdminFee) {
                                                    Brush.horizontalGradient(listOf(Color(0xFF00897B), Color(0xFF00ACC1)))
                                                } else {
                                                    Brush.horizontalGradient(listOf(Color(0xFF757575).copy(alpha = 0.2f), Color(0xFF757575).copy(alpha = 0.2f)))
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = if (enableAdminFee) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = if (isId) "Biaya Admin" else "Admin Fee",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!enableAdminFee) {
                                            Text(
                                                text = if (isId) "Tambah biaya admin transaksi" else "Add extra transaction fee",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Switch(
                                    checked = enableAdminFee,
                                    onCheckedChange = {
                                        enableAdminFee = it
                                        if (!it) {
                                            adminFeeStr = ""
                                        }
                                    }
                                )
                            }

                            AnimatedVisibility(visible = enableAdminFee) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = adminFeeStr,
                                        onValueChange = { if (it.all { char -> char.isDigit() }) adminFeeStr = it },
                                        label = { Text(if (isId) "Nominal Biaya Admin" else "Admin Fee Amount") },
                                        prefix = { Text("Rp ") },
                                        placeholder = { Text("0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    // Quick chips for Admin Fee
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("1000" to "1.000", "2500" to "2.500", "6500" to "6.500", "10000" to "10.000").forEach { (valStr, label) ->
                                            val isSelected = adminFeeStr == valStr
                                            Surface(
                                                onClick = { adminFeeStr = valStr },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                                )
                                            ) {
                                                Text(
                                                    text = "Rp $label",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }

                                    val parsedAmt = amountStr.toDoubleOrNull() ?: 0.0
                                    val parsedFee = adminFeeStr.toDoubleOrNull() ?: 0.0

                                    if (parsedFee > 0.0 && parsedAmt > 0.0) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(if (isId) "💵 Nominal Pokok:" else "💵 Base Amount:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                    Text(viewModel.formatRupiah(parsedAmt), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(if (isId) "🏷️ Biaya Admin:" else "🏷️ Admin Fee:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                    Text(viewModel.formatRupiah(parsedFee), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                                }
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        if (selectedType == "TRANSFER") {
                                                            if (isId) "💳 Total Dipotong Dompet Asal:" else "💳 Total Deducted from Source:"
                                                        } else {
                                                            if (isId) "💳 Total Pengeluaran (+Admin):" else "💳 Total Deducted (+Admin):"
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        viewModel.formatRupiah(parsedAmt + parsedFee),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                if (selectedType == "TRANSFER") {
                                                    Text(
                                                        text = if (isId) "• Bersih masuk ke dompet tujuan: ${viewModel.formatRupiah(parsedAmt)}" else "• Net received in target wallet: ${viewModel.formatRupiah(parsedAmt)}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

                // 3. Wallets Selection (Gradient-styled Mini Wallet Cards)
                val isFullDebtActive = enableDebtOption && debtModeOption == "FULL_DEBT"

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isId) "Dompet Asal:" else "Source Wallet:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (isFullDebtActive) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = if (isId) "Tanpa Dompet (Hutang 100%)" else "No Wallet (100% Debt)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (wallets.isEmpty()) {
                        Text(
                            if (isId) "Belum ada dompet. Tambah dulu di menu Dompet." else "No wallets found. Please add in Wallets tab.",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(wallets, key = { it.id }) { w ->
                                WalletGradientChip(
                                    wallet = w,
                                    isSelected = selectedWalletId == w.id && !isFullDebtActive,
                                    isDisabled = isFullDebtActive,
                                    formattedBalance = viewModel.formatRupiah(w.balance),
                                    onClick = {
                                        if (!isFullDebtActive) {
                                            selectedWalletId = w.id
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. Specific to Transfer Destination Wallet
                if (selectedType == "TRANSFER") {
                    Text(if (isId) "Dompet Tujuan:" else "Destination Wallet:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(wallets, key = { it.id }) { w ->
                            WalletGradientChip(
                                wallet = w,
                                isSelected = selectedTargetWalletId == w.id,
                                isDisabled = false,
                                formattedBalance = viewModel.formatRupiah(w.balance),
                                onClick = { selectedTargetWalletId = w.id }
                            )
                        }
                    }
                }

                // 5. Category Selection (Only for Income and Expense)
                if (selectedType != "TRANSFER") {
                    Text(if (isId) "Kategori:" else "Category:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    val listToShow = if (selectedType == "INCOME") incomeCategoryList else expenseCategoryList
                    
                    if (listToShow.isEmpty()) {
                        Text(if (isId) "Kategori tidak ditemukan." else "No categories found.")
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(listToShow, key = { it.id }) { cat ->
                                SimpleCustomChip(
                                    text = cat.name,
                                    isSelected = selectedCategoryId == cat.id,
                                    onClick = { selectedCategoryId = cat.id }
                                )
                            }
                        }
                    }
                }

                // 6. Notes Input (only show if no scanned receipts are active)
                if (scannedReceipts.isEmpty()) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(if (isId) "Catatan Tambahan" else "Additional Note") },
                        placeholder = { Text(if (isId) "misal: Belanja warung, kopi, makan siang, dll." else "e.g., Grocery store, coffee, lunch, etc.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 7. HUTANG & TALANGAN SECTION (With 2 Clear Options & Gradient Design)
                if (selectedType == "EXPENSE") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (enableDebtOption) {
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            }
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            1.dp,
                            if (enableDebtOption) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header Row toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { enableDebtOption = !enableDebtOption },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (enableDebtOption) {
                                                    Brush.horizontalGradient(listOf(Color(0xFF8E24AA), Color(0xFF5E35B1)))
                                                } else {
                                                    Brush.horizontalGradient(listOf(Color(0xFF757575).copy(alpha = 0.2f), Color(0xFF757575).copy(alpha = 0.2f)))
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Handshake,
                                            contentDescription = null,
                                            tint = if (enableDebtOption) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = if (isId) "Hutang & Split" else "Debt & Split",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!enableDebtOption) {
                                            Text(
                                                text = if (isId) "Catat hutang / split bayar" else "Log as debt or split payment",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Switch(
                                    checked = enableDebtOption,
                                    onCheckedChange = { enableDebtOption = it }
                                )
                            }

                            AnimatedVisibility(visible = enableDebtOption) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // 2 PRIMARY HUTANG OPTIONS (Cards with gradient style)
                                    Text(
                                        text = if (isId) "PILIH OPSI HUTANG:" else "SELECT DEBT MODE:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )

                                    // Option 1 Card: Hutang 1 Transaksi Penuh (Tanpa Dompet)
                                    DebtModeOptionCard(
                                        title = if (isId) "1. Hutang Transaksi Penuh" else "1. Full Transaction Debt",
                                        subtitle = if (isId) 
                                            "Tanpa dompet, langsung ke tab Hutang." 
                                        else 
                                            "No wallet used, directly to Debts.",
                                        badge = if (isId) "Tanpa Dompet" else "No Wallet",
                                        isSelected = debtModeOption == "FULL_DEBT",
                                        gradientBrush = Brush.horizontalGradient(listOf(Color(0xFF8E24AA), Color(0xFF5E35B1))),
                                        icon = Icons.Default.CreditCard,
                                        onClick = { debtModeOption = "FULL_DEBT" }
                                    )

                                    // Option 2 Card: Bayar Sebagian / Uang Kurang (Split Dompet + Hutang)
                                    DebtModeOptionCard(
                                        title = if (isId) "2. Bayar Sebagian (Uang Kurang)" else "2. Partial Payment (Underfunded)",
                                        subtitle = if (isId) 
                                            "Potong saldo dompet, sisanya ke Hutang." 
                                        else 
                                            "Deduct balance, remainder to Debt.",
                                        badge = if (isId) "Split Dompet + Hutang" else "Split Wallet + Debt",
                                        isSelected = debtModeOption == "SPLIT_DEBT",
                                        gradientBrush = Brush.horizontalGradient(listOf(Color(0xFF1E88E5), Color(0xFF3949AB))),
                                        icon = Icons.Default.AccountBalanceWallet,
                                        onClick = { debtModeOption = "SPLIT_DEBT" }
                                    )

                                    // Option 3 Card: Talangi Teman (Piutang)
                                    DebtModeOptionCard(
                                        title = if (isId) "3. Talangi Teman (Piutang)" else "3. Paid for Friend (Loan)",
                                        subtitle = if (isId) 
                                            "Bayar penuh, porsi teman ke Piutang." 
                                        else 
                                            "Pay in full, friend's share to Loan.",
                                        badge = if (isId) "Piutang" else "Friend Loan",
                                        isSelected = debtModeOption == "LEND_FRIEND",
                                        gradientBrush = Brush.horizontalGradient(listOf(Color(0xFF00897B), Color(0xFF00695C))),
                                        icon = Icons.Default.Group,
                                        onClick = { debtModeOption = "LEND_FRIEND" }
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // SPECIFIC INPUT FIELDS BASED ON SELECTED DEBT MODE
                                    when (debtModeOption) {
                                        "FULL_DEBT" -> {
                                            // OPTION 1 DETAILS
                                            OutlinedTextField(
                                                value = debtPersonName,
                                                onValueChange = { debtPersonName = it },
                                                label = { Text(if (isId) "Nama Pemberi Hutang / Toko / Orang" else "Creditor / Store / Person Name") },
                                                placeholder = { Text(if (isId) "misal: Warung Bu Siti, Pak Budi, Toko Bangunan" else "e.g., Grocery store, Alex, John") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                                            )

                                            // Visual Breakdown Card for Option 1
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surface
                                                ),
                                                border = BorderStroke(1.dp, Color(0xFF8E24AA).copy(alpha = 0.3f))
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(if (isId) "🛒 Total Pengeluaran:" else "🛒 Total Expense:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                        Text(viewModel.formatRupiah(parsedTotalAmount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black)
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(if (isId) "💳 Potong Saldo Dompet:" else "💳 Deduct Wallet:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF43A047), fontWeight = FontWeight.SemiBold)
                                                        Text("Rp 0 (Tanpa Dompet)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF43A047), fontWeight = FontWeight.Bold)
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(if (isId) "📋 Masuk ke Tab Hutang:" else "📋 Logged to Debts Tab:", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
                                                        Text(viewModel.formatRupiah(parsedTotalAmount), style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828), fontWeight = FontWeight.Black)
                                                    }
                                                }
                                            }
                                        }

                                        "SPLIT_DEBT" -> {
                                            // OPTION 2 DETAILS: Partial payment
                                            val walletBal = currentSelectedWallet?.balance ?: 0.0
                                            val walletPaidVal = debtWalletPaidStr.toDoubleOrNull() ?: 0.0
                                            val remainingDebtVal = (parsedTotalAmount - walletPaidVal).coerceAtLeast(0.0)

                                            OutlinedTextField(
                                                value = debtPersonName,
                                                onValueChange = { debtPersonName = it },
                                                label = { Text(if (isId) "Nama Pemberi Hutang / Toko (Penerima Sisa)" else "Creditor / Store Name (Remainder)") },
                                                placeholder = { Text(if (isId) "misal: Warung Makan, Swalayan, Toko" else "e.g., Restaurant, Supermarket") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                                            )

                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedTextField(
                                                    value = debtWalletPaidStr,
                                                    onValueChange = { if (it.all { char -> char.isDigit() }) debtWalletPaidStr = it },
                                                    label = { Text(if (isId) "Nominal Dibayar dari Dompet" else "Amount Paid from Wallet") },
                                                    prefix = { Text("Rp ") },
                                                    placeholder = { Text(minOf(walletBal, parsedTotalAmount).toInt().toString()) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true,
                                                    supportingText = {
                                                        Text(
                                                            text = if (isId) 
                                                                "Dompet: ${currentSelectedWallet?.name ?: "Pilih Dompet"} (Saldo: ${viewModel.formatRupiah(walletBal)})"
                                                            else 
                                                                "Wallet: ${currentSelectedWallet?.name ?: "Select Wallet"} (Balance: ${viewModel.formatRupiah(walletBal)})"
                                                        )
                                                    }
                                                )

                                                // Quick action button to use all available wallet balance
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            val allWallet = minOf(walletBal, parsedTotalAmount)
                                                            debtWalletPaidStr = allWallet.toInt().toString()
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(
                                                            text = if (isId) "Pakai Semua Saldo (${viewModel.formatRupiah(walletBal)})" else "Use Full Balance (${viewModel.formatRupiah(walletBal)})",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }

                                            // Visual Breakdown Card for Option 2 (Split)
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surface
                                                ),
                                                border = BorderStroke(1.dp, Color(0xFF1E88E5).copy(alpha = 0.35f))
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(if (isId) "🛒 Total Pembelian:" else "🛒 Total Purchase:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                        Text(viewModel.formatRupiah(parsedTotalAmount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black)
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(if (isId) "💳 Terpotong dari ${currentSelectedWallet?.name ?: "Dompet"}:" else "💳 Deducted from ${currentSelectedWallet?.name ?: "Wallet"}:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1565C0), fontWeight = FontWeight.SemiBold)
                                                        Text(viewModel.formatRupiah(walletPaidVal), style = MaterialTheme.typography.bodySmall, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(if (isId) "⚠️ Sisa Kekurangan (Masuk Hutang):" else "⚠️ Remaining Underfunded (Debt):", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                                        Text(viewModel.formatRupiah(remainingDebtVal), style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828), fontWeight = FontWeight.Black)
                                                    }
                                                }
                                            }
                                        }

                                        "LEND_FRIEND" -> {
                                            // OPTION 3 DETAILS: Friend loan
                                            OutlinedTextField(
                                                value = debtPersonName,
                                                onValueChange = { debtPersonName = it },
                                                label = { Text(if (isId) "Nama Teman yang Ditalangi" else "Friend's Name") },
                                                placeholder = { Text(if (isId) "misal: Kevin, Andi, Rian" else "e.g., Alex, Kevin") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                                            )

                                            OutlinedTextField(
                                                value = debtCustomAmountStr,
                                                onValueChange = { if (it.all { char -> char.isDigit() }) debtCustomAmountStr = it },
                                                label = { Text(if (isId) "Nominal Talangan (Kosongkan = Sesuai Total)" else "Loaned Amount (Blank = Full Total)") },
                                                prefix = { Text("Rp ") },
                                                placeholder = { Text(amountStr.ifEmpty { "0" }) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = if (isId) 
                                                        "💡 Dompet dipotong penuh, talangan teman dicatat ke Piutang."
                                                    else 
                                                        "💡 Wallet deducted in full, friend's split logged to Loans.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Due Date Presets
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = if (isId) "Tenggat Waktu / Jatuh Tempo:" else "Due Date Target:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val dayOptions = listOf(
                                                3 to (if (isId) "3 Hari" else "3 Days"),
                                                7 to (if (isId) "1 Minggu" else "1 Week"),
                                                14 to (if (isId) "2 Minggu" else "2 Weeks"),
                                                30 to (if (isId) "1 Bulan" else "1 Month")
                                            )
                                            dayOptions.forEach { (days, label) ->
                                                val isSelected = debtDueDays == days
                                                Surface(
                                                    onClick = { debtDueDays = days },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                                    )
                                                ) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
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

                Spacer(modifier = Modifier.height(8.dp))

                // Actions: Cancel & Save
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
                        Text(
                            text = if (isId) "Batal" else "Cancel",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = {
                            val amountVal = amountStr.toDoubleOrNull() ?: 0.0
                            val adminFeeVal = if (enableAdminFee) (adminFeeStr.toDoubleOrNull() ?: 0.0) else 0.0

                            if (scannedReceipts.isNotEmpty()) {
                                // Save all scanned receipts
                                for (receipt in scannedReceipts) {
                                    if (receipt.amount > 0.0) {
                                        viewModel.addTransaction(
                                            amount = receipt.amount,
                                            type = receipt.type,
                                            walletId = selectedWalletId,
                                            categoryId = selectedCategoryId,
                                            note = receipt.note,
                                            date = System.currentTimeMillis(),
                                            targetWalletId = null
                                        )
                                    }
                                }
                            } else {
                                if (amountVal <= 0.0 && !enableDebtOption) return@Button

                                if (selectedType == "TRANSFER" && selectedWalletId == selectedTargetWalletId) {
                                    // Can't transfer to same wallet
                                    return@Button
                                }

                                // Check if user enabled Hutang / Split options
                                if (enableDebtOption && debtPersonName.trim().isNotEmpty()) {
                                    val dueTimestamp = System.currentTimeMillis() + (debtDueDays * 86400000L)
                                    
                                    when (debtModeOption) {
                                        "FULL_DEBT" -> {
                                            // OPSI 1: Hutang 1 transaksi penuh (tidak pakai wallet apapun)
                                            // 1. Catat ke tab hutang
                                            viewModel.addDebt(
                                                personName = debtPersonName.trim(),
                                                totalAmount = amountVal,
                                                dueDate = dueTimestamp,
                                                type = "HUTANG",
                                                notes = if (note.isNotBlank()) "$note (Hutang Penuh)" else "Hutang transaksi penuh"
                                            )
                                            // 2. Catat ke pengeluaran dengan walletId = 0 (Tanpa Dompet)
                                            if (amountVal > 0.0) {
                                                viewModel.addTransaction(
                                                    amount = amountVal,
                                                    type = "EXPENSE",
                                                    walletId = 0,
                                                    categoryId = selectedCategoryId,
                                                    note = if (note.isNotBlank()) "$note (Hutang ke ${debtPersonName.trim()})" else "Hutang ke ${debtPersonName.trim()}",
                                                    date = System.currentTimeMillis(),
                                                    targetWalletId = null,
                                                    adminFee = adminFeeVal
                                                )
                                            }
                                        }

                                        "SPLIT_DEBT" -> {
                                            // OPSI 2: Bayar sebagian pakai wallet, sisa kekurangan masuk hutang
                                            val paidFromWallet = (debtWalletPaidStr.toDoubleOrNull() ?: amountVal).coerceIn(0.0, amountVal)
                                            val remainingDebt = (amountVal - paidFromWallet).coerceAtLeast(0.0)

                                            // 1. Catat sisa kekurangan ke tab hutang
                                            if (remainingDebt > 0.0) {
                                                viewModel.addDebt(
                                                    personName = debtPersonName.trim(),
                                                    totalAmount = remainingDebt,
                                                    dueDate = dueTimestamp,
                                                    type = "HUTANG",
                                                    notes = if (note.isNotBlank()) "$note (Sisa kekurangan belanja)" else "Kekurangan bayar belanja"
                                                )
                                            }

                                            // 2. Catat transaksi bagian yang dibayar dari dompet (memotong saldo dompet)
                                            if (paidFromWallet > 0.0) {
                                                viewModel.addTransaction(
                                                    amount = paidFromWallet,
                                                    type = "EXPENSE",
                                                    walletId = selectedWalletId,
                                                    categoryId = selectedCategoryId,
                                                    note = if (note.isNotBlank()) "$note (Bayar dari dompet)" else "Bayar belanja (dari dompet)",
                                                    date = System.currentTimeMillis(),
                                                    targetWalletId = null,
                                                    adminFee = adminFeeVal
                                                )
                                            }

                                            // 3. Catat transaksi bagian sisa hutang (walletId = 0, tanpa memotong dompet lagi)
                                            if (remainingDebt > 0.0) {
                                                viewModel.addTransaction(
                                                    amount = remainingDebt,
                                                    type = "EXPENSE",
                                                    walletId = 0,
                                                    categoryId = selectedCategoryId,
                                                    note = if (note.isNotBlank()) "$note (Sisa hutang ke ${debtPersonName.trim()})" else "Sisa hutang ke ${debtPersonName.trim()}",
                                                    date = System.currentTimeMillis(),
                                                    targetWalletId = null,
                                                    adminFee = 0.0
                                                )
                                            }
                                        }

                                        "LEND_FRIEND" -> {
                                            // OPSI 3: Talangi teman (piutang)
                                            val lendAmount = debtCustomAmountStr.toDoubleOrNull() ?: amountVal
                                            // 1. Catat piutang
                                            viewModel.addDebt(
                                                personName = debtPersonName.trim(),
                                                totalAmount = lendAmount,
                                                dueDate = dueTimestamp,
                                                type = "PIUTANG",
                                                notes = if (note.isNotBlank()) "Talangi: $note" else "Talangan pengeluaran"
                                            )
                                            // 2. Catat transaksi penuh dari dompet
                                            if (amountVal > 0.0) {
                                                viewModel.addTransaction(
                                                    amount = amountVal,
                                                    type = selectedType,
                                                    walletId = selectedWalletId,
                                                    categoryId = selectedCategoryId,
                                                    note = if (note.isNotBlank()) "$note (Talangi ${debtPersonName.trim()})" else "Talangi ${debtPersonName.trim()}",
                                                    date = System.currentTimeMillis(),
                                                    targetWalletId = null,
                                                    adminFee = adminFeeVal
                                                )
                                            }
                                        }
                                    }
                                } else if (amountVal > 0.0) {
                                    // Regular standard transaction
                                    viewModel.addTransaction(
                                        amount = amountVal,
                                        type = selectedType,
                                        walletId = selectedWalletId,
                                        categoryId = selectedCategoryId,
                                        note = note,
                                        date = System.currentTimeMillis(),
                                        targetWalletId = if (selectedType == "TRANSFER") selectedTargetWalletId else null,
                                        adminFee = adminFeeVal
                                    )
                                }
                            }
                            onDismiss()
                        },
                        enabled = (wallets.isNotEmpty() || (enableDebtOption && debtModeOption == "FULL_DEBT")) && (
                            scannedReceipts.isNotEmpty() || amountStr.isNotEmpty() || (enableDebtOption && debtPersonName.isNotEmpty())
                        ),
                        modifier = Modifier.weight(1.4f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isId) "Simpan Transaksi" else "Save Transaction",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern Gradient Chip for Wallet Selector
 */
@Composable
private fun WalletGradientChip(
    wallet: Wallet,
    isSelected: Boolean,
    isDisabled: Boolean,
    formattedBalance: String,
    onClick: () -> Unit
) {
    val gradientBrush = when (wallet.icon) {
        "bank" -> Brush.horizontalGradient(listOf(Color(0xFF1E88E5), Color(0xFF1565C0)))
        "wallet" -> Brush.horizontalGradient(listOf(Color(0xFF8E24AA), Color(0xFF5E35B1)))
        "savings" -> Brush.horizontalGradient(listOf(Color(0xFF43A047), Color(0xFF2E7D32)))
        else -> Brush.horizontalGradient(listOf(Color(0xFFFFA000), Color(0xFFF57C00)))
    }

    val iconPainter = when (wallet.icon) {
        "bank" -> painterResource(id = R.drawable.ic_wallet_type_bank)
        "wallet" -> painterResource(id = R.drawable.ic_wallet_type_wallet)
        "savings" -> painterResource(id = R.drawable.ic_wallet_type_savings)
        else -> painterResource(id = R.drawable.ic_wallet_type_cash)
    }

    val chipShape = RoundedCornerShape(14.dp)
    
    Card(
        modifier = Modifier
            .wrapContentWidth()
            .height(58.dp)
            .clip(chipShape)
            .clickable(enabled = !isDisabled) { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), chipShape)
                } else if (isDisabled) {
                    Modifier.border(BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)), chipShape)
                } else {
                    Modifier.border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), chipShape)
                }
            ),
        shape = chipShape,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    if (isDisabled) {
                        Brush.horizontalGradient(listOf(Color(0xFF424242), Color(0xFF303030)))
                    } else {
                        gradientBrush
                    }
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(
                    modifier = Modifier.widthIn(min = 80.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = wallet.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedBalance,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Option Card for Hutang / Split Modes with Gradient Accents
 */
@Composable
private fun DebtModeOptionCard(
    title: String,
    subtitle: String,
    badge: String,
    isSelected: Boolean,
    gradientBrush: Brush,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), cardShape)
                } else {
                    Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)), cardShape)
                }
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) gradientBrush else Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.2f), Color.Gray.copy(alpha = 0.2f)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SimpleCustomChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val chipShape = RoundedCornerShape(percent = 50)
    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        label = "chipBgColor"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chipContentColor"
    )
    Surface(
        onClick = onClick,
        shape = chipShape,
        color = animatedBgColor,
        contentColor = animatedContentColor,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
