package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToLobby: () -> Unit) {
    var loadingProgress by remember { mutableStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val crownScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crown_scale"
    )

    LaunchedEffect(Unit) {
        val duration = 2000
        val step = 100
        for (i in 0..step) {
            loadingProgress = i / step.toFloat()
            delay((duration / step).toLong())
        }
        onNavigateToLobby()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepDarkBg, Color(0xFF12101E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glowing Background Circles
        Box(
            modifier = Modifier
                .size(240.dp)
                .background(Brush.radialGradient(colors = listOf(GoldPrimary.copy(alpha = 0.15f), Color.Transparent)))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Crown Logo with Pulse and Metallic Borders
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(GlassCard)
                    .border(2.dp, GoldPrimary, CircleShape)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Gold crown symbol drawn conceptually with text or simply a beautifully layered character representation
                Text(
                    text = "👑",
                    fontSize = 54.sp,
                    modifier = Modifier.offset(y = (-4).dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Name with Royal & Neon Pairing
            Text(
                text = "ROYAL LUDO",
                color = GoldPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
            
            Text(
                text = "EMPIRE",
                color = NeonCyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                modifier = Modifier.offset(y = (-2).dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Premium loading bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(180.dp)
            ) {
                LinearProgressIndicator(
                    progress = { loadingProgress },
                    color = GoldPrimary,
                    trackColor = GlassCardBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Loading Palace resources...",
                    color = TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.sp
                )
            }
        }

        // Footer copyright info
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AAA IMPERIAL GAMES",
                color = GoldPrimary.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )
            Text(
                text = "v1.0.0 • Secured Multi-Lock",
                color = TextGray.copy(alpha = 0.5f),
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
