package com.example.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

/**
 * Immutable snapshot representing the authoritative game state at any moment.
 */
data class LudoGameState(
    val players: List<LudoPlayer> = emptyList(),
    val tokens: List<LudoToken> = emptyList(),
    val currentTurnColor: PlayerColor = PlayerColor.RED,
    val turnPhase: TurnPhase = TurnPhase.ROLL_DICE,
    val diceValue: Int = 1,
    val hasRolled: Boolean = false,
    val isRolling: Boolean = false,
    val isMovingPiece: Boolean = false,
    val consecutiveSixCount: Int = 0,
    val movableTokenIds: List<Int> = emptyList(),
    val turnNumber: Int = 1,
    val roundNumber: Int = 1,
    val winner: LudoPlayer? = null,
    val bonusReason: String? = null,
    val movingTokenId: Int? = null,
    val movingTokenColor: PlayerColor? = null,
    val turnHistory: List<String> = emptyList()
) {
    val isGameOver: Boolean get() = winner != null || turnPhase == TurnPhase.GAME_OVER

    fun getActivePlayer(): LudoPlayer? = players.firstOrNull { it.color == currentTurnColor }

    fun getTokensForPlayer(color: PlayerColor): List<LudoToken> = tokens.filter { it.color == color }

    fun isLegalMove(tokenId: Int): Boolean = movableTokenIds.contains(tokenId)
}

/**
 * Result data class for dice rolls executed through GameStateManager.
 */
sealed class RollResult {
    data class Success(
        val diceValue: Int,
        val legalMoves: List<Int>,
        val isThreeSixesPenalty: Boolean,
        val consecutiveSixes: Int,
        val autoMoveTokenId: Int? = null
    ) : RollResult()

    data class TurnForfeitedThreeSixes(
        val diceValue: Int,
        val nextTurnColor: PlayerColor
    ) : RollResult()

    data class NoMovesAvailable(
        val diceValue: Int
    ) : RollResult()

    data class IllegalRoll(
        val reason: String
    ) : RollResult()
}

/**
 * Result data class for token movements executed through GameStateManager.
 */
sealed class MoveExecutionResult {
    data class Success(
        val moveResult: MoveResult,
        val isVictory: Boolean,
        val winner: LudoPlayer?,
        val grantsExtraTurn: Boolean,
        val bonusReason: String?,
        val nextTurnColor: PlayerColor
    ) : MoveExecutionResult()

    data class IllegalMove(
        val reason: String
    ) : MoveExecutionResult()
}

/**
 * Authoritative GameStateManager that manages the authoritative state of the game
 * (positions, turns, dice, tokens) and ensures state transitions strictly adhere to
 * official Ludo rules, preventing concurrent moves, double rolls, or illegal state updates.
 */
class GameStateManager {

    private val mutex = Mutex()

    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<LudoGameState> = _state.asStateFlow()

    // Convenience read-only accessors
    val currentState: LudoGameState get() = _state.value
    val players: List<LudoPlayer> get() = _state.value.players
    val tokens: List<LudoToken> get() = _state.value.tokens
    val currentTurnColor: PlayerColor get() = _state.value.currentTurnColor
    val turnPhase: TurnPhase get() = _state.value.turnPhase
    val diceValue: Int get() = _state.value.diceValue
    val hasRolled: Boolean get() = _state.value.hasRolled
    val isRolling: Boolean get() = _state.value.isRolling
    val isMovingPiece: Boolean get() = _state.value.isMovingPiece
    val movableTokenIds: List<Int> get() = _state.value.movableTokenIds
    val winner: LudoPlayer? get() = _state.value.winner
    val turnNumber: Int get() = _state.value.turnNumber
    val roundNumber: Int get() = _state.value.roundNumber
    val bonusReason: String? get() = _state.value.bonusReason
    val consecutiveSixCount: Int get() = _state.value.consecutiveSixCount
    val isGameOver: Boolean get() = _state.value.isGameOver

