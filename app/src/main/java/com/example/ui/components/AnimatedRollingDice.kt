package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sound.LudoSoundManager
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Animated Physics-Based Rolling Dice Component for Jetpack Compose.
 *
 * Features:
 * - Real RNG generator trigger on tap or external trigger
 * - Physics-based 3D rotations (tumble along X, Y, and Z axes)
 * - Kinetic vertical bouncing with gravity & energy dampening
 * - Dynamic squash & stretch impact physics on landing
 * - Dynamic ground shadow that blurs & expands when airborne and sharpens on contact
 * - Decelerating face transitions simulating visual angular velocity
 * - Specular 3D lighting, beveled edges, and authentic recessed pips
 * - Skin support: Classic Ivory, Neon Gold, Epic Inferno, Deep Crystal, Cosmic Diamond, Obsidian Shadow
 */
@Composable
fun AnimatedRollingDice(
    modifier: Modifier = Modifier,
    diceSize: Dp = 60.dp,
    currentValue: Int = 1,
    skinName: String = "Neon Gold",
    accentColor: Color = GoldPrimary,
    enabled: Boolean = true,
    isExternalRolling: Boolean = false,
    externalTargetValue: Int? = null,
    onRollStarted: () -> Unit = {},
    onRollFinished: (result: Int) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Displayed face value during rolling and at rest
    var displayedValue by remember(currentValue) { mutableStateOf(currentValue.coerceIn(1, 6)) }
    var isLocallyRolling by remember { mutableStateOf(false) }

    val isRolling = isLocallyRolling || isExternalRolling

    // Physics 3D Animatable States
    val rotationX = remember { Animatable(0f) }
    val rotationY = remember { Animatable(0f) }
    val rotationZ = remember { Animatable(0f) }
    val bounceY = remember { Animatable(0f) } // Negative values indicate elevation above ground
    val diceScaleX = remember { Animatable(1f) }
    val diceScaleY = remember { Animatable(1f) }
    val shadowSpread = remember { Animatable(1f) }
    val shadowAlpha = remember { Animatable(0.45f) }
    val glowPulse = remember { Animatable(0f) }

    // Core Roll Execution Trigger
    fun executePhysicsRoll(targetVal: Int?) {
        if (isLocallyRolling) return
        isLocallyRolling = true
        onRollStarted()

        // Haptic feedback & audio
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            LudoSoundManager.playDiceRoll()
        } catch (_: Exception) { }

        coroutineScope.launch {
            // Determine result using real RNG or forced external target
            val result = targetVal ?: Random.nextInt(1, 7)

            // Random tumbling angles (several full 360-degree spins with chaotic axis bias)
            val turnsX = (3..5).random() * 360f + listOf(-30f, 0f, 30f, 45f).random()
            val turnsY = (3..5).random() * 360f + listOf(-45f, 0f, 30f, 60f).random()
            val turnsZ = (2..4).random() * 360f + listOf(-20f, 0f, 25f).random()

            // 1. Launch tumbling 3D rotations with FastOutSlowInEasing deceleration
            launch {
                rotationX.snapTo(0f)
                rotationY.snapTo(0f)
                rotationZ.snapTo(0f)

                launch {
                    rotationX.animateTo(
                        targetValue = turnsX,
                        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
                    )
                    rotationX.animateTo(0f, animationSpec = tween(120, easing = LinearEasing))
                }
                launch {
                    rotationY.animateTo(
                        targetValue = turnsY,
                        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
                    )
                    rotationY.animateTo(0f, animationSpec = tween(120, easing = LinearEasing))
                }
                launch {
                    rotationZ.animateTo(
                        targetValue = turnsZ,
                        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
                    )
                    rotationZ.animateTo(0f, animationSpec = tween(120, easing = LinearEasing))
                }
            }

            // 2. Physics-based Vertical Bounces with Gravitational Decay
            launch {
                // Bounce 1: High jump upwards
                launch {
                    shadowSpread.animateTo(1.6f, tween(200, easing = FastOutSlowInEasing))
                    shadowAlpha.animateTo(0.2f, tween(200))
                }
                bounceY.animateTo(-75f, tween(200, easing = FastOutLinearInEasing))

                // Fall down and hit ground
                launch {
                    shadowSpread.animateTo(0.9f, tween(180, easing = LinearOutSlowInEasing))
                    shadowAlpha.animateTo(0.55f, tween(180))
                }
                bounceY.animateTo(0f, tween(180, easing = LinearOutSlowInEasing))

                // Ground Impact 1: Squash and stretch
                diceScaleY.snapTo(0.82f)
                diceScaleX.snapTo(1.18f)
                delay(30)
                diceScaleY.animateTo(1f, tween(80, easing = FastOutSlowInEasing))
                diceScaleX.animateTo(1f, tween(80, easing = FastOutSlowInEasing))

                // Bounce 2: Medium rebound
                launch {
                    shadowSpread.animateTo(1.3f, tween(130))
                    shadowAlpha.animateTo(0.3f, tween(130))
                }
                bounceY.animateTo(-30f, tween(130, easing = FastOutLinearInEasing))

                launch {
                    shadowSpread.animateTo(0.95f, tween(120))
                    shadowAlpha.animateTo(0.5f, tween(120))
                }
                bounceY.animateTo(0f, tween(120, easing = LinearOutSlowInEasing))

                // Ground Impact 2: Smaller squash
                diceScaleY.snapTo(0.91f)
                diceScaleX.snapTo(1.09f)
                delay(20)
                diceScaleY.animateTo(1f, tween(60))
                diceScaleX.animateTo(1f, tween(60))

                // Bounce 3: Micro-settle vibration
                bounceY.animateTo(-10f, tween(80, easing = FastOutLinearInEasing))
                bounceY.animateTo(0f, tween(80, easing = LinearOutSlowInEasing))
                shadowSpread.animateTo(1f, tween(80))
                shadowAlpha.animateTo(0.45f, tween(80))
            }

            // 3. Face cycling with realistic deceleration curve
            val intervalSchedule = listOf(35L, 40L, 50L, 65L, 85L, 110L, 140L, 180L, 230L)
            for (interval in intervalSchedule) {
                displayedValue = (1..6).filter { it != displayedValue }.random()
                delay(interval)
            }

            // Lock onto the final calculated random number
            displayedValue = result

            // Trigger celebration glow effect if 6 or lucky roll
            if (result == 6) {
                launch {
                    glowPulse.snapTo(1f)
                    glowPulse.animateTo(0f, animationSpec = tween(600, easing = LinearOutSlowInEasing))
                }
            }

            delay(100)
            isLocallyRolling = false
            onRollFinished(result)
        }
    }

    // React to external roll triggers if coordinated by ViewModel
    LaunchedEffect(isExternalRolling) {
        if (isExternalRolling && !isLocallyRolling) {
            executePhysicsRoll(externalTargetValue)
        }
    }

    Box(
        modifier = modifier
            .size(diceSize + 24.dp)
            .clickable(
                enabled = enabled && !isRolling,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                executePhysicsRoll(externalTargetValue)
            },
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Ground Shadow that responds to dice elevation
        Canvas(
            modifier = Modifier
                .size(diceSize * 0.95f)
                .offset(y = (diceSize * 0.42f))
                .graphicsLayer {
                    this.scaleX = shadowSpread.value
                    this.scaleY = shadowSpread.value * 0.5f
                    this.alpha = shadowAlpha.value
                }
        ) {
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.8f),
                        Color.Black.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                )
            )
        }

        // Rolling 3D Cube Body
        Box(
            modifier = Modifier
                .offset(y = bounceY.value.dp)
                .graphicsLayer {
                    this.rotationX = rotationX.value
                    this.rotationY = rotationY.value
                    this.rotationZ = rotationZ.value
                    this.scaleX = diceScaleX.value
                    this.scaleY = diceScaleY.value
                    this.cameraDistance = 14f * density
                }
                .size(diceSize),
            contentAlignment = Alignment.Center
        ) {
            // Render 3D Dice Face
            PhysicsDiceFace(
                value = displayedValue,
                size = diceSize,
                skinName = skinName,
                accentColor = accentColor,
                isRolling = isRolling
            )

            // Celebration Ring for 6
            if (glowPulse.value > 0.01f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            width = (3 * glowPulse.value).dp,
                            color = GoldPrimary.copy(alpha = glowPulse.value),
                            shape = RoundedCornerShape((diceSize.value * 0.22f).dp)
                        )
                )
            }
        }
    }
}

