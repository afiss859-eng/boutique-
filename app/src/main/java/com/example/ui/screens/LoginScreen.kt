package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.service.AppLanguage
import com.example.service.tr
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WaveBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LoginMode {
    PIN,
    PASSWORD,
    BIOMETRIC
}

@Composable
fun LoginScreen(
    errorMessage: String?,
    onLoginWithPin: (String) -> Unit,
    onLoginWithPassword: (String, String) -> Unit,
    onLoginBiometric: () -> Unit,
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit
) {
    var loginMode by remember { mutableStateOf(LoginMode.PIN) }
    var enteredPin by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("admin") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isBiometricScanning by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Infinite Transitions for High-End Animations
    val infiniteTransition = rememberInfiniteTransition(label = "background_and_glow")
    
    // Background Orb 1 translation
    val orb1Offset by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1"
    )

    // Background Orb 2 translation
    val orb2Offset by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = -70f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2"
    )

    // Glowing Logo Pulse
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_pulse"
    )

    val logoRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logo_rotation"
    )

    // Biometric radar pulse
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse"
    )
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Deep slate navy
    ) {
        // High-End Animated Background Mesh Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Glowing Orange Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(OrangePrimary.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(width * 0.25f + orb1Offset, height * 0.2f + orb1Offset),
                    radius = width * 0.6f
                ),
                radius = width * 0.6f,
                center = Offset(width * 0.25f + orb1Offset, height * 0.2f + orb1Offset)
            )

            // Glowing Gold Accent Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GoldAccent.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(width * 0.8f + orb2Offset, height * 0.45f - orb2Offset),
                    radius = width * 0.55f
                ),
                radius = width * 0.55f,
                center = Offset(width * 0.8f + orb2Offset, height * 0.45f - orb2Offset)
            )

            // Glowing Wave Blue Orb at Bottom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(WaveBlue.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(width * 0.5f - orb1Offset, height * 0.85f + orb2Offset),
                    radius = width * 0.7f
                ),
                radius = width * 0.7f,
                center = Offset(width * 0.5f - orb1Offset, height * 0.85f + orb2Offset)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Language Selector & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Dropdown Selector Bar
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppLanguage.values().forEach { lang ->
                        val isSelected = lang == currentLanguage
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) OrangePrimary else Color.Transparent)
                                .clickable { onSelectLanguage(lang) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${lang.flag} ${lang.code.uppercase()}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                // Secure Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EmeraldSecondary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSecondary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Securisé",
                            tint = EmeraldSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "OFFLINE-READY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EmeraldSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Brand Hero with Animated Ring & Pulse
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer rotating dash ring
                    Canvas(
                        modifier = Modifier
                            .size(100.dp)
                            .rotate(logoRotation)
                    ) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    OrangePrimary,
                                    GoldAccent,
                                    WaveBlue,
                                    EmeraldSecondary,
                                    OrangePrimary
                                )
                            ),
                            radius = size.minDimension / 2f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Inner Pulsing Core
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(logoScale)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(OrangePrimary, Color(0xFFD97706))
                                )
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Wend-Lamita Logo",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = tr("app_name"),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                )

                Text(
                    text = tr("app_subtitle"),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF94A3B8)
                    )
                )
            }

            // Mode Selector Pill Tabs
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // PIN Mode Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (loginMode == LoginMode.PIN) OrangePrimary else Color.Transparent)
                            .clickable { loginMode = LoginMode.PIN }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dialpad,
                                contentDescription = "PIN",
                                tint = if (loginMode == LoginMode.PIN) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Code PIN",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (loginMode == LoginMode.PIN) Color.White else Color(0xFF94A3B8)
                                )
                            )
                        }
                    }

                    // Biometric Mode Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (loginMode == LoginMode.BIOMETRIC) OrangePrimary else Color.Transparent)
                            .clickable { loginMode = LoginMode.BIOMETRIC }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biométrie",
                                tint = if (loginMode == LoginMode.BIOMETRIC) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Empreinte",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (loginMode == LoginMode.BIOMETRIC) Color.White else Color(0xFF94A3B8)
                                )
                            )
                        }
                    }

                    // Password Mode Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (loginMode == LoginMode.PASSWORD) OrangePrimary else Color.Transparent)
                            .clickable { loginMode = LoginMode.PASSWORD }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Mot de passe",
                                tint = if (loginMode == LoginMode.PASSWORD) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Passe",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (loginMode == LoginMode.PASSWORD) Color.White else Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }
            }

            // Error Display Banner
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFBE123C).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE11D48)),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Erreur",
                            tint = Color(0xFFFB7185),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFFECDD3),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            // Dynamic Mode Viewport
            AnimatedContent(
                targetState = loginMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "login_mode_transition"
            ) { mode ->
                when (mode) {
                    LoginMode.PIN -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = tr("enter_pin"),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color(0xFFE2E8F0),
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // 4-Digit Indicator Dots with Pulse
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                for (i in 0 until 4) {
                                    val isFilled = i < enteredPin.length
                                    val isCurrent = i == enteredPin.length
                                    
                                    val dotScale by animateFloatAsState(
                                        targetValue = if (isFilled) 1.25f else 1.0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        label = "dot_$i"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .scale(dotScale)
                                            .clip(CircleShape)
                                            .background(
                                                if (isFilled) {
                                                    OrangePrimary
                                                } else if (isCurrent) {
                                                    Color(0xFF64748B)
                                                } else {
                                                    Color(0xFF334155)
                                                }
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                color = if (isFilled) GoldAccent else Color(0xFF475569),
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }

                            // Tactile Keypad (1 to 9, C, 0, Ok)
                            val keypad = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("C", "0", "✓")
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                keypad.forEach { row ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        row.forEach { key ->
                                            val isAction = key == "C" || key == "✓"
                                            val isOk = key == "✓"
                                            
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = when {
                                                    isOk -> OrangePrimary
                                                    isAction -> Color(0xFF334155)
                                                    else -> Color(0xFF1E293B)
                                                },
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isOk) GoldAccent else Color(0xFF475569).copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(52.dp)
                                                    .testTag("pin_key_$key")
                                                    .clickable {
                                                        when (key) {
                                                            "C" -> {
                                                                if (enteredPin.isNotEmpty()) {
                                                                    enteredPin = enteredPin.dropLast(1)
                                                                }
                                                            }
                                                            "✓" -> {
                                                                if (enteredPin.length == 4) {
                                                                    onLoginWithPin(enteredPin)
                                                                }
                                                            }
                                                            else -> {
                                                                if (enteredPin.length < 4) {
                                                                    val next = enteredPin + key
                                                                    enteredPin = next
                                                                    if (next.length == 4) {
                                                                        coroutineScope.launch {
                                                                            delay(150)
                                                                            onLoginWithPin(next)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    if (key == "C") {
                                                        Icon(
                                                            imageVector = Icons.Default.Backspace,
                                                            contentDescription = "Effacer",
                                                            tint = Color(0xFFCBD5E1),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = key,
                                                            style = MaterialTheme.typography.titleLarge.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
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

                    LoginMode.BIOMETRIC -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                        ) {
                            Text(
                                text = tr("biometric_login"),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Text(
                                text = tr("touch_sensor"),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF94A3B8)
                                ),
                                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                            )

                            // Animated Fingerprint Touch Radar
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clickable {
                                        isBiometricScanning = true
                                        coroutineScope.launch {
                                            delay(600)
                                            onLoginBiometric()
                                            isBiometricScanning = false
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Pulsing Radar Ring
                                Box(
                                    modifier = Modifier
                                        .size(130.dp)
                                        .scale(radarPulse)
                                        .clip(CircleShape)
                                        .background(EmeraldSecondary.copy(alpha = radarAlpha))
                                )

                                // Sensor Core Button
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(Color(0xFF047857), Color(0xFF064E3B))
                                            )
                                        )
                                        .border(2.dp, EmeraldSecondary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Capteur d'empreinte",
                                        tint = if (isBiometricScanning) GoldAccent else Color.White,
                                        modifier = Modifier.size(54.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    isBiometricScanning = true
                                    coroutineScope.launch {
                                        delay(400)
                                        onLoginBiometric()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldSecondary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null)
                                    Text(
                                        text = "Déverrouiller avec Empreinte",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    LoginMode.PASSWORD -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(vertical = 12.dp)
                        ) {
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it },
                                label = { Text("Nom d'utilisateur / Matricule") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = OrangePrimary)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = Color(0xFF475569),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF1E293B)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("login_username_input")
                            )

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Mot de passe") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = OrangePrimary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password",
                                            tint = Color(0xFF94A3B8)
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (usernameInput.isNotBlank() && passwordInput.isNotBlank()) {
                                            onLoginWithPassword(usernameInput, passwordInput)
                                        }
                                    }
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = Color(0xFF475569),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF1E293B)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("login_password_input")
                            )

                            Button(
                                onClick = {
                                    if (usernameInput.isNotBlank() && passwordInput.isNotBlank()) {
                                        onLoginWithPassword(usernameInput, passwordInput)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("login_submit_btn")
                            ) {
                                Text(
                                    text = tr("login_button"),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Quick Role Demo Badges (Admin: 1234, Caissier: 0000, Gerant: 2222)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Profils de caisse rapides :",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = {
                            enteredPin = "1234"
                            onLoginWithPin("1234")
                        },
                        label = { Text("👑 Admin (1234)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFF1E293B),
                            labelColor = GoldAccent
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = GoldAccent.copy(alpha = 0.5f)
                        )
                    )

                    SuggestionChip(
                        onClick = {
                            enteredPin = "0000"
                            onLoginWithPin("0000")
                        },
                        label = { Text("💼 Caissier (0000)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFF1E293B),
                            labelColor = EmeraldSecondary
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = EmeraldSecondary.copy(alpha = 0.5f)
                        )
                    )

                    SuggestionChip(
                        onClick = {
                            enteredPin = "2222"
                            onLoginWithPin("2222")
                        },
                        label = { Text("📊 Gérant (2222)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFF1E293B),
                            labelColor = WaveBlue
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = WaveBlue.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}
