package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.util.GoogleDriveManager
import com.example.ui.viewmodel.FinanceViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun GoogleSyncDialog(
    isId: Boolean,
    viewModel: FinanceViewModel,
    gdriveSyncState: String?,
    context: Context,
    onBackupFound: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var googleEmailInput by remember { mutableStateOf("") }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null && !account.email.isNullOrEmpty()) {
                    val email = account.email!!
                    viewModel.connectGDrive(email)
                    viewModel.fetchCloudBackup(email) { fileTime, fileData ->
                        onDismiss()
                        if (fileTime != null && fileData != null) {
                            onBackupFound(fileTime, fileData)
                        } else {
                            Toast.makeText(
                                context,
                                if (isId) "Akun Google (${account.displayName ?: email}) Terhubung ke Drive!" else "Google Drive connected for ${account.displayName ?: email}!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                authErrorMessage = if (isId) "Gagal login Google: ${e.localizedMessage}" else "Google sign-in error: ${e.localizedMessage}"
            }
        } else {
            authErrorMessage = if (isId) "Login dibatalkan atau izin Drive tidak diberikan." else "Sign-in cancelled or Drive permissions declined."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = if (isId) "Google Drive & OAuth" else "Google Drive & OAuth",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val loggingIn = gdriveSyncState == "LOGGING_IN"

                if (loggingIn) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (isId) "Memeriksa berkas di Google Drive Anda..." else "Checking files in your Google Drive...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (isId) {
                            "Hubungkan langsung dengan akun Google resmi untuk mencadangkan dan memulihkan seluruh data transaksi Anda ke Google Drive."
                        } else {
                            "Sign in with your official Google Account to seamlessly backup and restore financial transaction records on Google Drive."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Official Google Sign-In Button
                    Button(
                        onClick = {
                            authErrorMessage = null
                            val signInClient = GoogleDriveManager.getGoogleSignInClient(context)
                            googleSignInLauncher.launch(signInClient.signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isId) "Pilih Akun Google (Login Resmi)" else "Sign In with Google Account",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (authErrorMessage != null) {
                        Text(
                            text = authErrorMessage!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = if (isId) "Atau pilih akun tersimpan / masukkan manual:" else "Or choose saved account / enter manually:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val savedAccounts = viewModel.getAccountHistory()

                    if (savedAccounts.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            savedAccounts.forEach { suggestEmail ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { googleEmailInput = suggestEmail },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (googleEmailInput == suggestEmail) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    ),
                                    border = if (googleEmailInput == suggestEmail) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (googleEmailInput == suggestEmail) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = suggestEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = googleEmailInput,
                        onValueChange = { googleEmailInput = it },
                        label = { Text(if (isId) "Alamat Email Google" else "Google Email Address") },
                        placeholder = { Text("email@example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            val loggingIn = gdriveSyncState == "LOGGING_IN"
            if (!loggingIn) {
                Button(
                    onClick = {
                        val email = googleEmailInput.trim()
                        if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            viewModel.connectGDrive(email)
                            viewModel.fetchCloudBackup(email) { fileTime, fileData ->
                                onDismiss()
                                if (fileTime != null && fileData != null) {
                                    onBackupFound(fileTime, fileData)
                                } else {
                                    Toast.makeText(context, if (isId) "Akun Terhubung!" else "Account Connected successfully!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, if (isId) "Masukkan alamat email yang valid atau gunakan tombol Login di atas!" else "Please enter a valid email or use the Sign-In button above!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(100)
                ) {
                    Text(if (isId) "Hubungkan Manual" else "Connect Manual", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            val loggingIn = gdriveSyncState == "LOGGING_IN"
            if (!loggingIn) {
                TextButton(onClick = onDismiss) {
                    Text(if (isId) "Tutup" else "Close")
                }
            }
        }
    )
}

@Composable
fun CloudRestorePromptDialog(
    isId: Boolean,
    cloudBackupTime: String?,
    cloudBackupData: String?,
    viewModel: FinanceViewModel,
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
                    Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isId) "Cadangan Cloud Ditemukan" else "Cloud Backup Located",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isId) {
                        "Akun Google Drive Anda menyimpan berkas sinkronisasi cadangan transaksi DuitKu terdeteksi dari tanggal:"
                    } else {
                        "Your Google Drive contains an archived DuitKu transaction history recorded on:"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = cloudBackupTime ?: "Unknown Date",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isId) {
                        "Apakah Anda ingin memulihkan cadangan cloud ini sekarang? Tindakan ini akan menimpa seluruh status data lokal saku dan rincian transaksi DuitKu sekarang berjalan."
                    } else {
                        "Do you wish to restore this cloud backup now? Doing so will completely replace your current local financial ledger records."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (cloudBackupData != null) {
                        viewModel.importBackupJson(cloudBackupData) { ok ->
                            onDismiss()
                            if (ok) {
                                Toast.makeText(context, if (isId) "Cadangan Cloud Berhasil Dipulihkan!" else "Cloud Backup Successfully Restored!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, if (isId) "Gagal memulihkan cadangan cloud!" else "Cloud restore unsuccessful!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(100)
            ) {
                Text(if (isId) "Pulihkan Sekarang" else "Restore Cloud Data", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isId) "Lewati (Gunakan Lokal)" else "Keep Local Data")
            }
        }
    )
}