/**
 * High-fidelity 3D Dice Face with rounded bevels, gradient lighting, and recessed pips
 */
@Composable
fun PhysicsDiceFace(
    value: Int,
    size: Dp,
    skinName: String,
    accentColor: Color,
    isRolling: Boolean,
    modifier: Modifier = Modifier
) {
    val cornerRadius = (size.value * 0.22f).dp

    // Color theme based on selected skin
    val (backgroundBrush, borderBrush, pipColor, pipInnerGlow) = when (skinName) {
        "Epic Inferno" -> Quadruple(
            Brush.verticalGradient(listOf(Color(0xFFFF5722), Color(0xFFB71C1C))),
            Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFD84315))),
            Color(0xFFFFEB3B),
            Color(0xFFFFF9C4)
        )
        "Deep Crystal" -> Quadruple(
            Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF0D47A1))),
            Brush.linearGradient(listOf(Color(0xFF80D8FF), Color(0xFF0091EA))),
            Color(0xFFFFFFFF),
            Color(0xFF84FFFF)
        )
        "Cosmic Diamond" -> Quadruple(
            Brush.radialGradient(listOf(Color(0xFFE1BEE7), Color(0xFF4A148C))),
            Brush.linearGradient(listOf(Color(0xFFF48FB1), Color(0xFFBA68C8))),
            Color(0xFFFFFFFF),
            Color(0xFFEA80FC)
        )
        "Royal Emerald" -> Quadruple(
            Brush.verticalGradient(listOf(Color(0xFF00E676), Color(0xFF1B5E20))),
            Brush.linearGradient(listOf(Color(0xFFB9F6CA), Color(0xFF00C853))),
            Color(0xFFFFFFFF),
            Color(0xFF69F0AE)
        )
        "Obsidian Shadow" -> Quadruple(
            Brush.verticalGradient(listOf(Color(0xFF263238), Color(0xFF101416))),
            Brush.linearGradient(listOf(Color(0xFFCFD8DC), Color(0xFF37474F))),
            Color(0xFF00E5FF),
            Color(0xFF80D8FF)
        )
        "Solar Flare" -> Quadruple(
            Brush.linearGradient(listOf(Color(0xFFFF9100), Color(0xFFFF3D00))),
            Brush.linearGradient(listOf(Color(0xFFFFE57F), Color(0xFFFF6D00))),
            Color(0xFFFFFFFF),
            Color(0xFFFFD180)
        )
        else -> Quadruple( // Classic Ivory & Gold
            Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEDE7F6))),
            Brush.linearGradient(listOf(accentColor, GoldDark)),
            if (value == 6) LudoRed else Color(0xFF1C1921),
            if (value == 6) Color(0xFFFF8A80) else Color(0xFF424242)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (isRolling) 8.dp else 4.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = accentColor.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundBrush)
            .border(BorderStroke(2.dp, borderBrush), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        // Specular highlight gradient (light reflecting from top-left)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Ambient light specular sweep
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(this.size.width * 0.25f, this.size.height * 0.25f),
                    radius = this.size.width * 0.6f
                ),
                cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
            )

            // Bevel edge highlight
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                    start = Offset(0f, 0f),
                    end = Offset(this.size.width * 0.5f, this.size.height * 0.5f)
                ),
                cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                style = Stroke(width = 1.5f)
            )

            // Pip rendering coordinates
            val pipRadius = this.size.width * 0.11f
            val leftX = this.size.width * 0.25f
            val midX = this.size.width * 0.50f
            val rightX = this.size.width * 0.75f

            val topY = this.size.height * 0.25f
            val midY = this.size.height * 0.50f
            val bottomY = this.size.height * 0.75f

            val pips = when (value) {
                1 -> listOf(Offset(midX, midY))
                2 -> listOf(Offset(leftX, topY), Offset(rightX, bottomY))
                3 -> listOf(Offset(leftX, topY), Offset(midX, midY), Offset(rightX, bottomY))
                4 -> listOf(Offset(leftX, topY), Offset(rightX, topY), Offset(leftX, bottomY), Offset(rightX, bottomY))
                5 -> listOf(Offset(leftX, topY), Offset(rightX, topY), Offset(midX, midY), Offset(leftX, bottomY), Offset(rightX, bottomY))
                6 -> listOf(
                    Offset(leftX, topY), Offset(rightX, topY),
                    Offset(leftX, midY), Offset(rightX, midY),
                    Offset(leftX, bottomY), Offset(rightX, bottomY)
                )
                else -> listOf(Offset(midX, midY))
            }

            pips.forEach { center ->
                // Recessed pip drop shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = pipRadius * 1.05f,
                    center = center + Offset(1.5f, 1.5f)
                )
                // Pip main body
                drawCircle(
                    color = pipColor,
                    radius = pipRadius,
                    center = center
                )
                // Pip specular glossy highlight
                drawCircle(
                    color = pipInnerGlow.copy(alpha = 0.6f),
                    radius = pipRadius * 0.35f,
                    center = center - Offset(pipRadius * 0.25f, pipRadius * 0.25f)
                )
            }
        }
    }
}

