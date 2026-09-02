package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.ui.util.UpdateResult
import com.example.ui.viewmodel.FinanceViewModel
import java.io.File

@Composable
fun ScanReceiptOptionsDialog(
    context: Context,
    isId: Boolean,
    cameraLauncher: ActivityResultLauncher<Uri>,
    permissionLauncher: ActivityResultLauncher<String>,
    photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    onSetTempPhotoUri: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val configuration = LocalConfiguration.current
        val dialogWidth = if (configuration.screenWidthDp < 600) (configuration.screenWidthDp * 0.94).dp else 400.dp
        Card(
            modifier = Modifier
                .width(dialogWidth)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isId) "Pindai Nota Belanja" else "Scan Shopping Receipt",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(painterResource(id = R.drawable.ic_close_custom), contentDescription = "Close")
                    }
                }

                Text(
                    text = if (isId) 
                        "Gunakan asisten kecerdasan buatan Gemini AI untuk memindai struk belanja secara otomatis."
                        else "Use Gemini AI assistant to automatically extract details from your shopping receipts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 1: Camera
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.CAMERA
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            
                            if (hasPermission) {
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
                                    onSetTempPhotoUri(uri)
                                    try {
                                        cameraLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, (if (isId) "Gagal membuka kamera: " else "Failed to open camera: ") + e.message, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, if (isId) "Gagal membuat file foto" else "Failed to initialize camera file", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isId) "Ambil Foto via Kamera" else "Take Photo via Camera",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isId) "Ambil gambar nota instan" else "Snap an instant receipt photo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Option 2: Gallery
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isId) "Unggah dari Galeri" else "Upload from Gallery",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (isId) "Pilih satu/banyak gambar nota" else "Select single/multiple receipt images",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanningProgressDialog(isId: Boolean) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .width(320.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
                Text(
                    text = if (isId) "Menganalisis Nota" else "Analyzing Receipt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isId) 
                        "Asisten Kecerdasan Buatan Gemini AI sedang membaca detail rincian belanja Anda..." 
                        else "Gemini AI assistant is reading your shopping receipt details...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SavingsSimulationDialog(
    isId: Boolean,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val configuration = LocalConfiguration.current
        val dialogWidth = if (configuration.screenWidthDp < 600) (configuration.screenWidthDp * 0.94).dp else 520.dp
        Card(
            modifier = Modifier
                .width(dialogWidth)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isId) "Simulasi Anggaran Cerdas" else "Smart Budget Simulation",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(painterResource(id = R.drawable.ic_close_custom), contentDescription = "Close")
                    }
                }

                Text(
                    text = if (isId) 
                        "Gunakan kalkulator saku cerdas ini untuk memproyeksikan target tabungan bulanan Anda berdasarkan formula budgeting 50/30/20!" 
                        else "Use this budgeting projector to simulate your monthly savings goals based on the balanced 50/30/20 formula!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                var monthlyIncomeInput by remember { mutableStateOf("5000000") }
                OutlinedTextField(
                    value = monthlyIncomeInput,
                    onValueChange = { monthlyIncomeInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (isId) "Estimasi Pendapatan Bulanan (Rp)" else "Monthly Income Estimate (Rp)") },
                    leadingIcon = { Text("Rp", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                val incomeAmount = monthlyIncomeInput.toDoubleOrNull() ?: 0.0
                val needs = incomeAmount * 0.5
                val wants = incomeAmount * 0.3
                val savings = incomeAmount * 0.2

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isId) "Hasil Formula Jago 50/30/20:" else "Projected 50/30/20 Allocation:",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isId) "Kebutuhan Pokok (50%)" else "Needs & Dues (50%)", style = MaterialTheme.typography.bodySmall)
                            Text(viewModel.formatRupiah(needs), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isId) "Keinginan & Jajan (30%)" else "Wants & Lifestyle (30%)", style = MaterialTheme.typography.bodySmall)
                            Text(viewModel.formatRupiah(wants), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isId) "Masa Depan & Tabungan (20%)" else "Invest & Savings (20%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text(viewModel.formatRupiah(savings), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isId) "Terapkan Rencana Ini" else "Apply Plan Principles")
                }
            }
        }
    }
}

@Composable
fun DashboardTipsDialog(
    isId: Boolean,
    onDismiss: () -> Unit
) {
    val tipsId = listOf(
        "Formula 50/30/20: Taruh 50% untuk Kebutuhan Pokok, 30% Keinginan, dan 20% Tabungan/Utang.",
        "Hemat jajan kopi harian luar! Membuat sendiri di rumah/kantor menghemat puluhan ribu sehari.",
        "Aset terbaik adalah pencatatan keuangan! Rutin catat sekecil apapun pengeluaran Anda di DuitKu.",
        "Membagi kantong (misal Kantong Belanja vs Kantong Tabungan) menjaga saldo Anda tidak gampang boncos.",
        "Disiplin menabung di awal gajian, amankan juga dana darurat terlebih dahulu sebelum konsumtif berlebih."
    )
    val tipsEn = listOf(
        "The 50/30/20 Rule: Set 50% for Needs, 30% for Wants, and 20% for Savings.",
        "Avoid expensive daily takeout coffee! Making your own drink saves massive wallet pocket cash.",
        "Tracking builds security! Consistent financial logging in DuitKu secures early detection of leaks.",
        "Segregate your holdings! Having separate pockets for groceries and savings halts excessive spending.",
        "Pay yourself first! Transfer money to your savings pocket immediately upon getting paid to beat temptation."
    )
    val tipsToUse = if (isId) tipsId else tipsEn
    val randomTip = remember { tipsToUse.random() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(if (isId) "Tips Dompet Cerdas" else "Smart Saving Tips")
            }
        },
        text = {
            Text(
                text = randomTip,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isId) "Keren, Mengerti!" else "Got it!")
            }
        }
    )
}

@Composable
fun DashboardUpdateDialog(
    isId: Boolean,
    currentVersion: String,
    update: UpdateResult.NewUpdate,
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isId) "Pembaruan Tersedia" else "Update Available",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isId)
                        "Versi baru (${update.latestVersionName}) telah dirilis! Versi Anda saat ini adalah v$currentVersion."
                        else "A new version (${update.latestVersionName}) is available! Your current version is v$currentVersion.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (update.releaseNotes.isNotEmpty()) {
                    Text(
                        text = if (isId) "Catatan Rilis:" else "Release Notes:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = update.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                }
            ) {
                Text(if (isId) "Unduh & Perbarui" else "Download & Update", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isId) "Nanti" else "Later")
            }
        }
    )
}
