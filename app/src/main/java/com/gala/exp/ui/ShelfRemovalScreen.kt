package com.gala.exp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gala.exp.api.StoreDataRowDto
import com.gala.exp.vm.GalaViewModel

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
