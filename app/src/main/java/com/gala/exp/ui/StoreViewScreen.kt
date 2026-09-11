package com.gala.exp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gala.exp.api.StoreDataRowDto
import com.gala.exp.vm.GalaViewModel

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
    var dashStaffFilter by remember { mutableStateOf("") }
    var dashDrillFilter by remember { mutableStateOf("") }

    var monthExpanded by remember { mutableStateOf(false) }
    var weekExpanded by remember { mutableStateOf(false) }
    var deptExpanded by remember { mutableStateOf(false) }
    var staffExpanded by remember { mutableStateOf(false) }

    val staffList by viewModel.staffList.collectAsState()

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
        val availableStaffsInStore = remember(dashboardRows, staffList) {
            (staffList + dashboardRows.map { it.staffDisplayName }.filter { it.isNotBlank() }).distinct().sorted()
        }

        val filteredRows = remember(dashboardRows, dashMonthFilter, dashWeekFilter, dashDeptFilter, dashStaffFilter) {
            dashboardRows.filter { row ->
                val matchMonth = dashMonthFilter.isBlank() || row.SubMonth == dashMonthFilter
                val matchWeek = dashWeekFilter.isBlank() || row.Week == dashWeekFilter
                val matchDept = dashDeptFilter.isBlank() || row.Department == dashDeptFilter
                val matchStaff = dashStaffFilter.isBlank() || row.staffDisplayName.equals(dashStaffFilter, ignoreCase = true)
                matchMonth && matchWeek && matchDept && matchStaff
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

                            if (dashMonthFilter.isNotBlank() || dashWeekFilter.isNotBlank() || dashDeptFilter.isNotBlank() || dashStaffFilter.isNotBlank() || dashDrillFilter.isNotBlank()) {
                                Text(
                                    text = "Reset All Filters",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorAccent,
                                    modifier = Modifier.clickable {
                                        dashMonthFilter = ""
                                        dashWeekFilter = ""
                                        dashDeptFilter = ""
                                        dashStaffFilter = ""
                                        dashDrillFilter = ""
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                            // Staff Filter
                            FilterDropdown(
                                label = "Staff",
                                selectedValue = dashStaffFilter,
                                options = availableStaffsInStore,
                                onSelect = { dashStaffFilter = it },
                                expanded = staffExpanded,
                                onExpandedChange = { staffExpanded = it }
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

                    // KPI: All Time Total
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
                    dashDeptFilter = dashDeptFilter,
                    dashStaffFilter = dashStaffFilter
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
    dashDeptFilter: String,
    dashStaffFilter: String = ""
) {
    val scrollState = rememberScrollState()
    var displayLimit by remember(rows) { mutableStateOf(50) }

    val showWeek = dashWeekFilter.isBlank()
    val showMonth = dashMonthFilter.isBlank()
    val showDept = dashDeptFilter.isBlank()
    val showStaff = dashStaffFilter.isBlank()

    val weekWidth = 55
    val monthWidth = 70
    val articleWidth = 70
    val descriptionWidth = 130
    val deptWidth = 80
    val staffWidth = 80
    val stockWidth = 50
    val expiryWidth = 80
    val daysLeftWidth = 70
    val riskWidth = 80

    // Compute exact total width dynamically
    val totalWidth = articleWidth + descriptionWidth + stockWidth + expiryWidth + daysLeftWidth + riskWidth +
            (if (showWeek) weekWidth else 0) +
            (if (showMonth) monthWidth else 0) +
            (if (showDept) deptWidth else 0) +
            (if (showStaff) staffWidth else 0)

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
                        if (showStaff) TableHeaderCell("Staff", staffWidth)
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
                                if (showStaff) {
                                    TableCell(row.staffDisplayName.ifBlank { "—" }, staffWidth)
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
                            TextButton(
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
