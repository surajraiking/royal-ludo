package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.ui.theme.*
import com.example.viewmodel.LudoViewModel

data class StoreSkinItem(
    val name: String,
    val costCoins: Int,
    val costGems: Int,
    val description: String,
    val designBrush: Brush
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    viewModel: LudoViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val skinsForSale = listOf(
        StoreSkinItem(
            name = "Epic Inferno",
            costCoins = 800,
            costGems = 15,
            description = "Emanates volcanic flames upon roll of 6.",
            designBrush = Brush.radialGradient(colors = listOf(LudoRed, Color(0xFF6B0014)))
        ),
        StoreSkinItem(
            name = "Deep Crystal",
            costCoins = 1500,
            costGems = 25,
            description = "Pristine ice facets tracking maximum fair spins.",
            designBrush = Brush.radialGradient(colors = listOf(NeonCyan, LudoBlue))
        ),
        StoreSkinItem(
            name = "Cosmic Diamond",
            costCoins = 2500,
            costGems = 50,
            description = "Ultra high durability crystal aura.",
            designBrush = Brush.radialGradient(colors = listOf(Color.White, Color.Gray))
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IMPERIAL PALACE MALL", color = GoldPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
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
                .padding(16.dp)
        ) {
            // Balance row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassCard)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("YOUR TREASURY WALLET:", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row {
                    Text(text = "🪙 ${viewModel.userCoins}", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "💎 ${viewModel.userGems}", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "BOOST WALLET (SIMULATED MONETIZATION)",
                color = TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Simulated Buy buttons (instantly add coins)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Free Coin chest
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    border = BorderStroke(1.dp, GoldPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.rollDailyReward() // Awards coins
                            feedbackMessage = "🪙 Vault Refilled! +🪙 coins loaded successfully!"
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎁 CHEST OF GOLD", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Claim +500 Coins", color = TextWhite, fontSize = 10.sp)
                        Text("FREE BOOST", color = LudoGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Free Gem chest
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    border = BorderStroke(1.dp, NeonCyan),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.rollDailyRewardGems() // Awards gems
                            feedbackMessage = "💎 Diamonds Credited! +💎 gems loaded successfully!"
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("💎 VIP DIAMOND BAG", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Claim +10 Gems", color = TextWhite, fontSize = 10.sp)
                        Text("FREE BOOST", color = LudoGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "VIP UNLOCKABLE DICE SKINS",
                color = TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Grid list of skins
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(skinsForSale) { skin ->
                    val isUnlocked = viewModel.unlockedSkins.contains(skin.name)
                    val isSelected = viewModel.selectedDiceSkin == skin.name

                    Card(
                        colors = CardDefaults.cardColors(containerColor = GlassCard),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) GoldPrimary else GlassCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Mini Skin Canvas visual representation
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(skin.designBrush),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚅",
                                    color = if (skin.name == "Cosmic Diamond") Color.Blue else Color.White,
                                    fontSize = 32.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = skin.name,
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = skin.description,
                                color = TextGray,
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                modifier = Modifier.height(30.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isUnlocked) {
                                Button(
                                    onClick = { viewModel.selectSkin(skin.name) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) GoldPrimary else GlassCardBorder
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isSelected) "EQUIPPED" else "EQUIP",
                                        color = if (isSelected) DeepDarkBg else GoldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val ok = viewModel.purchaseSkin(skin.name, skin.costCoins, skin.costGems)
                                        feedbackMessage = if (ok) {
                                            "🎉 Skin ${skin.name} unlocked and equipped!"
                                        } else {
                                            "❌ Insufficient Treasures! Claim some rewards first."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "🪙 ${skin.costCoins} + 💎 ${skin.costGems}",
                                        color = DeepDarkBg,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Global feedback snack-bar container placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                if (feedbackMessage != null) {
                    LaunchedEffect(feedbackMessage) {
                        delay(2500)
                        feedbackMessage = null
                    }
                    Text(
                        text = feedbackMessage!!,
                        color = GoldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GlassCard)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
