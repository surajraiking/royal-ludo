package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.repository.LudoFirebaseRepository
import com.example.repository.UserPreferences
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.random.Random

class LudoViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = UserPreferences(application)
    val firebaseRepo = LudoFirebaseRepository(application)

    // Cloud status & Leaderboard
    var isCloudSyncing by mutableStateOf(false)
        private set
    private val _leaderboardUsers = MutableStateFlow<List<FirebaseUserProfile>>(emptyList())
    val leaderboardUsers: StateFlow<List<FirebaseUserProfile>> = _leaderboardUsers.asStateFlow()

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

    // Turn & Round Tracking
    var turnNumber by mutableStateOf(1)
        private set
    var roundNumber by mutableStateOf(1)
        private set
    var turnPhase by mutableStateOf(TurnPhase.ROLL_DICE)
        private set
    var bonusTurnReason by mutableStateOf<String?>(null)
        private set
    var turnHistory = mutableStateListOf<String>()
    var activeRoomCode by mutableStateOf<String?>(null)
        private set

    /**
     * Real-time GameTurnInfo snapshot for UI observation
     */
    val gameTurnInfo: GameTurnInfo by derivedStateOf {
        val curPlayer = getActivePlayer()
        GameTurnInfo(
            roundNumber = roundNumber,
            turnNumber = turnNumber,
            activeColor = currentTurnColor,
            activePlayerName = curPlayer?.name ?: currentTurnColor.displayName,
            phase = turnPhase,
            diceRoll = if (hasRolled) diceValue else null,
            isHumanTurn = curPlayer?.type == PlayerType.LOCAL_HUMAN,
            bonusReason = bonusTurnReason
        )
    }

    /**
     * Real-time snapshot of all player board positions and piece states (BASE, PATH, GOAL)
     */
    val playerBoardPositions: List<PlayerBoardPosition> by derivedStateOf {
        players.map { player ->
            val playerTokens = tokens.filter { it.color == player.color }
            val inBase = playerTokens.count { it.isInBase }
            val onPath = playerTokens.count { it.isOnPath }
            val inGoal = playerTokens.count { it.isInGoal }
            val maxStep = playerTokens.maxOfOrNull { it.stepCounter } ?: 0
            val totalSteps = playerTokens.sumOf { it.stepCounter }
            val goalProgress = (inGoal / 4f).coerceIn(0f, 1f)

            PlayerBoardPosition(
                player = player,
                color = player.color,
                pieces = playerTokens,
                piecesInBase = inBase,
                piecesOnPath = onPath,
                piecesInGoal = inGoal,
                leadingStep = maxStep,
                totalStepsWalked = totalSteps,
                progressPercent = goalProgress
            )
        }
    }

    /**
     * Map of 15x15 board cell coordinates to tokens present on that cell
     */
    val boardOccupancies: Map<Pair<Int, Int>, List<LudoToken>> by derivedStateOf {
        tokens.groupBy { getTokenGridPosition(it) }
    }

    // Piece State Queries
    fun getPieces(color: PlayerColor): List<LudoToken> = tokens.filter { it.color == color }
    fun getPiecesInBase(color: PlayerColor): List<LudoToken> = tokens.filter { it.color == color && it.isInBase }
    fun getPiecesOnPath(color: PlayerColor): List<LudoToken> = tokens.filter { it.color == color && it.isOnPath }
    fun getPiecesInGoal(color: PlayerColor): List<LudoToken> = tokens.filter { it.color == color && it.isInGoal }
    fun getPlayerBoardPosition(color: PlayerColor): PlayerBoardPosition? =
        playerBoardPositions.firstOrNull { it.color == color }
    fun getTokensAtCell(coordinate: Pair<Int, Int>): List<LudoToken> =
        boardOccupancies[coordinate] ?: emptyList()

    fun getLeadingPlayer(): LudoPlayer? {
        return playerBoardPositions.maxByOrNull { it.piecesInGoal * 1000 + it.totalStepsWalked }?.player
    }

    // Live indicators
    var rollingAnimActive by mutableStateOf(false)
        private set
    var chatMessages = mutableStateListOf<ChatMessage>()
    var particleList = mutableStateListOf<Particle>()
    var isVoiceChatActive by mutableStateOf(false)
        private set
    var showWinningCelebration by mutableStateOf(false)
    var screenShakeActive by mutableStateOf(false)

    // 3D Visual & Interactive Animation State
    var movingTokenId by mutableStateOf<Int?>(null)
    var active3DEffect by mutableStateOf<Interactive3DEffect?>(null)
    var center3DTextHeadline by mutableStateOf("ROYAL LUDO 3D")

    fun triggerRolledSixEffect(color: PlayerColor) {
        val player = players.firstOrNull { it.color == color }
        center3DTextHeadline = "★ LUCKY 6! ★"
        active3DEffect = Interactive3DEffect(
            type = InteractiveEffectType.ROLLED_SIX,
            headline = "★ LUCKY 6! ★",
            detail = "${player?.name ?: color.displayName} rolled a 6! Extra Turn!",
            color = color,
            icon = "🎲 6"
        )
        screenShakeActive = true
        viewModelScope.launch {
            delay(2200)
            if (active3DEffect?.type == InteractiveEffectType.ROLLED_SIX) {
                active3DEffect = null
            }
            screenShakeActive = false
        }
    }

    fun triggerPieceUnlockedEffect(color: PlayerColor) {
        val player = players.firstOrNull { it.color == color }
        center3DTextHeadline = "🚀 PIECE OPENED! 🚀"
        active3DEffect = Interactive3DEffect(
            type = InteractiveEffectType.TOKEN_UNLOCKED,
            headline = "🚀 PIECE OPENED! 🚀",
            detail = "${player?.name ?: color.displayName} deployed a chess piece into battle!",
            color = color,
            icon = "♟️"
        )
        viewModelScope.launch {
            delay(2000)
            if (active3DEffect?.type == InteractiveEffectType.TOKEN_UNLOCKED) {
                active3DEffect = null
            }
        }
    }

    fun triggerCaptureEffect(moverColor: PlayerColor, victimColor: PlayerColor) {
        val mover = players.firstOrNull { it.color == moverColor }
        center3DTextHeadline = "⚔️ CAPTURE! ⚔️"
        active3DEffect = Interactive3DEffect(
            type = InteractiveEffectType.CAPTURE,
            headline = "⚔️ ENEMY CAPTURED! ⚔️",
            detail = "${mover?.name ?: moverColor.displayName} sent opponent back to base!",
            color = moverColor,
            icon = "💥"
        )
        viewModelScope.launch {
            delay(2200)
            if (active3DEffect?.type == InteractiveEffectType.CAPTURE) {
                active3DEffect = null
            }
        }
    }

    fun dismiss3DEffect() {
        active3DEffect = null
    }

    // Matchmaking simulator visual variables
    var matchmakingStatusText by mutableStateOf("Initializing Imperial Servers...")
    var matchmakerFoundCount by mutableStateOf(1) // User is first

    init {
        resetGameBoard()
    }

    fun onUserAuthenticated() {
        if (Firebase.auth.currentUser == null) return
        isCloudSyncing = true
        viewModelScope.launch {
            try {
                firebaseRepo.saveOrInitUserProfile(
                    username = prefs.username,
                    avatarId = prefs.avatarId,
                    initialCoins = prefs.coins,
                    initialGems = prefs.gems
                )
            } catch (e: Exception) {
                // Handled in repo
            }

            launch {
                firebaseRepo.observeUserProfile()
                    .catch { }
                    .collect { profile ->
                        if (profile != null) {
                            username = profile.username
                            avatarId = profile.avatarId
                            userCoins = profile.coins
                            userGems = profile.gems
                            matchesPlayed = profile.matchesPlayed
                            matchesWon = profile.matchesWon

                            prefs.username = profile.username
                            prefs.avatarId = profile.avatarId
                            prefs.coins = profile.coins
                            prefs.gems = profile.gems
                            prefs.matchesPlayed = profile.matchesPlayed
                            prefs.matchesWon = profile.matchesWon
                        }
                        isCloudSyncing = false
                    }
            }

            launch {
                firebaseRepo.observeLeaderboard()
                    .catch { }
                    .collect { users ->
                        _leaderboardUsers.value = users
                    }
            }
        }
    }

    fun updateProfile(name: String, avId: String) {
        prefs.username = name
        prefs.avatarId = avId
        username = name
        avatarId = avId
        viewModelScope.launch {
            try {
                if (Firebase.auth.currentUser != null) {
                    firebaseRepo.updateProfile(name, avId)
                }
            } catch (e: Exception) { }
        }
    }

    fun rollDailyReward(): Int {
        val earned = listOf(100, 250, 500, 1000, 50, 150).random()
        prefs.earnCoins(earned)
        userCoins = prefs.coins
        prefs.lastSpinTimestamp = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                if (Firebase.auth.currentUser != null) {
                    firebaseRepo.updateCurrency(coinsDelta = earned, gemsDelta = 0)
                }
            } catch (e: Exception) { }
        }
        return earned
    }

    fun rollDailyRewardGems(): Int {
        val earned = listOf(5, 10, 15, 20).random()
        prefs.earnGems(earned)
        userGems = prefs.gems
        prefs.lastSpinTimestamp = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                if (Firebase.auth.currentUser != null) {
                    firebaseRepo.updateCurrency(coinsDelta = 0, gemsDelta = earned)
                }
            } catch (e: Exception) { }
        }
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
            viewModelScope.launch {
                try {
                    if (Firebase.auth.currentUser != null) {
                        firebaseRepo.updateCurrency(coinsDelta = -goldCost, gemsDelta = -gemsCost)
                    }
                } catch (e: Exception) { }
            }
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
        turnNumber = 1
        roundNumber = 1
        turnPhase = TurnPhase.ROLL_DICE
        bonusTurnReason = null
        turnHistory.clear()
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

    /**
     * Start match with custom friend names (Pass & Play on same device)
     */
    fun startFriendsCustomGame(friendNames: List<String>) {
        resetGameBoard()
        lobbyMode = PlayerType.LOCAL_HUMAN
        activeRoomCode = null

        val colors = PlayerColor.values()
        for (i in 0 until 4) {
            val isHuman = i < friendNames.size
            val pName = if (isHuman) {
                friendNames[i].ifBlank { if (i == 0) "$username (You)" else "Friend ${i + 1}" }
            } else {
                "AI Bot ${i + 1}"
            }
            players.add(
                LudoPlayer(
                    color = colors[i],
                    name = pName,
                    type = if (isHuman) PlayerType.LOCAL_HUMAN else PlayerType.AI_MEDIUM,
                    avatarId = "avatar_${i + 1}"
                )
            )
        }
        currentTurnColor = PlayerColor.RED
        gameState = GameState.PLAYING
    }

    /**
     * Start Private Room with friends via 6-digit Room Code
     */
    fun startPrivateRoomMatch(code: String, customNames: List<String> = emptyList()) {
        resetGameBoard()
        activeRoomCode = code
        lobbyMode = PlayerType.ONLINE_SIMULATED

        val colors = PlayerColor.values()
        val defaultOpponents = listOf("Royal Buddy 👑", "Imperial Champ 🔥", "Ludo Master 🎮")
        players.add(LudoPlayer(PlayerColor.RED, "$username (Host)", PlayerType.LOCAL_HUMAN, avatarId))

        for (i in 1..3) {
            val friendName = if (i - 1 < customNames.size && customNames[i - 1].isNotBlank()) {
                customNames[i - 1]
            } else {
                defaultOpponents.getOrElse(i - 1) { "Friend ${i + 1}" }
            }
            players.add(
                LudoPlayer(
                    color = colors[i],
                    name = friendName,
                    type = PlayerType.ONLINE_SIMULATED,
                    avatarId = "avatar_online_$i"
                )
            )
        }

        currentTurnColor = PlayerColor.RED
        gameState = GameState.PLAYING
        sendChatMessage("System", "👑 Private Room $code initialized! Battle your friends to victory.")
        isVoiceChatActive = true
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
            bonusTurnReason = null
            // Quick physical roll loop
            for (i in 1..8) {
                diceValue = Random.nextInt(1, 7)
                delay(90)
            }
            rollingAnimActive = false
            hasRolled = true

            if (diceValue == 6) {
                triggerRolledSixEffect(currentTurnColor)
            }

            calculateMovableTokens()

            if (movableTokenIds.isEmpty()) {
                turnPhase = TurnPhase.NO_MOVES
                // Instantly pass Turn indicator after small delay
                delay(1200)
                advanceTurn()
            } else {
                turnPhase = TurnPhase.SELECT_PIECE
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

        val wasInYard = token.state == TokenState.YARD
        movableTokenIds.clear()
        hasRolled = false
        turnPhase = TurnPhase.PIECE_MOVING
        movingTokenId = token.id

        viewModelScope.launch {
            if (wasInYard) {
                triggerPieceUnlockedEffect(token.color)
            }

            val targetSteps = diceValue
            var currentTokenIdx = tokens.indexOf(token)

            for (step in 1..targetSteps) {
                // Real progressive stepping logic with 3D jump intervals
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
                delay(180)
            }

            movingTokenId = null

            // Checks final destination logic (Goal, Kills, etc.)
            val finalToken = tokens[currentTokenIdx]
            val hasKilled = checkCollisionsAndKills(finalToken)
            val hasReachedGoal = finalToken.state == TokenState.GOAL

            if (checkGrandVictory(currentTurnColor)) {
                triggerVictoryCelebration()
            } else {
                // Bonus roll rule on rolling a 6, reaching a goal, or getting a kill!
                if (diceValue == 6 || hasKilled || hasReachedGoal) {
                    val reason = when {
                        hasKilled -> "Enemy captured! ⚔️ Bonus Roll!"
                        hasReachedGoal -> "Goal reached! 🏆 Bonus Roll!"
                        diceValue == 6 -> "Rolled a 6! 🎲 Bonus Roll!"
                        else -> "Bonus Roll!"
                    }
                    bonusTurnReason = reason
                    turnPhase = TurnPhase.EXTRA_TURN
                    turnHistory.add("Turn $turnNumber: ${getActivePlayer()?.name} earned bonus turn ($reason)")
                    if (turnHistory.size > 25) turnHistory.removeAt(0)

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

                    // Trigger 3D capture announcement & sparks explosion
                    triggerCaptureEffect(mover.color, tok.color)
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
        turnPhase = TurnPhase.GAME_OVER
        showWinningCelebration = true

        // Record User Preferences
        val userWon = activeP?.color == PlayerColor.RED
        prefs.recordMatch(userWon)

        // Sync local preferences state
        userCoins = prefs.coins
        userGems = prefs.gems
        matchesPlayed = prefs.matchesPlayed
        matchesWon = prefs.matchesWon

        viewModelScope.launch {
            try {
                if (Firebase.auth.currentUser != null) {
                    firebaseRepo.recordMatch(
                        gameMode = lobbyMode.name,
                        won = userWon,
                        coinsEarned = if (userWon) 250 else 50
                    )
                }
            } catch (e: Exception) { }
        }

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
        bonusTurnReason = null
        turnPhase = TurnPhase.ROLL_DICE
        turnNumber++
        roundNumber = ((turnNumber - 1) / 4) + 1

        val nextPlayer = getActivePlayer()
        turnHistory.add("Round $roundNumber, Turn $turnNumber: ${nextPlayer?.name ?: currentTurnColor.displayName}'s turn")
        if (turnHistory.size > 25) turnHistory.removeAt(0)

        // Sync AI Turn trigger immediately if the next player is not human
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