    companion object {
        fun createDefaultTokens(): List<LudoToken> {
            val list = mutableListOf<LudoToken>()
            for (col in PlayerColor.values()) {
                for (id in 0..3) {
                    list.add(LudoToken(id = id, color = col, state = TokenState.YARD, stepCounter = 0))
                }
            }
            return list
        }

        fun createInitialState(): LudoGameState {
            return LudoGameState(
                players = emptyList(),
                tokens = createDefaultTokens(),
                currentTurnColor = PlayerColor.RED,
                turnPhase = TurnPhase.ROLL_DICE,
                diceValue = 1,
                hasRolled = false,
                isRolling = false,
                isMovingPiece = false,
                consecutiveSixCount = 0,
                movableTokenIds = emptyList(),
                turnNumber = 1,
                roundNumber = 1,
                winner = null,
                bonusReason = null,
                movingTokenId = null,
                movingTokenColor = null,
                turnHistory = emptyList()
            )
        }
    }

    /**
     * Initializes or resets the game board with a given set of players.
     */
    suspend fun startNewGame(
        newPlayers: List<LudoPlayer>,
        startingColor: PlayerColor = PlayerColor.RED
    ) = mutex.withLock {
        val initialTokens = createDefaultTokens()
        _state.value = LudoGameState(
            players = newPlayers,
            tokens = initialTokens,
            currentTurnColor = startingColor,
            turnPhase = TurnPhase.ROLL_DICE,
            diceValue = 1,
            hasRolled = false,
            isRolling = false,
            isMovingPiece = false,
            consecutiveSixCount = 0,
            movableTokenIds = emptyList(),
            turnNumber = 1,
            roundNumber = 1,
            winner = null,
            bonusReason = null,
            movingTokenId = null,
            movingTokenColor = null,
            turnHistory = listOf("Game started with ${newPlayers.size} players. First turn: ${startingColor.displayName}")
        )
    }

    /**
     * Synchronous reset without coroutines.
     */
    fun resetSync() {
        _state.value = createInitialState()
    }

    /**
     * Verifies if a dice roll is currently legal for the specified player color.
     */
    fun canRollDice(playerColor: PlayerColor): Boolean {
        val current = _state.value
        if (current.isGameOver) return false
        if (current.isRolling || current.isMovingPiece) return false
        if (current.hasRolled) return false
        if (current.currentTurnColor != playerColor) return false
        return current.turnPhase == TurnPhase.ROLL_DICE || current.turnPhase == TurnPhase.EXTRA_TURN
    }

    /**
     * Verifies if a token move is currently legal.
     */
    fun canMoveToken(color: PlayerColor, tokenId: Int): Boolean {
        val current = _state.value
        if (current.isGameOver) return false
        if (current.isRolling || current.isMovingPiece) return false
        if (!current.hasRolled) return false
        if (current.currentTurnColor != color) return false
        if (current.turnPhase != TurnPhase.SELECT_PIECE) return false
        return current.movableTokenIds.contains(tokenId)
    }

