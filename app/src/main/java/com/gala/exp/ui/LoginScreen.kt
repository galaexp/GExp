package com.gala.exp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    storeCode: String,
    password: String,
    rememberMe: Boolean,
    isAuthLoading: Boolean,
    authError: String?,
    onStoreCodeChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onRememberMeChanged: (Boolean) -> Unit,
    onLoginClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorInk)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background 'GALA' watermark
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = "GALA",
                color = Color.White.copy(alpha = 0.03f),
                fontSize = 110.sp,
                fontWeight = FontWeight.ExtraBold,
                style = androidx.compose.ui.text.TextStyle(letterSpacing = (-4).sp)
            )
        }

        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        Box(contentAlignment = Alignment.Center) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Header Brand Logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ColorPrimary, RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                        Column {
                            Text(
                                text = "Gala Markets",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = ColorInk
                            )
                            Text(
                                text = "EXPIRY TRACKER",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = ColorSoft,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.2.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Store Login",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorInk
                    )
                    Text(
                        text = "Enter your Store Code and password to continue.",
                        fontSize = 12.sp,
                        color = ColorMuted,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Store Code Input
                    GalaFormTextField(
                        value = storeCode,
                        onValueChange = onStoreCodeChanged,
                        label = "STORE CODE (USERNAME)",
                        placeholder = "e.g. 9217",
                        leadingIcon = Icons.Default.Person,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.testTag("login_code_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Input
                    GalaFormTextField(
                        value = password,
                        onValueChange = onPasswordChanged,
                        label = "PASSWORD",
                        placeholder = "••••••••",
                        leadingIcon = Icons.Default.Lock,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                onLoginClick()
                            }
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.testTag("login_password_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Remember me checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRememberMeChanged(!rememberMe) }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = onRememberMeChanged,
                            colors = CheckboxDefaults.colors(checkedColor = ColorPrimary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Remember me on this device",
                            color = ColorMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Auth error text
                    if (authError != null) {
                        Text(
                            text = authError,
                            color = ColorAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            onLoginClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_button"),
                        enabled = !isAuthLoading,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorInk,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Sign In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Spinner Overlay covering card when loading
            if (isAuthLoading) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = 0.8f))
                        .clip(RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = ColorPrimary,
                            modifier = Modifier.size(44.dp),
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = "Signing in...",
                            color = ColorPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
