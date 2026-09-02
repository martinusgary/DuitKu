package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    var adminFeeStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // "INCOME", "EXPENSE", "TRANSFER"
    var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: 0) }
    var selectedTargetWalletId by remember { mutableStateOf(wallets.getOrNull(1)?.id ?: wallets.firstOrNull()?.id ?: 0) }
    var selectedCategoryId by remember { mutableStateOf(0) }
    var note by remember { mutableStateOf("") }
    var scannedReceipts by remember { mutableStateOf<List<GeminiClient.ScanResult>>(initialScannedReceipts) }

    // Debt / Loan (Talangan) states
    var enableDebtOption by remember { mutableStateOf(false) }
    var debtTypeOption by remember { mutableStateOf("HUTANG") } // "HUTANG" or "PIUTANG"
    var debtPersonName by remember { mutableStateOf("") }
    var debtCustomAmountStr by remember { mutableStateOf("") }
    var debtDueDays by remember { mutableIntStateOf(7) } // 3, 7, 14, 30 days
    var deductWalletForHutang by remember { mutableStateOf(true) }

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
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isId) "Tambah Transaksi Baru" else "Add New Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // 1. Selector Tab (High-polish Segmented Control)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val items = listOf(
                        "EXPENSE" to (if (isId) "Pengeluaran" else "Expense"),
                        "INCOME" to (if (isId) "Pemasukan" else "Income"),
                        "TRANSFER" to (if (isId) "Transfer" else "Transfer")
                    )
                    items.forEach { (type, label) ->
                        val isSelected = selectedType == type
                        val tabItemShape = RoundedCornerShape(8.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = tabItemShape
                                )
                                .clip(tabItemShape)
                                .clickable { selectedType = type }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Quick Scan Receipt Section (Camera & Gallery)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = "Scan Struk",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isId) "Pindai Struk / Upload Gambar" else "Scan Receipt / Upload Image",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (isScanningInsideDialog) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            }
                        }
                        Text(
                            text = if (isId) 
                                "Otomatis isi nominal, catatan, dan tipe dari foto struk belanja dengan AI Gemini." 
                            else 
                                "Auto-populate amount, notes, and category from receipt photo via Gemini AI.",
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

                    // 2b. Admin Fee Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = adminFeeStr,
                            onValueChange = { if (it.all { char -> char.isDigit() }) adminFeeStr = it },
                            label = { Text(if (isId) "Biaya Admin (Opsional)" else "Admin Fee (Optional)") },
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
                            listOf("0" to "Rp 0", "1000" to "1.000", "2500" to "2.500", "6500" to "6.500").forEach { (valStr, label) ->
                                val isSelected = adminFeeStr == valStr || (valStr == "0" && (adminFeeStr.isEmpty() || adminFeeStr == "0"))
                                Surface(
                                    onClick = { adminFeeStr = if (valStr == "0") "" else valStr },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        val parsedAmt = amountStr.toDoubleOrNull() ?: 0.0
                        val parsedFee = adminFeeStr.toDoubleOrNull() ?: 0.0

                        if (selectedType == "TRANSFER" && parsedAmt > 0.0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (isId) "Ringkasan Aliran Dana Transfer:" else "Transfer Fund Flow Summary:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isId) 
                                            "• Terpotong dari dompet asal: ${viewModel.formatRupiah(parsedAmt + parsedFee)} (Nominal + Admin)"
                                        else 
                                            "• Deducted from source: ${viewModel.formatRupiah(parsedAmt + parsedFee)} (Amount + Admin)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isId) 
                                            "• Masuk ke dompet tujuan: ${viewModel.formatRupiah(parsedAmt)} (Bersih tanpa admin)"
                                        else 
                                            "• Received in target: ${viewModel.formatRupiah(parsedAmt)} (Net amount)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (parsedFee > 0.0 && parsedAmt > 0.0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text(
                                    text = if (isId) 
                                        "Total saldo terpotong: ${viewModel.formatRupiah(parsedAmt + parsedFee)} (${viewModel.formatRupiah(parsedAmt)} + Admin ${viewModel.formatRupiah(parsedFee)})"
                                    else 
                                        "Total deducted: ${viewModel.formatRupiah(parsedAmt + parsedFee)} (${viewModel.formatRupiah(parsedAmt)} + Fee ${viewModel.formatRupiah(parsedFee)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Wallets Selection (From / Source Wallet)
                Text(if (isId) "Dompet Asal:" else "Source Wallet:", style = MaterialTheme.typography.labelMedium)
                if (wallets.isEmpty()) {
                    Text(if (isId) "Dompet tidak ditemukan. Silakan tambahkan dompet terlebih dahulu di tab Dompet." else "No wallets found. Please add a wallet first in the Wallets tab.", color = MaterialTheme.colorScheme.error)
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(wallets, key = { it.id }) { w ->
                            SimpleCustomChip(
                                text = w.name,
                                isSelected = selectedWalletId == w.id,
                                onClick = { selectedWalletId = w.id }
                            )
                        }
                    }
                }

                // 4. Specific to Transfer Destination Wallet
                if (selectedType == "TRANSFER") {
                    Text(if (isId) "Dompet Tujuan:" else "Destination Wallet:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(wallets, key = { it.id }) { w ->
                            SimpleCustomChip(
                                text = w.name,
                                isSelected = selectedTargetWalletId == w.id,
                                onClick = { selectedTargetWalletId = w.id }
                            )
                        }
                    }
                }

                // 5. Category Selection (Only for Income and Expense)
                if (selectedType != "TRANSFER") {
                    Text(if (isId) "Kategori:" else "Category:", style = MaterialTheme.typography.labelMedium)
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
                        placeholder = { Text(if (isId) "misal: Belanja bulanan, bonus gaji, kopi bareng teman, dll." else "e.g., Grocery store, salary bonus, coffee with friends, etc.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 7. Compact Expandable Hutang & Talangan Teman (Piutang) Section
                if (selectedType != "TRANSFER") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (enableDebtOption) {
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (enableDebtOption) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Handshake,
                                        contentDescription = null,
                                        tint = if (enableDebtOption) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (isId) "Opsi Hutang / Talangi Teman" else "Debt & Split Bill (Loan)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (enableDebtOption) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (enableDebtOption) {
                                                if (debtTypeOption == "HUTANG") (if (isId) "Tercatat di menu Hutang Saya" else "Logged to My Debts")
                                                else (if (isId) "Tercatat di menu Piutang / Ditalangi" else "Logged to Debts Owed to Me")
                                            } else {
                                                if (isId) "Ketuk untuk catat hutang atau talangan otomatis" else "Tap to link with debts or friend loans"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = enableDebtOption,
                                    onCheckedChange = { enableDebtOption = it },
                                    modifier = Modifier.height(28.dp)
                                )
                            }

                            AnimatedVisibility(visible = enableDebtOption) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Mode Selector
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                            .padding(3.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val isHutang = debtTypeOption == "HUTANG"
                                        
                                        // Saya Berhutang (Hutang)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (isHutang) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { debtTypeOption = "HUTANG" }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isHutang) {
                                                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = if (isId) "Saya Berhutang" else "I Owe (Debt)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isHutang) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Talangi Teman (Piutang)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (!isHutang) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { debtTypeOption = "PIUTANG" }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (!isHutang) {
                                                    Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = if (isId) "Talangi Teman" else "Paid for Friend",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (!isHutang) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    // Fields for Saya Berhutang vs Talangi Teman
                                    if (debtTypeOption == "HUTANG") {
                                        OutlinedTextField(
                                            value = debtPersonName,
                                            onValueChange = { debtPersonName = it },
                                            label = { Text(if (isId) "Nama Pemberi Hutang / Toko" else "Creditor / Store Name") },
                                            placeholder = { Text(if (isId) "misal: Warung Bu Siti, Budi" else "e.g. Store name, John") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = debtCustomAmountStr,
                                            onValueChange = { if (it.all { char -> char.isDigit() }) debtCustomAmountStr = it },
                                            label = { Text(if (isId) "Nominal Hutang (Kosongkan = Sesuai Total)" else "Debt Amount (Blank = Full Total)") },
                                            prefix = { Text("Rp ") },
                                            placeholder = { Text(amountStr.ifEmpty { "0" }) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        // Wallet deduction toggle for Hutang
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isId) "Potong saldo dompet sekarang?" else "Deduct wallet balance now?",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Switch(
                                                checked = deductWalletForHutang,
                                                onCheckedChange = { deductWalletForHutang = it }
                                            )
                                        }
                                    } else {
                                        // Talangi Teman (Piutang)
                                        OutlinedTextField(
                                            value = debtPersonName,
                                            onValueChange = { debtPersonName = it },
                                            label = { Text(if (isId) "Nama Teman yang Ditalangi" else "Friend's Name") },
                                            placeholder = { Text(if (isId) "misal: Kevin, Andi, Rian" else "e.g. Kevin, Alex") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
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
                                                    "💡 Saldo dompet Anda akan tetap dipotong sesuai total pengeluaran, dan nominal talangan teman akan otomatis dicatat di menu Piutang."
                                                else 
                                                    "💡 Your wallet will be deducted for the full bill, and your friend's split portion will be recorded in the Debts menu.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(10.dp)
                                            )
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

                // Actions
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
                            if (selectedWalletId == 0) return@Button

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
                                // Save single manual transaction
                                val amountVal = amountStr.toDoubleOrNull() ?: 0.0
                                if (amountVal <= 0.0 && !enableDebtOption) return@Button

                                if (selectedType == "TRANSFER" && selectedWalletId == selectedTargetWalletId) {
                                    // Can't transfer to same wallet
                                    return@Button
                                }

                                val adminFeeVal = adminFeeStr.toDoubleOrNull() ?: 0.0

                                // Check if user enabled Hutang / Piutang sync
                                if (enableDebtOption && debtPersonName.trim().isNotEmpty()) {
                                    val debtAmount = debtCustomAmountStr.toDoubleOrNull() ?: amountVal
                                    val dueTimestamp = System.currentTimeMillis() + (debtDueDays * 86400000L)
                                    
                                    if (debtTypeOption == "HUTANG") {
                                        viewModel.addDebt(
                                            personName = debtPersonName.trim(),
                                            totalAmount = debtAmount,
                                            dueDate = dueTimestamp,
                                            type = "HUTANG",
                                            notes = if (note.isNotBlank()) "Hutang: $note" else "Hutang transaksi"
                                        )
                                        // If wallet deduction is enabled, record transaction
                                        if (deductWalletForHutang && amountVal > 0.0) {
                                            viewModel.addTransaction(
                                                amount = amountVal,
                                                type = selectedType,
                                                walletId = selectedWalletId,
                                                categoryId = selectedCategoryId,
                                                note = if (note.isNotBlank()) "$note (Hutang ke ${debtPersonName.trim()})" else "Hutang ke ${debtPersonName.trim()}",
                                                date = System.currentTimeMillis(),
                                                targetWalletId = null,
                                                adminFee = adminFeeVal
                                            )
                                        }
                                    } else {
                                        // PIUTANG (Talangi teman)
                                        viewModel.addDebt(
                                            personName = debtPersonName.trim(),
                                            totalAmount = debtAmount,
                                            dueDate = dueTimestamp,
                                            type = "PIUTANG",
                                            notes = if (note.isNotBlank()) "Talangi: $note" else "Talangan pengeluaran"
                                        )
                                        // Record main transaction paid from wallet
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
                        enabled = wallets.isNotEmpty() && (
                            scannedReceipts.isNotEmpty() || amountStr.isNotEmpty() || (enableDebtOption && debtPersonName.isNotEmpty())
                        )
                    ) {
                        Text(if (isId) "Simpan" else "Save")
                    }
                }
            }
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
