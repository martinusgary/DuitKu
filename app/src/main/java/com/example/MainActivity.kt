package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel

class MainActivity : androidx.fragment.app.FragmentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup full-bleed edge-to-edge drawing
        enableEdgeToEdge()

        setContent {
            val viewModel: FinanceViewModel = viewModel()
            val themeName by viewModel.appTheme.collectAsState()
            MyApplicationTheme(themeName = themeName) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val prefs = remember { context.getSharedPreferences("security_settings", android.content.Context.MODE_PRIVATE) }
                var isRegistered by remember { mutableStateOf(prefs.getBoolean("is_registered", false)) }
                var isAuthenticated by remember { mutableStateOf(!isRegistered) }

                var selectedTab by remember { mutableStateOf(0) }
                var showBackupDialog by remember { mutableStateOf(false) }
                var showCategoryDialog by remember { mutableStateOf(false) }
                var isTransactionsBulkMode by remember { mutableStateOf(false) }

                val appLang by viewModel.appLanguage.collectAsState()
                val isId = appLang == "id"

                LaunchedEffect(selectedTab) {
                    if (selectedTab != 2) {
                        isTransactionsBulkMode = false
                    }
                }

                AnimatedContent(
                    targetState = isAuthenticated,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.95f, animationSpec = tween(400)))
                            .togetherWith(fadeOut(animationSpec = tween(250)) + scaleOut(targetScale = 1.05f, animationSpec = tween(250)))
                    },
                    label = "AppAuthenticationTransition"
                ) { authenticatedState ->
                    if (!authenticatedState) {
                        LoginScreen(
                            viewModel = viewModel,
                            onLoginSuccess = {
                                isAuthenticated = true
                            }
                        )
                    } else {
                val uiStyle by viewModel.uiStyle.collectAsState()
                val isFresh = uiStyle == "FRESH"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                if (isFresh && selectedTab != 0) {
                                    IconButton(onClick = { selectedTab = 0 }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Back to Dashboard",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isFresh) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = if (isFresh) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "DuitKu",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = if (isFresh) 0.5.sp else 0.sp
                                            ),
                                            color = if (isFresh) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                                        )
                                        if (isFresh) {
                                            Text(
                                                text = if (isId) "Dompet Digital Cerdas" else "Smart Financial Wallet",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            )
                                        }
                                    }
                                }
                            },
                            actions = {
                                var showMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Menu Options",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false },
                                        offset = DpOffset(x = (-16).dp, y = 8.dp),
                                        modifier = Modifier.widthIn(min = 160.dp, max = 260.dp)
                                    ) {
                                        if (selectedTab == 0) {
                                            DropdownMenuItem(
                                                text = { Text(if (isId) "Kelola Kategori" else "Manage Categories") },
                                                onClick = {
                                                    showMenu = false
                                                    showCategoryDialog = true
                                                }
                                            )
                                        }
                                        if (selectedTab == 2) {
                                            DropdownMenuItem(
                                                text = { Text(if (isTransactionsBulkMode) (if (isId) "Batal Hapus Massal" else "Cancel Bulk Delete") else (if (isId) "Hapus Massal" else "Bulk Delete")) },
                                                onClick = {
                                                    showMenu = false
                                                    isTransactionsBulkMode = !isTransactionsBulkMode
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text(if (isId) "Pengaturan & Keamanan" else "Settings & Security") },
                                            onClick = {
                                                showMenu = false
                                                selectedTab = 5
                                            }
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = if (isFresh) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        if (!isFresh) {
                            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                            val screenWidth = configuration.screenWidthDp
                            val labelStyle = if (screenWidth < 380) {
                                MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                            } else {
                                MaterialTheme.typography.labelSmall
                            }

                            NavigationBar(
                                containerColor = if (isFresh) Color.Transparent else MaterialTheme.colorScheme.surface,
                                tonalElevation = if (isFresh) 0.dp else 3.dp,
                                modifier = if (isFresh) {
                                    Modifier
                                        .navigationBarsPadding()
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .border(
                                            width = 1.5.dp, 
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), 
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                                } else {
                                    Modifier
                                }
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                                    label = {
                                        Text(
                                            text = if (isId) "Dasbor" else "Dashboard",
                                            style = labelStyle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallets") },
                                    label = {
                                        Text(
                                            text = if (isId) "Dompet" else "Wallets",
                                            style = labelStyle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transactions") },
                                    label = {
                                        Text(
                                            text = if (isId) "Transaksi" else "Transactions",
                                            style = labelStyle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 3,
                                    onClick = { selectedTab = 3 },
                                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Analytics") },
                                    label = {
                                        Text(
                                            text = if (isId) "Analisis" else "Analytics",
                                            style = labelStyle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 4,
                                    onClick = { selectedTab = 4 },
                                    icon = { Icon(Icons.Default.PriceChange, contentDescription = "Debts/Bills") },
                                    label = {
                                        Text(
                                            text = if (isId) "Utang/Tagihan" else "Debts/Bills",
                                            style = labelStyle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 5,
                                    onClick = { selectedTab = 5 },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = {
                                        Text(
                                            text = if (isId) "Pengaturan" else "Settings",
                                            style = labelStyle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                            // Navigation routing container using an animated fluid tab switcher
                            Box(modifier = Modifier.padding(innerPadding)) {
                                AnimatedContent(
                                    targetState = selectedTab,
                                    transitionSpec = {
                                        val isRightSwipe = targetState > initialState
                                        
                                        // Menggunakan lambda fullWidth agar slide dinamis sesuai ukuran layar
                                        (fadeIn(animationSpec = tween(220, delayMillis = 80)) +
                                                slideInHorizontally(
                                                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                                                    initialOffsetX = { fullWidth -> if (isRightSwipe) fullWidth else -fullWidth }
                                                )
                                        ).togetherWith(
                                            fadeOut(animationSpec = tween(180)) +
                                                    slideOutHorizontally(
                                                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                                                        targetOffsetX = { fullWidth -> if (isRightSwipe) -fullWidth else fullWidth }
                                                    )
                                        )
                                    },
                                    label = "MainTabTransition"
                                ) { tabState ->
                                    when (tabState) {
                                        0 -> DashboardScreen(
                                            viewModel = viewModel,
                                            onNavigateToTab = { selectedTab = it }
                                        )
                                        1 -> WalletsScreen(viewModel = viewModel)
                                        2 -> TransactionsScreen(
                                            viewModel = viewModel,
                                            isBulkMode = isTransactionsBulkMode,
                                            onBulkModeChange = { isTransactionsBulkMode = it }
                                        )
                                        3 -> AnalyticsScreen(viewModel = viewModel)
                                        4 -> DebtsBillsScreen(viewModel = viewModel)
                                        5 -> SettingsScreen(viewModel = viewModel)
                                    }
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
            }
        }
    }
}