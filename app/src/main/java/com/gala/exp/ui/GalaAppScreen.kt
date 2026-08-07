package com.gala.exp.ui

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.painterResource
import com.gala.exp.api.LastWeekRowDto
import com.gala.exp.api.ThisWeekRowDto
import com.gala.exp.api.StoreDataRowDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import com.gala.exp.vm.GalaViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.util.concurrent.Executors

// Hex Colors matching the CSS:
val ColorInk = Color(0xFF1A1916) // Root ink dark gray #1a1916
val ColorPrimary = Color(0xFF222D65) // Dark Navy #222d65
val ColorPrimaryL = Color(0xFFE8F5EE) // Gentle Green light #e8f5ee
val ColorAccent = Color(0xFFE85D04) // Alert orange #e85d04
val ColorAccentL = Color(0xFFFFF4EE) // Alert orange light #fff4ee
val ColorBorder = Color(0xFFE4E7EC) // Soft border gray #e4e7ec
val ColorMuted = Color(0xFF6B7280) // Muted slate gray #6b7280
val ColorSoft = Color(0xFF9CA3AF) // Secondary soft gray #9ca3af
val ColorGreen = Color(0xFF16A34A) // M3 Green #16a34a
val ColorBlueCombined = Color(0xFF2563EB) // Combined Blue accent #2563eb
val ColorBlueL = Color(0xFFEFF6FF) // Combined Blue light #eff6ff
val ColorPurple = Color(0xFF7C3AED) // M3 Purple #7c3aed
val ColorPurpleL = Color(0xFFF5F3FF) // Light purple #f5f3ff

private var activeToastRef: android.widget.Toast? = null

fun showToast(context: android.content.Context, message: String, duration: Int = android.widget.Toast.LENGTH_SHORT) {
    if (message.isBlank()) return
    try {
        activeToastRef?.cancel()
    } catch (_: Exception) {}
    val toast = android.widget.Toast.makeText(context, message, duration)
    activeToastRef = toast
    toast.show()
}

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
                            icon = Icons.Default.AddCircle,
                            label = "Add New Item",
                            selected = activeView == "add_item",
                            enabled = selectedWeek.isNotBlank(),
                            onClick = {
                                activeView = "add_item"
                                scope.launch { drawerState.close() }
                            }
                        )

                        DrawerItem(
                            icon = Icons.Default.Assignment,
                            label = "Follow Up",
                            selected = activeView == "follow_up",
                            enabled = selectedWeek.isNotBlank(),
                            onClick = {
                                viewModel.startFollowUp()
                                activeView = "follow_up"
                                scope.launch { drawerState.close() }
                            }
                        )

                        DrawerItem(
                            icon = Icons.Default.RateReview,
                            label = "Review & Submit",
                            selected = activeView == "review",
                            enabled = selectedWeek.isNotBlank(),
                            onClick = {
                                activeView = "review"
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

/* ══ DRAWER ROW COMPONENT ══ */
@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = if (selected) ColorPrimaryL else Color.Transparent,
        contentColor = if (!enabled) ColorSoft else if (selected) ColorPrimary else ColorInk
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (!enabled) ColorSoft else if (selected) ColorPrimary else ColorMuted
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!enabled) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Feature locked until week search selected",
                    modifier = Modifier.size(14.dp),
                    tint = ColorSoft
                )
            }
        }
    }
}

