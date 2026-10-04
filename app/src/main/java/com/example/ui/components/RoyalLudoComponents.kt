package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlayerColor
import com.example.ui.screens.ClassicLudoDiceView
import com.example.ui.theme.*

/**
 * 👑 ROYAL LUDO HERO LOGO
 * Features:
 * - 3D Golden Crown on top
 * - Colorful 3D embossed bubble badges for "ROYAL LUDO" (Red, Cyan, Yellow, Green, Purple)
 * - 3D mini board centerpiece with pawns and tumbling dice in Santa/Royal crown style
 */
@Composable
fun RoyalLudoHeroLogo(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
    val crownBob by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crown_bob"
    )
    val diceRotate by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dice_rot"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 3D Golden Crown with Gems
        Box(
            modifier = Modifier
                .offset(y = crownBob.dp)
                .size(width = 90.dp, height = 50.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val crownPath = Path().apply {
                    moveTo(size.width * 0.15f, size.height * 0.85f)
                    lineTo(size.width * 0.85f, size.height * 0.85f)
                    lineTo(size.width * 0.90f, size.height * 0.35f)
                    lineTo(size.width * 0.70f, size.height * 0.55f)
                    lineTo(size.width * 0.50f, size.height * 0.15f)
                    lineTo(size.width * 0.30f, size.height * 0.55f)
                    lineTo(size.width * 0.10f, size.height * 0.35f)
                    close()
                }
                // Crown Gold Gradient
                drawPath(
                    path = crownPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFD700), Color(0xFFFFA000))
                    )
                )
                // Crown Base Rim
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFFFECB3), Color(0xFFFFD54F))),
                    topLeft = Offset(size.width * 0.12f, size.height * 0.80f),
                    size = Size(size.width * 0.76f, size.height * 0.14f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // 3 Crown Jewels
                drawCircle(color = Color(0xFF00E5FF), radius = 4f, center = Offset(size.width * 0.10f, size.height * 0.35f))
                drawCircle(color = Color(0xFFFF2E63), radius = 5.5f, center = Offset(size.width * 0.50f, size.height * 0.15f))
                drawCircle(color = Color(0xFF00E5FF), radius = 4f, center = Offset(size.width * 0.90f, size.height * 0.35f))
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // "ROYAL" Bubble Badges Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val letters = listOf(
                Pair("R", Color(0xFFE52521)),
                Pair("O", Color(0xFF0080FF)),
                Pair("Y", Color(0xFFFFC20E)),
                Pair("A", Color(0xFF00A859)),
                Pair("L", Color(0xFFAB47BC))
            )
            letters.forEach { (char, bgCol) ->
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(bgCol.copy(alpha = 0.9f), bgCol, Color.Black.copy(alpha = 0.4f))
                            )
                        )
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Specular gloss arc
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                                center = Offset(size.width * 0.35f, size.height * 0.3f),
                                radius = size.width * 0.4f
                            )
                        )
                    }
                    Text(
                        text = char,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.8f),
                                offset = Offset(1.5f, 1.5f),
                                blurRadius = 2f
                            )
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // "LUDO" in Gold & White 3D Ribbon Banner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val ludoLetters = listOf(
                Pair("L", Color(0xFF0080FF)),
                Pair("U", Color(0xFFE52521)),
                Pair("D", Color(0xFFFFC20E)),
                Pair("O", Color(0xFF00A859))
            )
            ludoLetters.forEach { (char, col) ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .shadow(3.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White, Color(0xFFF0F0F0))
                            )
                        )
                        .border(1.5.dp, col, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char,
                        color = col,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Center 3D Mini-Board Graphic with Pawns & Tumbling Dice
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F3A75), Color(0xFF081C3D))
                    )
                )
                .border(1.5.dp, GoldPrimary, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Blue Pawn
                Text("♟️", fontSize = 28.sp, modifier = Modifier.scale(scaleX = -1f, scaleY = 1f))
                // Red Pawn
                Text("🔺", fontSize = 20.sp, color = LudoRed)
                // Center 3D Tumbling Dice
                Box(
                    modifier = Modifier
                        .rotate(diceRotate)
                        .size(38.dp)
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.5.dp, GoldPrimary, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚅", fontSize = 28.sp, color = Color.Black)
                }
                // Green Pawn
                Text("🟢", fontSize = 20.sp)
                // Right Yellow Pawn
                Text("👑", fontSize = 26.sp)
            }
        }
    }
}