    /**
     * Authoritatively executes a dice roll with rule checks and concurrency locks.
     * [forcedValue] allows deterministic rolls for testing or special game rules.
     */
    suspend fun rollDice(forcedValue: Int? = null): RollResult = mutex.withLock {
        val current = _state.value

        if (current.isGameOver) {
            return@withLock RollResult.IllegalRoll("Game is already over. Winner: ${current.winner?.name}")
        }
        if (current.isRolling) {
            return@withLock RollResult.IllegalRoll("A roll animation is already active")
        }
        if (current.isMovingPiece) {
            return@withLock RollResult.IllegalRoll("Cannot roll while a piece is moving")
        }
        if (current.hasRolled) {
            return@withLock RollResult.IllegalRoll("Player has already rolled this turn")
        }
        if (current.turnPhase != TurnPhase.ROLL_DICE && current.turnPhase != TurnPhase.EXTRA_TURN) {
            return@withLock RollResult.IllegalRoll("Illegal turn phase for rolling: ${current.turnPhase}")
        }

        // Set rolling state
        _state.value = current.copy(isRolling = true, bonusReason = null)

        val roll = forcedValue ?: Random.nextInt(1, 7)

        // Check 3 consecutive sixes rule
        val (newSixCount, isThreeSixesPenalty) = LudoEngine.handleConsecutiveSix(current.consecutiveSixCount, roll)

        if (isThreeSixesPenalty) {
            val nextColor = LudoEngine.getNextTurnColor(current.currentTurnColor, current.players)
            val updatedHistory = current.turnHistory.toMutableList().apply {
                add("Turn ${current.turnNumber}: ${current.getActivePlayer()?.name ?: current.currentTurnColor.displayName} rolled 3 consecutive 6s! Turn forfeited.")
                if (size > 30) removeAt(0)
            }

            _state.value = current.copy(
                diceValue = roll,
                hasRolled = true,
                isRolling = false,
                consecutiveSixCount = 0,
                turnPhase = TurnPhase.NO_MOVES,
                movableTokenIds = emptyList(),
                turnHistory = updatedHistory
            )

            return@withLock RollResult.TurnForfeitedThreeSixes(roll, nextColor)
        }

        // Compute legal moves
        val legalMoves = LudoEngine.getLegalMoves(current.currentTurnColor, roll, current.tokens)

        if (legalMoves.isEmpty()) {
            val updatedHistory = current.turnHistory.toMutableList().apply {
                add("Turn ${current.turnNumber}: ${current.getActivePlayer()?.name ?: current.currentTurnColor.displayName} rolled $roll - No moves available.")
                if (size > 30) removeAt(0)
            }

            _state.value = current.copy(
                diceValue = roll,
                hasRolled = true,
                isRolling = false,
                consecutiveSixCount = newSixCount,
                turnPhase = TurnPhase.NO_MOVES,
                movableTokenIds = emptyList(),
                turnHistory = updatedHistory
            )

            return@withLock RollResult.NoMovesAvailable(roll)
        } else {
            // Determine auto-move candidate:
            // Single legal move OR all legal moves are in YARD (all identical deployment)
            val movableTokens = current.tokens.filter { it.color == current.currentTurnColor && legalMoves.contains(it.id) }
            val allInYard = movableTokens.isNotEmpty() && movableTokens.all { it.state == TokenState.YARD }
            val autoMoveId = if (legalMoves.size == 1 || allInYard) movableTokens.first().id else null

            _state.value = current.copy(
                diceValue = roll,
                hasRolled = true,
                isRolling = false,
                consecutiveSixCount = newSixCount,
                turnPhase = TurnPhase.SELECT_PIECE,
                movableTokenIds = legalMoves
            )

            return@withLock RollResult.Success(
                diceValue = roll,
                legalMoves = legalMoves,
                isThreeSixesPenalty = false,
                consecutiveSixes = newSixCount,
                autoMoveTokenId = autoMoveId
            )
        }
    }