/* ══ LOGIN PAGE SCREEN ══ */
@Composable
fun LoginScreen(
    storeCode: String,
    password: String,
    rememberMe: Boolean,
    isAuthLoading: Boolean,
    authError: String?,
    onStoreCodeChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onRememberMeChanged: (Boolean) -> Unit,
    onLoginClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorInk)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background 'GALA' watermark
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = "GALA",
                color = Color.White.copy(alpha = 0.03f),
                fontSize = 110.sp,
                fontWeight = FontWeight.ExtraBold,
                style = androidx.compose.ui.text.TextStyle(letterSpacing = (-4).sp)
            )
        }

        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        Box(contentAlignment = Alignment.Center) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Header Brand Logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ColorPrimary, RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                        Column {
                            Text(
                                text = "Gala Markets",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = ColorInk
                            )
                            Text(
                                text = "EXPIRY TRACKER",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = ColorSoft,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.2.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Store Login",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorInk
                    )
                    Text(
                        text = "Enter your Store Code and password to continue.",
                        fontSize = 12.sp,
                        color = ColorMuted,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Store Code Input
                    GalaFormTextField(
                        value = storeCode,
                        onValueChange = onStoreCodeChanged,
                        label = "STORE CODE (USERNAME)",
                        placeholder = "e.g. 9217",
                        leadingIcon = Icons.Default.Person,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.testTag("login_code_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Input
                    GalaFormTextField(
                        value = password,
                        onValueChange = onPasswordChanged,
                        label = "PASSWORD",
                        placeholder = "••••••••",
                        leadingIcon = Icons.Default.Lock,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                onLoginClick()
                            }
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.testTag("login_password_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Remember me checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRememberMeChanged(!rememberMe) }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = onRememberMeChanged,
                            colors = CheckboxDefaults.colors(checkedColor = ColorPrimary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Remember me on this device",
                            color = ColorMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Auth error text
                    if (authError != null) {
                        Text(
                            text = authError,
                            color = ColorAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            onLoginClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_button"),
                        enabled = !isAuthLoading,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorInk,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Sign In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Spinner Overlay covering card when loading
            if (isAuthLoading) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = 0.8f))
                        .clip(RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = ColorPrimary,
                            modifier = Modifier.size(44.dp),
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = "Signing in...",
                            color = ColorPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/* ══ LANDING (HOME) SCREEN ══ */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    selectedWeek: String,
    lockedWeeks: Set<String>,
    dashboardRows: List<StoreDataRowDto>,
    dashboardLoading: Boolean,
    onWeekSelected: (String) -> Unit,
    onFollowUpClick: () -> Unit,
    onAddNewClick: () -> Unit,
    onReviewClick: () -> Unit,
    onStoreViewClick: () -> Unit,
    onShelfRemovalClick: () -> Unit
) {
    var expandedWeekDropdown by remember { mutableStateOf(false) }
    var weekHintVisible by remember { mutableStateOf(false) }

    val weeks = listOf(
        "Week 1 (1–7)" to "Week 1 (Days 1–7)",
        "Week 2 (8–14)" to "Week 2 (Days 8–14)",
        "Week 3 (15–21)" to "Week 3 (Days 15–21)",
        "Week 4 (22–31)" to "Week 4 (Days 22–31)"
    )

    val expiredCount = remember(dashboardRows) {
        dashboardRows.count { row ->
            val days = getDaysLeft(row.ExpiryDate, row.DaysLeft)
            days != null && days in -7..0
        }
    }
    val totalExpiredStock = remember(dashboardRows) {
        dashboardRows.filter { row ->
            val days = getDaysLeft(row.ExpiryDate, row.DaysLeft)
            days != null && days in -7..0
        }.sumOf { row ->
            row.Stock?.toIntOrNull() ?: 0
        }
    }
    val expiring7DaysCount = remember(dashboardRows) {
        dashboardRows.count { row ->
            val days = getDaysLeft(row.ExpiryDate, row.DaysLeft)
            days != null && days in 1..7
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Visual Banner/Hero card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(ColorPrimary, Color(0xFF4A5D9E))
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Expiry Tracker",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select your submission week, then choose an action below.",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.5.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Store View button with a small arrow
                    Surface(
                        onClick = onStoreViewClick,
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { onStoreViewClick() }
                            .testTag("hero_store_view_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Store View",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Direct week selection Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        if (selectedWeek.isNotBlank()) ColorPrimary else ColorBorder,
                        RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "📅   SELECT SUBMISSION WEEK",
                        color = ColorMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.4.sp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Direct selection pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        weeks.forEach { (backendValue, displayValue) ->
                            val isSelected = selectedWeek == backendValue
                            val isWeekLocked = lockedWeeks.contains(backendValue)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ColorPrimary else Color(0xFFF9FAFB))
                                    .border(1.dp, if (isSelected) ColorPrimary else ColorBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        onWeekSelected(backendValue)
                                        weekHintVisible = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val weekNum = backendValue.substringAfter("Week ").substringBefore(" (")
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "W$weekNum",
                                            color = if (isSelected) Color.White else ColorInk,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isWeekLocked) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Locked",
                                                tint = if (isSelected) Color.White else Color(0xFFDC2626),
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = backendValue.substringAfter("(").substringBefore(")"),
                                        color = if (isSelected) ColorPrimaryL.copy(alpha = 0.9f) else ColorSoft,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dropdown validation alert
        if (weekHintVisible) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorAccentL, RoundedCornerShape(10.dp))
                        .border(1.dp, ColorAccent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠",
                        color = ColorAccent,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Select a week first to continue list operations",
                        color = ColorAccent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Card(
                onClick = onShelfRemovalClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.dp, if (expiredCount > 0) Color(0xFFFCA5A5) else Color(0xFFFED7AA)),
                        RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (expiredCount > 0) Color(0xFFFEF2F2) else Color(0xFFFFF7ED)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (expiredCount > 0) Color(0xFFFEE2E2) else Color(0xFFFFEDD5),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (expiredCount > 0) "🚨" else "⏰",
                            fontSize = 20.sp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SHELF REMOVAL ALERT",
                            color = if (expiredCount > 0) Color(0xFF991B1B) else Color(0xFFC2410C),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.2.sp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (dashboardLoading) {
                            Text(
                                text = "Checking shelf expiry status...",
                                color = ColorMuted,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = androidx.compose.ui.text.buildAnnotatedString {
                                    if (expiredCount > 0) {
                                        append("We found ")
                                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF991B1B)))
                                        append("$expiredCount expired items")
                                        pop()
                                        append(" ($totalExpiredStock units) to remove. ")
                                    } else {
                                        append("No expired items this week. ")
                                    }
                                    if (expiring7DaysCount > 0) {
                                        append("Also, ")
                                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFC2410C)))
                                        append("$expiring7DaysCount items")
                                        pop()
                                        append(" are expiring in the next 7 days.")
                                    }
                                },
                                color = ColorInk,
                                fontSize = 12.5.sp,
                                lineHeight = 16.5.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Open Shelf Removal Details",
                        tint = if (expiredCount > 0) Color(0xFFB91C1C) else Color(0xFFEA580C),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = "WHAT WOULD YOU LIKE TO DO?",
                color = ColorMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                style = androidx.compose.ui.text.TextStyle(letterSpacing = 2.sp)
            )
        }

        // Mode Navigation Cards
        item {
            ModeCard(
                icon = "📋",
                iconColorBg = ColorBlueL,
                title = "Follow Up",
                description = "View last week's items and update stock counts for this week",
                enabled = selectedWeek.isNotBlank(),
                colorFocus = ColorBlueCombined,
                onClick = onFollowUpClick,
                onLockedClick = { weekHintVisible = true }
            )
        }

        item {
            ModeCard(
                icon = "➕",
                iconColorBg = ColorPrimaryL,
                title = "Add New Item",
                description = "Log a new near-expiry item for this week",
                enabled = selectedWeek.isNotBlank(),
                colorFocus = ColorPrimary,
                onClick = onAddNewClick,
                onLockedClick = { weekHintVisible = true }
            )
        }

        item {
            ModeCard(
                icon = "👁",
                iconColorBg = ColorPurpleL,
                title = "Review",
                description = "View this week's submitted items and send email to buyer",
                enabled = selectedWeek.isNotBlank(),
                colorFocus = ColorPurple,
                onClick = onReviewClick,
                onLockedClick = { weekHintVisible = true }
            )
        }
    }
}

@Composable
fun ModeCard(
    icon: String,
    iconColorBg: Color,
    title: String,
    description: String,
    enabled: Boolean,
    colorFocus: Color,
    onClick: () -> Unit,
    onLockedClick: () -> Unit
) {
    Surface(
        onClick = { if (enabled) onClick() else onLockedClick() },
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                if (enabled) ColorBorder else ColorBorder.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .semantics { contentDescription = "Mode button: $title" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (enabled) iconColorBg else ColorBorder)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (enabled) ColorInk else ColorSoft
                )
                Text(
                    text = description,
                    color = ColorMuted,
                    fontSize = 11.5.sp,
                    lineHeight = 14.sp
                )
            }
            Text(
                text = "›",
                fontSize = 18.sp,
                color = if (enabled) ColorSoft else ColorSoft.copy(alpha = 0.4f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/* ══ ADD NEW EXPIRY ITEM SCREEN ══ */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AddItemScreen(
    viewModel: GalaViewModel,
    storeName: String,
    selectedWeek: String,
    isLocked: Boolean,
    onBackToHome: () -> Unit
) {
    val barcode by viewModel.barcode.collectAsState()
    val articleCode by viewModel.articleCode.collectAsState()
    val description by viewModel.description.collectAsState()
    val department by viewModel.department.collectAsState()
    val stock by viewModel.stock.collectAsState()
    val expiry by viewModel.expiry.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val lastWeekRows by viewModel.lastWeekRows.collectAsState()
    val reviewRows by viewModel.reviewRows.collectAsState()

    var showScannerView by remember { mutableStateOf(false) }
    var showFollowUpConfirmDialog by remember { mutableStateOf(false) }
    var showDuplicateSubmittedDialog by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val barcodeFocusRequester = remember { FocusRequester() }
    val articleFocusRequester = remember { FocusRequester() }
    val stockFocusRequester = remember { FocusRequester() }

    val calendar = java.util.Calendar.getInstance()
    val year = calendar.get(java.util.Calendar.YEAR)
    val month = calendar.get(java.util.Calendar.MONTH)
    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, cyLocal, cmLocal, cdLocal ->
            val formattedDate = String.format(
                java.util.Locale.US,
                "%04d-%02d-%02d",
                cyLocal,
                cmLocal + 1,
                cdLocal
            )
            viewModel.setExpiry(formattedDate)
        },
        year,
        month,
        day
    )

    LaunchedEffect(Unit) {
        viewModel.startFollowUp()
        viewModel.loadThisWeekReview()
        viewModel.barcodeMatchEvent.collect {
            stockFocusRequester.requestFocus()
            keyboardController?.hide()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorPrimary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = storeName,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Logged in Store",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = selectedWeek,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Lock notice banner
            if (isLocked) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF991B1B),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Submission Completed For This Week, If any Updation Needed-Contact Buyer Directly",
                            fontSize = 12.sp,
                            color = Color(0xFF991B1B),
                            fontWeight = FontWeight.Bold,
                            lineHeight = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // SECTION 1: FIND ARTICLE
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(ColorAccentL, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1", color = ColorAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "FIND ARTICLE",
                            color = ColorInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ColorBorder, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Barcode Input + Scan Button Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GalaFormTextField(
                                    value = barcode,
                                    onValueChange = { newVal ->
                                        val containsNewline = newVal.contains('\n') || newVal.contains('\r') || newVal.contains('\t')
                                        val cleanValue = newVal.replace("\n", "").replace("\r", "").replace("\t", "")
                                        viewModel.setBarcode(cleanValue)
                                        if (containsNewline) {
                                            stockFocusRequester.requestFocus()
                                            keyboardController?.hide()
                                        }
                                    },
                                    label = "BARCODE NUMBER",
                                    placeholder = "Scan or enter barcode...",
                                    leadingIcon = Icons.Default.QrCode,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = {
                                            stockFocusRequester.requestFocus()
                                            keyboardController?.hide()
                                        }
                                    ),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(barcodeFocusRequester)
                                        .onPreviewKeyEvent { keyEvent ->
                                            val keyCode = keyEvent.nativeKeyEvent.keyCode
                                            val isEnterOrTab = keyCode == android.view.KeyEvent.KEYCODE_ENTER ||
                                                    keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER ||
                                                    keyCode == android.view.KeyEvent.KEYCODE_TAB ||
                                                    keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN
                                            if (isEnterOrTab) {
                                                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                                    stockFocusRequester.requestFocus()
                                                    keyboardController?.hide()
                                                }
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                        .testTag("barcode_input")
                                )

                                Button(
                                    onClick = {
                                        if (cameraPermissionState.status.isGranted) {
                                            showScannerView = true
                                        } else {
                                            cameraPermissionState.launchPermissionRequest()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorAccentL),
                                    border = BorderStroke(1.5.dp, ColorAccent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .size(54.dp)
                                        .testTag("scan_barcode_btn"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan Barcode",
                                        tint = ColorAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // OR Separator Divider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Divider(modifier = Modifier.weight(1f), color = ColorBorder)
                                Text("OR", color = ColorSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Divider(modifier = Modifier.weight(1f), color = ColorBorder)
                            }

                            // Article Code Input
                            GalaFormTextField(
                                value = articleCode,
                                onValueChange = viewModel::setArticleCode,
                                label = "ARTICLE CODE",
                                placeholder = "Enter article number...",
                                leadingIcon = Icons.Default.Tag,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        stockFocusRequester.requestFocus()
                                    }
                                ),
                                modifier = Modifier
                                    .focusRequester(articleFocusRequester)
                                    .testTag("article_code_input")
                            )
                        }
                    }
                }
            }

            // SECTION 2: ARTICLE DETAILS & INPUTS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(ColorPrimaryL, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("2", color = ColorPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "ENTER STOCK & EXPIRY",
                            color = ColorInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ColorBorder, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Read-only info section
                            val isArticleFound = description.isNotBlank()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isArticleFound) ColorPrimaryL.copy(alpha = 0.5f) else Color(0xFFF8FAFC))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Description Row
                                    Column {
                                        Text(
                                            text = "DESCRIPTION",
                                            color = ColorMuted,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.8.sp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = description.ifBlank { "Scan or search above..." },
                                            color = if (isArticleFound) ColorInk else ColorSoft,
                                            fontSize = 15.sp,
                                            fontWeight = if (isArticleFound) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }

                                    // Department Row
                                    Column {
                                        Text(
                                            text = "DEPARTMENT",
                                            color = ColorMuted,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.8.sp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = department.ifBlank { "Scan or search above..." },
                                            color = if (isArticleFound) ColorInk else ColorSoft,
                                            fontSize = 15.sp,
                                            fontWeight = if (isArticleFound) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Divider(color = ColorBorder)

                            // Form inputs: Stock Qty & Expiry
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                GalaFormTextField(
                                    value = stock,
                                    onValueChange = viewModel::setStock,
                                    label = "STOCK QUANTITY",
                                    placeholder = "e.g. 5",
                                    leadingIcon = Icons.Default.Numbers,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            keyboardController?.hide()
                                            if (expiry.isBlank()) {
                                                datePickerDialog.show()
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .focusRequester(stockFocusRequester)
                                        .testTag("stock_input")
                                )

                                // Expiry Date Selector Button
                                Column {
                                    Text(
                                        text = "EXPIRY DATE",
                                        color = ColorMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    Surface(
                                        onClick = { datePickerDialog.show() },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.5.dp, if (expiry.isNotBlank()) ColorAccent else ColorBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarToday,
                                                    contentDescription = null,
                                                    tint = if (expiry.isNotBlank()) ColorAccent else ColorSoft,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = expiry.ifBlank { "Select Expiry Date..." },
                                                    color = if (expiry.isNotBlank()) ColorInk else ColorSoft,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (expiry.isNotBlank()) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = ColorSoft
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ACTIONS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (isLocked) {
                                showToast(
                                    context,
                                    "Submission Completed For This Week, If any Updation Needed-Contact Buyer Directly"
                                )
                            } else {
                                val alreadySubmittedRow = reviewRows.firstOrNull { thisWeek ->
                                    isSameItemExpiry(
                                        barcodeA = barcode, articleA = articleCode, descA = description, expiryA = expiry,
                                        barcodeB = thisWeek.Barcode, articleB = thisWeek.Article, descB = thisWeek.Description, expiryB = thisWeek.ExpiryDate
                                    )
                                }
                                if (alreadySubmittedRow != null) {
                                    showDuplicateSubmittedDialog = true
                                } else {
                                    val matchingFollowUpRow = lastWeekRows.firstOrNull { row ->
                                        isSameItemExpiry(
                                            barcodeA = barcode, articleA = articleCode, descA = description, expiryA = expiry,
                                            barcodeB = row.Barcode, articleB = row.Article, descB = row.Description, expiryB = row.ExpiryDate
                                        )
                                    }
                                    if (matchingFollowUpRow != null) {
                                        showFollowUpConfirmDialog = true
                                    } else {
                                        viewModel.submitExpiryItem {
                                            showToast(
                                                context,
                                                "Item successfully added!"
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_item_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLocked) Color(0xFFCBD5E1) else ColorPrimary,
                            contentColor = if (isLocked) Color(0xFF64748B) else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = isLocked || (selectedWeek.isNotBlank() && description.isNotBlank() && stock.isNotBlank() && expiry.isNotBlank() && !isSubmitting)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isLocked) Color(0xFF64748B) else Color.White
                                )
                                Text(
                                    text = if (isLocked) "Submission Locked" else "Submit Expiry Item",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isLocked) Color(0xFF64748B) else Color.White
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onBackToHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ColorMuted),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, ColorBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Back to Home", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Follow-up item stock update confirmation dialog
        if (showFollowUpConfirmDialog) {
            val matchingFollowUpRow = lastWeekRows.firstOrNull { row ->
                isSameItemExpiry(
                    barcodeA = barcode, articleA = articleCode, descA = description, expiryA = expiry,
                    barcodeB = row.Barcode, articleB = row.Article, descB = row.Description, expiryB = row.ExpiryDate
                )
            }
            if (matchingFollowUpRow != null) {
                AlertDialog(
                    onDismissRequest = { showFollowUpConfirmDialog = false },
                    title = {
                        Text(
                            text = "Item Already On Follow-Up. Update Stock Count?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorInk
                        )
                    },
                    text = {
                        Text(
                            text = "This item (${matchingFollowUpRow.Description ?: matchingFollowUpRow.Article}) with expiry date $expiry is already on follow-up from last week. Do you want to update its stock count to $stock instead of adding it as a separate record?",
                            fontSize = 13.sp,
                            color = ColorInk
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val matchingReviewRow = reviewRows.firstOrNull { thisWeek ->
                                    isSameItemExpiry(
                                        barcodeA = barcode, articleA = articleCode, descA = description, expiryA = expiry,
                                        barcodeB = thisWeek.Barcode, articleB = thisWeek.Article, descB = thisWeek.Description, expiryB = thisWeek.ExpiryDate
                                    )
                                }
                                if (matchingReviewRow != null && matchingReviewRow.RowIndex != null) {
                                    // already has a record this week: update its stock
                                    viewModel.editReviewRow(
                                        rowIndex = matchingReviewRow.RowIndex,
                                        arrayIndex = -1,
                                        stock = stock,
                                        expiry = expiry,
                                        onSuccess = {
                                            showFollowUpConfirmDialog = false
                                            viewModel.clearInputs()
                                            showToast(context, "Stock count updated successfully!")
                                        },
                                        onError = {
                                            showToast(context, it)
                                        }
                                    )
                                } else {
                                    // not yet added this week: submit as follow-up stock
                                    viewModel.submitFollowUpStock(
                                        stockRow = matchingFollowUpRow,
                                        newStock = stock,
                                        onSuccess = {
                                            showFollowUpConfirmDialog = false
                                            viewModel.clearInputs()
                                            viewModel.startFollowUp()
                                            viewModel.loadThisWeekReview()
                                            showToast(context, "Follow-up completed and stock updated!")
                                        },
                                        onError = {
                                            showToast(context, it)
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
                        ) {
                            Text("Yes", color = Color.White)
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = { showFollowUpConfirmDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ColorMuted),
                            border = BorderStroke(1.dp, ColorBorder)
                        ) {
                            Text("No")
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color.White
                )
            }
        }

        if (showDuplicateSubmittedDialog) {
            AlertDialog(
                onDismissRequest = { showDuplicateSubmittedDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚠️", fontSize = 20.sp)
                        Text(
                            text = "Item Already Submitted",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorInk
                        )
                    }
                },
                text = {
                    Text(
                        text = "Item Already Submitted For This Week, You May Edit In Review page If Edit Needed.",
                        fontSize = 13.5.sp,
                        color = ColorInk
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showDuplicateSubmittedDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
                    ) {
                        Text("OK", color = Color.White)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White
            )
        }

        // Camera barcode scanner dialog
        if (showScannerView) {
            CameraBarcodeScanner(
                onBarcodeScanned = { scanned ->
                    viewModel.setBarcode(scanned)
                    showScannerView = false
                    // Auto focus stock quantity input field!
                    stockFocusRequester.requestFocus()
                },
                onClose = { showScannerView = false }
            )
        }
    }
}

/* ══ GENERIC TEXTFIELD WRAPPER ══ */
@Composable
fun BasicTextFieldWithPlaceholder(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Box(contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) {
            Text(text = placeholder, color = ColorSoft, fontSize = 15.sp)
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = keyboardOptions,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = ColorInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/* ══ UNIFIED PREMIUM FORM TEXT FIELD ══ */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalaFormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, fontWeight = FontWeight.Bold, fontSize = 10.sp, style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)) },
        placeholder = { Text(text = placeholder, color = ColorSoft, fontSize = 13.sp) },
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ColorPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ColorPrimary,
            unfocusedBorderColor = ColorBorder,
            focusedLabelColor = ColorPrimary,
            unfocusedLabelColor = ColorMuted,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFF8FAFC),
            focusedTextColor = ColorInk,
            unfocusedTextColor = ColorInk
        )
    )
}

/* ══ UNIVERSAL SEARCH BAR COMPONENT ══ */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search barcode, description, article...",
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 12.sp,
            color = ColorInk
        ),
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = query,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = {
                    Text(
                        text = placeholder,
                        color = ColorSoft,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ColorPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = ColorMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrimary,
                            unfocusedBorderColor = ColorBorder,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            )
        }
    )
}

/* ══ FOLLOW UP LAST WEEK SCREEN ══ */
@Composable
fun FollowUpScreen(
    viewModel: GalaViewModel,
    storeName: String,
    selectedWeek: String,
    isLocked: Boolean,
    onBackToHome: () -> Unit
) {
    val lastWeekRows by viewModel.lastWeekRows.collectAsState()
    val lastWeekLoading by viewModel.lastWeekLoading.collectAsState()
    val lastWeekError by viewModel.lastWeekError.collectAsState()
    val lastWeekLabel by viewModel.lastWeekLabel.collectAsState()
    val reviewRows by viewModel.reviewRows.collectAsState()

    var showUpdateSheet by remember { mutableStateOf(false) }
    var selectedStockRow by remember { mutableStateOf<LastWeekRowDto?>(null) }
    var updatedStockValue by remember { mutableStateOf("") }
    var followUpActionError by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(followUpActionError) {
        if (followUpActionError != null) {
            kotlinx.coroutines.delay(3500)
            followUpActionError = null
        }
    }

    val filteredLastWeekRows = remember(lastWeekRows, searchQuery) {
        if (searchQuery.isBlank()) {
            lastWeekRows
        } else {
            val q = searchQuery.trim().lowercase()
            lastWeekRows.filter { row ->
                (row.Barcode?.lowercase()?.contains(q) == true) ||
                (row.Description?.lowercase()?.contains(q) == true) ||
                (row.Article?.lowercase()?.contains(q) == true)
            }
        }
    }

    val matchingReviewMap = remember(lastWeekRows, reviewRows) {
        if (lastWeekRows.isEmpty() || reviewRows.isEmpty()) {
            emptyMap<LastWeekRowDto, ThisWeekRowDto?>()
        } else {
            val barcodeMap = mutableMapOf<String, ThisWeekRowDto>()
            val articleMap = mutableMapOf<String, ThisWeekRowDto>()
            val descMap = mutableMapOf<String, ThisWeekRowDto>()

            for (thisWeek in reviewRows) {
                val exp = normalizeDateString(thisWeek.ExpiryDate)
                if (exp.isBlank()) continue

                val b = cleanBarcode(thisWeek.Barcode)
                if (b.isNotBlank()) {
                    barcodeMap.putIfAbsent("$b|$exp", thisWeek)
                }
                val a = cleanCode(thisWeek.Article)
                if (a.isNotBlank()) {
                    articleMap.putIfAbsent("$a|$exp", thisWeek)
                }
                val d = cleanCode(thisWeek.Description)
                if (d.isNotBlank()) {
                    descMap.putIfAbsent("$d|$exp", thisWeek)
                }
            }

            lastWeekRows.associateWith { row ->
                val exp = normalizeDateString(row.ExpiryDate)
                if (exp.isBlank()) null
                else {
                    val b = cleanBarcode(row.Barcode)
                    val a = cleanCode(row.Article)
                    val d = cleanCode(row.Description)

                    var matched: ThisWeekRowDto? = null
                    if (b.isNotBlank()) matched = barcodeMap["$b|$exp"]
                    if (matched == null && a.isNotBlank()) matched = articleMap["$a|$exp"]
                    if (matched == null && b.isBlank() && a.isBlank() && d.isNotBlank()) matched = descMap["$d|$exp"]
                    matched
                }
            }
        }
    }

    val totalCount = lastWeekRows.size
    val completedCount = remember(lastWeekRows, matchingReviewMap) {
        lastWeekRows.count { matchingReviewMap[it] != null }
    }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadThisWeekReview()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            val followUpTitle = remember(lastWeekLabel) {
                if (lastWeekLabel.isBlank()) {
                    "Follow Up"
                } else {
                    var clean = lastWeekLabel.removePrefix("Last week: ").removePrefix("Follow Up for ").trim()
                    val ymRegex = Regex("""\(?\b(20\d{2})-(0[1-9]|1[0-2])\b\)?""")
                    clean = ymRegex.replace(clean) { matchResult ->
                        val monthNum = matchResult.groupValues[2].toIntOrNull() ?: 1
                        val dfs = java.text.DateFormatSymbols.getInstance(java.util.Locale.US)
                        dfs.months[monthNum - 1]
                    }.trim()
                    if (clean.startsWith("Follow Up for ")) {
                        clean
                    } else {
                        "Follow Up for $clean"
                    }
                }
            }

            Text(
                text = followUpTitle,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ColorInk,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Text(
                text = "Completed $completedCount/$totalCount",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (totalCount > 0 && completedCount == totalCount) Color(0xFF15803D) else ColorPrimary,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            UniversalSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search barcode, description, article...",
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Lock notice banner
            if (isLocked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF991B1B),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Submission Completed For This Week, If any Updation Needed-Contact Buyer Directly",
                        fontSize = 12.sp,
                        color = Color(0xFF991B1B),
                        fontWeight = FontWeight.Bold,
                        lineHeight = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Content body list of last week products
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (lastWeekLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ColorPrimary, modifier = Modifier.size(34.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading last week data…", color = ColorMuted, fontSize = 13.sp)
                    }
                } else if (lastWeekError != null) {
                    Text(
                        text = lastWeekError ?: "Error",
                        color = ColorAccent,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                } else if (lastWeekRows.isEmpty()) {
                    Text(
                        text = "No items found from last week.",
                        color = ColorMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                } else if (filteredLastWeekRows.isEmpty()) {
                    Text(
                        text = "No items matching \"$searchQuery\"",
                        color = ColorMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(filteredLastWeekRows, key = { idx, row -> "${row.Barcode ?: ""}_${row.Article ?: ""}_${row.ExpiryDate ?: ""}_$idx" }) { idx, row ->
                            val matchingReviewRow = matchingReviewMap[row]
                            val isCompleted = matchingReviewRow != null

                            val displayStock = if (isCompleted && !matchingReviewRow?.Stock.isNullOrBlank()) {
                                matchingReviewRow!!.Stock!!
                            } else {
                                row.Stock ?: "0"
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isCompleted) Color(0xFF86EFAC) else if (isLocked) Color(0xFFE2E8F0) else ColorBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isLocked) {
                                            showToast(
                                                context,
                                                "Submission Completed For This Week, If any Updation Needed-Contact Buyer Directly"
                                            )
                                        } else {
                                            selectedStockRow = row
                                            updatedStockValue = matchingReviewRow?.Stock ?: ""
                                            followUpActionError = null
                                            showUpdateSheet = true
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCompleted) Color(0xFFF2FBF6) else if (isLocked) Color(0xFFF8FAFC) else Color.White
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Text(
                                                text = row.Article ?: "",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = ColorInk
                                            )
                                            if (!row.Barcode.isNullOrBlank()) {
                                                Text(
                                                    text = "(${row.Barcode})",
                                                    fontSize = 11.sp,
                                                    color = ColorMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(ColorPrimaryL, RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color(0xFFC3E6D4), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Stock: $displayStock",
                                                    color = ColorPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            if (isCompleted) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFDCFCE7), RoundedCornerShape(6.dp))
                                                        .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Completed",
                                                        color = Color(0xFF15803D),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = row.Description ?: "No description",
                                        fontWeight = FontWeight.Bold,
                                        color = ColorInk,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Dept: ${row.Department ?: "—"}",
                                            color = ColorMuted,
                                            fontSize = 11.sp
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = "Expiry: ${row.ExpiryDate ?: "—"}",
                                                color = Color(0xFFD97706),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Divider(color = ColorBorder)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onBackToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ColorMuted),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ColorBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Back to Home", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Action Sheet overlay to edit Stock
        if (showUpdateSheet && selectedStockRow != null) {
            val sRow = selectedStockRow!!
            Dialog(
                onDismissRequest = { showUpdateSheet = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showUpdateSheet = false }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .clickable(enabled = false) {},
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                                    .padding(20.dp)
                            ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp, 4.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(ColorBorder)
                                    .align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Update Stock",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorInk
                            )
                            Text(
                                text = "Enter new stock count for this week",
                                fontSize = 12.5.sp,
                                color = ColorMuted
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Item Detail Read Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, ColorBorder, RoundedCornerShape(10.dp)),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Article: ${sRow.Article} (${sRow.Barcode ?: ""})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = ColorInk
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Description: ${sRow.Description}",
                                        fontSize = 12.5.sp,
                                        color = ColorMuted
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Expiry Date: ${sRow.ExpiryDate}",
                                        fontSize = 12.5.sp,
                                        color = ColorMuted
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Last Week Stock: ${sRow.Stock}",
                                        fontSize = 12.5.sp,
                                        color = ColorMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Input Box for current stock count
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.5.dp, ColorPrimary, RoundedCornerShape(10.dp)),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "NEW STOCK COUNT (THIS WEEK)",
                                        color = ColorSoft,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = updatedStockValue,
                                        onValueChange = { updatedStockValue = it },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                            }
                                        ),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            color = ColorPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            if (followUpActionError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = followUpActionError ?: "",
                                    color = ColorAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val matchingReviewRow = reviewRows.firstOrNull { thisWeek ->
                                        isSameItemExpiry(
                                            barcodeA = sRow.Barcode, articleA = sRow.Article, descA = sRow.Description, expiryA = sRow.ExpiryDate,
                                            barcodeB = thisWeek.Barcode, articleB = thisWeek.Article, descB = thisWeek.Description, expiryB = thisWeek.ExpiryDate
                                        )
                                    }

                                    if (matchingReviewRow != null && matchingReviewRow.RowIndex != null) {
                                        // Item already completed: perform an edit instead of creating a duplicate entry
                                        viewModel.editReviewRow(
                                            rowIndex = matchingReviewRow.RowIndex,
                                            arrayIndex = -1,
                                            stock = updatedStockValue,
                                            expiry = matchingReviewRow.ExpiryDate ?: "",
                                            onSuccess = {
                                                showUpdateSheet = false
                                                viewModel.loadThisWeekReview()
                                            },
                                            onError = {
                                                followUpActionError = it
                                            }
                                        )
                                    } else {
                                        // Item not completed: perform standard follow-up submission
                                        viewModel.submitFollowUpStock(
                                            stockRow = sRow,
                                            newStock = updatedStockValue,
                                            onSuccess = {
                                                showUpdateSheet = false
                                                viewModel.loadThisWeekReview()
                                                viewModel.startFollowUp()
                                            },
                                            onError = {
                                                followUpActionError = it
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ColorBlueCombined, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Text("Save & Continue", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { showUpdateSheet = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ColorMuted),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ColorBorder)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
    }
}

/* ══ WEEKLY LOGS REVIEW & SUBMISSIONS SCREEN ══ */
@Composable
fun ReviewScreen(
    viewModel: GalaViewModel,
    selectedWeek: String,
    isLocked: Boolean,
    onBackToHome: () -> Unit
) {
    val reviewRows by viewModel.reviewRows.collectAsState()
    val reviewLoading by viewModel.reviewLoading.collectAsState()
    val reviewError by viewModel.reviewError.collectAsState()
    val reviewTonalMessage by viewModel.reviewTonalMessage.collectAsState()
    val emailSending by viewModel.emailSending.collectAsState()
    val articles by viewModel.articles.collectAsState()
    val lastWeekRows by viewModel.lastWeekRows.collectAsState()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val allFollowUpCompleted = remember(lastWeekRows, reviewRows) {
        lastWeekRows.all { row ->
            reviewRows.any { thisWeek ->
                isSameItemExpiry(
                    barcodeA = row.Barcode, articleA = row.Article, descA = row.Description, expiryA = row.ExpiryDate,
                    barcodeB = thisWeek.Barcode, articleB = thisWeek.Article, descB = thisWeek.Description, expiryB = thisWeek.ExpiryDate
                )
            }
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var expandedItemIndex by remember { mutableStateOf<Int?>(null) }
    var selectedEditRow by remember { mutableStateOf<ThisWeekRowDto?>(null) }
    var selectedEditIndex by remember { mutableStateOf(-1) }
    var editStockValue by remember { mutableStateOf("") }
    var editExpiryValue by remember { mutableStateOf("") }
    var editErrorMsg by remember { mutableStateOf<String?>(null) }
    var isSavingEdit by remember { mutableStateOf(false) }

    // State variables for delete confirmation popup
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSendConfirmDialog by remember { mutableStateOf(false) }
    var itemToDeleteRowIndex by remember { mutableStateOf<Int?>(null) }
    var itemToDeleteArrayIndex by remember { mutableStateOf(-1) }
    var itemToDeleteDescription by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredReviewRows = remember(reviewRows, searchQuery) {
        if (searchQuery.isBlank()) {
            reviewRows
        } else {
            val q = searchQuery.trim().lowercase()
            reviewRows.filter { row ->
                (row.Barcode?.lowercase()?.contains(q) == true) ||
                (row.Description?.lowercase()?.contains(q) == true) ||
                (row.Article?.lowercase()?.contains(q) == true)
            }
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val calendar = java.util.Calendar.getInstance()
    val editDatePickerDialog = android.app.DatePickerDialog(
        context,
        { _, cyLocal, cmLocal, cdLocal ->
            val formattedDate = String.format(
                java.util.Locale.US,
                "%04d-%02d-%02d",
                cyLocal,
                cmLocal + 1,
                cdLocal
            )
            editExpiryValue = formattedDate
        },
        calendar.get(java.util.Calendar.YEAR),
        calendar.get(java.util.Calendar.MONTH),
        calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )

    LaunchedEffect(Unit) {
        viewModel.loadThisWeekReview()
        viewModel.startFollowUp()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Redesigned Header: "Weekly submissions" with "3 items" pill badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Weekly submissions",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ColorInk
            )
            
            // Total Items Pill Badge
            Box(
                modifier = Modifier
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "${filteredReviewRows.size}/${reviewRows.size} items" else "${reviewRows.size} items",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle: "Submitted this week · Week 2 (8-14)"
        Text(
            text = "Submitted this week · $selectedWeek",
            fontSize = 12.5.sp,
            color = ColorMuted
        )

        Spacer(modifier = Modifier.height(10.dp))

        UniversalSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Search barcode, description, article...",
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Scrollable Table view of recent additions
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (reviewLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ColorPrimary, modifier = Modifier.size(34.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Loading this week's items…", color = ColorMuted, fontSize = 13.sp)
                }
            } else if (reviewError != null) {
                Text(
                    text = reviewError ?: "Error",
                    color = ColorAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (reviewRows.isEmpty()) {
                Text(
                    text = "No items submitted this week yet.",
                    color = ColorMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            } else if (filteredReviewRows.isEmpty()) {
                Text(
                    text = "No items matching \"$searchQuery\"",
                    color = ColorMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(filteredReviewRows, key = { idx, row -> "${row.Barcode ?: ""}_${row.Article ?: ""}_${row.ExpiryDate ?: ""}_$idx" }) { idx, row ->
                        val isExpanded = expandedItemIndex == idx
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isExpanded) ColorPrimary.copy(alpha = 0.5f) else ColorBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    expandedItemIndex = if (isExpanded) null else idx
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = row.Article ?: "",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = ColorInk
                                        )
                                        val resolvedBarcode = row.Barcode?.takeIf { it.isNotBlank() }
                                            ?: articles.firstOrNull { it.article.trim() == row.Article?.trim() }?.barcode?.takeIf { it.isNotBlank() }
                                        if (!resolvedBarcode.isNullOrBlank()) {
                                            Text(
                                                text = "  $resolvedBarcode",
                                                fontSize = 11.sp,
                                                color = ColorMuted
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(ColorPrimaryL, RoundedCornerShape(6.dp))
                                            .border(1.dp, Color(0xFFC3E6D4), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Stock: ${row.Stock ?: "0"}",
                                            color = ColorPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = row.Description ?: "No description",
                                    fontWeight = FontWeight.Bold,
                                    color = ColorInk,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Dept: ${row.Department ?: "—"}",
                                        color = ColorMuted,
                                        fontSize = 11.sp
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "Expiry: ${row.ExpiryDate ?: "—"}",
                                            color = Color(0xFFD97706),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(ColorBorder.copy(alpha = 0.5f))
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Edit Button (Half width with text)
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isLocked) Color(0xFFF1F5F9) else Color(0xFFEFF6FF))
                                                    .clickable {
                                                        if (isLocked) {
                                                            showToast(
                                                                context,
                                                                "Submission Completed For This Week, If any Updation Needed-Contact Buyer Directly"
                                                            )
                                                        } else {
                                                            selectedEditRow = row
                                                            selectedEditIndex = idx
                                                            editStockValue = row.Stock ?: ""
                                                            editExpiryValue = row.ExpiryDate ?: ""
                                                            editErrorMsg = null
                                                            isSavingEdit = false
                                                            showEditDialog = true
                                                        }
                                                    },
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit item",
                                                    tint = if (isLocked) Color(0xFF94A3B8) else Color(0xFF2563EB),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Edit",
                                                    color = if (isLocked) Color(0xFF94A3B8) else Color(0xFF2563EB),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Delete Button (Half width with text)
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isLocked) Color(0xFFF1F5F9) else Color(0xFFFEE2E2))
                                                    .clickable {
                                                        if (isLocked) {
                                                            showToast(
                                                                context,
                                                                "Submission Completed For This Week, If any Updation Needed-Contact Buyer Directly"
                                                            )
                                                        } else {
                                                            row.RowIndex?.let { rIdx ->
                                                                itemToDeleteRowIndex = rIdx
                                                                itemToDeleteArrayIndex = idx
                                                                itemToDeleteDescription = row.Description ?: "No description"
                                                                showDeleteDialog = true
                                                            }
                                                        }
                                                    },
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete item",
                                                    tint = if (isLocked) Color(0xFF94A3B8) else Color(0xFFDC2626),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Delete",
                                                    color = if (isLocked) Color(0xFF94A3B8) else Color(0xFFDC2626),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
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
        }

        if (reviewTonalMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (reviewTonalMessage!!.contains("✅")) ColorPrimaryL else ColorAccentL,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (reviewTonalMessage!!.contains("✅")) Color(0xFFBBF7D0) else ColorAccent.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = reviewTonalMessage ?: "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (reviewTonalMessage!!.contains("✅")) Color(0xFF15803D) else ColorAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pinned Bottom buttons matching screenshot layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Back Button
            Button(
                onClick = onBackToHome,
                modifier = Modifier
                    .size(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ColorInk),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                border = BorderStroke(1.dp, ColorBorder.copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to home",
                    modifier = Modifier.size(20.dp),
                    tint = ColorInk
                )
            }

            // "Sent Submission Email" button (Brown-orange color, pill-shaped)
            Button(
                onClick = {
                    if (isLocked) {
                        showToast(
                            context,
                            "Submission Completed For This Week, If any Updation Needed-Contact Buyer Directly"
                        )
                    } else if (!allFollowUpCompleted) {
                        showToast(
                            context,
                            "Follow-Up Not Completed. Complete the follow up and Try Again"
                        )
                    } else {
                        showSendConfirmDialog = true
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("send_email_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLocked) Color(0xFFCBD5E1) else Color(0xFF9E3E1F),
                    contentColor = if (isLocked) Color(0xFF64748B) else Color.White
                ),
                shape = RoundedCornerShape(24.dp), // Pill-shaped as requested
                enabled = (reviewRows.isNotEmpty() || isLocked) && !emailSending
            ) {
                if (emailSending) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = if (isLocked) Color(0xFF64748B) else Color.White
                        )
                        Text(
                            text = if (isLocked) "Submission Completed" else "Sent Submission Email",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isLocked) Color(0xFF64748B) else Color.White
                        )
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text(
                        text = "Confirm Deletion",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ColorInk
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete \"$itemToDeleteDescription\"? This action cannot be undone.",
                        fontSize = 14.sp,
                        color = ColorMuted
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val rIdx = itemToDeleteRowIndex
                            val aIdx = itemToDeleteArrayIndex
                            if (rIdx != null) {
                                viewModel.deleteReviewRow(rowIndex = rIdx, arrayIndex = aIdx) {
                                    // Deleted complete
                                }
                            }
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorAccent)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = ColorMuted)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }

    // Edit Dialog overlay for Log Review row editing
    if (showEditDialog && selectedEditRow != null) {
        val sRow = selectedEditRow!!
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showEditDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showEditDialog = false }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .clickable(enabled = false) {}, // prevent click-through
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                                    .padding(20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp, 4.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(ColorBorder)
                                        .align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = "Edit Expiry Item",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorInk
                                )
                                Text(
                                    text = "Update stock quantity and expiry date for this item",
                                    fontSize = 12.5.sp,
                                    color = ColorMuted
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Item Detail Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, ColorBorder, RoundedCornerShape(10.dp)),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Article: ${sRow.Article ?: ""}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = ColorInk
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = sRow.Description ?: "",
                                            fontSize = 12.5.sp,
                                            color = ColorMuted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Input Box for stock count
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.5.dp, ColorPrimary, RoundedCornerShape(10.dp)),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "STOCK QUANTITY",
                                            color = ColorSoft,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = editStockValue,
                                            onValueChange = { editStockValue = it },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onDone = {
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                }
                                            ),
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = ColorPrimary,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Expiry Date Selection Button
                                Column {
                                    Text(
                                        text = "EXPIRY DATE",
                                        color = ColorMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    Surface(
                                        onClick = { editDatePickerDialog.show() },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.5.dp, if (editExpiryValue.isNotBlank()) ColorAccent else ColorBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarToday,
                                                    contentDescription = null,
                                                    tint = if (editExpiryValue.isNotBlank()) ColorAccent else ColorSoft,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = editExpiryValue.ifBlank { "Select Expiry Date..." },
                                                    color = if (editExpiryValue.isNotBlank()) ColorInk else ColorSoft,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (editExpiryValue.isNotBlank()) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                if (editErrorMsg != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = editErrorMsg ?: "",
                                        color = ColorAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        if (editStockValue.isBlank()) {
                                            editErrorMsg = "Stock quantity is required"
                                            return@Button
                                        }
                                        if (editExpiryValue.isBlank()) {
                                            editErrorMsg = "Expiry date is required"
                                            return@Button
                                        }
                                        isSavingEdit = true
                                        viewModel.editReviewRow(
                                            rowIndex = sRow.RowIndex ?: -1,
                                            arrayIndex = selectedEditIndex,
                                            stock = editStockValue,
                                            expiry = editExpiryValue,
                                            onSuccess = {
                                                showEditDialog = false
                                                isSavingEdit = false
                                            },
                                            onError = {
                                                editErrorMsg = it
                                                isSavingEdit = false
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorBlueCombined, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !isSavingEdit
                                ) {
                                    if (isSavingEdit) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                            Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { showEditDialog = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ColorMuted),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ColorBorder)
                                ) {
                                    Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

    if (showSendConfirmDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showSendConfirmDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Confirm Email Submission",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorInk,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val confirmMsg = androidx.compose.ui.text.buildAnnotatedString {
                        append("⚠️ Before sending the email, please review and confirm that all entries are accurate. This function is intended for ")
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold))
                        append("Manager")
                        pop()
                        append(" use only.\n\n")
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                        append("Once the submission is completed, you cannot add or edit data.")
                        pop()
                    }

                    Text(
                        text = confirmMsg,
                        fontSize = 14.sp,
                        color = ColorInk,
                        lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel Button
                        Button(
                            onClick = { showSendConfirmDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ColorInk),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ColorBorder)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Send Button (Red)
                        Button(
                            onClick = {
                                showSendConfirmDialog = false
                                viewModel.sendSubmissionEmail {}
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Send", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/* ══ NATIVE ML KIT BARCODE SCANNER CAMERA VIEW ══ */
@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraBarcodeScanner(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var cameraInitialized by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // ML Kit Scanner setup
                    val scanner = BarcodeScanning.getClient()

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        barcode.rawValue?.let { value ->
                                            if (value.isNotBlank()) {
                                                onBarcodeScanned(value)
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    it.printStackTrace()
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        cameraInitialized = true
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }

                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Semi-transparent Scanning overlay guidelines
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Center the barcode inside the safe area",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close barcode scanner view",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Viewfinder Guide rectangle
            Box(
                modifier = Modifier
                    .size(280.dp, 160.dp)
                    .border(2.dp, ColorPrimary, RoundedCornerShape(12.dp))
                    .background(Color.Transparent)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!cameraInitialized) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "📷 Scanning retail barcodes...",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

/* ══ STORE VIEW (DASHBOARD) SCREEN ══ */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreViewScreen(
    viewModel: GalaViewModel,
    onBackToHome: () -> Unit
) {
    val dashboardRows by viewModel.storeDashboardRows.collectAsState()
    val loading by viewModel.storeDashboardLoading.collectAsState()
    val error by viewModel.storeDashboardError.collectAsState()

    var dashMonthFilter by remember { mutableStateOf("") }
    var dashWeekFilter by remember { mutableStateOf("") }
    var dashDeptFilter by remember { mutableStateOf("") }
    var dashDrillFilter by remember { mutableStateOf("") }

    var monthExpanded by remember { mutableStateOf(false) }
    var weekExpanded by remember { mutableStateOf(false) }
    var deptExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadStoreDashboardData()
    }

    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = ColorPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Loading store data...", color = ColorMuted, fontSize = 14.sp)
            }
        }
    } else if (error != null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "⚠️ Error loading data",
                    color = ColorAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error ?: "Unknown error",
                    color = ColorInk,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.loadStoreDashboardData() },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
                ) {
                    Text("Retry", color = Color.White)
                }
            }
        }
    } else {
        val monthsList = remember(dashboardRows) {
            dashboardRows.mapNotNull { it.SubMonth }.filter { it.isNotBlank() }.distinct().sorted()
        }
        val deptsList = remember(dashboardRows) {
            dashboardRows.mapNotNull { it.Department }.filter { it.isNotBlank() }.distinct().sorted()
        }
        val monthDisplayMap = remember(monthsList) {
            monthsList.associateWith { getSubMonthDisp(it, null) }
        }

        // Apply base filters
        val filteredRows = remember(dashboardRows, dashMonthFilter, dashWeekFilter, dashDeptFilter) {
            dashboardRows.filter { row ->
                val matchMonth = dashMonthFilter.isBlank() || row.SubMonth == dashMonthFilter
                val matchWeek = dashWeekFilter.isBlank() || row.Week == dashWeekFilter
                val matchDept = dashDeptFilter.isBlank() || row.Department == dashDeptFilter
                matchMonth && matchWeek && matchDept
            }
        }

        // Apply drill down filter
        val drilledRows = remember(filteredRows, dashDrillFilter) {
            if (dashDrillFilter.isBlank()) {
                filteredRows
            } else {
                filteredRows.filter { row ->
                    val days = getDaysLeft(row.ExpiryDate, row.DaysLeft)
                    when (dashDrillFilter) {
                        "critical_7" -> days != null && days <= 7
                        "less_30" -> days != null && days <= 30
                        "<7 Days" -> {
                            val b = getBucket(days)
                            b == "Expired" || b == "<7 Days"
                        }
                        "7–30 Days" -> getBucket(days) == "7–30 Days"
                        "31–60 Days" -> getBucket(days) == "31–60 Days"
                        "61–90 Days" -> getBucket(days) == "61–90 Days"
                        ">90 Days" -> getBucket(days) == ">90 Days"
                        else -> true
                    }
                }
            }
        }

        val drillLabel = when (dashDrillFilter) {
            "critical_7" -> "Critical ≤7 Days"
            "less_30" -> "≤30 Days Expiry"
            "<7 Days" -> "Risk: <7 Days"
            "7–30 Days" -> "Risk: 7–30 Days"
            "31–60 Days" -> "Risk: 31–60 Days"
            "61–90 Days" -> "Risk: 61–90 Days"
            ">90 Days" -> "Risk: >90 Days"
            else -> ""
        }

        // Compute KPI Values
        val itemsCount = filteredRows.size
        
        val criticalCount = remember(filteredRows) {
            filteredRows.count { row ->
                val days = getDaysLeft(row.ExpiryDate, row.DaysLeft)
                days != null && days <= 7
            }
        }
        
        val less30Count = remember(filteredRows) {
            filteredRows.count { row ->
                val days = getDaysLeft(row.ExpiryDate, row.DaysLeft)
                days != null && days <= 30
            }
        }

        val allTimeCount = dashboardRows.size

        // Chart buckets counts
        val bucketCounts = remember(filteredRows) {
            val buckets = mutableMapOf(
                "<7 Days" to 0,
                "7–30 Days" to 0,
                "31–60 Days" to 0,
                "61–90 Days" to 0,
                ">90 Days" to 0
            )
            filteredRows.forEach { row ->
                val days = getDaysLeft(row.ExpiryDate, row.DaysLeft)
                val b = getBucket(days)
                if (b == "Expired" || b == "<7 Days") {
                    buckets["<7 Days"] = (buckets["<7 Days"] ?: 0) + 1
                } else if (buckets.containsKey(b)) {
                    buckets[b] = (buckets[b] ?: 0) + 1
                }
            }
            buckets
        }
        val maxBucketCount = bucketCounts.values.maxOrNull() ?: 0

        // Week over week data
        val weeksList = listOf("Week 1 (1–7)", "Week 2 (8–14)", "Week 3 (15–21)", "Week 4 (22–31)")
        val weekCounts = remember(filteredRows) {
            weeksList.map { w ->
                w to filteredRows.count { it.Week == w }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackToHome,
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, ColorBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go back to Home Selector Screen",
                            tint = ColorPrimary
                        )
                    }

                    Text(
                        text = "Store Insights Dashboard",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPrimary
                    )

                    IconButton(
                        onClick = { viewModel.loadStoreDashboardData() },
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, ColorBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Dashboard data from server",
                            tint = ColorPrimary
                        )
                    }
                }
            }

            // Filters Panel
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, ColorBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DATA FILTERS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorPrimary,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
                            )

                            if (dashMonthFilter.isNotBlank() || dashWeekFilter.isNotBlank() || dashDeptFilter.isNotBlank() || dashDrillFilter.isNotBlank()) {
                                Text(
                                    text = "Reset All Filters",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorAccent,
                                    modifier = Modifier.clickable {
                                        dashMonthFilter = ""
                                        dashWeekFilter = ""
                                        dashDeptFilter = ""
                                        dashDrillFilter = ""
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Month Filter
                            FilterDropdown(
                                label = "Month",
                                selectedValue = monthDisplayMap[dashMonthFilter] ?: "",
                                options = monthsList.map { monthDisplayMap[it] ?: it },
                                onSelect = { disp ->
                                    val realVal = monthDisplayMap.entries.find { it.value == disp }?.key ?: ""
                                    dashMonthFilter = realVal
                                },
                                expanded = monthExpanded,
                                onExpandedChange = { monthExpanded = it }
                            )

                            // Week Filter
                            FilterDropdown(
                                label = "Week",
                                selectedValue = dashWeekFilter,
                                options = weeksList,
                                onSelect = { dashWeekFilter = it },
                                expanded = weekExpanded,
                                onExpandedChange = { weekExpanded = it }
                            )

                            // Department Filter
                            FilterDropdown(
                                label = "Department",
                                selectedValue = dashDeptFilter,
                                options = deptsList,
                                onSelect = { dashDeptFilter = it },
                                expanded = deptExpanded,
                                onExpandedChange = { deptExpanded = it }
                            )
                        }
                    }
                }
            }

            // Dedicated Active Drill Down Banner
            if (dashDrillFilter.isNotBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, ColorPrimary.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = ColorPrimary.copy(alpha = 0.12f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FilterList,
                                            contentDescription = null,
                                            tint = ColorPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "DRILL FILTER ACTIVE",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorMuted,
                                        style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.8.sp)
                                    )
                                    Text(
                                        text = "$drillLabel (${drilledRows.size} items)",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorPrimary
                                    )
                                }
                            }

                            Button(
                                onClick = { dashDrillFilter = "" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = ColorAccent
                                ),
                                border = BorderStroke(1.dp, ColorAccent.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear Drill Filter",
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Clear Drill",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // KPIs Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // KPI: Items
                    KpiCard(
                        title = "Items",
                        value = itemsCount.toString(),
                        tintColor = ColorPrimary,
                        modifier = Modifier.weight(1f),
                        isSelected = dashDrillFilter.isBlank(),
                        onClick = { dashDrillFilter = "" }
                    )

                    // KPI: Critical <=7 Days
                    KpiCard(
                        title = "Critical ≤7 Days",
                        value = criticalCount.toString(),
                        tintColor = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f),
                        isSelected = dashDrillFilter == "critical_7",
                        onClick = {
                            dashDrillFilter = if (dashDrillFilter == "critical_7") "" else "critical_7"
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // KPI: <=30 Days
                    KpiCard(
                        title = "≤30 Days",
                        value = less30Count.toString(),
                        tintColor = Color(0xFFF97316),
                        modifier = Modifier.weight(1f),
                        isSelected = dashDrillFilter == "less_30",
                        onClick = {
                            dashDrillFilter = if (dashDrillFilter == "less_30") "" else "less_30"
                        }
                    )

                    // KPI: All Time Total (instead of Stock KPI which was removed)
                    KpiCard(
                        title = "All Time Total",
                        value = allTimeCount.toString(),
                        tintColor = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f),
                        isSelected = false,
                        onClick = {
                            dashMonthFilter = ""
                            dashWeekFilter = ""
                            dashDeptFilter = ""
                            dashDrillFilter = ""
                        }
                    )
                }
            }

            // Analytics Section: Risk Bucket Chart
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, ColorBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RISK BUCKETS DISTRIBUTION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorMuted,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
                            )
                            Text(
                                text = "Tap bar to drill",
                                fontSize = 9.sp,
                                color = ColorSoft,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        RiskBar(
                            label = "<7 Days",
                            count = bucketCounts["<7 Days"] ?: 0,
                            maxCount = maxBucketCount,
                            barColor = Color(0xFFDC2626),
                            isSelected = dashDrillFilter == "<7 Days",
                            onClick = {
                                dashDrillFilter = if (dashDrillFilter == "<7 Days") "" else "<7 Days"
                            }
                        )
                        RiskBar(
                            label = "7–30 Days",
                            count = bucketCounts["7–30 Days"] ?: 0,
                            maxCount = maxBucketCount,
                            barColor = Color(0xFFF97316),
                            isSelected = dashDrillFilter == "7–30 Days",
                            onClick = {
                                dashDrillFilter = if (dashDrillFilter == "7–30 Days") "" else "7–30 Days"
                            }
                        )
                        RiskBar(
                            label = "31–60 Days",
                            count = bucketCounts["31–60 Days"] ?: 0,
                            maxCount = maxBucketCount,
                            barColor = Color(0xFFEAB308),
                            isSelected = dashDrillFilter == "31–60 Days",
                            onClick = {
                                dashDrillFilter = if (dashDrillFilter == "31–60 Days") "" else "31–60 Days"
                            }
                        )
                        RiskBar(
                            label = "61–90 Days",
                            count = bucketCounts["61–90 Days"] ?: 0,
                            maxCount = maxBucketCount,
                            barColor = Color(0xFF16A34A),
                            isSelected = dashDrillFilter == "61–90 Days",
                            onClick = {
                                dashDrillFilter = if (dashDrillFilter == "61–90 Days") "" else "61–90 Days"
                            }
                        )
                        RiskBar(
                            label = ">90 Days",
                            count = bucketCounts[">90 Days"] ?: 0,
                            maxCount = maxBucketCount,
                            barColor = Color(0xFF2563EB),
                            isSelected = dashDrillFilter == ">90 Days",
                            onClick = {
                                dashDrillFilter = if (dashDrillFilter == ">90 Days") "" else ">90 Days"
                            }
                        )
                    }
                }
            }

            // Week comparison Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, ColorBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "WEEK-OVER-WEEK LOG COUNTS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorMuted,
                            style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        WeekComparisonGrid(weekCounts = weekCounts)
                    }
                }
            }

            // Table Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (dashDrillFilter.isBlank()) "Filtered Items List (${drilledRows.size})" else "Drilled Items List: $drillLabel (${drilledRows.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorInk
                    )
                }
            }

            // Scrollable Items Table
            item {
                StoreViewTable(
                    rows = drilledRows,
                    dashMonthFilter = dashMonthFilter,
                    dashWeekFilter = dashWeekFilter,
                    dashDeptFilter = dashDeptFilter
                )
            }
        }
    }
}

@Composable
fun FilterDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Box(modifier = Modifier.width(105.dp)) {
        Column {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = ColorMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                onClick = { onExpandedChange(!expanded) },
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ColorBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedValue.ifBlank { "All" },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = ColorMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(Color.White)
        ) {
            DropdownMenuItem(
                text = { Text("All", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Black) },
                onClick = {
                    onSelect("")
                    onExpandedChange(false)
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Black) },
                    onClick = {
                        onSelect(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    tintColor: Color,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) tintColor.copy(alpha = 0.08f) else Color.White
        ),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) tintColor else ColorBorder
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) tintColor else ColorMuted
                )
                if (onClick != null) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.FilterListOff else Icons.Default.FilterList,
                        contentDescription = "Drill down filter",
                        tint = if (isSelected) tintColor else ColorMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(tintColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorInk
                )
            }
            if (onClick != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isSelected) "Filtered (Tap to clear)" else "Tap to drill down",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) tintColor else ColorSoft
                )
            }
        }
    }
}

