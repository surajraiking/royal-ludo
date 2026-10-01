package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.LudoBoardComponent
import com.example.ui.theme.*
import com.example.viewmodel.LudoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: LudoViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var messageText by remember { mutableStateOf("") }
    
    // Dice rotation animation state
    val diceRotation = remember { Animatable(0f) }
    
    // Safe drawing insets
    val insetsPadding = WindowInsets.safeDrawing.asPaddingValues()

    // Confetti particles local falling state
    val confettiList = remember { mutableStateListOf<Pair<Offset, Color>>() }
    
    LaunchedEffect(viewModel.gameState) {
        if (viewModel.gameState == GameState.PLAYING) {
            // Keep simulated voice fluctuations
            while (true) {
                delay(120)
            }
        }
    }

    // Capture dice animation trigger
    LaunchedEffect(viewModel.rollingAnimActive) {
        if (viewModel.rollingAnimActive) {
            diceRotation.animateTo(
                targetValue = diceRotation.value + 1080f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }
    }

    // Render Confetti falling loop for celebration
    if (viewModel.showWinningCelebration) {
        LaunchedEffect(Unit) {
            val colors = listOf(GoldPrimary, NeonCyan, LudoRed, LudoGreen, LudoYellow, Color.White)
            while (viewModel.showWinningCelebration) {
                if (confettiList.size < 50) {
                    confettiList.add(
                        Pair(
                            Offset((0..1000).random().toFloat(), -10f),
                            colors.random()
                        )
                    )
                }
                // Drift falling down
                for (i in confettiList.indices) {
                    val current = confettiList[i]
                    val updatedPos = Offset(
                        current.first.x + (-10..10).random(),
                        current.first.y + (15..30).random()
                    )
                    confettiList[i] = Pair(updatedPos, current.second)
                }
                // Exclude out of screen
                confettiList.removeAll { it.first.y > 2000f }
                delay(30)
            }
        }
    }

    // Layout shaker for captures
    val shakeOffset by animateDpAsState(
        targetValue = if (viewModel.screenShakeActive) listOf(-8.dp, 8.dp, -4.dp, 4.dp).random() else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "shake"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDarkBg)
            .padding(top = insetsPadding.calculateTopPadding(), bottom = insetsPadding.calculateBottomPadding() + 8.dp)
            .offset(x = shakeOffset, y = shakeOffset)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP STATS BAR & BACK BUTTON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        viewModel.exitGame()
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(GlassCard)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Exit", tint = GoldPrimary)
                }

                // Voice Stream Fluctuating Indicator
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassCard)
                        .border(1.dp, GlassCardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (viewModel.isVoiceChatActive) LudoGreen else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VOICE",
                        color = if (viewModel.isVoiceChatActive) TextWhite else TextGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    // Simulating Voice Waves
                    Canvas(modifier = Modifier.size(width = 40.dp, height = 12.dp)) {
                        val amplitude = 5f
                        val frequency = 0.5f
                        val phase = System.currentTimeMillis() * 0.015f
                        val points = 15
                        val step = size.width / points
                        val path = androidx.compose.ui.graphics.Path()

                        for (i in 0..points) {
                            val x = i * step
                            val y = (size.height / 2f) + sin((x * frequency + phase).toDouble()).toFloat() * amplitude
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path = path, color = LudoGreen, style = Stroke(width = 3f))
                    }
                }

                // Selected Dice visual
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassCard)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(text = "👑 ${viewModel.selectedDiceSkin}", color = GoldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 2. PLAYER AVATARS ROW - GREEN AND YELLOW (TOP OPPONENTS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Player GREEN (Turn Index 1)
                val greenPlayer = viewModel.players.getOrNull(1)
                if (greenPlayer != null) {
                    PlayerWidget(
                        player = greenPlayer,
                        isActiveTurn = viewModel.currentTurnColor == PlayerColor.GREEN,
                        color = PlayerColor.GREEN,
                        score = viewModel.tokens.filter { it.color == PlayerColor.GREEN && it.state == TokenState.GOAL }.size
                    )
                }

                // Player YELLOW (Turn Index 2)
                val yellowPlayer = viewModel.players.getOrNull(2)
                if (yellowPlayer != null) {
                    PlayerWidget(
                        player = yellowPlayer,
                        isActiveTurn = viewModel.currentTurnColor == PlayerColor.YELLOW,
                        color = PlayerColor.YELLOW,
                        score = viewModel.tokens.filter { it.color == PlayerColor.YELLOW && it.state == TokenState.GOAL }.size
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. MAIN CENTER LUDO BOARD COMPONENT
            LudoBoardComponent(
                tokens = viewModel.tokens,
                movableTokenIds = viewModel.movableTokenIds,
                currentTurnColor = viewModel.currentTurnColor,
                onTokenClicked = { tok -> viewModel.moveToken(tok) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 4. PLAYER AVATARS ROW - RED AND BLUE (BOTTOM PLAYERS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Player RED (You) (Turn Index 0)
                val redPlayer = viewModel.players.getOrNull(0)
                if (redPlayer != null) {
                    PlayerWidget(
                        player = redPlayer,
                        isActiveTurn = viewModel.currentTurnColor == PlayerColor.RED,
                        color = PlayerColor.RED,
                        score = viewModel.tokens.filter { it.color == PlayerColor.RED && it.state == TokenState.GOAL }.size
                    )
                }

                // Player BLUE (Turn Index 3)
                val bluePlayer = viewModel.players.getOrNull(3)
                if (bluePlayer != null) {
                    PlayerWidget(
                        player = bluePlayer,
                        isActiveTurn = viewModel.currentTurnColor == PlayerColor.BLUE,
                        color = PlayerColor.BLUE,
                        score = viewModel.tokens.filter { it.color == PlayerColor.BLUE && it.state == TokenState.GOAL }.size
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. INTERACTIVE DICE ROLLER & HINT STATUS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassCard)
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Hint display
                Column(modifier = Modifier.weight(1f)) {
                    val activePlayer = viewModel.getActivePlayer()
                    val isUserTurn = viewModel.currentTurnColor == PlayerColor.RED && activePlayer?.type == PlayerType.LOCAL_HUMAN
                    Text(
                        text = if (isUserTurn) {
                            if (!viewModel.hasRolled) "YOUR TURN! ROLL DICE 🎲" else "TAP ACTION HIGHLIGHTS TOKEN!"
                        } else {
                            "Player ${activePlayer?.name ?: "Opponent"} thinking..."
                        },
                        color = if (isUserTurn) NeonCyan else GoldPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (viewModel.hasRolled) "Dice Value: ${viewModel.diceValue}" else "Empower your destiny",
                        color = TextGray,
                        fontSize = 10.sp
                    )
                }

                // Premium Physics 3D dice simulation
                val isMyTurnToRoll = viewModel.currentTurnColor == PlayerColor.RED && !viewModel.hasRolled && !viewModel.rollingAnimActive
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isMyTurnToRoll) Brush.radialGradient(colors = listOf(GoldPrimary, Color(0xFFC59F00)))
                            else Brush.radialGradient(colors = listOf(GlassCardBorder, GlassCard))
                        )
                        .border(
                            1.5.dp,
                            if (isMyTurnToRoll) NeonCyan else GlassCardBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .scale(if (isMyTurnToRoll && !viewModel.rollingAnimActive) 1.05f else 1f)
                        .rotate(diceRotation.value)
                        .clickable(enabled = isMyTurnToRoll) {
                            viewModel.rollDice()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.rollingAnimActive) {
                        Text(
                            text = listOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅").random(),
                            color = Color.White,
                            fontSize = 42.sp
                        )
                    } else {
                        val skinSymbol = when (viewModel.selectedDiceSkin) {
                            "Epic Inferno" -> "🔥"
                            "Deep Crystal" -> "❄️"
                            "Cosmic Diamond" -> "💎"
                            else -> ""
                        }

                        if (skinSymbol.isNotEmpty() && viewModel.diceValue == 6) {
                            Text(text = skinSymbol, fontSize = 28.sp)
                        } else {
                            Text(
                                text = when (viewModel.diceValue) {
                                    1 -> "⚀"
                                    2 -> "⚁"
                                    3 -> "⚂"
                                    4 -> "⚃"
                                    5 -> "⚄"
                                    6 -> "⚅"
                                    else -> "⚀"
                                },
                                color = if (isMyTurnToRoll) DeepDarkBg else GoldPrimary,
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6. LIVE SLIDING SOCIAL CHAT FEED
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassCard)
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                // Chats list
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE SPEECH FEED",
                            color = TextGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        
                        // Floating Emojis Keyboard Pop-up Row
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("😂", "😎", "👑", "🔥", "😭", "😮").forEach { r ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(GlassCardBorder)
                                        .clickable { viewModel.sendUserEmoji(r) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = r, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Feed Container
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        reverseLayout = true
                    ) {
                        items(viewModel.chatMessages.asReversed()) { chat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${chat.sender}: ",
                                    color = if (chat.sender.contains("You")) NeonCyan else GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = chat.message,
                                    color = if (chat.isEmojiOnly) Color.Unspecified else TextWhite,
                                    fontSize = if (chat.isEmojiOnly) 16.sp else 11.sp,
                                    style = LocalTextStyle.current
                                )
                            }
                        }
                    }

                    // Input Field Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Send quick message...", fontSize = 11.sp, color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GlassCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            textStyle = TextStyle(fontSize = 11.sp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendChatMessage("${viewModel.username} (You)", messageText)
                                    messageText = ""
                                }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.Send,
                                contentDescription = "Send",
                                tint = DeepDarkBg,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 7. MULTIPLAYER MATCHMAKING SIMULATOR LOADER OVERLAY
        if (viewModel.gameState == GameState.MATCHMAKING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepDarkBg.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Matchmaker spinning crown
                    Box(
                        modifier = Modifier
                            .size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 4.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(text = "👑", fontSize = 36.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "ROYAL EMPIRE CLUB",
                        color = GoldPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = viewModel.matchmakingStatusText,
                        color = TextWhite,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Simulated synced players roster
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..4) {
                            val isActive = i <= viewModel.matchmakerFoundCount
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) GoldPrimary.copy(alpha = 0.2f) else GlassCardBorder)
                                    .border(
                                        2.dp,
                                        if (isActive) GoldPrimary else Color.Transparent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isActive) {
                                    Text(text = "👤", fontSize = 20.sp)
                                } else {
                                    CircularProgressIndicator(color = TextGray, strokeWidth = 1.5.dp, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. WINNING CELEBRATION CONFETTI OVERLAY
        if (viewModel.showWinningCelebration) {
            // Overlay blocking interactions and drawing falling confetti on custom canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                confettiList.forEach { (pos, col) ->
                    drawCircle(color = col, radius = 6f, center = pos)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .clickable { /* Block taps */ },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    border = BorderStroke(2.dp, GoldPrimary),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🏆", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "IMPERIAL CONQUEST!",
                            color = GoldPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Winner: ${viewModel.winner?.name ?: "Unknown sovereign"}",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        val weWon = viewModel.winner?.color == PlayerColor.RED
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassCardBorder)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🪙", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (weWon) "+250 Gold Coins" else "+50 Coins (Consolation)",
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                viewModel.exitGame()
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "RETURN TO PALACE LOBBY",
                                color = DeepDarkBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerWidget(
    player: LudoPlayer,
    isActiveTurn: Boolean,
    color: PlayerColor,
    score: Int
) {
    val transition = rememberInfiniteTransition(label = "active_player")
    val outlineGlow by transition.animateColor(
        initialValue = color.color,
        targetValue = color.color.copy(alpha = 0.2f),
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        border = BorderStroke(
            1.5.dp,
            if (isActiveTurn) outlineGlow else GlassCardBorder
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(155.dp)
            .shadow(if (isActiveTurn) 8.dp else 0.dp, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile symbol
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.color.copy(alpha = 0.2f))
                    .border(1.5.dp, color.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (player.avatarId) {
                        "avatar_crown" -> "👑"
                        "avatar_lion" -> "🦁"
                        "avatar_car" -> "🏎️"
                        "avatar_dragon" -> "🐉"
                        "avatar_unicorn" -> "🦄"
                        "avatar_alien" -> "👽"
                        "avatar_ai_1" -> "🤖"
                        "avatar_ai_2" -> "🦾"
                        "avatar_ai_3" -> "🧠"
                        "avatar_online_1" -> "⚡"
                        "avatar_online_2" -> "☄️"
                        "avatar_online_3" -> "🌌"
                        else -> "👤"
                    },
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Goal: ", color = TextGray, fontSize = 9.sp)
                    Text(text = "$score/4", color = color.color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