/**
 * 🎮 2x2 GAME MODE CARD (Authentic Ludo King Golden-Framed Blue Box)
 */
@Composable
fun RoyalGameModeCard(
    title: String,
    subtitle: String,
    iconSlot: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(138.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(3.dp, Brush.verticalGradient(listOf(GoldAccentLight, GoldPrimary, GoldAccentDark)))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1B63C6), Color(0xFF0E387A), Color(0xFF09214D))
                    )
                )
        ) {
            // Top Gloss Bevel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Graphic / Icon Container
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF12438C), Color(0xFF082452))
                            )
                        )
                        .border(1.5.dp, GoldPrimary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    iconSlot()
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title Banner in Gold Plaque
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFFE082), Color(0xFFFFB300), Color(0xFFFFA000))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = title,
                        color = Color(0xFF2E1C00),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 🔘 QUICK ACTION BUTTON (Leaderboard, Settings, Tournament)
 * Glossy golden rounded-square button with embossed bevel
 */
@Composable
fun RoyalQuickActionButton(
    iconText: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .shadow(6.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2A74DE), Color(0xFF12438C), Color(0xFF092552))
                    )
                )
                .border(2.5.dp, Brush.verticalGradient(listOf(GoldAccentLight, GoldPrimary, GoldAccentDark)), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Bevel shine
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .align(Alignment.TopCenter)
                    .background(Color.White.copy(alpha = 0.22f))
            )
            Text(text = iconText, fontSize = 28.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 📍 CORNER PLAYER WIDGET FOR GAME SCREEN (Matching Screenshots 2 & 3)
 * Features:
 * - Rectangular blue card with golden border
 * - Colored teardrop marker pin on outer edge
 * - Dedicated square Dice Holder Box with recessed pink/white gloss finish
 * - Animated pulsing Golden Arrow pointing towards active player's dice
 */
@Composable
fun RoyalCornerPlayerWidget(
    playerName: String,
    playerColor: PlayerColor,
    isActiveTurn: Boolean,
    score: Int,
    diceValue: Int,
    isRolling: Boolean,
    hasRolled: Boolean,
    turnRemainingSeconds: Int,
    onRollDice: () -> Unit,
    modifier: Modifier = Modifier,
    isHuman: Boolean = true
) {
    // Arrow bounce animation
    val infiniteTransition = rememberInfiniteTransition(label = "turn_arrow")
    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_bounce"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(if (isActiveTurn) 6.dp else 2.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    if (isActiveTurn)
                        listOf(Color(0xFF1E67D1), Color(0xFF0E3E85))
                    else
                        listOf(Color(0xFF102747), Color(0xFF091629))
                )
            )
            .border(
                width = if (isActiveTurn) 2.5.dp else 1.2.dp,
                color = if (isActiveTurn) GoldPrimary else Color(0xFF2C568E),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Colored Teardrop / Pin Goti Marker
        Box(
            modifier = Modifier
                .size(34.dp)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, playerColor.color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(playerColor.color)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 2. Player Name & Score
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playerName,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Goal: $score/4",
                    color = playerColor.color,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (isActiveTurn) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "⏱️${turnRemainingSeconds}s",
                        color = GoldPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Animated Golden Arrow pointing to Dice when active
        if (isActiveTurn && !hasRolled) {
            Text(
                text = "▶",
                color = GoldPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.offset(x = arrowOffset.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
        }

        // 4. Dedicated Square Dice Holder Box
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(3.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF0F5), Color(0xFFE8D0D8))
                    )
                )
                .border(
                    width = 2.dp,
                    color = if (isActiveTurn) GoldPrimary else Color(0xFFC0A0A8),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable(enabled = isActiveTurn && !hasRolled && !isRolling && isHuman) {
                    onRollDice()
                },
            contentAlignment = Alignment.Center
        ) {
            if (isActiveTurn) {
                ClassicLudoDiceView(
                    value = diceValue,
                    color = playerColor.color,
                    isRolling = isRolling
                )
            } else {
                // Blank recessed pocket waiting for player's turn
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.08f))
                )
            }
        }
    }
}

