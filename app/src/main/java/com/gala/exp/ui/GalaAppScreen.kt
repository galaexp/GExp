package com.gala.exp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gala.exp.vm.GalaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalaAppScreen(viewModel: GalaViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    val storeCode by viewModel.storeCode.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val password by viewModel.password.collectAsState()
    val rememberMe by viewModel.rememberMe.collectAsState()

    val selectedWeek by viewModel.selectedWeek.collectAsState()
    val lockedWeeks by viewModel.lockedWeeks.collectAsState()
    val isLocked = lockedWeeks.contains(selectedWeek)

    val dashboardRows by viewModel.storeDashboardRows.collectAsState()
    val dashboardLoading by viewModel.storeDashboardLoading.collectAsState()

    val staffList by viewModel.staffList.collectAsState()
    val selectedStaff by viewModel.selectedStaff.collectAsState()

    val appUpdateState by viewModel.appUpdateState.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { msg ->
            showToast(context, msg)
        }
    }

    LaunchedEffect(isLoggedIn, storeCode) {
        if (isLoggedIn && storeCode.isNotBlank()) {
            viewModel.loadStoreDashboardData()
        }
    }

    // Active navigational View
    var activeView by remember { mutableStateOf("home") }

    BackHandler(enabled = isLoggedIn && activeView != "home") {
        activeView = "home"
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showSplashScreen by remember { mutableStateOf(true) }

    if (showSplashScreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = com.gala.exp.R.drawable.gala_icon),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(32.dp))
            )
        }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1800)
            showSplashScreen = false
        }
    } else if (!isLoggedIn) {
        LoginScreen(
            storeCode = storeCode,
            password = password,
            rememberMe = rememberMe,
            isAuthLoading = isAuthLoading,
            authError = authError,
            onStoreCodeChanged = viewModel::setStoreCode,
            onPasswordChanged = viewModel::setPassword,
            onRememberMeChanged = viewModel::setRememberMe,
            onLoginClick = viewModel::login
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 310.dp),
                    drawerContainerColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ColorPrimary)
                                .padding(vertical = 32.dp, horizontal = 20.dp)
                        ) {
                            Column {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(id = com.gala.exp.R.drawable.gala_icon),
                                    contentDescription = "App Logo",
                                    modifier = Modifier
                                        .size(52.dp)
                                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                                        .border(1.5.dp, Color.White, RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = storeName.ifBlank { "Gala Markets" },
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Store: $storeCode",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "OPERATIONS WORKFLOWS",
                            color = ColorMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.2.sp),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )

                        DrawerItem(
                            icon = Icons.Default.Home,
                            label = "Home / Selector",
                            selected = activeView == "home",
                            onClick = {
                                activeView = "home"
                                scope.launch { drawerState.close() }
                            }
                        )

                        DrawerItem(
                            icon = Icons.Default.Dashboard,
                            label = "Store View",
                            selected = activeView == "store_view",
                            onClick = {
                                viewModel.loadStoreDashboardData()
                                activeView = "store_view"
                                scope.launch { drawerState.close() }
                            }
                        )

                        DrawerItem(
                            icon = Icons.Default.People,
                            label = "Manage Staff",
                            selected = activeView == "manage_staff",
                            onClick = {
                                activeView = "manage_staff"
                                scope.launch { drawerState.close() }
                            }
                        )

                        DrawerItem(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            selected = activeView == "settings",
                            onClick = {
                                activeView = "settings"
                                scope.launch { drawerState.close() }
                            }
                        )

                        DrawerItem(
                            icon = Icons.Default.SystemUpdate,
                            label = "Check for Updates",
                            selected = false,
                            onClick = {
                                viewModel.checkAppVersion(isManualCheck = true)
                                scope.launch { drawerState.close() }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        DrawerItem(
                            icon = Icons.Default.ExitToApp,
                            label = "Sign Out",
                            selected = false,
                            onClick = {
                                viewModel.logout()
                                activeView = "home"
                                scope.launch { drawerState.close() }
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column(
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = when (activeView) {
                                        "home" -> storeName
                                        "store_view" -> "Store View"
                                        "add_item" -> "Add New Item"
                                        "follow_up" -> "Follow Up"
                                        "review" -> "Log Review"
                                        "manage_staff" -> "Manage Staff"
                                        "settings" -> "Settings"
                                        "shelf_removal" -> "Shelf Removal"
                                        else -> "Gala Markets"
                                    },
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    lineHeight = 18.sp
                                )
                                Text(
                                    text = if (selectedWeek.isNotBlank()) selectedWeek else "No Week Selected",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 12.sp
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() } }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Drawer menu",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = viewModel::logout) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Logout icon",
                                    tint = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = ColorPrimary
                        )
                    )
                },
                contentWindowInsets = WindowInsets.systemBars
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF0F2F5))
                        .padding(innerPadding)
                ) {
                    when (activeView) {
                        "home" -> HomeScreen(
                            selectedWeek = selectedWeek,
                            lockedWeeks = lockedWeeks,
                            dashboardRows = dashboardRows,
                            dashboardLoading = dashboardLoading,
                            staffList = staffList,
                            selectedStaff = selectedStaff,
                            onStaffSelected = viewModel::setSelectedStaff,
                            onWeekSelected = viewModel::setSelectedWeek,
                            onFollowUpClick = {
                                viewModel.startFollowUp()
                                activeView = "follow_up"
                            },
                            onAddNewClick = { activeView = "add_item" },
                            onReviewClick = { activeView = "review" },
                            onStoreViewClick = {
                                viewModel.loadStoreDashboardData()
                                activeView = "store_view"
                            },
                            onShelfRemovalClick = {
                                activeView = "shelf_removal"
                            }
                        )
                        "shelf_removal" -> ShelfRemovalScreen(
                            viewModel = viewModel,
                            onBackToHome = { activeView = "home" }
                        )
                        "store_view" -> StoreViewScreen(
                            viewModel = viewModel,
                            onBackToHome = { activeView = "home" }
                        )
                        "add_item" -> AddItemScreen(
                            viewModel = viewModel,
                            storeName = storeName,
                            selectedWeek = selectedWeek,
                            isLocked = isLocked,
                            onBackToHome = { activeView = "home" }
                        )
                        "follow_up" -> FollowUpScreen(
                            viewModel = viewModel,
                            storeName = storeName,
                            selectedWeek = selectedWeek,
                            isLocked = isLocked,
                            onBackToHome = { activeView = "home" }
                        )
                        "review" -> ReviewScreen(
                            viewModel = viewModel,
                            selectedWeek = selectedWeek,
                            isLocked = isLocked,
                            onBackToHome = { activeView = "home" }
                        )
                        "manage_staff" -> ManageStaffScreen(
                            viewModel = viewModel,
                            onBackToHome = { activeView = "home" }
                        )
                        "settings" -> SettingsScreen(
                            onBackToHome = { activeView = "home" }
                        )
                    }
                }
            }
        }

        appUpdateState?.let { info ->
            if (info.isUpdateAvailable || info.isForceUpdate) {
                UpdateDialog(
                    updateState = info,
                    downloadStatus = downloadStatus,
                    onUpdateClick = { url ->
                        viewModel.downloadAndInstallApk(context, url)
                    },
                    onDismissClick = {
                        viewModel.dismissOptionalUpdate()
                    },
                    onInstallApk = { file ->
                        installApk(context, file)
                    }
                )
            }
        }
    }
}
