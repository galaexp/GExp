package com.gala.exp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.gala.exp.BuildConfig
import com.gala.exp.vm.AppUpdateState
import com.gala.exp.vm.DownloadStatus
import java.io.File

@Composable
fun UpdateDialog(
    updateState: AppUpdateState,
    downloadStatus: DownloadStatus,
    onUpdateClick: (String) -> Unit,
    onDismissClick: () -> Unit,
    onInstallApk: (File) -> Unit
) {
    LaunchedEffect(downloadStatus) {
        if (downloadStatus is DownloadStatus.Success) {
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
                    text = "Current: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
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
                    is DownloadStatus.Idle -> {
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

                    is DownloadStatus.Downloading -> {
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

                    is DownloadStatus.Success -> {
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

                    is DownloadStatus.Error -> {
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

fun installApk(context: Context, apkFile: File) {
    try {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        showToast(context, "Could not launch installer: ${e.localizedMessage}")
    }
}
