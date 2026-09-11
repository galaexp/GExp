package com.gala.exp.ui

import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gala.exp.vm.GalaViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

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
                val selectedStaff by viewModel.selectedStaff.collectAsState()
                val isStaffSelected = selectedStaff.isNotBlank()

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isStaffSelected) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isStaffSelected) Color(0xFFBBF7D0) else Color(0xFFFCA5A5)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isStaffSelected) Color(0xFF16A34A) else Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Submitting process as staff: ",
                                fontSize = 12.sp,
                                color = ColorInk
                            )
                            Text(
                                text = if (isStaffSelected) selectedStaff else "Not Selected (Required)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isStaffSelected) Color(0xFF15803D) else Color(0xFFB91C1C)
                            )
                        }
                    }

                    Button(
                        enabled = !isLocked && isStaffSelected && articleCode.isNotBlank() && stock.isNotBlank() && expiry.isNotBlank() && !isSubmitting,
                        onClick = {
                            if (isLocked) {
                                showToast(
                                    context,
                                    "Submission Completed For This Week, If any Updation Needed-Contact Buyer Directly"
                                )
                            } else if (!isStaffSelected) {
                                showToast(context, "Please select a staff member on Home screen first")
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
                        shape = RoundedCornerShape(12.dp)
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
