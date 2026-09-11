package com.gala.exp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    var toastEnabled by remember { mutableStateOf(AppSettings.areToastsEnabled(context)) }
    var autoClearEnabled by remember { mutableStateOf(AppSettings.isAutoClearInputsEnabled(context)) }
    var hapticEnabled by remember { mutableStateOf(AppSettings.isHapticFeedbackEnabled(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onBackToHome) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = ColorInk
                )
            }
            Text(
                text = "Application Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ColorInk
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 1: Notification & Feedback
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ColorBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "NOTIFICATION & ALERTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorMuted,
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.8.sp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle 1: Toast Messages
                SettingToggleRow(
                    icon = Icons.Default.Notifications,
                    title = "Toast Messages",
                    description = "Enable or disable pop-up toast alerts across all screens",
                    checked = toastEnabled,
                    onCheckedChange = {
                        toastEnabled = it
                        AppSettings.setToastsEnabled(context, it)
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = ColorBorder)

                // Toggle 2: Haptic Feedback
                SettingToggleRow(
                    icon = Icons.Default.Tune,
                    title = "Haptic Vibration",
                    description = "Vibrate device upon completing barcode scans & actions",
                    checked = hapticEnabled,
                    onCheckedChange = {
                        hapticEnabled = it
                        AppSettings.setHapticFeedbackEnabled(context, it)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: App Workflow Preferences
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ColorBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ENTRY & LOGGING PREFERENCES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorMuted,
                    style = androidx.compose.ui.text.TextStyle(letterSpacing = 0.8.sp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle 3: Auto-Clear Form Inputs
                SettingToggleRow(
                    icon = Icons.Default.AutoFixHigh,
                    title = "Auto-Clear Input Fields",
                    description = "Automatically reset article code & quantity after adding item",
                    checked = autoClearEnabled,
                    onCheckedChange = {
                        autoClearEnabled = it
                        AppSettings.setAutoClearInputsEnabled(context, it)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = ColorPrimary.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ColorPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorInk
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = ColorMuted,
                    lineHeight = 15.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ColorPrimary,
                uncheckedThumbColor = ColorMuted,
                uncheckedTrackColor = ColorBorder
            )
        )
    }
}