@Composable
fun RiskBar(
    label: String,
    count: Int,
    maxCount: Int,
    barColor: Color,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val percentage = if (maxCount > 0) count.toFloat() / maxCount else 0f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) barColor.copy(alpha = 0.12f) else Color.Transparent)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 5.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (isSelected) barColor else ColorInk,
            modifier = Modifier.width(76.dp),
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF1F5F9))
                .border(0.5.dp, if (isSelected) barColor else ColorBorder, RoundedCornerShape(6.dp))
        ) {
            if (percentage > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(percentage)
                        .clip(RoundedCornerShape(6.dp))
                        .background(barColor),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (percentage >= 0.35f) {
                        Text(
                            text = "$count",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = if (isSelected) barColor else barColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "$count",
                color = if (isSelected) Color.White else barColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WeekComparisonGrid(weekCounts: List<Pair<String, Int>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        weekCounts.forEachIndexed { index, (week, count) ->
            val prevCount = if (index > 0) weekCounts[index - 1].second else -1
            val trend = when {
                prevCount == -1 -> "—"
                count > prevCount -> "↑ Rising"
                count < prevCount -> "↓ Falling"
                else -> "→ Same"
            }
            val (bgColor, textColor, borderColor) = when {
                prevCount == -1 -> Triple(Color(0xFFF8FAFC), ColorMuted, ColorBorder)
                count > prevCount -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), Color(0xFFFCA5A5))
                count < prevCount -> Triple(Color(0xFFF0FDF4), Color(0xFF16A34A), Color(0xFF86EFAC))
                else -> Triple(Color(0xFFF8FAFC), ColorMuted, ColorBorder)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = bgColor),
                border = BorderStroke(1.dp, borderColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val weekNum = week.substringAfter("Week ").substringBefore(" (")
                    Text(
                        text = "Week $weekNum",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = count.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorInk,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = trend,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun StoreViewTable(
    rows: List<StoreDataRowDto>,
    dashMonthFilter: String,
    dashWeekFilter: String,
    dashDeptFilter: String
) {
    val scrollState = rememberScrollState()
    var displayLimit by remember(rows) { mutableStateOf(50) }

    val showWeek = dashWeekFilter.isBlank()
    val showMonth = dashMonthFilter.isBlank()
    val showDept = dashDeptFilter.isBlank()

    val weekWidth = 55
    val monthWidth = 70
    val articleWidth = 70
    val descriptionWidth = 130
    val deptWidth = 80
    val stockWidth = 50
    val expiryWidth = 80
    val daysLeftWidth = 70
    val riskWidth = 80

    // Compute exact total width dynamically
    val totalWidth = articleWidth + descriptionWidth + stockWidth + expiryWidth + daysLeftWidth + riskWidth +
            (if (showWeek) weekWidth else 0) +
            (if (showMonth) monthWidth else 0) +
            (if (showDept) deptWidth else 0)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ColorBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Horizontal scroll container for the table
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Column(modifier = Modifier.width(totalWidth.dp)) {
                    // Header row
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFF8FAFC))
                            .padding(vertical = 12.dp)
                    ) {
                        if (showWeek) TableHeaderCell("Week", weekWidth)
                        if (showMonth) TableHeaderCell("Month", monthWidth)
                        TableHeaderCell("Article", articleWidth)
                        TableHeaderCell("Description", descriptionWidth)
                        if (showDept) TableHeaderCell("Dept", deptWidth)
                        TableHeaderCell("Stock", stockWidth)
                        TableHeaderCell("Expiry", expiryWidth)
                        TableHeaderCell("Days Left", daysLeftWidth)
                        TableHeaderCell("Risk", riskWidth)
                    }

                    // Divider
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ColorBorder))

                    // Table rows
                    if (rows.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No items match filters", color = ColorSoft, fontSize = 13.sp)
                        }
                    } else {
                        rows.take(displayLimit).forEach { row ->
                            val daysLeft = getDaysLeft(row.ExpiryDate, row.DaysLeft)
                            val bucket = getBucket(daysLeft)
                            val dlColor = when {
                                daysLeft == null -> ColorMuted
                                daysLeft <= 7 -> Color(0xFFDC2626)
                                daysLeft <= 30 -> Color(0xFFD97706)
                                daysLeft <= 60 -> Color(0xFFCA8A04)
                                else -> Color(0xFF16A34A)
                            }
                            val (badgeBg, badgeText, badgeBorder) = when (bucket) {
                                "Expired", "<7 Days" -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), Color(0xFFFCA5A5))
                                "7–30 Days" -> Triple(Color(0xFFFFF7ED), Color(0xFFEA580C), Color(0xFFFED7AA))
                                "31–60 Days" -> Triple(Color(0xFFFEFCE8), Color(0xFFCA8A04), Color(0xFFFEF08A))
                                "61–90 Days" -> Triple(Color(0xFFF0FDF4), Color(0xFF16A34A), Color(0xFF86EFAC))
                                else -> Triple(Color(0xFFF8FAFC), ColorMuted, ColorBorder)
                            }

                            Row(
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .background(Color.White),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showWeek) {
                                    TableCell(row.Week?.substringBefore(" (") ?: "—", weekWidth)
                                }
                                if (showMonth) {
                                    val parsedMonth = getSubMonthDisp(row.SubMonth, row.SubMonthDisp)
                                    TableCell(parsedMonth.ifBlank { "—" }, monthWidth)
                                }
                                TableCell(row.Article ?: "—", articleWidth, isMono = true, isBold = true)
                                TableCell(row.Description ?: "—", descriptionWidth)
                                if (showDept) {
                                    TableCell(row.Department ?: "—", deptWidth)
                                }
                                TableCell(row.Stock ?: "—", stockWidth, isMono = true)
                                TableCell(row.ExpiryDate ?: "—", expiryWidth, isMono = true)
                                TableCell(daysLeft?.toString() ?: "—", daysLeftWidth, isMono = true, isBold = true, textColor = dlColor)
                                
                                // Risk Badge
                                Box(
                                    modifier = Modifier
                                        .width(riskWidth.dp)
                                        .padding(horizontal = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(badgeBg, RoundedCornerShape(12.dp))
                                            .border(0.5.dp, badgeBorder, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = bucket.ifBlank { "—" },
                                            color = badgeText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(ColorBorder.copy(alpha = 0.5f)))
                        }

                        if (rows.size > displayLimit) {
                            androidx.compose.material3.TextButton(
                                onClick = { displayLimit += 100 },
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text = "Show More (+100 items, ${rows.size - displayLimit} remaining)",
                                    color = ColorPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableHeaderCell(text: String, width: Int) {
    Text(
        text = text.uppercase(),
        fontSize = 9.sp,
        fontWeight = FontWeight.ExtraBold,
        color = ColorMuted,
        modifier = Modifier.width(width.dp).padding(horizontal = 4.dp)
    )
}

@Composable
fun TableCell(
    text: String,
    width: Int,
    isMono: Boolean = false,
    isBold: Boolean = false,
    textColor: Color = ColorInk
) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
        fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
        color = textColor,
        modifier = Modifier.width(width.dp).padding(horizontal = 4.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

fun parseRowDate(dateStr: String?): Date? {
    if (dateStr.isNullOrBlank()) return null
    val clean = dateStr.trim()
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd",
        "dd/MM/yyyy", "MM/dd/yyyy", "yyyy/MM/dd", "d/M/yyyy", "d-M-yyyy", "dd-MM-yyyy"
    )
    for (fmt in formats) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.US)
            sdf.isLenient = false
            return sdf.parse(clean)
        } catch (e: Exception) {}
    }
    val wordy = listOf("MMM d yyyy", "d MMM yyyy", "d MMMM yyyy")
    for (fmt in wordy) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.US)
            sdf.isLenient = false
            return sdf.parse(clean)
        } catch (e: Exception) {}
    }
    return null
}

