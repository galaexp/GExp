package com.gala.exp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gala.exp.api.LastWeekRowDto
import com.gala.exp.api.ThisWeekRowDto
import com.gala.exp.vm.GalaViewModel

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
    val staffList by viewModel.staffList.collectAsState()
    val selectedStaff by viewModel.selectedStaff.collectAsState()

    var showUpdateSheet by remember { mutableStateOf(false) }
    var selectedStockRow by remember { mutableStateOf<LastWeekRowDto?>(null) }
    var updatedStockValue by remember { mutableStateOf("") }
    var followUpActionError by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var staffFilter by remember { mutableStateOf("") }
    var sortByStaff by remember { mutableStateOf(false) }

    LaunchedEffect(followUpActionError) {
        if (followUpActionError != null) {
            kotlinx.coroutines.delay(3500)
            followUpActionError = null
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

    val filteredLastWeekRows = remember(lastWeekRows, searchQuery, staffFilter, sortByStaff) {
        val filtered = lastWeekRows.filter { row ->
            val staffName = row.staffDisplayName.trim()
            val matchSearch = searchQuery.isBlank() || (
                (row.Barcode?.lowercase()?.contains(searchQuery.trim().lowercase()) == true) ||
                (row.Description?.lowercase()?.contains(searchQuery.trim().lowercase()) == true) ||
                (row.Article?.lowercase()?.contains(searchQuery.trim().lowercase()) == true) ||
                (staffName.lowercase().contains(searchQuery.trim().lowercase()))
            )
            val matchStaff = staffFilter.isBlank() || staffName.equals(staffFilter.trim(), ignoreCase = true)
            matchSearch && matchStaff
        }

        if (sortByStaff) {
            filtered.sortedWith(
                compareBy<LastWeekRowDto, String>(String.CASE_INSENSITIVE_ORDER) { row ->
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

    val totalCount = lastWeekRows.size
    val completedCount = remember(lastWeekRows, matchingReviewMap) {
        lastWeekRows.count { matchingReviewMap[it] != null }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadThisWeekReview()
    }

    val isStaffSelected = selectedStaff.isNotBlank()

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
                overflow = TextOverflow.Ellipsis
            )

            // Top Status Bar: Completion count + Active Updating as Staff Selector Dropdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Completed $completedCount/$totalCount",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (totalCount > 0 && completedCount == totalCount) Color(0xFF15803D) else ColorPrimary
                )

                // Updating as: StaffName (shows which staff is selected in homepage staff selector)
                Surface(
                    color = if (isStaffSelected) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isStaffSelected) Color(0xFFBBF7D0) else Color(0xFFFCA5A5))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isStaffSelected) Color(0xFF16A34A) else Color(0xFFDC2626),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Updating as: ",
                            fontSize = 11.sp,
                            color = ColorMuted
                        )
                        Text(
                            text = if (isStaffSelected) selectedStaff else "No Staff Selected",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isStaffSelected) Color(0xFF15803D) else Color(0xFFB91C1C)
                        )
                    }
                }
            }

            // Search Bar + Staff Filter & Sorting Row (same as ReviewScreen)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    UniversalSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search items...",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                var staffFilterDropdownExpanded by remember { mutableStateOf(false) }
                val isStaffFilterActive = staffFilter.isNotBlank() || sortByStaff
                val staffButtonLabel = when {
                    staffFilter.isNotBlank() -> "Staff: $staffFilter"
                    sortByStaff -> "Sorted: Staff"
                    else -> "Staff"
                }

                Box {
                    OutlinedButton(
                        onClick = { staffFilterDropdownExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isStaffFilterActive) ColorPrimary else ColorBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isStaffFilterActive) ColorPrimary.copy(alpha = 0.08f) else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (sortByStaff) Icons.Default.Sort else Icons.Default.Person,
                            contentDescription = "Staff Filter & Sort",
                            tint = if (isStaffFilterActive) ColorPrimary else ColorMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = staffButtonLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isStaffFilterActive) ColorPrimary else ColorInk
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = if (isStaffFilterActive) ColorPrimary else ColorMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = staffFilterDropdownExpanded,
                        onDismissRequest = { staffFilterDropdownExpanded = false },
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
                                staffFilterDropdownExpanded = false
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
                                staffFilterDropdownExpanded = false
                            }
                        )

                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = ColorBorder)

                        val availableStaffs = remember(lastWeekRows, staffList) {
                            (staffList + lastWeekRows.map { it.staffDisplayName }.filter { it.isNotBlank() })
                                .distinctBy { it.trim().lowercase() }
                                .sortedWith(String.CASE_INSENSITIVE_ORDER)
                        }

                        availableStaffs.forEach { name ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = name,
                                        fontWeight = if (staffFilter.equals(name, ignoreCase = true)) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (staffFilter.equals(name, ignoreCase = true)) ColorPrimary else Color.Black,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    staffFilter = name
                                    sortByStaff = false
                                    staffFilterDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

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
                        text = "No items matching criteria",
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
                        itemsIndexed(filteredLastWeekRows, key = { idx, row -> "${row.Barcode ?: ""}_${row.Article ?: ""}_${row.ExpiryDate ?: ""}_$idx" }) { _, row ->
                            val matchingReviewRow = matchingReviewMap[row]
                            val isCompleted = matchingReviewRow != null
                            val rowStaff = row.staffDisplayName.trim()

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
                                        } else if (!isStaffSelected) {
                                            showToast(context, "Staff selection is required! Please select a staff member using the dropdown above.")
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = row.Department ?: "—",
                                                color = ColorMuted,
                                                fontSize = 11.sp
                                            )
                                            if (rowStaff.isNotBlank()) {
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
                                                        text = rowStaff,
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
                                            text = "Description: ${sRow.Description ?: ""}",
                                            fontSize = 12.5.sp,
                                            color = ColorMuted
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Expiry Date: ${sRow.ExpiryDate ?: ""}",
                                            fontSize = 12.5.sp,
                                            color = ColorMuted
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Last Week Stock: ${sRow.Stock ?: ""}",
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
                                        BasicTextField(
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
