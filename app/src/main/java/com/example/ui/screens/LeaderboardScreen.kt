package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.LudoViewModel

data class LeaderboardEntry(
    val name: String,
    val wins: Int,
    val rate: String,
    val coins: Int,
    val avatar: String,
    val isUser: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LudoViewModel,
    onNavigateBack: () -> Unit
) {
    val firestoreUsers by viewModel.leaderboardUsers.collectAsState()

    val leaders = if (firestoreUsers.isNotEmpty()) {
        firestoreUsers.map { user ->
            LeaderboardEntry(
                name = if (user.userId == viewModel.firebaseRepo.currentUserId) "${user.username} (You)" else user.username,
                wins = user.matchesWon,
                rate = if (user.matchesPlayed == 0) "0%" else "${(user.matchesWon * 100) / user.matchesPlayed}%",
                coins = user.coins,
                avatar = when (user.avatarId) {
                    "avatar_crown" -> "👑"
                    "avatar_lion" -> "🦁"
                    "avatar_car" -> "🏎️"
                    "avatar_dragon" -> "🐉"
                    "avatar_unicorn" -> "🦄"
                    "avatar_alien" -> "👽"
                    else -> "👑"
                },
                isUser = user.userId == viewModel.firebaseRepo.currentUserId
            )
        }.sortedByDescending { it.coins }
    } else {
        listOf(
            LeaderboardEntry("GoldLudoLord", 125, "74%", 42000, "👑"),
            LeaderboardEntry("NeonQueen_99", 108, "68%", 31500, "🦁"),
            LeaderboardEntry("SlayerSovereign", 92, "62%", 24000, "🐉"),
            LeaderboardEntry("${viewModel.username} (You)", viewModel.matchesWon, if (viewModel.matchesPlayed == 0) "0%" else "${(viewModel.matchesWon * 100) / viewModel.matchesPlayed}%", viewModel.userCoins, "👑", isUser = true),
            LeaderboardEntry("CyberWarrior", 54, "58%", 15400, "🏎️"),
            LeaderboardEntry("ShadowFighter", 48, "55%", 12000, "🐉"),
            LeaderboardEntry("LudoUnicorn", 39, "51%", 9800, "🦄"),
            LeaderboardEntry("StarSeeker", 22, "45%", 4500, "👽")
        ).sortedByDescending { it.wins }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IMPERIAL GLOBAL SCORES", color = GoldPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DeepDarkBg)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // TOP PODIUM (Top 3 Visualizers)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Pos 2 Silver
                val pos2 = leaders.getOrNull(1)
                if (pos2 != null) {
                    PodiumWidget(pos2, 2, SilverColor, 95.dp, "🥈")
                }

                // Pos 1 Gold
                val pos1 = leaders.getOrNull(0)
                if (pos1 != null) {
                    PodiumWidget(pos1, 1, GoldPrimary, 125.dp, "👑")
                }

                // Pos 3 Bronze
                val pos3 = leaders.getOrNull(2)
                if (pos3 != null) {
                    PodiumWidget(pos3, 3, BronzeColor, 84.dp, "🥉")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "GLOBAL LEADERBOARD STANDINGS",
                color = TextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // ROSTER LIST
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(leaders) { idx, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (entry.isUser) GlassCardBorder else GlassCard)
                            .border(
                                1.dp,
                                if (entry.isUser) GoldPrimary else GlassCardBorder.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${idx + 1}",
                            color = when (idx) {
                                0 -> GoldPrimary
                                1 -> SilverColor
                                2 -> BronzeColor
                                else -> TextGray
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.width(36.dp)
                        )

                        // Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GlassCardBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = entry.avatar, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.name,
                                color = if (entry.isUser) GoldPrimary else TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Win Rate: ${entry.rate}",
                                color = TextGray,
                                fontSize = 10.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${entry.wins} Wins",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "🪙 ${entry.coins}",
                                color = GoldPrimary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private val SilverColor = Color(0xFFC0C0C0)
private val BronzeColor = Color(0xFFCD7F32)

@Composable
fun PodiumWidget(
    entry: LeaderboardEntry,
    rank: Int,
    rankColor: Color,
    height: androidx.compose.ui.unit.Dp,
    badge: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(text = badge, fontSize = 24.sp)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(GlassCard)
                .border(1.5.dp, rankColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = entry.avatar, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = entry.name,
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = "${entry.wins} Wins",
            color = rankColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Visual Podium Block
        Box(
            modifier = Modifier
                .width(66.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(GlassCard)
                .border(
                    1.dp,
                    rankColor.copy(alpha = 0.5f),
                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                color = rankColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )
        }
    }
}
