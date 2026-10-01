package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.repository.UserPreferences
import com.example.ui.theme.LudoRed
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoYellow
import com.example.ui.theme.LudoBlueVibrant
import com.example.ui.theme.GoldPrimary
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class LudoViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = UserPreferences(application)

    // User Currency & Customizations
    var userCoins by mutableStateOf(prefs.coins)
        private set
    var userGems by mutableStateOf(prefs.gems)
        private set
    var selectedDiceSkin by mutableStateOf(prefs.selectedDiceSkin)
        private set
    var unlockedSkins by mutableStateOf(prefs.unlockedSkins)
        private set
    var username by mutableStateOf(prefs.username)
        private set
    var avatarId by mutableStateOf(prefs.avatarId)
        private set
    var matchesPlayed by mutableStateOf(prefs.matchesPlayed)
        private set
    var matchesWon by mutableStateOf(prefs.matchesWon)
        private set

    // Game core states
    var gameState by mutableStateOf(GameState.IDLE)
        private set
    var lobbyMode by mutableStateOf(PlayerType.LOCAL_HUMAN) // LOCAL_HUMAN / AI_MEDIUM / ONLINE_SIMULATED
    var aiDifficulty by mutableStateOf(PlayerType.AI_MEDIUM)

    var players = mutableStateListOf<LudoPlayer>()
    var tokens = mutableStateListOf<LudoToken>()
    var currentTurnColor by mutableStateOf(PlayerColor.RED)
        private set
    var diceValue by mutableStateOf(1)
        private set
    var hasRolled by mutableStateOf(false)
        private set
    var movableTokenIds = mutableStateListOf<Int>()
    var winner by mutableStateOf<LudoPlayer?>(null)
        private set

    // Live indicators
    var rollingAnimActive by mutableStateOf(false)
        private set
    var chatMessages = mutableStateListOf<ChatMessage>()
    var particleList = mutableStateListOf<Particle>()
    var isVoiceChatActive by mutableStateOf(false)
        private set
    var showWinningCelebration by mutableStateOf(false)
    var screenShakeActive by mutableStateOf(false)

    // Matchmaking simulator visual variables
    var matchmakingStatusText by mutableStateOf("Initializing Imperial Servers...")
    var matchmakerFoundCount by mutableStateOf(1) // User is first

    init {
        resetGameBoard()
    }

    fun updateProfile(name: String, avId: String) {
        prefs.username = name
        prefs.avatarId = avId
        username = name
        avatarId = avId
    }

    fun rollDailyReward(): Int {
        val earned = listOf(100, 250, 500, 1000, 50, 150).random()
        prefs.earnCoins(earned)
        userCoins = prefs.coins
        prefs.lastSpinTimestamp = System.currentTimeMillis()
        return earned
    }

    fun rollDailyRewardGems(): Int {
        val earned = listOf(5, 10, 15, 20).random()
        prefs.earnGems(earned)
        userGems = prefs.gems
        prefs.lastSpinTimestamp = System.currentTimeMillis()
        return earned
    }

    fun hasSpunDaily(): Boolean {
        val last = prefs.lastSpinTimestamp
        val oneday = 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - last) < oneday
    }

    fun purchaseSkin(skin: String, goldCost: Int, gemsCost: Int): Boolean {
        val success = prefs.unlockSkin(skin, goldCost, gemsCost)
        if (success) {
            userCoins = prefs.coins
            userGems = prefs.gems
            unlockedSkins = prefs.unlockedSkins
            selectSkin(skin)
        }
        return success
    }

    fun selectSkin(skin: String) {
        if (unlockedSkins.contains(skin)) {
            prefs.selectedDiceSkin = skin
            selectedDiceSkin = skin
        }
    }

    fun resetGameBoard() {
        tokens.clear()
        PlayerColor.values().forEach { col ->
            for (id in 0..3) {
                tokens.add(LudoToken(id = id, color = col))
            }
        }
        players.clear()
        winner = null
        hasRolled = false
        movableTokenIds.clear()
        chatMessages.clear()
        showWinningCelebration = false
    }

    fun startLocalGame(humanCount: Int) {
        resetGameBoard()
        lobbyMode = PlayerType.LOCAL_HUMAN

        val colors = PlayerColor.values()
        for (i in 0 until 4) {
            val pType = if (i < humanCount) PlayerType.LOCAL_HUMAN else PlayerType.AI_MEDIUM
            players.add(
                LudoPlayer(
                    color = colors[i],
                    name = if (i == 0) "$username (You)" else "Ludo Warlord ${i + 1}",
                    type = pType,
                    avatarId = "avatar_${i + 1}"
                )
            )
        }
        currentTurnColor = PlayerColor.RED
        gameState = GameState.PLAYING
    }

    fun startAiGame(diffType: PlayerType) {
        resetGameBoard()
        lobbyMode = diffType
        aiDifficulty = diffType

        players.add(LudoPlayer(PlayerColor.RED, "$username (You)", PlayerType.LOCAL_HUMAN, avatarId))
        players.add(LudoPlayer(PlayerColor.GREEN, "C-3PO AI", diffType, "avatar_ai_1"))
        players.add(LudoPlayer(PlayerColor.YELLOW, "T-800 AI", diffType, "avatar_ai_2"))
        players.add(LudoPlayer(PlayerColor.BLUE, "HAL 9000 AI", diffType, "avatar_ai_3"))

        currentTurnColor = PlayerColor.RED
        gameState = GameState.PLAYING
    }

    fun startMatchmaking() {
        resetGameBoard()
        lobbyMode = PlayerType.ONLINE_SIMULATED
        gameState = GameState.MATCHMAKING
        matchmakerFoundCount = 1
        matchmakingStatusText = "Connecting to Global Lobby..."

        viewModelScope.launch {
            delay(1000)
            matchmakerFoundCount = 2
            matchmakingStatusText = "Searching elite champions (2/4)..."
            delay(1000)
            matchmakerFoundCount = 3
            matchmakingStatusText = "Establishing peer channels (3/4)..."
            delay(800)
            matchmakerFoundCount = 4
            matchmakingStatusText = "Room matched! Syncing fair dice boards (4/4)..."
            delay(1000)

            // Setup Online Opponent Details
            players.add(LudoPlayer(PlayerColor.RED, "$username (You)", PlayerType.LOCAL_HUMAN, avatarId))
            players.add(LudoPlayer(PlayerColor.GREEN, listOf("NeonSlSlayer", "GoldenGamer", "CyberDancer").random(), PlayerType.ONLINE_SIMULATED, "avatar_online_1"))
            players.add(LudoPlayer(PlayerColor.YELLOW, listOf("RoyalDiceLover", "ShadowLudo", "ApexSniper").random(), PlayerType.ONLINE_SIMULATED, "avatar_online_2"))
            players.add(LudoPlayer(PlayerColor.BLUE, listOf("QueenLuna", "DiamondGems", "GoldDigger").random(), PlayerType.ONLINE_SIMULATED, "avatar_online_3"))

            currentTurnColor = PlayerColor.RED
            gameState = GameState.PLAYING

            // Welcome Chat message
            chatMessages.add(ChatMessage("System", "Welcome to Royal Palace! Emojis and Voice stream connected.", isEmojiOnly = false))
            isVoiceChatActive = true
            
            // Random simulated chat greetings
            delay(1500)
            sendSimulatedChat(players[1], "Good luck everyone! 👍")
            delay(1000)
            sendSimulatedChat(players[3], "Let the battle begin! 🎮🔥")
        }
    }

    fun rollDice() {
        if (hasRolled || rollingAnimActive) return

        viewModelScope.launch {
            rollingAnimActive = true
            // Quick physical roll loop
            for (i in 1..8) {
                diceValue = Random.nextInt(1, 7)
                delay(90)
            }
            rollingAnimActive = false
            hasRolled = true

            calculateMovableTokens()

            if (movableTokenIds.isEmpty()) {
                // Instantly pass Turn indicator after small delay
                delay(1200)
                advanceTurn()
            } else {
                // If AI/Online opponent, automatically choose the best move!
                val curPlayer = getActivePlayer()
                if (curPlayer != null && curPlayer.type != PlayerType.LOCAL_HUMAN) {
                    delay(1000)
                    triggerAutomatedMove(curPlayer)
                }
            }
        }
    }

    private fun calculateMovableTokens() {
        movableTokenIds.clear()
        val pTokens = tokens.filter { it.color == currentTurnColor }

        pTokens.forEach { token ->
            when (token.state) {
                TokenState.YARD -> {
                    if (diceValue == 6) {
                        movableTokenIds.add(token.id)
                    }
                }
                TokenState.TRACK -> {
                    // Maximum steps on track is 50. Then it moves into home stretch.
                    if (token.stepCounter + diceValue <= 56) {
                        movableTokenIds.add(token.id)
                    }
                }
                TokenState.HOME_STRETCH -> {
                    if (token.stepCounter + diceValue <= 56) {
                        movableTokenIds.add(token.id)
                    }
                }
                TokenState.GOAL -> {
                    // Completed, cannot move
                }
            }
        }
    }

    fun moveToken(token: LudoToken) {
        if (!hasRolled || rollingAnimActive) return
        if (token.color != currentTurnColor || !movableTokenIds.contains(token.id)) return

        movableTokenIds.clear()
        hasRolled = false

        viewModelScope.launch {
            val targetSteps = diceValue
            var currentTokenIdx = tokens.indexOf(token)

            for (step in 1..targetSteps) {
                // Real progressive stepping logic!
                val t = tokens[currentTokenIdx]
                when (t.state) {
                    TokenState.YARD -> {
                        t.state = TokenState.TRACK
                        t.stepCounter = 0
                    }
                    TokenState.TRACK -> {
                        t.stepCounter += 1
                        if (t.stepCounter > 50) {
                            t.state = TokenState.HOME_STRETCH
                        }
                    }
                    TokenState.HOME_STRETCH -> {
                        t.stepCounter += 1
                        if (t.stepCounter == 56) {
                            t.state = TokenState.GOAL
                        }
                    }
                    TokenState.GOAL -> {}
                }
                // Trigger quick visual sync refresh
                tokens[currentTokenIdx] = t.copy()
                delay(150)
            }

            // Checks final destination logic (Goal, Kills, etc.)
            val finalToken = tokens[currentTokenIdx]
            val hasKilled = checkCollisionsAndKills(finalToken)
            val hasReachedGoal = finalToken.state == TokenState.GOAL

            if (checkGrandVictory(currentTurnColor)) {
                triggerVictoryCelebration()
            } else {
                // Bonus roll rule on rolling a 6, reaching a goal, or getting a kill!
                if (diceValue == 6 || hasKilled || hasReachedGoal) {
                    // Retain turn, clear rolled state
                    hasRolled = false
                    val activePlayer = getActivePlayer()
                    if (activePlayer != null && activePlayer.type != PlayerType.LOCAL_HUMAN) {
                        delay(1200)
                        rollDice()
                    }
                } else {
                    advanceTurn()
                }
            }
        }
    }

    private fun checkCollisionsAndKills(mover: LudoToken): Boolean {
        if (mover.state != TokenState.TRACK) return false

        val moverGridPos = getTokenGridPosition(mover)
        val startIndex = getStartTrackIndex(mover.color)
        val absoluteIdx = (startIndex + mover.stepCounter) % 52

        // If sitting in a designated safe spot, cannot capture or be captured
        if (ludoSafeIndices.contains(absoluteIdx)) return false

        var capturedAny = false

        // Check if there are enemy tokens on the exact coordinate
        tokens.forEachIndexed { idx, tok ->
            if (tok.color != mover.color && tok.state == TokenState.TRACK) {
                val tokGridPos = getTokenGridPosition(tok)
                if (tokGridPos == moverGridPos) {
                    // Knock back to yard!
                    tok.state = TokenState.YARD
                    tok.stepCounter = 0
                    tokens[idx] = tok.copy()
                    capturedAny = true

                    // Trigger sparks particles explosion and visual feedback shake!
                    triggerCaptureSparks(moverGridPos)

                    // Simulated chat reaction
                    viewModelScope.launch {
                        val activePlayer = getActivePlayer()
                        val victimPlayer = players.firstOrNull { it.color == tok.color }
                        if (activePlayer != null && victimPlayer != null) {
                            delay(500)
                            if (activePlayer.type == PlayerType.ONLINE_SIMULATED) {
                                sendSimulatedChat(activePlayer, listOf("Boom! 🔥👑", "Gotcha!", "No mercy! 😎", "Yes! Move back!").random())
                            }
                            delay(800)
                            if (victimPlayer.type == PlayerType.ONLINE_SIMULATED) {
                                sendSimulatedChat(victimPlayer, listOf("Oh nooo! 😭", "Are you serious?", "Unlucky...", "I'll get you back! 😠").random())
                            }
                        }
                    }
                }
            }
        }

        return capturedAny
    }

    private fun triggerCaptureSparks(gridPos: Pair<Int, Int>) {
        screenShakeActive = true
        particleList.clear()
        val colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, GoldPrimary)
        for (i in 0..15) {
            particleList.add(
                Particle(
                    id = i,
                    x = gridPos.second * 100f + Random.nextFloat() * 50f,
                    y = gridPos.first * 100f + Random.nextFloat() * 50f,
                    color = colors.random(),
                    velocityX = Random.nextFloat() * 20f - 10f,
                    velocityY = Random.nextFloat() * 20f - 10f
                )
            )
        }
        viewModelScope.launch {
            delay(500)
            screenShakeActive = false
            particleList.clear()
        }
    }

    private fun triggerVictoryCelebration() {
        val activeP = getActivePlayer()
        winner = activeP
        gameState = GameState.GAME_OVER
        showWinningCelebration = true

        // Record User Preferences
        val userWon = activeP?.color == PlayerColor.RED
        prefs.recordMatch(userWon)

        // Sync local preferences state
        userCoins = prefs.coins
        userGems = prefs.gems
        matchesPlayed = prefs.matchesPlayed
        matchesWon = prefs.matchesWon

        if (userWon) {
            sendChatMessage("Royal Palace", "🏆 Congratulations $username! You are the Empire Sovereign!")
        } else {
            sendChatMessage("Royal Palace", "👑 Match Finished! Player ${winner?.name} conquers the Throne!")
        }
    }

    private fun checkGrandVictory(color: PlayerColor): Boolean {
        // Human wins or other if all 4 tokens are in GOAL
        val pTokens = tokens.filter { it.color == color }
        return pTokens.all { it.state == TokenState.GOAL }
    }

    fun exitGame() {
        gameState = GameState.IDLE
        resetGameBoard()
    }

    private fun advanceTurn() {
        val curIndex = currentTurnColor.ordinal
        val nextIndex = (curIndex + 1) % 4
        currentTurnColor = PlayerColor.values()[nextIndex]
        hasRolled = false
        movableTokenIds.clear()

        // Sync AI Turn trigger immediately if the next player is not human
        val nextPlayer = getActivePlayer()
        if (nextPlayer != null && nextPlayer.type != PlayerType.LOCAL_HUMAN) {
            viewModelScope.launch {
                delay(1200)
                rollDice()
            }
        }
    }

    private fun triggerAutomatedMove(player: LudoPlayer) {
        viewModelScope.launch {
            if (movableTokenIds.isEmpty()) {
                advanceTurn()
                return@launch
            }

            // Strategic Selection Core:
            var selectedTokenId = movableTokenIds.first()
            val availableMovableTokens = tokens.filter { it.color == player.color && movableTokenIds.contains(it.id) }

            // Priority 1: Pick a token that CAPTURES an enemy
            val capturingToken = availableMovableTokens.firstOrNull { tok ->
                if (tok.state != TokenState.TRACK) false
                else {
                    val futureStep = tok.stepCounter + diceValue
                    val start = getStartTrackIndex(tok.color)
                    val absoluteFuture = (start + futureStep) % 52
                    val coordinatesFuture = ludoCommonPath[absoluteFuture]

                    // Check if non-safe and coordinates match an enemy
                    !ludoSafeIndices.contains(absoluteFuture) && tokens.any { enemy ->
                        enemy.color != tok.color && enemy.state == TokenState.TRACK && getTokenGridPosition(enemy) == coordinatesFuture
                    }
                }
            }

            // Priority 2: Release a token from yard if rolled 6
            val yardToken = availableMovableTokens.firstOrNull { it.state == TokenState.YARD }

            // Priority 3: Enter Goal/Safe space priority
            val safeTransitionToken = availableMovableTokens.firstOrNull { tok ->
                val futureStep = tok.stepCounter + diceValue
                futureStep == 56 || (tok.state == TokenState.TRACK && futureStep > 50)
            }

            // Choose the best strategic decision
            selectedTokenId = when {
                capturingToken != null -> capturingToken.id
                yardToken != null -> yardToken.id
                safeTransitionToken != null -> safeTransitionToken.id
                else -> {
                    // Easy difficulty: random move. Medium/Hard: move the advanced piece.
                    if (player.type == PlayerType.AI_EASY) {
                        movableTokenIds.random()
                    } else {
                        availableMovableTokens.maxByOrNull { it.stepCounter }?.id ?: movableTokenIds.first()
                    }
                }
            }

            val matchedToken = tokens.first { it.color == player.color && it.id == selectedTokenId }

            // Periodical Online Chat Emoji simulations on AI turns!
            if (player.type == PlayerType.ONLINE_SIMULATED && Random.nextInt(0, 4) == 0) {
                sendSimulatedEmoji(player, listOf("😎", "🥳", "🤔", "🎲", "🔥").random())
            }

            delay(600)
            moveToken(matchedToken)
        }
    }

    fun sendChatMessage(sender: String, msg: String) {
        chatMessages.add(ChatMessage(sender, msg, isEmojiOnly = false))
        if (chatMessages.size > 20) chatMessages.removeAt(0)
    }

    fun sendUserEmoji(emoji: String) {
        chatMessages.add(ChatMessage("$username (You)", emoji, isEmojiOnly = true))
        if (chatMessages.size > 20) chatMessages.removeAt(0)
    }

    private fun sendSimulatedChat(player: LudoPlayer, msg: String) {
        chatMessages.add(ChatMessage(player.name, msg, isEmojiOnly = false))
    }

    private fun sendSimulatedEmoji(player: LudoPlayer, emoji: String) {
        chatMessages.add(ChatMessage(player.name, emoji, isEmojiOnly = true))
    }

    fun getActivePlayer(): LudoPlayer? {
        return players.firstOrNull { it.color == currentTurnColor }
    }
}
