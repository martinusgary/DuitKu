package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup full-bleed edge-to-edge drawing
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: FinanceViewModel = viewModel()
                var selectedTab by remember { mutableStateOf(0) }
                var showBackupDialog by remember { mutableStateOf(false) }
                var showCategoryDialog by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = "DuitKu",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            actions = {
                                var showMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Opsi Menu",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Kelola Kategori") },
                                            onClick = {
                                                showMenu = false
                                                showCategoryDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Cadangkan & Impor Data") },
                                            onClick = {
                                                showMenu = false
                                                showBackupDialog = true
                                            }
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                                label = { Text("Dasbor") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallets") },
                                label = { Text("Dompet") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.PieChart, contentDescription = "Analytics") },
                                label = { Text("Analisis") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.PriceChange, contentDescription = "Debts/Bills") },
                                label = { Text("Hutang/Tagihan") }
                            )
                        }
                    }
                ) { innerPadding ->
                    // Navigation routing container using a simple state-based view switcher
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (selectedTab) {
                            0 -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToWallets = { selectedTab = 1 }
                            )
                            1 -> WalletsScreen(viewModel = viewModel)
                            2 -> AnalyticsScreen(viewModel = viewModel)
                            3 -> DebtsBillsScreen(viewModel = viewModel)
                        }
                    }
                }

                if (showBackupDialog) {
                    SettingsDialog(
                        viewModel = viewModel,
                        onDismiss = { showBackupDialog = false }
                    )
                }

                if (showCategoryDialog) {
                    CategoryManagementDialog(
                        viewModel = viewModel,
                        onDismiss = { showCategoryDialog = false }
                    )
                }
            }
        }
    }
}
