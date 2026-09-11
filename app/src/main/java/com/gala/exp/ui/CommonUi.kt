package com.gala.exp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Hex Colors matching the design system
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

object AppSettings {
    private const val PREFS_NAME = "gala_app_settings"
    private const val KEY_TOAST_ENABLED = "toast_enabled"
    private const val KEY_AUTO_CLEAR_INPUTS = "auto_clear_inputs"
    private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"

    fun areToastsEnabled(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TOAST_ENABLED, true)
    }

    fun setToastsEnabled(context: android.content.Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_TOAST_ENABLED, enabled).apply()
    }

    fun isAutoClearInputsEnabled(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_CLEAR_INPUTS, true)
    }

    fun setAutoClearInputsEnabled(context: android.content.Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_CLEAR_INPUTS, enabled).apply()
    }

    fun isHapticFeedbackEnabled(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
    }

    fun setHapticFeedbackEnabled(context: android.content.Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
    }
}

private var activeToastRef: android.widget.Toast? = null

fun showToast(context: android.content.Context, message: String, duration: Int = android.widget.Toast.LENGTH_SHORT) {
    if (message.isBlank()) return
    if (!AppSettings.areToastsEnabled(context)) return
    try {
        activeToastRef?.cancel()
    } catch (_: Exception) {}
    val toast = android.widget.Toast.makeText(context, message, duration)
    activeToastRef = toast
    toast.show()
}

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
