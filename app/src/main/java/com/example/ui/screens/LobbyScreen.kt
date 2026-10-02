package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.PlayerType
import com.example.ui.theme.*
import com.example.viewmodel.LudoViewModel
import androidx.credentials.CredentialManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    viewModel: LudoViewModel,
    onNavigateToGame: () -> Unit,
    onNavigateToSpin: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToStore: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showApkGuideDialog by remember { mutableStateOf(false) }
    var showCreatorDialog by remember { mutableStateOf(false) }
    var showFriendsDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(viewModel.username) }
    var tempAvatar by remember { mutableStateOf(viewModel.avatarId) }

    val avatars = listOf("👑", "🦁", "🏎️", "🐉", "🦄", "👽")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            
            // 1. TOP HEADER STATUS BAR (Profile, Coins, Gems)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassCard)
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(16.dp))
                    .clickable {
                        tempName = viewModel.username
                        tempAvatar = viewModel.avatarId
                        showEditProfileDialog = true
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(GlassCardBorder)
                        .border(1.5.dp, GoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        // Resolve emoji or char based on id
                        text = when (viewModel.avatarId) {
                            "avatar_crown" -> "👑"
                            "avatar_lion" -> "🦁"
                            "avatar_car" -> "🏎️"
                            "avatar_dragon" -> "🐉"
                            "avatar_unicorn" -> "🦄"
                            "avatar_alien" -> "👽"
                            else -> "👑"
                        },
                        fontSize = 28.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = viewModel.username,
                        color = GoldPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "🏆 Win rate: ${if (viewModel.matchesPlayed == 0) "0%" else "${(viewModel.matchesWon * 100) / viewModel.matchesPlayed}%"} (${viewModel.matchesWon}/${viewModel.matchesPlayed})",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }

                // Balance Caps
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Coins Display
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassCardBorder)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🪙", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = viewModel.userCoins.toString(),
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Gems Display
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassCardBorder)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💎", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = viewModel.userGems.toString(),
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. MOTIF / BRAND BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF2C1600), Color(0xFF130932))
                        )
                    )
                    .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Background radial circles
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = GoldPrimary.copy(alpha = 0.08f),
                        radius = size.width / 2.5f,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ROYAL LUDO",
                        color = GoldPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "MULTIPLAYER CHampionship",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 4.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. GAME MODES SELECTORS
            Text(
                text = "SELECT BATTLE MODE",
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Mode: PLAY WITH FRIENDS (PRIVATE ROOM / PASS & PLAY)
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(LudoGreen, GoldPrimary))),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clickable { showFriendsDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(LudoGreen.copy(alpha = 0.2f))
                            .border(1.dp, LudoGreen, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👥", fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Play with Friends",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(LudoGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("FRIENDS", color = LudoGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = "Private Room Code (कमरा बनाएं/जुड़ें) & Custom Pass-N-Play",
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    }
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play with Friends", tint = LudoGreen)
                }
            }

            // Mode 1: ONLINE WORLD CUP MATCHMAKING
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clickable {
                            viewModel.startMatchmaking()
                            onNavigateToGame()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Online",
                            tint = NeonCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Global Arena Match",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Simulated Online matchmaking and live emoji chat.",
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    }
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = GoldPrimary)
                }
            }

            // Mode 2: VS KINGS AI
            var showAiDifficultySelector by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GlassCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .clickable { showAiDifficultySelector = !showAiDifficultySelector }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GoldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Computer AI",
                                tint = GoldPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vs Kings AI Battle",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Test tactics against Easy / Medium / Hard AI bots.",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = if (showAiDifficultySelector) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = TextGray
                        )
                    }

                    AnimatedVisibility(visible = showAiDifficultySelector) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlassCardBorder.copy(alpha = 0.5f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(
                                Pair(PlayerType.AI_EASY, "Easy"),
                                Pair(PlayerType.AI_MEDIUM, "Medium"),
                                Pair(PlayerType.AI_HARD, "Hard")
                            ).forEach { (type, label) ->
                                Button(
                                    onClick = {
                                        viewModel.startAiGame(type)
                                        onNavigateToGame()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (viewModel.aiDifficulty == type) GoldPrimary else GlassCard
                                    ),
                                    border = BorderStroke(1.dp, GoldPrimary),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (viewModel.aiDifficulty == type) DeepDarkBg else GoldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Mode 3: LOCAL MULTIPLAYER (PASS AND PLAY)
            var showLocalModeSelector by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GlassCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .clickable { showLocalModeSelector = !showLocalModeSelector }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(LudoRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Local Game",
                                tint = LudoRed,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Local Imperial Pass & Play",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Play locally with 2, 3 or 4 physical friends.",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = if (showLocalModeSelector) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = TextGray
                        )
                    }

                    AnimatedVisibility(visible = showLocalModeSelector) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlassCardBorder.copy(alpha = 0.5f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(2, 3, 4).forEach { count ->
                                Button(
                                    onClick = {
                                        viewModel.startLocalGame(count)
                                        onNavigateToGame()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GlassCardBorder),
                                    border = BorderStroke(1.dp, LudoRed),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "$count Players",
                                        color = LudoRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. DICE SKINS CAROUSEL
            Text(
                text = "EQUIP YOUR DICE SKIN",
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val skins = listOf("Neon Gold", "Epic Inferno", "Deep Crystal", "Cosmic Diamond", "Royal Emerald", "Obsidian Shadow", "Solar Flare")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(skins) { skin ->
                    val isUnlocked = viewModel.unlockedSkins.contains(skin)
                    val isSelected = viewModel.selectedDiceSkin == skin

                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassCard)
                            .border(
                                1.5.dp,
                                if (isSelected) GoldPrimary else GlassCardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (isUnlocked) {
                                    viewModel.selectSkin(skin)
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Mini dice simulation visual
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when (skin) {
                                            "Neon Gold" -> Brush.radialGradient(colors = listOf(GoldPrimary, GoldDark))
                                            "Epic Inferno" -> Brush.radialGradient(colors = listOf(LudoRed, Color(0xFF630018)))
                                            "Deep Crystal" -> Brush.radialGradient(colors = listOf(NeonCyan, LudoBlue))
                                            "Royal Emerald" -> Brush.radialGradient(colors = listOf(LudoGreen, Color(0xFF00382B)))
                                            "Obsidian Shadow" -> Brush.radialGradient(colors = listOf(Color(0xFF4A3E5C), Color(0xFF15101F)))
                                            "Solar Flare" -> Brush.radialGradient(colors = listOf(Color(0xFFFF9800), Color(0xFFB23A00)))
                                            else -> Brush.radialGradient(colors = listOf(Color.White, Color.Gray))
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚄",
                                    color = if (skin == "Cosmic Diamond") Color.Blue else Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = skin,
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )

                            Text(
                                text = if (isSelected) "EQUIPPED" else if (isUnlocked) "READY" else "LOCKED",
                                color = if (isSelected) GoldPrimary else if (isUnlocked) NeonCyan else Color.Red,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 5. BUTTON HIGHLIGHTS (Leaderboard, Spin Wheel, Store)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Spin Wheel Button
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToSpin() }
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎡", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Lucky Wheel", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Leaderboard Button
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToLeaderboard() }
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏆", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Leaderboard", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Store Button
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    border = BorderStroke(1.dp, LudoGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToStore() }
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏪", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Imperial Mall", color = LudoGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // APK & GITHUB EXPORT / DOWNLOAD CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showApkGuideDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GoldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📦", fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Download APK / Build AAB",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "APK compiled & ready • GitHub auto-build included",
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "APK Ready",
                        tint = GoldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 👑 CREATOR SHOWCASE CARD - SURAJ RAI (@surajraiking22)
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(GoldPrimary, NeonCyan))),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCreatorDialog = true }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(GoldPrimary, Color(0xFFB8860B))))
                                .border(2.dp, NeonCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👑", fontSize = 26.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Suraj Rai",
                                    color = GoldPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonCyan.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("LEAD CREATOR", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "@surajraiking",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Creator",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Royal Ludo Empire Architect • Connect & follow on social media:",
                        color = TextWhite.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Social Media Handle Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Instagram
                        SocialChip(
                            icon = "📸",
                            label = "Insta",
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/surajraiking")))
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Facebook
                        SocialChip(
                            icon = "📘",
                            label = "Facebook",
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com/surajraiking21")))
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // YouTube
                        SocialChip(
                            icon = "▶️",
                            label = "YouTube",
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/sanatanmythologytales")))
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Email
                        SocialChip(
                            icon = "✉️",
                            label = "Email",
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("mailto:surajraiking22@gmail.com")))
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }

        // CREATOR SHOWCASE DIALOG
        if (showCreatorDialog) {
            Dialog(onDismissRequest = { showCreatorDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    border = BorderStroke(2.dp, Brush.linearGradient(listOf(GoldPrimary, NeonCyan))),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(22.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(GoldPrimary, Color(0xFF8B6508))))
                                .border(2.5.dp, NeonCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👑", fontSize = 38.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Suraj Rai",
                            color = GoldPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "@surajraiking",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Lead Creator & Visionary behind Royal Ludo Empire. Designed with AAA visuals, real-time multiplayer, and intelligent AI.",
                            color = TextWhite.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Connect Links
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CreatorLinkButton(
                                title = "Instagram: @surajraiking",
                                subtitle = "Follow updates & announcements",
                                icon = "📸",
                                color = Color(0xFFE1306C),
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/surajraiking")))
                                    } catch (e: Exception) {}
                                }
                            )

                            CreatorLinkButton(
                                title = "Facebook: @surajraiking21",
                                subtitle = "Official Facebook profile & community",
                                icon = "📘",
                                color = Color(0xFF1877F2),
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com/surajraiking21")))
                                    } catch (e: Exception) {}
                                }
                            )

                            CreatorLinkButton(
                                title = "YouTube: Sanatan Mythology Tales",
                                subtitle = "Epic mythology episodes & stories",
                                icon = "▶️",
                                color = Color(0xFFFF0000),
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/sanatanmythologytales")))
                                    } catch (e: Exception) {}
                                }
                            )

                            CreatorLinkButton(
                                title = "Email: surajraiking22@gmail.com",
                                subtitle = "Business inquiries & collaborations",
                                icon = "✉️",
                                color = LudoGreen,
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("mailto:surajraiking22@gmail.com")))
                                    } catch (e: Exception) {}
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { showCreatorDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CLOSE", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // APK & GITHUB GUIDE DIALOG
        if (showApkGuideDialog) {
            Dialog(onDismissRequest = { showApkGuideDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    border = BorderStroke(1.5.dp, GoldPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📦", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "APK & AAB DOWNLOAD GUIDE",
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LudoGreen.copy(alpha = 0.2f))
                                .border(1.dp, LudoGreen, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "✅ Debug APK built successfully:\napp/build/outputs/apk/debug/app-debug.apk",
                                color = LudoGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "1. AI Studio Se Direct Download:",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Top-right menu (⚙️ / ⋮) par click karke 'Download APK' ya 'Export ZIP' select karein.",
                            color = TextWhite,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "2. GitHub Se Auto APK & AAB Builder:",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Is project me .github/workflows/build.yml configured hai. GitHub par push karte hi GitHub Actions automatically ready-to-install APK aur Play Store AAB bana deta hai! (GitHub -> Actions tab -> Artifacts)",
                            color = TextWhite,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "3. Mobile / PC Command:",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• gradle assembleDebug (APK)\n• gradle bundleRelease (AAB for Play Store)",
                            color = GoldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { showApkGuideDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("GOT IT / समझ आ गया", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // EDIT PROFILE DIALOG
        if (showEditProfileDialog) {
            Dialog(onDismissRequest = { showEditProfileDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    border = BorderStroke(1.5.dp, GoldPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "EDIT PALACE CREDENTIALS",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Avatar Picker Label
                        Text("Choose Avatar", color = TextWhite, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                Pair("avatar_crown", "👑"),
                                Pair("avatar_lion", "🦁"),
                                Pair("avatar_car", "🏎️"),
                                Pair("avatar_dragon", "🐉"),
                                Pair("avatar_unicorn", "🦄"),
                                Pair("avatar_alien", "👽")
                            ).forEach { (id, avatarEmoji) ->
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (tempAvatar == id) GoldPrimary else GlassCardBorder)
                                        .clickable { tempAvatar = id }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(avatarEmoji, fontSize = 18.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text("Enter Warrior Name", color = TextWhite, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { if (it.length <= 15) tempName = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GlassCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            placeholder = { Text("Warrior Name", color = TextGray) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Google Account Cloud Info
                        val googleUser = Firebase.auth.currentUser
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(GlassCardBorder)
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("☁️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cloud Account Synced",
                                        color = NeonCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = googleUser?.email ?: "Google Account",
                                        color = TextWhite.copy(alpha = 0.8f),
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        signOutGoogle(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onSignOutComplete = {
                                                showEditProfileDialog = false
                                            },
                                            scope = coroutineScope
                                        )
                                    }
                                ) {
                                    Text("Sign Out", color = LudoRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { showEditProfileDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = GlassCardBorder)
                            ) {
                                Text("CANCEL", color = TextWhite)
                            }

                            Button(
                                onClick = {
                                    if (tempName.isNotBlank()) {
                                        viewModel.updateProfile(tempName, tempAvatar)
                                        showEditProfileDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                            ) {
                                Text("SAVE", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 👥 PLAY WITH FRIENDS DIALOG (ROOM CODE & PASS-N-PLAY)
        if (showFriendsDialog) {
            Dialog(onDismissRequest = { showFriendsDialog = false }) {
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                var friendTab by remember { mutableStateOf(0) } // 0: Room Code, 1: Pass & Play
                var isCreateRoomMode by remember { mutableStateOf(true) }
                var generatedRoomCode by remember { mutableStateOf("ROYAL-" + (1000..9999).random()) }
                var enteredRoomCode by remember { mutableStateOf("") }
                var codeCopiedToast by remember { mutableStateOf(false) }

                // Pass & play state
                var playerCount by remember { mutableStateOf(4) }
                var p1Name by remember { mutableStateOf(viewModel.username) }
                var p2Name by remember { mutableStateOf("Friend 2") }
                var p3Name by remember { mutableStateOf("Friend 3") }
                var p4Name by remember { mutableStateOf("Friend 4") }

                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(LudoGreen, GoldPrimary))),
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
                            Text("👥", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "PLAY WITH FRIENDS",
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "दोस्तों के साथ खेलें (Room Code / Pass & Play)",
                                    color = NeonCyan,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tab Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(GlassCardBorder)
                                .padding(3.dp)
                        ) {
                            listOf("Room Code 🔑", "Pass & Play 📱").forEachIndexed { idx, label ->
                                val isSelected = friendTab == idx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) GoldPrimary else Color.Transparent)
                                        .clickable { friendTab = idx }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) DeepDarkBg else TextWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (friendTab == 0) {
                            // ROOM CODE TAB
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TextButton(
                                    onClick = { isCreateRoomMode = true }
                                ) {
                                    Text(
                                        "Create Room",
                                        color = if (isCreateRoomMode) GoldPrimary else TextGray,
                                        fontWeight = if (isCreateRoomMode) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(" | ", color = GlassCardBorder, modifier = Modifier.align(Alignment.CenterVertically))
                                TextButton(
                                    onClick = { isCreateRoomMode = false }
                                ) {
                                    Text(
                                        "Join Room",
                                        color = if (!isCreateRoomMode) GoldPrimary else TextGray,
                                        fontWeight = if (!isCreateRoomMode) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isCreateRoomMode) {
                                Text(
                                    text = "Share this Room Code with your friends:",
                                    color = TextWhite.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Glowing Room Code Display
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DeepDarkBg)
                                        .border(1.5.dp, NeonCyan, RoundedCornerShape(12.dp))
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = generatedRoomCode,
                                        color = GoldPrimary,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 3.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(generatedRoomCode))
                                            codeCopiedToast = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                        border = BorderStroke(1.dp, NeonCyan)
                                    ) {
                                        Text(if (codeCopiedToast) "COPIED! ✅" else "📋 COPY CODE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            try {
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, "Let's battle in Royal Ludo Empire! 👑 Join my Private Room with Code: $generatedRoomCode\nDownload now and enter code in 'Play with Friends'!")
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, "Share Room Code"))
                                            } catch (e: Exception) {}
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                                    ) {
                                        Text("📲 SHARE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        showFriendsDialog = false
                                        viewModel.startPrivateRoomMatch(generatedRoomCode)
                                        onNavigateToGame()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                                ) {
                                    Text("START PRIVATE MATCH", color = DeepDarkBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            } else {
                                // Join Room Mode
                                Text(
                                    text = "Enter 6-digit Room Code given by friend:",
                                    color = TextWhite.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = enteredRoomCode,
                                    onValueChange = { enteredRoomCode = it.uppercase() },
                                    label = { Text("Room Code (e.g. ROYAL-1234)", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = GlassCardBorder,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedLabelColor = NeonCyan,
                                        unfocusedLabelColor = TextGray
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (enteredRoomCode.isNotBlank()) {
                                            showFriendsDialog = false
                                            viewModel.startPrivateRoomMatch(enteredRoomCode)
                                            onNavigateToGame()
                                        }
                                    },
                                    enabled = enteredRoomCode.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                ) {
                                    Text("JOIN ROOM & PLAY", color = DeepDarkBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        } else {
                            // PASS & PLAY TAB
                            Text(
                                text = "Select Number of Players on this device:",
                                color = TextWhite.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(2, 3, 4).forEach { count ->
                                    val isSelected = playerCount == count
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) GoldPrimary else GlassCardBorder)
                                            .border(1.dp, if (isSelected) GoldPrimary else GlassCardBorder, RoundedCornerShape(8.dp))
                                            .clickable { playerCount = count }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$count Players",
                                            color = if (isSelected) DeepDarkBg else TextWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Enter Names for each Friend:",
                                color = TextWhite.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Name Inputs
                            OutlinedTextField(
                                value = p1Name,
                                onValueChange = { if (it.length <= 15) p1Name = it },
                                label = { Text("🔴 Player 1 (Red)", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LudoRed,
                                    unfocusedBorderColor = GlassCardBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedLabelColor = LudoRed,
                                    unfocusedLabelColor = TextGray
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = p2Name,
                                onValueChange = { if (it.length <= 15) p2Name = it },
                                label = { Text("🟢 Player 2 (Green)", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LudoGreen,
                                    unfocusedBorderColor = GlassCardBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedLabelColor = LudoGreen,
                                    unfocusedLabelColor = TextGray
                                )
                            )

                            if (playerCount >= 3) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = p3Name,
                                    onValueChange = { if (it.length <= 15) p3Name = it },
                                    label = { Text("🟡 Player 3 (Yellow)", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LudoYellow,
                                        unfocusedBorderColor = GlassCardBorder,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedLabelColor = LudoYellow,
                                        unfocusedLabelColor = TextGray
                                    )
                                )
                            }

                            if (playerCount >= 4) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = p4Name,
                                    onValueChange = { if (it.length <= 15) p4Name = it },
                                    label = { Text("🔵 Player 4 (Blue)", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LudoBlueVibrant,
                                        unfocusedBorderColor = GlassCardBorder,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedLabelColor = LudoBlueVibrant,
                                        unfocusedLabelColor = TextGray
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val names = when (playerCount) {
                                        2 -> listOf(p1Name, p2Name)
                                        3 -> listOf(p1Name, p2Name, p3Name)
                                        else -> listOf(p1Name, p2Name, p3Name, p4Name)
                                    }
                                    showFriendsDialog = false
                                    viewModel.startFriendsCustomGame(names)
                                    onNavigateToGame()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = LudoGreen)
                            ) {
                                Text("START PASS & PLAY BATTLE", color = DeepDarkBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(onClick = { showFriendsDialog = false }) {
                            Text("CLOSE", color = TextGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SocialChip(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GlassCardBorder)
            .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = TextWhite,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CreatorLinkButton(
    title: String,
    subtitle: String,
    icon: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GlassCardBorder),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = TextGray,
                    fontSize = 10.sp
                )
            }
            Text("➔", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

