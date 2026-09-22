package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CinematicSplashScreen(
    onContinueWithGmail: () -> Unit,
    onDirectAccess: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    // Cinematic Reveal Stages:
    // Stage 1: Reveal thin minimalist "&GEN" slowly
    // Stage 2: Reveal main line in solid background
    // Stage 3: Reveal Sign in / Sign up options & copyright
    val logoAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    var showAuthSection by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Slow, cinematic revealing style without loading bar
        delay(400)
        logoAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
        )
        delay(300)
        subtitleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
        delay(400)
        showAuthSection = true
    }

    val bgColor = if (isDark) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.background
    val primaryText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accentCyan = if (isDark) Color(0xFF00E5FF) else Color(0xFF1D4ED8)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1.2f))

            // Stage 1: Slowly reveal "&GEN" with minimalistic thin text
            Text(
                text = "&GEN",
                color = primaryText,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraLight,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(logoAlpha.value)
                    .testTag("splash_logo_gen")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Stage 2: Slowly reveal main line - small size in solid background
            Text(
                text = "STRUCTURE-BASED MOLECULAR DOCKING & LIGAND DESIGN",
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 2.4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(subtitleAlpha.value)
                    .testTag("splash_main_line")
            )

            Spacer(modifier = Modifier.weight(1f))

            // Stage 3: Sign in / Sign up Section
            AnimatedVisibility(
                visible = showAuthSection,
                enter = fadeIn(animationSpec = tween(800)) + slideInVertically(
                    initialOffsetY = { 60 },
                    animationSpec = tween(800)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Continue with Gmail
                    Button(
                        onClick = onContinueWithGmail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("continue_gmail_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentCyan,
                            contentColor = if (isDark) Color(0xFF041E24) else Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Gmail icon",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continue with Gmail",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Direct Access (without signin)
                    OutlinedButton(
                        onClick = onDirectAccess,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("direct_access_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF1E2E4A) else Color(0xFFCBD5E1)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = primaryText
                        )
                    ) {
                        Text(
                            text = "Direct Access (without signin)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Direct access",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Small text: copyright SHADMAN SHAKIB, &GEN
                    Text(
                        text = "copyright SHADMAN SHAKIB, &GEN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("copyright_text")
                    )
                }
            }
        }
    }
}
