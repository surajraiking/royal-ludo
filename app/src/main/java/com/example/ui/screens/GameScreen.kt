package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ui.components.LudoBoardGridComponent
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
    var showBoardStatsDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    // Intercept hardware and gesture back button to ask confirmation
    BackHandler {
        showExitConfirmDialog = true
    }
    
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
                        showExitConfirmDialog = true
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(GlassCard)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit", tint = GoldPrimary)
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

                // Board Tracker & Stats Dialog Trigger
                IconButton(
                    onClick = { showBoardStatsDialog = true },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(GlassCard)
                        .border(1.dp, GlassCardBorder, CircleShape)
                ) {
                    Text("📊", fontSize = 14.sp)
                }
            }

            // 2. TOP PLAYER AVATARS ROW - RED (TOP-LEFT) AND GREEN (TOP-RIGHT)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Player RED (Top-Left Yard)
                val redPlayer = viewModel.players.getOrNull(0)
                if (redPlayer != null) {
                    val isRedTurn = viewModel.currentTurnColor == PlayerColor.RED
                    PlayerWidget(
                        player = redPlayer,
                        isActiveTurn = isRedTurn,
                        color = PlayerColor.RED,
                        score = viewModel.tokens.count { it.color == PlayerColor.RED && it.state == TokenState.GOAL },
                        position = viewModel.getPlayerBoardPosition(PlayerColor.RED),
                        diceValue = if (isRedTurn) viewModel.diceValue else 1,
                        isRolling = isRedTurn && viewModel.rollingAnimActive,
                        hasRolled = isRedTurn && viewModel.hasRolled,
                        isHuman = redPlayer.type == PlayerType.LOCAL_HUMAN,
                        onRollDice = { if (isRedTurn) viewModel.rollDice() },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Player GREEN (Top-Right Yard)
                val greenPlayer = viewModel.players.getOrNull(1)
                if (greenPlayer != null) {
                    val isGreenTurn = viewModel.currentTurnColor == PlayerColor.GREEN
                    PlayerWidget(
                        player = greenPlayer,
                        isActiveTurn = isGreenTurn,
                        color = PlayerColor.GREEN,
                        score = viewModel.tokens.count { it.color == PlayerColor.GREEN && it.state == TokenState.GOAL },
                        position = viewModel.getPlayerBoardPosition(PlayerColor.GREEN),
                        diceValue = if (isGreenTurn) viewModel.diceValue else 1,
                        isRolling = isGreenTurn && viewModel.rollingAnimActive,
                        hasRolled = isGreenTurn && viewModel.hasRolled,
                        isHuman = greenPlayer.type == PlayerType.LOCAL_HUMAN,
                        onRollDice = { if (isGreenTurn) viewModel.rollDice() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Real-Time Turn & Phase HUD Banner
            val turnInfo = viewModel.gameTurnInfo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(GlassCard)
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Round ${turnInfo.roundNumber} • Turn ${turnInfo.turnNumber}",
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${turnInfo.activePlayerName})",
                        color = turnInfo.activeColor.color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                val phaseBadgeColor = when (turnInfo.phase) {
                    TurnPhase.ROLL_DICE -> GoldPrimary
                    TurnPhase.SELECT_PIECE -> NeonCyan
                    TurnPhase.PIECE_MOVING -> LudoYellow
                    TurnPhase.EXTRA_TURN -> LudoGreen
                    TurnPhase.NO_MOVES -> LudoRed
                    TurnPhase.GAME_OVER -> GoldPrimary
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(phaseBadgeColor.copy(alpha = 0.2f))
                        .border(1.dp, phaseBadgeColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = turnInfo.bonusReason ?: turnInfo.phase.title,
                        color = phaseBadgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. MAIN CENTER 3D LUDO BOARD COMPONENT (3D CHESS PIECES & 3D AREA TEXT)
            LudoBoardGridComponent(
                tokens = viewModel.tokens,
                movableTokenIds = viewModel.movableTokenIds,
                currentTurnColor = viewModel.currentTurnColor,
                onTokenClicked = { tok -> viewModel.moveToken(tok) },
                movingTokenId = viewModel.movingTokenId,
                movingTokenColor = viewModel.movingTokenColor,
                center3DText = viewModel.center3DTextHeadline,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 4. BOTTOM PLAYER AVATARS ROW - BLUE (BOTTOM-LEFT) AND YELLOW (BOTTOM-RIGHT)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Player BLUE (Bottom-Left Yard)
                val bluePlayer = viewModel.players.getOrNull(3)
                if (bluePlayer != null) {
                    val isBlueTurn = viewModel.currentTurnColor == PlayerColor.BLUE
                    PlayerWidget(
                        player = bluePlayer,
                        isActiveTurn = isBlueTurn,
                        color = PlayerColor.BLUE,
                        score = viewModel.tokens.count { it.color == PlayerColor.BLUE && it.state == TokenState.GOAL },
                        position = viewModel.getPlayerBoardPosition(PlayerColor.BLUE),
                        diceValue = if (isBlueTurn) viewModel.diceValue else 1,
                        isRolling = isBlueTurn && viewModel.rollingAnimActive,
                        hasRolled = isBlueTurn && viewModel.hasRolled,
                        isHuman = bluePlayer.type == PlayerType.LOCAL_HUMAN,
                        onRollDice = { if (isBlueTurn) viewModel.rollDice() },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Player YELLOW (Bottom-Right Yard)
                val yellowPlayer = viewModel.players.getOrNull(2)
                if (yellowPlayer != null) {
                    val isYellowTurn = viewModel.currentTurnColor == PlayerColor.YELLOW
                    PlayerWidget(
                        player = yellowPlayer,
                        isActiveTurn = isYellowTurn,
                        color = PlayerColor.YELLOW,
                        score = viewModel.tokens.count { it.color == PlayerColor.YELLOW && it.state == TokenState.GOAL },
                        position = viewModel.getPlayerBoardPosition(PlayerColor.YELLOW),
                        diceValue = if (isYellowTurn) viewModel.diceValue else 1,
                        isRolling = isYellowTurn && viewModel.rollingAnimActive,
                        hasRolled = isYellowTurn && viewModel.hasRolled,
                        isHuman = yellowPlayer.type == PlayerType.LOCAL_HUMAN,
                        onRollDice = { if (isYellowTurn) viewModel.rollDice() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 5. INTERACTIVE 3D TURN GUIDANCE & TACTICAL ACTION HUD
            val activePlayer = viewModel.getActivePlayer()
            val isCurrentPlayerHuman = activePlayer?.type == PlayerType.LOCAL_HUMAN
            val activeColor = viewModel.currentTurnColor

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassCard)
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isCurrentPlayerHuman) {
                                if (!viewModel.hasRolled) "🎲 ROLL DICE IN ${activeColor.displayName.uppercase()} CORNER OR TAP BUTTON"
                                else "♟️ TAP YOUR CHESS PIECE ON BOARD TO MOVE"
                            } else {
                                "⏳ ${activePlayer?.name ?: "Opponent"} thinking & rolling dice..."
                            },
                            color = if (isCurrentPlayerHuman) activeColor.color else GoldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (viewModel.hasRolled) "Dice: ${viewModel.diceValue} • Active square-by-square 3D hop enabled"
                            else "Each player's 3D dice is in their corner section",
                            color = TextGray,
                            fontSize = 10.sp
                        )
                    }

                    if (isCurrentPlayerHuman && !viewModel.hasRolled && !viewModel.rollingAnimActive) {
                        Button(
                            onClick = { viewModel.rollDice() },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("🎲 ROLL", color = DeepDarkBg, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }

                // Quick direct action chips for movable tokens (Zero friction Ludo King experience)
                if (isCurrentPlayerHuman && viewModel.hasRolled && viewModel.movableTokenIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tap to move: ", color = TextWhite.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        val movableTokens = viewModel.tokens.filter { it.color == activeColor && viewModel.movableTokenIds.contains(it.id) }
                        movableTokens.forEach { tok ->
                            val chipText = when (tok.state) {
                                TokenState.YARD -> "🚀 Release (#${tok.id + 1})"
                                TokenState.TRACK -> "♟️ Step ${tok.stepCounter} (+${viewModel.diceValue})"
                                TokenState.HOME_STRETCH -> "👑 Home ${tok.stepCounter - 50} (+${viewModel.diceValue})"
                                TokenState.GOAL -> "Goal"
                            }
                            Button(
                                onClick = { viewModel.moveToken(tok) },
                                colors = ButtonDefaults.buttonColors(containerColor = activeColor.color),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(chipText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
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

        // BOARD STATS & PIECE POSITION TRACKER DIALOG
        if (showBoardStatsDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showBoardStatsDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    border = BorderStroke(1.5.dp, GoldPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📊", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "BOARD POSITION & PIECE TRACKER",
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Round ${viewModel.roundNumber} • Total Turns: ${viewModel.turnNumber}",
                                    color = NeonCyan,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Player Positions List
                        viewModel.playerBoardPositions.forEach { pos ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GlassCardBorder)
                                    .border(
                                        1.dp,
                                        if (pos.color == viewModel.currentTurnColor) pos.color.color else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(pos.color.color)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = pos.player.name,
                                                color = TextWhite,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = if (pos.color == viewModel.currentTurnColor) "👉 ACTIVE TURN" else "",
                                            color = pos.color.color,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Piece States: Base, Path, Goal
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "🏠 Base: ${pos.piecesInBase}",
                                            color = TextGray,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "🛣️ Path: ${pos.piecesOnPath}",
                                            color = NeonCyan,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "🏆 Goal: ${pos.piecesInGoal}/4",
                                            color = GoldPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Linear Progress Bar towards victory
                                    LinearProgressIndicator(
                                        progress = { pos.progressPercent },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = pos.color.color,
                                        trackColor = Color.DarkGray
                                    )
                                }
                            }
                        }

                        // Recent Turn History
                        if (viewModel.turnHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Recent Turn Events",
                                color = GoldPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            viewModel.turnHistory.takeLast(4).reversed().forEach { event ->
                                Text(
                                    text = "• $event",
                                    color = TextWhite.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showBoardStatsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CLOSE", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // CONFIRM MATCH EXIT / FORFEIT DIALOG
        if (showExitConfirmDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showExitConfirmDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    border = BorderStroke(1.5.dp, GoldPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚠️", fontSize = 38.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "LEAVE MATCH? / मैच छोड़ें?",
                            color = GoldPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Are you sure you want to exit? If you leave now, the current game session and match progress will be terminated.",
                            color = TextWhite.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showExitConfirmDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = GlassCardBorder),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("CANCEL", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    showExitConfirmDialog = false
                                    viewModel.exitGame()
                                    onNavigateBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LudoRed),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("EXIT GAME", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3D INTERACTIVE EVENT CELEBRATION POPUP (LUCKY 6, PIECE OPENED, ENEMY CAPTURE)
        val activeEffect = viewModel.active3DEffect
        if (activeEffect != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xF2160E27)),
                    border = BorderStroke(2.5.dp, Brush.linearGradient(listOf(GoldPrimary, activeEffect.color.color, GoldPrimary))),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .shadow(24.dp, RoundedCornerShape(22.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (activeEffect.type) {
                                InteractiveEffectType.ROLLED_SIX -> "🎲"
                                InteractiveEffectType.TOKEN_UNLOCKED -> "🚀"
                                InteractiveEffectType.CAPTURE -> "⚔️"
                                InteractiveEffectType.GOAL_SCORED -> "👑"
                                InteractiveEffectType.EXTRA_TURN -> "✨"
                            },
                            fontSize = 52.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activeEffect.headline,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 1.2.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = activeEffect.detail,
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
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
    score: Int,
    position: PlayerBoardPosition? = null,
    diceValue: Int = 1,
    isRolling: Boolean = false,
    hasRolled: Boolean = false,
    isHuman: Boolean = false,
    onRollDice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "active_player")
    val outlineGlow by transition.animateColor(
        initialValue = color.color,
        targetValue = color.color.copy(alpha = 0.25f),
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val dicePulseScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dice_pulse"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        border = BorderStroke(
            if (isActiveTurn) 2.dp else 1.dp,
            if (isActiveTurn) outlineGlow else GlassCardBorder
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .shadow(if (isActiveTurn) 10.dp else 0.dp, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile symbol with 3D ring
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.color.copy(alpha = 0.25f))
                    .border(2.dp, if (isActiveTurn) color.color else GlassCardBorder, CircleShape),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = player.name,
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isActiveTurn) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(color.color.copy(alpha = 0.3f))
                                .border(0.8.dp, color.color, RoundedCornerShape(4.dp))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text("TURN", color = color.color, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (position != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🏠${position.piecesInBase}", color = TextGray, fontSize = 9.sp)
                        Text(text = "🛣️${position.piecesOnPath}", color = NeonCyan, fontSize = 9.sp)
                        Text(text = "🏆${position.piecesInGoal}", color = GoldPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Goal: ", color = TextGray, fontSize = 9.sp)
                        Text(text = "$score/4", color = color.color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // DEDICATED 3D PLAYER DICE IN ACTIVE PLAYER'S PLAY SECTION ONLY
            if (isActiveTurn) {
                val canClickToRoll = isHuman && !hasRolled && !isRolling
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .scale(if (canClickToRoll) dicePulseScale else 1f)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    color.color.copy(alpha = 0.9f),
                                    Color(0xFF241A32),
                                    color.color.copy(alpha = 0.65f)
                                )
                            )
                        )
                        .border(
                            2.dp,
                            color.color,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = canClickToRoll) {
                            onRollDice()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isRolling) {
                        Text(
                            text = listOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅").random(),
                            color = Color.White,
                            fontSize = 28.sp
                        )
                    } else if (hasRolled) {
                        Text(
                            text = when (diceValue) {
                                1 -> "⚀"
                                2 -> "⚁"
                                3 -> "⚂"
                                4 -> "⚃"
                                5 -> "⚄"
                                6 -> "⚅"
                                else -> "⚀"
                            },
                            color = if (diceValue == 6) GoldPrimary else Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )
                    } else {
                        // Awaiting Roll prompt
                        if (canClickToRoll) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎲", fontSize = 16.sp)
                                Text("ROLL", color = GoldPrimary, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        } else {
                            Text("🎲", fontSize = 22.sp)
                        }
                    }
                }
            } else {
                // Inactive player: no dice shown (strictly only the active player shows dice)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(0.8.dp, GlassCardBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⏳",
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
