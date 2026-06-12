package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var backupJson by remember { mutableStateOf("") }
    var importJson by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0: Ekspor, 1: Impor

    // Pre-generate JSON on opening
    LaunchedEffect(Unit) {
        backupJson = viewModel.getBackupJson()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Cadangkan & Impor (Backup)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Pindahkan catatan keuangan Anda ke perangkat lain dengan mudah.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TabRow(selectedTabIndex = activeTab) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Ekspor Data") }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Impor Data") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (activeTab == 0) {
                    // EXPORT LAYOUT
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Salin kode teks di bawah ini ke HP baru Anda atau bagikan:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        OutlinedTextField(
                            value = backupJson,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("DuitKu Backup", backupJson)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Kode backup disalin ke papan klip!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Salin")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salin Kode")
                            }

                            FilledTonalButton(
                                onClick = {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, backupJson)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Simpan Cadangan DuitKu")
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Bagikan")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bagikan")
                            }
                        }
                    }
                } else {
                    // IMPORT LAYOUT
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Tempelkan kode teks cadangan dari HP lama Anda di bawah ini:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = importJson,
                            onValueChange = { importJson = it },
                            placeholder = { Text("Mulai tempelkan kode cadangan JSON di sini...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clipData = clipboard.primaryClip
                                    if (clipData != null && clipData.itemCount > 0) {
                                        importJson = clipData.getItemAt(0).text.toString()
                                        Toast.makeText(context, "Berhasil menempelkan teks!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Klipbort kosong!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Tempel")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tempel Teks")
                            }

                            Button(
                                onClick = {
                                    if (importJson.trim().isEmpty()) {
                                        Toast.makeText(context, "Mohon tempel kode backup yang valid!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.importBackupJson(importJson) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Data berhasil dipulihkan!", Toast.LENGTH_LONG).show()
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "Gagal memulihkan. Periksa format teks!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Impor Sekarang")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
