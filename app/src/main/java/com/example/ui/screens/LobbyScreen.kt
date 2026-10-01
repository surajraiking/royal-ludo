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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    viewModel: LudoViewModel,
    onNavigateToGame: () -> Unit,
    onNavigateToSpin: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToStore: () -> Unit
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showApkGuideDialog by remember { mutableStateOf(false) }
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

            val skins = listOf("Neon Gold", "Epic Inferno", "Deep Crystal", "Cosmic Diamond")
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
            
            Spacer(modifier = Modifier.height(40.dp))
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

                        Spacer(modifier = Modifier.height(24.dp))

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
    }
}
