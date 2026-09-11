package com.gala.exp.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gala.exp.api.ThisWeekRowDto
import com.gala.exp.vm.GalaViewModel
import java.util.*

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
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

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
    var staffFilter by remember { mutableStateOf("") }
    var sortByStaff by remember { mutableStateOf(false) }
    val staffList by viewModel.staffList.collectAsState()

    val filteredReviewRows = remember(reviewRows, searchQuery, staffFilter, sortByStaff) {
        val filtered = reviewRows.filter { row ->
            val matchSearch = searchQuery.isBlank() || (
                (row.Barcode?.lowercase()?.contains(searchQuery.trim().lowercase()) == true) ||
                (row.Description?.lowercase()?.contains(searchQuery.trim().lowercase()) == true) ||
                (row.Article?.lowercase()?.contains(searchQuery.trim().lowercase()) == true) ||
                (row.staffDisplayName.lowercase().contains(searchQuery.trim().lowercase()))
            )
            val matchStaff = staffFilter.isBlank() || row.staffDisplayName.equals(staffFilter, ignoreCase = true)
            matchSearch && matchStaff
        }
        if (sortByStaff) {
            filtered.sortedWith(
                compareBy<ThisWeekRowDto, String>(String.CASE_INSENSITIVE_ORDER) { row ->
                    val name = row.staffDisplayName.trim()
                    if (name.isBlank()) "zzz_no_staff" else name
                }.thenBy(String.CASE_INSENSITIVE_ORDER) { row ->
                    row.Description?.trim() ?: ""
                }
            )
        } else {
            filtered
        }
    }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val editDatePickerDialog = android.app.DatePickerDialog(
        context,
        { _, cyLocal, cmLocal, cdLocal ->
            val formattedDate = String.format(
                Locale.US,
                "%04d-%02d-%02d",
                cyLocal,
                cmLocal + 1,
                cdLocal
            )
            editExpiryValue = formattedDate
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                UniversalSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search barcode, description...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            var staffDropdownExpanded by remember { mutableStateOf(false) }
            val isStaffActive = staffFilter.isNotBlank() || sortByStaff
            val buttonLabel = when {
                staffFilter.isNotBlank() -> "Staff: $staffFilter"
                sortByStaff -> "Sorted: Staff"
                else -> "Staff"
            }

            Box {
                OutlinedButton(
                    onClick = { staffDropdownExpanded = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isStaffActive) ColorPrimary else ColorBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isStaffActive) ColorPrimary.copy(alpha = 0.08f) else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (sortByStaff) Icons.Default.Sort else Icons.Default.Person,
                        contentDescription = "Staff Filter & Sort",
                        tint = if (isStaffActive) ColorPrimary else ColorMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = buttonLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isStaffActive) ColorPrimary else ColorInk
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = if (isStaffActive) ColorPrimary else ColorMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = staffDropdownExpanded,
                    onDismissRequest = { staffDropdownExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "All Staff (Default)",
                                fontWeight = if (staffFilter.isBlank() && !sortByStaff) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (staffFilter.isBlank() && !sortByStaff) ColorPrimary else Color.Black,
                                fontSize = 14.sp
                            )
                        },
                        onClick = {
                            staffFilter = ""
                            sortByStaff = false
                            staffDropdownExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = null,
                                    tint = if (sortByStaff) ColorPrimary else ColorMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sort All by Staff Name",
                                    fontWeight = if (sortByStaff) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (sortByStaff) ColorPrimary else Color.Black,
                                    fontSize = 14.sp
                                )
                            }
                        },
                        onClick = {
                            staffFilter = ""
                            sortByStaff = true
                            staffDropdownExpanded = false
                        }
                    )

                    Divider(modifier = Modifier.padding(vertical = 4.dp), color = ColorBorder)

                    val availableStaffs = remember(reviewRows, staffList) {
                        (staffList + reviewRows.map { it.staffDisplayName }.filter { it.isNotBlank() })
                            .distinctBy { it.trim().lowercase() }
                            .sortedWith(String.CASE_INSENSITIVE_ORDER)
                    }
                    availableStaffs.forEach { name ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = name,
                                    fontWeight = if (staffFilter == name) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (staffFilter == name) ColorPrimary else Color.Black,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                staffFilter = name
                                sortByStaff = false
                                staffDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

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
                                            ?: articles.firstOrNull { it.article?.trim() == row.Article?.trim() }?.barcode?.takeIf { it.isNotBlank() }
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = row.Department ?: "—",
                                            color = ColorMuted,
                                            fontSize = 11.sp
                                        )
                                        if (row.staffDisplayName.isNotBlank()) {
                                            Text(
                                                text = "•",
                                                color = ColorMuted,
                                                fontSize = 11.sp
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = ColorPrimary,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = row.staffDisplayName,
                                                    color = ColorPrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
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

        // Pinned Bottom buttons
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
                modifier = Modifier.size(48.dp),
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
                shape = RoundedCornerShape(24.dp),
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
            AlertDialog(
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
                    TextButton(onClick = { showDeleteDialog = false }) {
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
        Dialog(
            onDismissRequest = { showEditDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
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
                            .clickable(enabled = false) {},
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
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
                                    BasicTextField(
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
                                        textStyle = TextStyle(
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
                                    style = TextStyle(letterSpacing = 1.sp),
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
        Dialog(
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

                    val confirmMsg = buildAnnotatedString {
                        append("⚠️ Before sending the email, please review and confirm that all entries are accurate. This function is intended for ")
                        pushStyle(SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold))
                        append("Manager")
                        pop()
                        append(" use only.\n\n")
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append("Once the submission is completed, you cannot add or edit data.")
                        pop()
                    }

                    Text(
                        text = confirmMsg,
                        fontSize = 14.sp,
                        color = ColorInk,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
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