/**
 * 📜 ROYAL VICTORY SCROLL (Matching Screenshot 1)
 * Beautiful blue parchment scroll with gold rod handles, "Congratulations!" title,
 * 1st, 2nd, 3rd place crowns, winner banners, and replay / menu capsule buttons.
 */
@Composable
fun RoyalVictoryScrollDialog(
    winnerName: String,
    winnerColor: PlayerColor,
    rankedPlayers: List<Pair<String, PlayerColor>>,
    coinsEarned: Int,
    onMenuClicked: () -> Unit,
    onReplayClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Golden Top Scroll Rod Handle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFF59D), Color(0xFFFFD700), Color(0xFFFFA000), Color(0xFF795548))
                        )
                    )
                    .border(1.5.dp, Color(0xFFFFF9C4), RoundedCornerShape(9.dp))
            ) {}

            // Scroll Body
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(2.5.dp, GoldPrimary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF072147), Color(0xFF031229), Color(0xFF020B1A))
                            )
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // "Congratulations!" Title
                    Text(
                        text = "Congratulations!",
                        color = GoldPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color(0xFFFF6F00),
                                offset = Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "🏆 Royal Ludo Tournament Champion 🏆",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Player Rankings List
                    rankedPlayers.take(4).forEachIndexed { index, (name, col) ->
                        val isWinner = index == 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isWinner)
                                        Brush.horizontalGradient(listOf(Color(0xFF1E4C8A), Color(0xFF0D284F)))
                                    else
                                        Brush.horizontalGradient(listOf(Color(0xFF0D1E36), Color(0xFF071221)))
                                )
                                .border(
                                    width = if (isWinner) 1.5.dp else 0.8.dp,
                                    color = if (isWinner) GoldPrimary else Color(0xFF1B3A68),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Crown icon badge (1st, 2nd, 3rd)
                            Text(
                                text = when (index) {
                                    0 -> "👑 #1"
                                    1 -> "🥈 #2"
                                    2 -> "🥉 #3"
                                    else -> "  #4"
                                },
                                color = when (index) {
                                    0 -> GoldPrimary
                                    1 -> Color(0xFFE0E0E0)
                                    2 -> Color(0xFFCD7F32)
                                    else -> Color.Gray
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Colored pin teardrop
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(col.color)
                                    .border(1.dp, Color.White, CircleShape)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Name
                            Text(
                                text = name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )

                            // Status Tag (Winner / Loser)
                            Text(
                                text = if (index < 3) "Winner" else "Loser",
                                color = if (index < 3) GoldPrimary else Color(0xFFFF5252),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Coins Prize Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0A2247))
                            .border(1.dp, GoldPrimary, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🪙", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+$coinsEarned Coins Won",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bottom Capsule Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Menu Button
                        Button(
                            onClick = onMenuClicked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F3A75)
                            ),
                            border = BorderStroke(1.5.dp, GoldPrimary),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text("☰ Menu", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Replay Button
                        Button(
                            onClick = onReplayClicked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1976D2)
                            ),
                            border = BorderStroke(1.5.dp, GoldPrimary),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text("🔄 Replay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
