package com.gala.exp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gala.exp.api.StoreDataRowDto

@Composable
fun HomeScreen(
    selectedWeek: String,
    lockedWeeks: Set<String>,
    dashboardRows: List<StoreDataRowDto>,
    dashboardLoading: Boolean,
    staffList: List<String>,
    selectedStaff: String,
    onStaffSelected: (String) -> Unit,
    onWeekSelected: (String) -> Unit,
    onFollowUpClick: () -> Unit,
    onAddNewClick: () -> Unit,
    onReviewClick: () -> Unit,
    onStoreViewClick: () -> Unit,
    onShelfRemovalClick: () -> Unit
) {
    val localContext = LocalContext.current
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
                        weeks.forEach { (backendValue, _) ->
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

        // Staff Selection Card (Single clean dropdown for active staff member)
        item {
            var staffDropdownExpanded by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, if (selectedStaff.isBlank()) ColorPrimary else ColorBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { staffDropdownExpanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = ColorPrimary.copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Staff",
                                    tint = ColorPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "LOGGING PROCESS AS STAFF",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorMuted,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.8.sp)
                            )
                            Text(
                                text = if (selectedStaff.isNotBlank()) selectedStaff else "Select Staff Name *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedStaff.isNotBlank()) ColorInk else ColorPrimary
                            )
                        }
                    }

                    Box {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Staff",
                            tint = ColorPrimary,
                            modifier = Modifier.size(28.dp)
                        )

                        DropdownMenu(
                            expanded = staffDropdownExpanded,
                            onDismissRequest = { staffDropdownExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            if (staffList.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No staff found (Add in Manage Staff)", fontSize = 13.sp, color = ColorMuted) },
                                    onClick = { staffDropdownExpanded = false }
                                )
                            } else {
                                staffList.forEach { staffName ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = staffName,
                                                fontWeight = if (staffName == selectedStaff) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (staffName == selectedStaff) ColorPrimary else Color.Black,
                                                fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            onStaffSelected(staffName)
                                            staffDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
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
                enabled = selectedWeek.isNotBlank() && selectedStaff.isNotBlank(),
                colorFocus = ColorBlueCombined,
                onClick = onFollowUpClick,
                onLockedClick = {
                    if (selectedWeek.isBlank()) {
                        weekHintVisible = true
                        showToast(localContext, "Please select a week first")
                    } else if (selectedStaff.isBlank()) {
                        showToast(localContext, "Please select a staff member first")
                    }
                }
            )
        }

        item {
            ModeCard(
                icon = "➕",
                iconColorBg = ColorPrimaryL,
                title = "Add New Item",
                description = "Log a new near-expiry item for this week",
                enabled = selectedWeek.isNotBlank() && selectedStaff.isNotBlank(),
                colorFocus = ColorPrimary,
                onClick = onAddNewClick,
                onLockedClick = {
                    if (selectedWeek.isBlank()) {
                        weekHintVisible = true
                        showToast(localContext, "Please select a week first")
                    } else if (selectedStaff.isBlank()) {
                        showToast(localContext, "Please select a staff member first")
                    }
                }
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
