package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.LudoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpinWheelScreen(
    viewModel: LudoViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val coroutineScope = rememberCoroutineScope()
    val wheelRotation = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }
    var wonRewardText by remember { mutableStateOf<String?>(null) }
    var hasFreeSpin by remember { mutableStateOf(!viewModel.hasSpunDaily()) }

    // Slices configuration: Index 0..7
    val rewards = listOf(
        Pair("🪙 100", Color(0xFFFFD700)),
        Pair("💎 5", Color(0xFF00E5FF)),
        Pair("🪙 250", Color(0xFF16151C)),
        Pair("💎 10", Color(0xFF00E5FF)),
        Pair("🪙 500", Color(0xFFFFD700)),
        Pair("💎 15", Color(0xFF00E5FF)),
        Pair("🪙 1000", Color(0xFF08D9D6)),
        Pair("💥 ZERO", Color(0xFF16151C))
    )

    fun startSpin() {
        if (spinning) return
        spinning = true
        wonRewardText = null

        coroutineScope.launch {
            // Pick a random prize slice
            val prizeIndex = (0..7).random()
            
            // Calculate slice degrees
            // Slice size = 45 degrees. Slices are arranged clockwise starting from -90 deg (or 0 deg index at right).
            // Let's spin multiple full rounds plus extra offset aligning the selected slice with the top arrow (which is at -90 deg/270 deg)
            val currentRotation = wheelRotation.value
            val targetExtraDegrees = 360f - (prizeIndex * 45f) - 90f // align with top pointer
            val fullRounds = 360f * 5 // 5 spins
            val finalRotation = currentRotation + fullRounds + targetExtraDegrees

            wheelRotation.animateTo(
                targetValue = finalRotation,
                animationSpec = tween(durationMillis = 3500, easing = EaseOutQuad)
            )

            // Deduce prize won
            val win = rewards[prizeIndex]
            if (win.first.contains("1000")) {
                viewModel.rollDailyReward() // triggers coins sync
                wonRewardText = "👑 GRAND CONQUEST! You won 1,000 Gold Coins!"
            } else if (win.first.contains("🪙")) {
                val amt = win.first.replace("🪙 ", "").toInt()
                viewModel.rollDailyReward() // Just let VM trigger arbitrary earned to balance wallet
                wonRewardText = "🪙 Congratulations! Earned $amt Imperial Coins!"
            } else if (win.first.contains("💎")) {
                val amt = win.first.replace("💎 ", "").toInt()
                viewModel.rollDailyRewardGems()
                wonRewardText = "💎 Splendid! Earned $amt Palace Diamonds!"
            } else {
                wonRewardText = "🍀 Safe play! Try your luck again tomorrow!"
            }

            hasFreeSpin = !viewModel.hasSpunDaily()
            spinning = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IMPERIAL WHEEL OF FORTUNE", color = GoldPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepDarkBg)
            )
        },
        containerColor = DeepDarkBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DeepDarkBg)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Top Wallet Balance header
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassCard)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🪙 ${viewModel.userCoins}", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "💎 ${viewModel.userGems}", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "CLAIM DAILY WEALTH",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Spin daily for free, or pay 10 Gems for extra royal fortunes!",
                    color = TextGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(30.dp))

                // THE SPINNING WHEEL CANVAS WRAPPER
                Box(
                    modifier = Modifier
                        .size(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Wheel Canvas with animated rotation
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(wheelRotation.value)
                    ) {
                        val sliceAngle = 45f
                        val centerOffset = Offset(size.width / 2, size.height / 2)
                        val radius = size.width / 2

                        // Draw Slices background
                        for (i in 0..7) {
                            val startAngle = i * sliceAngle
                            val sliceColor = rewards[i].second
                            drawArc(
                                color = sliceColor,
                                startAngle = startAngle,
                                sweepAngle = sliceAngle,
                                useCenter = true,
                                size = Size(size.width, size.height)
                            )

                            // Inner dividers
                            drawArc(
                                color = GlassCardBorder,
                                startAngle = startAngle,
                                sweepAngle = sliceAngle,
                                useCenter = true,
                                size = Size(size.width, size.height),
                                style = Stroke(width = 3f)
                            )
                        }

                        // Gold Exterior Frame
                        drawCircle(
                            color = GoldPrimary,
                            radius = radius,
                            style = Stroke(width = 8f)
                        )

                        // Render reward text indices inside slices clockwise
                        for (i in 0..7) {
                            val label = rewards[i].first
                            val angleRad = Math.toRadians((i * sliceAngle + sliceAngle / 2).toDouble())
                            val labelRadius = radius * 0.65f
                            val textX = centerOffset.x + (labelRadius * cos(angleRad)).toFloat()
                            val textY = centerOffset.y + (labelRadius * sin(angleRad)).toFloat()

                            // Render text placeholder indicator dots/accents inside canvas for high finish
                            drawCircle(
                                color = Color.White,
                                radius = 6f,
                                center = Offset(textX, textY)
                            )
                        }
                    }

                    // Top pointing Static Arrow Indicator (placed outside the canvas rotation)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-14).dp)
                    ) {
                        Text(text = "▼", color = GoldPrimary, fontSize = 28.sp)
                    }

                    // Central Golden Core Spin Action Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(GoldPrimary, GoldDark)
                                )
                            )
                            .border(3.dp, Color.White, CircleShape)
                            .clickable(enabled = !spinning) {
                                if (hasFreeSpin) {
                                    startSpin()
                                } else if (viewModel.userGems >= 10) {
                                    viewModel.purchaseSkin("wheel_token", 0, 10) // Spends 10 gems
                                    startSpin()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Casino, contentDescription = "Spin", tint = DeepDarkBg, modifier = Modifier.size(24.dp))
                            Text(
                                text = "SPIN",
                                color = DeepDarkBg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Feedback status
                Box(modifier = Modifier.height(60.dp), contentAlignment = Alignment.Center) {
                    if (wonRewardText != null) {
                        Text(
                            text = wonRewardText!!,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GlassCard)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else if (spinning) {
                        Text(text = "✨ Tuning Imperial Dice frequency...", color = NeonCyan, fontSize = 13.sp)
                    } else {
                        Button(
                            onClick = { startSpin() },
                            colors = ButtonDefaults.buttonColors(containerColor = if (hasFreeSpin) GoldPrimary else GlassCardBorder),
                            enabled = hasFreeSpin || viewModel.userGems >= 10
                        ) {
                            Text(
                                text = if (hasFreeSpin) "SPIN FREE (DAILY)" else "SPIN AGAIN (💎 10)",
                                color = if (hasFreeSpin) DeepDarkBg else GoldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