fun normalizeDateString(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    val s = dateStr.trim()
    if (s.length >= 10 && s[4] == '-' && s[7] == '-') return s.substring(0, 10)
    if (s.contains("T")) return s.substringBefore("T").trim()
    if (s.contains("/")) {
        val parts = s.split("/")
        if (parts.size == 3) {
            val p0 = parts[0].padStart(2, '0')
            val p1 = parts[1].padStart(2, '0')
            var p2 = parts[2].trim()
            if (p2.contains(" ")) p2 = p2.substringBefore(" ")
            if (p2.length == 4) {
                return "$p2-$p1-$p0"
            }
        }
    }
    if (s.contains("-")) {
        val parts = s.split("-")
        if (parts.size == 3) {
            val p0 = parts[0].padStart(2, '0')
            val p1 = parts[1].padStart(2, '0')
            var p2 = parts[2].trim()
            if (p2.contains(" ")) p2 = p2.substringBefore(" ")
            if (p0.length == 4) {
                return "$p0-$p1-$p2"
            } else if (p2.length == 4) {
                return "$p2-$p1-$p0"
            }
        }
    }
    val parsed = parseRowDate(s)
    if (parsed != null) {
        val cal = java.util.Calendar.getInstance().apply { time = parsed }
        val y = cal.get(java.util.Calendar.YEAR)
        val m = (cal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$y-$m-$d"
    }
    return s.lowercase().replace("/", "-")
}

fun cleanBarcode(barcode: String?): String {
    if (barcode.isNullOrBlank()) return ""
    val trimmed = barcode.trim().lowercase()
    return trimmed.dropWhile { it == '0' }.ifEmpty { trimmed }
}

fun cleanCode(code: String?): String {
    return code?.trim()?.lowercase() ?: ""
}

fun isSameItemExpiry(
    barcodeA: String?, articleA: String?, descA: String?, expiryA: String?,
    barcodeB: String?, articleB: String?, descB: String?, expiryB: String?
): Boolean {
    val normExpiryA = normalizeDateString(expiryA)
    val normExpiryB = normalizeDateString(expiryB)

    val sameExpiry = if (normExpiryA.isNotBlank() && normExpiryB.isNotBlank()) {
        normExpiryA == normExpiryB
    } else {
        normExpiryA == normExpiryB
    }

    if (!sameExpiry) return false

    val bA = cleanBarcode(barcodeA)
    val bB = cleanBarcode(barcodeB)
    val aA = cleanCode(articleA)
    val aB = cleanCode(articleB)
    val dA = cleanCode(descA)
    val dB = cleanCode(descB)

    val sameBarcode = bA.isNotBlank() && bB.isNotBlank() && bA == bB
    val sameArticle = aA.isNotBlank() && aB.isNotBlank() && aA == aB
    val sameDesc = dA.isNotBlank() && dB.isNotBlank() && dA == dB

    return sameBarcode || sameArticle || (bA.isBlank() && bB.isBlank() && aA.isBlank() && aB.isBlank() && sameDesc)
}

fun getDaysLeft(expiryDateStr: String?, daysLeftStr: String?): Int? {
    val parsedInt = daysLeftStr?.toIntOrNull()
    if (parsedInt != null) return parsedInt
    val expDate = parseRowDate(expiryDateStr) ?: return null
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
    val diffMs = expDate.time - today.time
    return (diffMs / (1000 * 60 * 60 * 24)).toInt()
}

fun getBucket(days: Int?): String {
    if (days == null) return ""
    return when {
        days <= 0 -> "Expired"
        days <= 7 -> "<7 Days"
        days <= 30 -> "7–30 Days"
        days <= 60 -> "31–60 Days"
        days <= 90 -> "61–90 Days"
        else -> ">90 Days"
    }
}

fun getSubMonthDisp(subMonth: String?, subMonthDisp: String?): String {
    if (!subMonthDisp.isNullOrBlank()) return subMonthDisp
    if (subMonth.isNullOrBlank()) return ""
    try {
        val parts = subMonth.split("-")
        if (parts.size == 2) {
            val year = parts[0]
            val monthInt = parts[1].toIntOrNull()
            if (monthInt != null && monthInt in 1..12) {
                val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                return "${months[monthInt - 1]} $year"
            }
        }
    } catch (e: Exception) {}
    return subMonth
}

/* ══ SHELF REMOVAL SCREEN ══ */
@Composable
fun ShelfRemovalScreen(
    viewModel: GalaViewModel,
    onBackToHome: () -> Unit
) {
    val dashboardRows by viewModel.storeDashboardRows.collectAsState()
    val loading by viewModel.storeDashboardLoading.collectAsState()
    val error by viewModel.storeDashboardError.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStoreDashboardData()
    }

    val expiredRows = remember(dashboardRows) {
        dashboardRows.filter { row ->
            val days = getDaysLeft(row.ExpiryDate, row.DaysLeft)
            days != null && days in -7..0
        }
    }

    val expiringUpcomingRows = remember(dashboardRows) {
        dashboardRows.filter { row ->
            val days = getDaysLeft(row.ExpiryDate, row.DaysLeft)
            days != null && days in 1..7
        }.sortedBy { row ->
            getDaysLeft(row.ExpiryDate, row.DaysLeft) ?: 999
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // App Bar / Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorPrimary)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToHome,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Shelf Removal Assistant",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Keep shelves clean and safe",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ColorPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Analyzing products shelf dates...", color = ColorMuted, fontSize = 13.sp)
                }
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚠️ Error loading store data", color = ColorAccent, fontWeight = FontWeight.Bold)
                    Text(error ?: "Unknown error", color = ColorInk, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Button(
                        onClick = { viewModel.loadStoreDashboardData() },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // SECTION 1: EXPIRED ITEMS
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🚨  EXPIRED ITEMS (${expiredRows.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (expiredRows.isNotEmpty()) Color(0xFFDC2626) else ColorGreen,
                            style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
                        )
                    }
                }

                if (expiredRows.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, ColorBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🎉", fontSize = 28.sp)
                                    Text("All Clean!", fontWeight = FontWeight.Bold, color = ColorInk, fontSize = 14.sp)
                                    Text("No expired items found on shelves.", color = ColorMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    items(expiredRows) { row ->
                        ShelfRemovalItemCard(
                            row = row,
                            isExpired = true
                        )
                    }
                }

                // SECTION 2: EXPIRING IN 7 DAYS
                item {
                    Text(
                        text = "⏰  EXPIRING IN NEXT 7 DAYS (${expiringUpcomingRows.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD97706),
                        style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp),
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                if (expiringUpcomingRows.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, ColorBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("👍", fontSize = 24.sp)
                                    Text("Looking Good!", fontWeight = FontWeight.Bold, color = ColorInk, fontSize = 14.sp)
                                    Text("No items expiring in the next 7 days.", color = ColorMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    items(expiringUpcomingRows) { row ->
                        ShelfRemovalItemCard(
                            row = row,
                            isExpired = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShelfRemovalItemCard(
    row: StoreDataRowDto,
    isExpired: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                ColorBorder,
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = row.Article ?: "",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = ColorInk
                    )
                    if (!row.Barcode.isNullOrBlank()) {
                        Text(
                            text = "  ${row.Barcode}",
                            fontSize = 11.sp,
                            color = ColorMuted
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val daysLeft = getDaysLeft(row.ExpiryDate, row.DaysLeft) ?: 0
                    val daysText = when {
                        daysLeft < 0 -> "${-daysLeft}d ago"
                        daysLeft == 0 -> "Today"
                        else -> "${daysLeft}d left"
                    }
                    val daysBg = if (isExpired) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)
                    val daysBorder = if (isExpired) Color(0xFFFCA5A5) else Color(0xFFFDE68A)
                    val daysTextColor = if (isExpired) Color(0xFFB91C1C) else Color(0xFFB45309)

                    Box(
                        modifier = Modifier
                            .background(daysBg, RoundedCornerShape(6.dp))
                            .border(1.dp, daysBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = daysText,
                            color = daysTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(ColorPrimaryL, RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFC3E6D4), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Stock: ${row.Stock ?: "0"}",
                            color = ColorPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = row.Description ?: "No description",
                fontWeight = FontWeight.Bold,
                color = ColorInk,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "📅 Expiry: ${row.ExpiryDate ?: "—"}  •  Dept: ${row.Department ?: "—"}",
                color = ColorMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun UpdateDialog(
    updateState: com.gala.exp.vm.AppUpdateState,
    downloadStatus: com.gala.exp.vm.DownloadStatus,
    onUpdateClick: (String) -> Unit,
    onDismissClick: () -> Unit,
    onInstallApk: (java.io.File) -> Unit
) {
    LaunchedEffect(downloadStatus) {
        if (downloadStatus is com.gala.exp.vm.DownloadStatus.Success) {
            onInstallApk(downloadStatus.apkFile)
        }
    }

    Dialog(
        onDismissRequest = {
            if (!updateState.isForceUpdate) {
                onDismissClick()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateState.isForceUpdate,
            dismissOnClickOutside = !updateState.isForceUpdate,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            if (updateState.isForceUpdate) Color(0xFFFEE2E2) else Color(0xFFE0F2FE),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (updateState.isForceUpdate) Icons.Default.SystemUpdateAlt else Icons.Default.CloudDownload,
                        contentDescription = "Update Icon",
                        tint = if (updateState.isForceUpdate) Color(0xFFDC2626) else ColorPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = if (updateState.isForceUpdate) Color(0xFFFEE2E2) else ColorPrimaryL,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (updateState.isForceUpdate) "MANDATORY UPDATE REQUIRED" else "NEW UPDATE AVAILABLE",
                        color = if (updateState.isForceUpdate) Color(0xFF991B1B) else ColorPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Version ${updateState.latestVersionName}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorInk
                )

                Text(
                    text = "Current: v${com.gala.exp.BuildConfig.VERSION_NAME} (Build ${com.gala.exp.BuildConfig.VERSION_CODE})",
                    fontSize = 12.sp,
                    color = ColorMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (updateState.releaseNotes.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .border(1.dp, ColorBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "What's New:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = updateState.releaseNotes,
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                when (downloadStatus) {
                    is com.gala.exp.vm.DownloadStatus.Idle -> {
                        Button(
                            onClick = { onUpdateClick(updateState.apkUrl) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (updateState.isForceUpdate) Color(0xFFDC2626) else ColorPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Update Now",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!updateState.isForceUpdate) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = onDismissClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Remind Me Later",
                                    color = ColorMuted,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    is com.gala.exp.vm.DownloadStatus.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (downloadStatus.progressPercent >= 0) {
                                    "Downloading update... ${downloadStatus.progressPercent}%"
                                } else {
                                    "Downloading update..."
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            if (downloadStatus.progressPercent >= 0) {
                                LinearProgressIndicator(
                                    progress = { downloadStatus.progressPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = ColorPrimary,
                                    trackColor = ColorPrimaryL
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = ColorPrimary,
                                    trackColor = ColorPrimaryL
                                )
                            }
                        }
                    }

                    is com.gala.exp.vm.DownloadStatus.Success -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "✅ Download Complete!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorGreen
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onInstallApk(downloadStatus.apkFile) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ColorGreen)
                            ) {
                                Text(
                                    text = "Install Now",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    is com.gala.exp.vm.DownloadStatus.Error -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Download failed: ${downloadStatus.message}",
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onUpdateClick(updateState.apkUrl) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
                            ) {
                                Text("Retry Download", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun installApk(context: android.content.Context, apkFile: java.io.File) {
    try {
        val apkUri: android.net.Uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        showToast(context, "Could not launch installer: ${e.localizedMessage}")
    }
}