    /**
     * Authoritatively executes a token move, enforces Ludo rules, updates token positions,
     * checks for captures, checks for victory, awards extra turns, or advances to next player.
     * Prevents concurrent moves via mutex locking.
     */
    suspend fun executeMove(
        color: PlayerColor,
        tokenId: Int,
        onIntermediateStep: (suspend (token: LudoToken, step: Pair<TokenState, Int>) -> Unit)? = null
    ): MoveExecutionResult = mutex.withLock {
        val current = _state.value

        if (current.isGameOver) {
            return@withLock MoveExecutionResult.IllegalMove("Game is already over")
        }
        if (current.isMovingPiece) {
            return@withLock MoveExecutionResult.IllegalMove("Another piece is already moving")
        }
        if (!current.hasRolled) {
            return@withLock MoveExecutionResult.IllegalMove("Cannot move before rolling dice")
        }
        if (current.currentTurnColor != color) {
            return@withLock MoveExecutionResult.IllegalMove("Not $color's turn (active: ${current.currentTurnColor})")
        }
        if (current.turnPhase != TurnPhase.SELECT_PIECE) {
            return@withLock MoveExecutionResult.IllegalMove("Illegal turn phase for moving: ${current.turnPhase}")
        }
        if (!current.movableTokenIds.contains(tokenId)) {
            return@withLock MoveExecutionResult.IllegalMove("Token $tokenId is not a legal move for roll ${current.diceValue}")
        }

        val tokenIndex = current.tokens.indexOfFirst { it.color == color && it.id == tokenId }
        if (tokenIndex == -1) {
            return@withLock MoveExecutionResult.IllegalMove("Token $color #$tokenId not found")
        }

        // Lock in moving state
        _state.value = current.copy(
            isMovingPiece = true,
            turnPhase = TurnPhase.PIECE_MOVING,
            movingTokenId = tokenId,
            movingTokenColor = color,
            movableTokenIds = emptyList()
        )

        val workingTokens = current.tokens.map { it.copy() }.toMutableList()
        val targetToken = workingTokens[tokenIndex]
        val wasInYard = targetToken.state == TokenState.YARD

        // Execute deterministic rule-based move
        val moveResult = LudoEngine.applyMove(targetToken, current.diceValue, workingTokens)

        // Invoke intermediate stepping animations if requested
        if (onIntermediateStep != null) {
            if (wasInYard) {
                onIntermediateStep(targetToken, Pair(TokenState.TRACK, 0))
            } else {
                for (step in moveResult.intermediateSteps) {
                    onIntermediateStep(targetToken, step)
                }
            }
        }

        // Apply final state to working tokens
        workingTokens[tokenIndex] = moveResult.movedToken.copy()

        // Check Victory Condition
        val isVictory = LudoEngine.checkVictory(color, workingTokens)
        if (isVictory) {
            val winnerPlayer = current.getActivePlayer()
            val updatedHistory = current.turnHistory.toMutableList().apply {
                add("🏆 Game Over! ${winnerPlayer?.name ?: color.displayName} has won the match!")
                if (size > 30) removeAt(0)
            }

            _state.value = current.copy(
                tokens = workingTokens,
                turnPhase = TurnPhase.GAME_OVER,
                winner = winnerPlayer,
                isMovingPiece = false,
                movingTokenId = null,
                movingTokenColor = null,
                hasRolled = false,
                turnHistory = updatedHistory
            )

            return@withLock MoveExecutionResult.Success(
                moveResult = moveResult,
                isVictory = true,
                winner = winnerPlayer,
                grantsExtraTurn = false,
                bonusReason = null,
                nextTurnColor = color
            )
        }

        // If move grants extra turn (rolled 6, captured enemy, or reached goal)
        if (moveResult.grantsExtraTurn) {
            val reason = when {
                moveResult.capturedTokens.isNotEmpty() -> "Enemy captured! ⚔️ Bonus Roll!"
                moveResult.reachedGoal -> "Goal reached! 🏆 Bonus Roll!"
                current.diceValue == 6 -> "Rolled a 6! 🎲 Bonus Roll!"
                else -> "Bonus Roll!"
            }

            val updatedHistory = current.turnHistory.toMutableList().apply {
                add("Turn ${current.turnNumber}: ${current.getActivePlayer()?.name ?: color.displayName} earned bonus turn ($reason)")
                if (size > 30) removeAt(0)
            }

            _state.value = current.copy(
                tokens = workingTokens,
                turnPhase = TurnPhase.EXTRA_TURN,
                bonusReason = reason,
                hasRolled = false,
                isMovingPiece = false,
                movingTokenId = null,
                movingTokenColor = null,
                turnHistory = updatedHistory
            )

            return@withLock MoveExecutionResult.Success(
                moveResult = moveResult,
                isVictory = false,
                winner = null,
                grantsExtraTurn = true,
                bonusReason = reason,
                nextTurnColor = color
            )
        } else {
            // Turn advances to next player in active players rotation
            val nextColor = LudoEngine.getNextTurnColor(color, current.players)
            val newTurnNumber = current.turnNumber + 1
            val activeCount = if (current.players.isEmpty()) 4 else current.players.size
            val newRoundNumber = ((newTurnNumber - 1) / activeCount) + 1

            val nextPlayer = current.players.firstOrNull { it.color == nextColor }
            val updatedHistory = current.turnHistory.toMutableList().apply {
                add("Round $newRoundNumber, Turn $newTurnNumber: ${nextPlayer?.name ?: nextColor.displayName}'s turn")
                if (size > 30) removeAt(0)
            }

            _state.value = current.copy(
                tokens = workingTokens,
                currentTurnColor = nextColor,
                turnPhase = TurnPhase.ROLL_DICE,
                hasRolled = false,
                isMovingPiece = false,
                movingTokenId = null,
                movingTokenColor = null,
                consecutiveSixCount = 0,
                turnNumber = newTurnNumber,
                roundNumber = newRoundNumber,
                bonusReason = null,
                turnHistory = updatedHistory
            )

            return@withLock MoveExecutionResult.Success(
                moveResult = moveResult,
                isVictory = false,
                winner = null,
                grantsExtraTurn = false,
                bonusReason = null,
                nextTurnColor = nextColor
            )
        }
    }