/**
 * Interactive Playground / Showcase Component for the Physics Dice
 */
@Composable
fun PhysicsDiceShowcaseCard(
    modifier: Modifier = Modifier,
    onRollResult: (Int) -> Unit = {}
) {
    var rollCount by remember { mutableIntStateOf(0) }
    var lastRollResult by remember { mutableIntStateOf(1) }
    var sixStreak by remember { mutableIntStateOf(0) }
    val rollHistory = remember { mutableStateListOf<Int>() }
    var selectedSkin by remember { mutableStateOf("Neon Gold") }

    val skinList = listOf("Neon Gold", "Epic Inferno", "Deep Crystal", "Cosmic Diamond", "Royal Emerald", "Obsidian Shadow")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        border = BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🎲 PHYSICS 3D DICE ENGINE",
                    color = GoldPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Badge(containerColor = NeonCyan) {
                    Text("Interactive Compose", color = DeepDarkBg, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // The animated physics rolling dice
            AnimatedRollingDice(
                diceSize = 78.dp,
                currentValue = lastRollResult,
                skinName = selectedSkin,
                accentColor = GoldPrimary,
                onRollStarted = {
                    rollCount++
                },
                onRollFinished = { res ->
                    lastRollResult = res
                    rollHistory.add(0, res)
                    if (rollHistory.size > 8) rollHistory.removeAt(rollHistory.lastIndex)
                    if (res == 6) sixStreak++ else sixStreak = 0
                    onRollResult(res)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap dice above to roll with physics tumble & bounce!",
                color = TextGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DeepDarkBg.copy(alpha = 0.6f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RESULT", color = TextGray, fontSize = 9.sp)
                    Text("$lastRollResult", color = if (lastRollResult == 6) LudoRed else GoldPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL ROLLS", color = TextGray, fontSize = 9.sp)
                    Text("$rollCount", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SIXES", color = TextGray, fontSize = 9.sp)
                    Text("${rollHistory.count { it == 6 }}", color = LudoGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Skin Selector Chips
            Spacer(modifier = Modifier.height(12.dp))
            Text("Select Skin:", color = TextWhite.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                skinList.take(3).forEach { skin ->
                    val isSelected = selectedSkin == skin
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSkin = skin },
                        label = { Text(skin, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = DeepDarkBg,
                            labelColor = TextWhite
                        )
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
