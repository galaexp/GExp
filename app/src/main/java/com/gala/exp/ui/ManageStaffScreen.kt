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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gala.exp.vm.GalaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageStaffScreen(
    viewModel: GalaViewModel,
    onBackToHome: () -> Unit
) {
    val staffList by viewModel.staffList.collectAsState()
    val isStaffLoading by viewModel.isStaffLoading.collectAsState()
    val staffError by viewModel.staffError.collectAsState()
    val storeCode by viewModel.storeCode.collectAsState()
    val storeName by viewModel.storeName.collectAsState()

    var newStaffName by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
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
                    contentDescription = "Back to Home",
                    tint = ColorPrimary
                )
            }

            Text(
                text = "Manage Store Staff",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorPrimary
            )

            Spacer(modifier = Modifier.width(48.dp))
        }

        // Store info banner
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ColorBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = ColorPrimary.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = ColorPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = storeName.ifBlank { "Store $storeCode" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorInk
                    )
                    Text(
                        text = "Store Code: $storeCode",
                        fontSize = 12.sp,
                        color = ColorMuted
                    )
                }
            }
        }

        // Add Staff Input Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ColorBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ADD NEW STAFF MEMBER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorMuted,
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newStaffName,
                        onValueChange = { newStaffName = it },
                        placeholder = { Text("Enter staff name (e.g. John)", fontSize = 13.sp, color = ColorMuted) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ColorInk,
                            unfocusedTextColor = ColorInk,
                            focusedBorderColor = ColorPrimary,
                            unfocusedBorderColor = ColorBorder,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedPlaceholderColor = ColorMuted,
                            unfocusedPlaceholderColor = ColorMuted
                        )
                    )

                    Button(
                        onClick = {
                            if (newStaffName.isNotBlank()) {
                                val nameToAdd = newStaffName.trim()
                                viewModel.addStaffMember(
                                    name = nameToAdd,
                                    onSuccess = {
                                        newStaffName = ""
                                        showToast(context, "Added staff '$nameToAdd'")
                                    },
                                    onError = { err ->
                                        showToast(context, err)
                                    }
                                )
                            }
                        },
                        enabled = newStaffName.isNotBlank() && !isStaffLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorPrimary,
                            contentColor = Color.White,
                            disabledContainerColor = ColorPrimary.copy(alpha = 0.4f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isStaffLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Add", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                if (staffError != null) {
                    Text(
                        text = staffError ?: "",
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Staff List Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STAFF MEMBERS (${staffList.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ColorMuted,
                style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
            )
        }

        // Staff List
        if (staffList.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ColorBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No staff members added yet.\nAdd names above to track who logs items.",
                        color = ColorMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(staffList) { staff ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, ColorBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                    color = ColorPrimary.copy(alpha = 0.08f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = staff.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = ColorPrimary,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                Text(
                                    text = staff,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorInk
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.removeStaffMember(
                                        name = staff,
                                        onSuccess = {
                                            showToast(context, "Removed staff '$staff'")
                                        },
                                        onError = { err ->
                                            showToast(context, err)
                                        }
                                    )
                                },
                                enabled = !isStaffLoading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Staff",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