    /**
     * Manually advances the turn to the next player (used when no moves are possible or turn passes).
     */
    suspend fun advanceTurn(): PlayerColor = mutex.withLock {
        val current = _state.value
        val nextColor = LudoEngine.getNextTurnColor(current.currentTurnColor, current.players)
        val newTurnNumber = current.turnNumber + 1
        val activeCount = if (current.players.isEmpty()) 4 else current.players.size
        val newRoundNumber = ((newTurnNumber - 1) / activeCount) + 1

        val nextPlayer = current.players.firstOrNull { it.color == nextColor }
        val updatedHistory = current.turnHistory.toMutableList().apply {
            add("Round $newRoundNumber, Turn $newTurnNumber: ${nextPlayer?.name ?: nextColor.displayName}'s turn")
            if (size > 30) removeAt(0)
        }

        _state.value = current.copy(
            currentTurnColor = nextColor,
            turnPhase = TurnPhase.ROLL_DICE,
            hasRolled = false,
            isRolling = false,
            isMovingPiece = false,
            movableTokenIds = emptyList(),
            movingTokenId = null,
            movingTokenColor = null,
            consecutiveSixCount = 0,
            turnNumber = newTurnNumber,
            roundNumber = newRoundNumber,
            bonusReason = null,
            turnHistory = updatedHistory
        )

        return@withLock nextColor
    }

    /**
     * Allows AI or automated player to select the best legal move using the LudoEngine heuristics.
     */
    fun selectBestAiMove(): Int? {
        val current = _state.value
        if (current.movableTokenIds.isEmpty()) return null
        return LudoEngine.selectBestAiMove(
            playerColor = current.currentTurnColor,
            diceValue = current.diceValue,
            legalTokenIds = current.movableTokenIds,
            allTokens = current.tokens
        )
    }

    /**
     * Updates an intermediate token state during step-by-step UI animation.
     */
    suspend fun setIntermediateTokenStep(color: PlayerColor, tokenId: Int, state: TokenState, stepCounter: Int) = mutex.withLock {
        val current = _state.value
        val list = current.tokens.toMutableList()
        val idx = list.indexOfFirst { it.color == color && it.id == tokenId }
        if (idx != -1) {
            list[idx] = list[idx].copy(state = state, stepCounter = stepCounter)
            _state.value = current.copy(tokens = list)
        }
    }
}
