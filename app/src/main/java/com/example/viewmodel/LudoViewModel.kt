package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.repository.LudoFirebaseRepository
import com.example.repository.UserPreferences
import com.example.sound.LudoSoundManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.example.ui.theme.LudoRed
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoYellow
import com.example.ui.theme.LudoBlueVibrant
import com.example.ui.theme.GoldPrimary
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Job
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

    // Authoritative State Manager
    val gameStateManager = GameStateManager()

    // Game core states
    var lastScreenState by mutableStateOf("lobby")
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

    // REAL-TIME ONLINE ROOM MULTIPLAYER STATES (Ludo King Play with Friends)
    var isOnlineRoomMatch by mutableStateOf(false)
    var myOnlineColor by mutableStateOf(PlayerColor.RED)
    var isRoomHost by mutableStateOf(false)
    var roomStake by mutableStateOf(500)
    var waitingRoomPlayers = mutableStateListOf<RoomPlayer>()
    var isWaitingRoomVisible by mutableStateOf(false)
    var isCreatingOrJoiningRoom by mutableStateOf(false)
    var roomErrorMessage by mutableStateOf<String?>(null)
    private var roomObserverJob: Job? = null
    private var lastProcessedMoveTimestamp: Long = 0L

    /**
     * Real-time GameTurnInfo snapshot for UI observation
     */
    val gameTurnInfo: GameTurnInfo by derivedStateOf {
        val curPlayer = getActivePlayer()
        val isHumanTurn = if (isOnlineRoomMatch) {
            currentTurnColor == myOnlineColor
        } else {
            curPlayer?.type == PlayerType.LOCAL_HUMAN
        }
        GameTurnInfo(
            roundNumber = roundNumber,
            turnNumber = turnNumber,
            activeColor = currentTurnColor,
            activePlayerName = curPlayer?.name ?: currentTurnColor.displayName,
            phase = turnPhase,
            diceRoll = if (hasRolled) diceValue else null,
            isHumanTurn = isHumanTurn,
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
    var movingTokenColor by mutableStateOf<PlayerColor?>(null)
    var consecutiveSixCount by mutableStateOf(0)
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

    fun triggerGoalEffect(color: PlayerColor) {
        val player = players.firstOrNull { it.color == color }
        center3DTextHeadline = "🏆 GOAL SCORED! 🏆"
        active3DEffect = Interactive3DEffect(
            type = InteractiveEffectType.GOAL_SCORED,
            headline = "🏆 GOAL REACHED! 🏆",
            detail = "${player?.name ?: color.displayName} moved a piece into the central Royal Palace!",
            color = color,
            icon = "👑"
        )
        viewModelScope.launch {
            delay(2200)
            if (active3DEffect?.type == InteractiveEffectType.GOAL_SCORED) {
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

                            if (profile.lastScreenState.isNotBlank()) {
                                lastScreenState = profile.lastScreenState
                            }
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

    fun updateScreenState(screen: String) {
        lastScreenState = screen
        viewModelScope.launch {
            try {
                if (Firebase.auth.currentUser != null) {
                    firebaseRepo.updateLastScreenState(screen)
                }
            } catch (e: Exception) { }
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
        gameStateManager.resetSync()
        tokens.clear()
        tokens.addAll(gameStateManager.tokens)
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
        consecutiveSixCount = 0
        movingTokenId = null
        movingTokenColor = null
        center3DTextHeadline = "ROYAL LUDO 3D"
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
        viewModelScope.launch {
            gameStateManager.startNewGame(players.toList(), PlayerColor.RED)
            tokens.clear()
            tokens.addAll(gameStateManager.tokens)
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
        viewModelScope.launch {
            gameStateManager.startNewGame(players.toList(), PlayerColor.RED)
            tokens.clear()
            tokens.addAll(gameStateManager.tokens)
        }
        currentTurnColor = PlayerColor.RED
        gameState = GameState.PLAYING
    }

    /**
     * Creates an authoritative Cloud Multiplayer Room in Firestore (Ludo King Play with Friends).
     */
    fun createOnlineRoom(stake: Int = 500, code: String? = null) {
        val uid = firebaseRepo.currentUserId
        if (uid == null) {
            roomErrorMessage = "Please sign in with Email or Google to create a Private Room."
            return
        }
        val roomCode = code?.trim()?.uppercase()?.ifBlank { null }
            ?: (100000..999999).random().toString()

        isCreatingOrJoiningRoom = true
        roomErrorMessage = null

        viewModelScope.launch {
            try {
                val room = firebaseRepo.createMultiplayerRoom(
                    roomId = roomCode,
                    hostName = username.ifBlank { "Host" },
                    avatarId = avatarId,
                    stake = stake
                )
                activeRoomCode = roomCode
                isRoomHost = true
                myOnlineColor = PlayerColor.RED
                roomStake = stake
                waitingRoomPlayers.clear()
                waitingRoomPlayers.addAll(room.players)
                isWaitingRoomVisible = true
                isCreatingOrJoiningRoom = false

                startObservingOnlineRoom(roomCode)
            } catch (e: Exception) {
                isCreatingOrJoiningRoom = false
                roomErrorMessage = e.localizedMessage ?: "Failed to create room. Please check internet connection."
            }
        }
    }

    /**
     * Joins an existing Cloud Multiplayer Room in Firestore via 6-digit Room Code.
     */
    fun joinOnlineRoom(code: String) {
        val uid = firebaseRepo.currentUserId
        if (uid == null) {
            roomErrorMessage = "Please sign in with Email or Google to join a Private Room."
            return
        }
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) {
            roomErrorMessage = "Please enter a valid 6-digit Room Code."
            return
        }

        isCreatingOrJoiningRoom = true
        roomErrorMessage = null

        viewModelScope.launch {
            try {
                val (room, assignedColor) = firebaseRepo.joinMultiplayerRoom(
                    roomId = cleanCode,
                    playerName = username.ifBlank { "Player" },
                    avatarId = avatarId
                )
                activeRoomCode = cleanCode
                isRoomHost = false
                myOnlineColor = assignedColor
                roomStake = room.stake
                waitingRoomPlayers.clear()
                waitingRoomPlayers.addAll(room.players)
                isWaitingRoomVisible = true
                isCreatingOrJoiningRoom = false

                startObservingOnlineRoom(cleanCode)
            } catch (e: Exception) {
                isCreatingOrJoiningRoom = false
                roomErrorMessage = e.localizedMessage ?: "Could not join room. Verify code with your friend."
            }
        }
    }

    /**
     * Host starts the Cloud Multiplayer Match once at least 2 players have joined.
     */
    fun startOnlineRoomMatch() {
        val code = activeRoomCode ?: return
        if (!isRoomHost) {
            roomErrorMessage = "Only the Host can start the match!"
            return
        }
        if (waitingRoomPlayers.size < 2) {
            roomErrorMessage = "Waiting for friends to join! Share code $code (Need at least 2 players)."
            return
        }

        viewModelScope.launch {
            try {
                firebaseRepo.startMultiplayerMatch(code)
            } catch (e: Exception) {
                roomErrorMessage = e.localizedMessage ?: "Failed to start match"
            }
        }
    }

    /**
     * Leaves or closes an online room.
     */
    fun leaveOnlineRoom() {
        val code = activeRoomCode
        roomObserverJob?.cancel()
        roomObserverJob = null
        isWaitingRoomVisible = false
        isOnlineRoomMatch = false
        activeRoomCode = null
        roomErrorMessage = null
        waitingRoomPlayers.clear()

        if (code != null) {
            viewModelScope.launch {
                try {
                    firebaseRepo.leaveMultiplayerRoom(code)
                } catch (e: Exception) { }
            }
        }
    }

    /**
     * Observes real-time room state from Firestore.
     */
    fun startObservingOnlineRoom(roomId: String) {
        roomObserverJob?.cancel()
        roomObserverJob = viewModelScope.launch {
            firebaseRepo.observeMultiplayerRoom(roomId).collect { room ->
                if (room == null) {
                    if (isWaitingRoomVisible) {
                        roomErrorMessage = "Host has closed the room."
                        isWaitingRoomVisible = false
                    }
                    return@collect
                }

                // Update waiting room roster in real-time
                waitingRoomPlayers.clear()
                waitingRoomPlayers.addAll(room.players)

                // Check status
                if (room.status == "playing") {
                    if (!isOnlineRoomMatch) {
                        // Match just started! Transition into the game!
                        isWaitingRoomVisible = false
                        setupOnlineGameFromRoom(room)
                    } else {
                        // Game is already running: sync real-time moves & rolls
                        syncGameStateFromRemote(room)
                    }
                } else if (room.status == "finished") {
                    if (room.winnerColor != null && winner == null) {
                        val winCol = try { PlayerColor.valueOf(room.winnerColor) } catch (e: Exception) { null }
                        if (winCol != null) {
                            winner = players.firstOrNull { it.color == winCol }
                            triggerVictoryCelebration()
                        }
                    }
                }
            }
        }
    }

    private fun setupOnlineGameFromRoom(room: FirebaseGameRoom) {
        resetGameBoard()
        isOnlineRoomMatch = true
        activeRoomCode = room.roomId
        lobbyMode = PlayerType.ONLINE_SIMULATED

        players.clear()
        val sortedPlayers = room.players.sortedBy { it.seat }
        sortedPlayers.forEach { rp ->
            val col = try { PlayerColor.valueOf(rp.color) } catch (e: Exception) { PlayerColor.RED }
            val isMe = rp.uid == firebaseRepo.currentUserId
            val type = if (isMe) PlayerType.LOCAL_HUMAN else PlayerType.ONLINE_SIMULATED
            val nameDisplay = if (isMe) "${rp.name} (You)" else rp.name
            players.add(LudoPlayer(color = col, name = nameDisplay, type = type, avatarId = rp.avatarId))
        }

        viewModelScope.launch {
            gameStateManager.startNewGame(players.toList(), PlayerColor.RED)
            tokens.clear()
            tokens.addAll(gameStateManager.tokens)
        }

        currentTurnColor = PlayerColor.RED
        turnPhase = TurnPhase.ROLL_DICE
        hasRolled = false
        movableTokenIds.clear()
        gameState = GameState.PLAYING
        sendChatMessage("System", "👑 Online Room ${room.roomId} started! Battle your friends to victory.")
        isVoiceChatActive = true
    }

    private fun syncGameStateFromRemote(room: FirebaseGameRoom) {
        val activeTurnColorName = room.currentTurnColor
        val activeTurnCol = try { PlayerColor.valueOf(activeTurnColorName) } catch (e: Exception) { PlayerColor.RED }

        // 1. Sync remote roll:
        if (currentTurnColor != myOnlineColor && room.hasRolled && !hasRolled) {
            diceValue = room.diceValue
            hasRolled = true
            movableTokenIds.clear()
            movableTokenIds.addAll(room.movableTokenIds)
            turnPhase = try { TurnPhase.valueOf(room.turnPhase) } catch (e: Exception) { TurnPhase.SELECT_PIECE }
            LudoSoundManager.playDiceRoll()
            if (diceValue == 6) {
                LudoSoundManager.playLuckySix()
                triggerRolledSixEffect(activeTurnCol)
            }
        }

        // 2. Sync remote move action:
        val lastMove = room.lastMove
        if (lastMove != null && lastMove.timestamp > lastProcessedMoveTimestamp && currentTurnColor != myOnlineColor) {
            lastProcessedMoveTimestamp = lastMove.timestamp
            LudoSoundManager.playPieceStep()
            if (lastMove.wasCapture) {
                LudoSoundManager.playPieceCapture()
                val moveCol = try { PlayerColor.valueOf(lastMove.playerColor) } catch (e: Exception) { PlayerColor.RED }
                triggerCaptureEffect(moveCol, activeTurnCol)
            }
            if (lastMove.reachedGoal) {
                LudoSoundManager.playGoalScored()
            }
        }

        // 3. Sync board tokens directly from Firestore:
        if (movingTokenId == null && currentTurnColor != myOnlineColor) {
            if (room.tokens.isNotEmpty()) {
                room.tokens.forEach { rt ->
                    val c = try { PlayerColor.valueOf(rt.color) } catch (e: Exception) { null }
                    val s = try { TokenState.valueOf(rt.state) } catch (e: Exception) { null }
                    if (c != null && s != null) {
                        val idx = tokens.indexOfFirst { it.color == c && it.id == rt.id }
                        if (idx != -1 && (tokens[idx].state != s || tokens[idx].stepCounter != rt.stepCounter)) {
                            tokens[idx] = tokens[idx].copy(state = s, stepCounter = rt.stepCounter)
                        }
                    }
                }
            }
        }

        // 4. Sync turn changes:
        if (currentTurnColor != activeTurnCol) {
            currentTurnColor = activeTurnCol
            hasRolled = room.hasRolled
            turnPhase = try { TurnPhase.valueOf(room.turnPhase) } catch (e: Exception) { TurnPhase.ROLL_DICE }
            movableTokenIds.clear()
            movableTokenIds.addAll(room.movableTokenIds)
            bonusTurnReason = null
        }
    }

    /**
     * Start Private Room fallback for local simulated match
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
        viewModelScope.launch {
            gameStateManager.startNewGame(players.toList(), PlayerColor.RED)
            tokens.clear()
            tokens.addAll(gameStateManager.tokens)
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

        viewModelScope.launch {
            gameStateManager.startNewGame(players.toList(), PlayerColor.RED)
            tokens.clear()
            tokens.addAll(gameStateManager.tokens)
        }
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

            gameStateManager.startNewGame(players.toList(), PlayerColor.RED)
            tokens.clear()
            tokens.addAll(gameStateManager.tokens)

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
        if (!gameStateManager.canRollDice(currentTurnColor)) return
        if (rollingAnimActive) return

        // In online room match: ONLY the active player whose turn it is can roll
        if (isOnlineRoomMatch && currentTurnColor != myOnlineColor) return

        viewModelScope.launch {
            rollingAnimActive = true
            bonusTurnReason = null
            LudoSoundManager.playDiceRoll()

            // Realistic physical roll animation
            for (i in 1..8) {
                diceValue = Random.nextInt(1, 7)
                delay(70)
            }
            rollingAnimActive = false

            // Authoritative roll execution via GameStateManager
            val rollResult = gameStateManager.rollDice(diceValue)

            when (rollResult) {
                is RollResult.TurnForfeitedThreeSixes -> {
                    consecutiveSixCount = 0
                    hasRolled = true
                    turnPhase = TurnPhase.NO_MOVES
                    center3DTextHeadline = "3 CONSECUTIVE 6s!"
                    active3DEffect = Interactive3DEffect(
                        type = InteractiveEffectType.ROLLED_SIX,
                        headline = "3 CONSECUTIVE 6s! ❌",
                        detail = "Three consecutive sixes! Turn forfeited by Ludo rules.",
                        color = currentTurnColor,
                        icon = "⚠️"
                    )
                    LudoSoundManager.playTurnPass()

                    if (isOnlineRoomMatch) {
                        val nextCol = LudoEngine.getNextTurnColor(currentTurnColor, players)
                        activeRoomCode?.let { code ->
                            firebaseRepo.syncOnlineAdvanceTurn(code, nextCol)
                        }
                    }

                    delay(1500)
                    active3DEffect = null
                    advanceTurn()
                }
                is RollResult.NoMovesAvailable -> {
                    hasRolled = true
                    consecutiveSixCount = gameStateManager.consecutiveSixCount
                    movableTokenIds.clear()
                    LudoSoundManager.playTurnPass()
                    turnPhase = TurnPhase.NO_MOVES
                    center3DTextHeadline = "NO MOVES POSSIBLE"

                    if (isOnlineRoomMatch) {
                        val nextCol = LudoEngine.getNextTurnColor(currentTurnColor, players)
                        activeRoomCode?.let { code ->
                            firebaseRepo.syncOnlineDiceRoll(
                                roomId = code,
                                diceValue = diceValue,
                                consecutiveSixCount = consecutiveSixCount,
                                legalMoves = emptyList(),
                                nextPhase = TurnPhase.NO_MOVES.name
                            )
                            firebaseRepo.syncOnlineAdvanceTurn(code, nextCol)
                        }
                    }

                    delay(1000)
                    advanceTurn()
                }
                is RollResult.Success -> {
                    hasRolled = true
                    consecutiveSixCount = rollResult.consecutiveSixes
                    movableTokenIds.clear()
                    movableTokenIds.addAll(rollResult.legalMoves)

                    if (isOnlineRoomMatch) {
                        activeRoomCode?.let { code ->
                            firebaseRepo.syncOnlineDiceRoll(
                                roomId = code,
                                diceValue = diceValue,
                                consecutiveSixCount = consecutiveSixCount,
                                legalMoves = rollResult.legalMoves,
                                nextPhase = TurnPhase.SELECT_PIECE.name
                            )
                        }
                    }

                    if (rollResult.diceValue == 6) {
                        LudoSoundManager.playLuckySix()
                        triggerRolledSixEffect(currentTurnColor)
                    }

                    val curPlayer = getActivePlayer()
                    val isHuman = if (isOnlineRoomMatch) (currentTurnColor == myOnlineColor) else (curPlayer != null && curPlayer.type == PlayerType.LOCAL_HUMAN)

                    if (isHuman) {
                        if (rollResult.autoMoveTokenId != null) {
                            turnPhase = TurnPhase.SELECT_PIECE
                            delay(400)
                            val tokenToMove = tokens.firstOrNull { it.color == currentTurnColor && it.id == rollResult.autoMoveTokenId }
                            if (tokenToMove != null) {
                                moveToken(tokenToMove)
                            }
                        } else {
                            turnPhase = TurnPhase.SELECT_PIECE
                            center3DTextHeadline = "CHOOSE PIECE TO MOVE"
                        }
                    } else {
                        turnPhase = TurnPhase.SELECT_PIECE
                        delay(750)
                        triggerAutomatedMove(curPlayer ?: return@launch)
                    }
                }
                is RollResult.IllegalRoll -> {
                    // Ignored to prevent corrupt concurrent roll
                }
            }
        }
    }

    private fun calculateMovableTokens() {
        movableTokenIds.clear()
        movableTokenIds.addAll(gameStateManager.movableTokenIds)
    }

    fun moveToken(token: LudoToken) {
        if (!gameStateManager.canMoveToken(token.color, token.id)) return
        if (isOnlineRoomMatch && token.color != myOnlineColor) return

        movableTokenIds.clear()
        hasRolled = false
        turnPhase = TurnPhase.PIECE_MOVING
        movingTokenColor = token.color
        movingTokenId = token.id

        viewModelScope.launch {
            val tokenIndex = tokens.indexOfFirst { it.color == token.color && it.id == token.id }
            if (tokenIndex == -1) {
                movingTokenColor = null
                movingTokenId = null
                advanceTurn()
                return@launch
            }

            val targetToken = tokens[tokenIndex]
            val wasInYard = targetToken.state == TokenState.YARD

            // Execute authoritative rule-based move via GameStateManager
            val moveResult = gameStateManager.executeMove(
                color = token.color,
                tokenId = token.id,
                onIntermediateStep = { intermediateTok, step ->
                    if (wasInYard) {
                        LudoSoundManager.playPieceUnlocked()
                        triggerPieceUnlockedEffect(token.color)
                        val idx = tokens.indexOfFirst { it.color == token.color && it.id == token.id }
                        if (idx != -1) {
                            tokens[idx] = tokens[idx].copy(
                                state = step.first,
                                stepCounter = step.second
                            )
                        }
                        delay(350)
                    } else {
                        LudoSoundManager.playPieceStep()
                        val idx = tokens.indexOfFirst { it.color == token.color && it.id == token.id }
                        if (idx != -1) {
                            tokens[idx] = tokens[idx].copy(
                                state = step.first,
                                stepCounter = step.second
                            )
                        }
                        delay(160)
                    }
                }
            )

            when (moveResult) {
                is MoveExecutionResult.Success -> {
                    // Sync authoritative tokens from GameStateManager
                    tokens.clear()
                    tokens.addAll(gameStateManager.tokens)
                    movingTokenColor = null
                    movingTokenId = null

                    val wasCapture = moveResult.moveResult.capturedTokens.isNotEmpty()
                    val reachedGoal = moveResult.moveResult.reachedGoal

                    // Captures
                    if (wasCapture) {
                        LudoSoundManager.playPieceCapture()
                        val captured = moveResult.moveResult.capturedTokens.first()
                        triggerCaptureEffect(token.color, captured.color)
                        triggerCaptureSparks(getTokenGridPosition(tokens[tokenIndex]))
                    }

                    // Goal
                    if (reachedGoal) {
                        LudoSoundManager.playGoalScored()
                        triggerGoalEffect(token.color)
                    }

                    // Check Grand Victory
                    if (moveResult.isVictory) {
                        LudoSoundManager.playGrandVictory()
                        triggerVictoryCelebration()

                        if (isOnlineRoomMatch) {
                            activeRoomCode?.let { code ->
                                firebaseRepo.syncOnlineTokenMove(
                                    roomId = code,
                                    updatedTokens = gameStateManager.tokens,
                                    nextTurnColor = currentTurnColor,
                                    nextPhase = TurnPhase.GAME_OVER.name,
                                    winnerColor = currentTurnColor,
                                    winnerName = username,
                                    lastMoveAction = RoomMoveAction(
                                        playerColor = token.color.name,
                                        tokenId = token.id,
                                        diceValue = diceValue,
                                        wasCapture = wasCapture,
                                        reachedGoal = reachedGoal,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                        return@launch
                    }

                    val grantsExtra = moveResult.grantsExtraTurn
                    val nextColor = if (grantsExtra) currentTurnColor else moveResult.nextTurnColor

                    if (isOnlineRoomMatch) {
                        activeRoomCode?.let { code ->
                            firebaseRepo.syncOnlineTokenMove(
                                roomId = code,
                                updatedTokens = gameStateManager.tokens,
                                nextTurnColor = nextColor,
                                nextPhase = TurnPhase.ROLL_DICE.name,
                                lastMoveAction = RoomMoveAction(
                                    playerColor = token.color.name,
                                    tokenId = token.id,
                                    diceValue = diceValue,
                                    wasCapture = wasCapture,
                                    reachedGoal = reachedGoal,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    // Bonus turn or advance turn
                    if (grantsExtra) {
                        val reason = moveResult.bonusReason ?: "Bonus Roll!"
                        bonusTurnReason = reason
                        turnPhase = TurnPhase.EXTRA_TURN
                        center3DTextHeadline = "BONUS TURN!"
                        turnHistory.add("Turn $turnNumber: ${getActivePlayer()?.name} earned bonus turn ($reason)")
                        if (turnHistory.size > 25) turnHistory.removeAt(0)

                        hasRolled = false
                        val activeP = getActivePlayer()
                        val isHuman = if (isOnlineRoomMatch) (currentTurnColor == myOnlineColor) else (activeP != null && activeP.type == PlayerType.LOCAL_HUMAN)
                        if (!isHuman && activeP != null) {
                            delay(1000)
                            rollDice()
                        }
                    } else {
                        advanceTurnVisuals(moveResult.nextTurnColor)
                    }
                }
                is MoveExecutionResult.IllegalMove -> {
                    movingTokenColor = null
                    movingTokenId = null
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

    private fun advanceTurnVisuals(nextColor: PlayerColor) {
        currentTurnColor = nextColor
        hasRolled = false
        movableTokenIds.clear()
        bonusTurnReason = null
        turnPhase = TurnPhase.ROLL_DICE
        center3DTextHeadline = "ROYAL LUDO 3D"
        turnNumber = gameStateManager.turnNumber
        roundNumber = gameStateManager.roundNumber

        val nextPlayer = getActivePlayer()
        turnHistory.add("Round $roundNumber, Turn $turnNumber: ${nextPlayer?.name ?: currentTurnColor.displayName}'s turn")
        if (turnHistory.size > 25) turnHistory.removeAt(0)

        // Sync AI Turn trigger immediately ONLY if offline AI game! In online room matches, remote players roll!
        if (!isOnlineRoomMatch && nextPlayer != null && nextPlayer.type != PlayerType.LOCAL_HUMAN) {
            viewModelScope.launch {
                delay(1000)
                rollDice()
            }
        }
    }

    private fun advanceTurn() {
        viewModelScope.launch {
            val nextColor = gameStateManager.advanceTurn()
            if (isOnlineRoomMatch) {
                activeRoomCode?.let { code ->
                    firebaseRepo.syncOnlineAdvanceTurn(code, nextColor)
                }
            }
            advanceTurnVisuals(nextColor)
        }
    }

    private fun triggerAutomatedMove(player: LudoPlayer) {
        viewModelScope.launch {
            if (movableTokenIds.isEmpty()) {
                advanceTurn()
                return@launch
            }

            val bestId = gameStateManager.selectBestAiMove() ?: movableTokenIds.first()

            val matchedToken = tokens.firstOrNull { it.color == player.color && it.id == bestId }
            if (matchedToken != null) {
                // Periodical Online Chat Emoji simulations on AI turns!
                if (player.type == PlayerType.ONLINE_SIMULATED && Random.nextInt(0, 4) == 0) {
                    sendSimulatedEmoji(player, listOf("😎", "🥳", "🤔", "🎲", "🔥").random())
                }
                delay(300)
                moveToken(matchedToken)
            } else {
                advanceTurn()
            }
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
