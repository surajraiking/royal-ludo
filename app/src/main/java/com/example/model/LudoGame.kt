package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.LudoBlueVibrant
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoRed
import com.example.ui.theme.LudoYellow
import kotlin.random.Random

enum class PlayerColor(val displayName: String, val color: Color) {
    RED("Royal Red", LudoRed),
    GREEN("Emerald Green", LudoGreen),
    YELLOW("Gold Yellow", LudoYellow),
    BLUE("Neon Blue", LudoBlueVibrant)
}

enum class PlayerType {
    LOCAL_HUMAN,
    AI_EASY,
    AI_MEDIUM,
    AI_HARD,
    ONLINE_SIMULATED
}

data class LudoPlayer(
    val color: PlayerColor,
    val name: String,
    val type: PlayerType,
    val avatarId: String,
    var isReady: Boolean = true,
    var wins: Int = 0
)

/**
 * Three fundamental states of a Ludo piece as requested: BASE, PATH, GOAL.
 */
enum class PieceState(val label: String, val iconEmoji: String) {
    BASE("Base", "🏠"),
    PATH("Path", "🛣️"),
    GOAL("Goal", "🏆")
}

enum class TokenState {
    YARD,
    TRACK,
    HOME_STRETCH,
    GOAL
}

data class LudoToken(
    val id: Int, // 0..3
    val color: PlayerColor,
    var state: TokenState = TokenState.YARD,
    var stepCounter: Int = 0 // 0..50 on track, 51..55 on home stretch, 56 is Goal
) {
    val pieceState: PieceState
        get() = when (state) {
            TokenState.YARD -> PieceState.BASE
            TokenState.TRACK, TokenState.HOME_STRETCH -> PieceState.PATH
            TokenState.GOAL -> PieceState.GOAL
        }

    val isInBase: Boolean get() = pieceState == PieceState.BASE
    val isOnPath: Boolean get() = pieceState == PieceState.PATH
    val isInGoal: Boolean get() = pieceState == PieceState.GOAL

    val remainingSteps: Int get() = (56 - stepCounter).coerceAtLeast(0)
    val progressPercent: Float get() = (stepCounter / 56f).coerceIn(0f, 1f)
}

/**
 * Distinct phases within each player's game turn.
 */
enum class TurnPhase(val title: String, val instruction: String) {
    ROLL_DICE("Roll Dice", "Tap dice to roll"),
    SELECT_PIECE("Select Piece", "Tap glowing piece to move"),
    PIECE_MOVING("Moving...", "Piece advancing along path"),
    NO_MOVES("No Moves", "No moves possible, skipping turn"),
    EXTRA_TURN("Bonus Turn!", "Extra roll awarded!"),
    GAME_OVER("Game Over", "Match finished")
}

/**
 * Detailed real-time snapshot of the current game turn.
 */
data class GameTurnInfo(
    val roundNumber: Int = 1,
    val turnNumber: Int = 1,
    val activeColor: PlayerColor = PlayerColor.RED,
    val activePlayerName: String = "",
    val phase: TurnPhase = TurnPhase.ROLL_DICE,
    val diceRoll: Int? = null,
    val isHumanTurn: Boolean = true,
    val bonusReason: String? = null
)

/**
 * Comprehensive tracking of a player's position, piece distribution, and board progress.
 */
data class PlayerBoardPosition(
    val player: LudoPlayer,
    val color: PlayerColor,
    val pieces: List<LudoToken>,
    val piecesInBase: Int,
    val piecesOnPath: Int,
    val piecesInGoal: Int,
    val leadingStep: Int,
    val totalStepsWalked: Int,
    val progressPercent: Float
)

enum class GameState {
    IDLE,
    MATCHMAKING,
    PLAYING,
    GAME_OVER
}

// Global Ludo Board Coordinates (15x15 grid)
val ludoCommonPath = listOf(
    Pair(6, 1), Pair(6, 2), Pair(6, 3), Pair(6, 4), Pair(6, 5),   // Left arm bottom row (going right)
    Pair(5, 6), Pair(4, 6), Pair(3, 6), Pair(2, 6), Pair(1, 6), Pair(0, 6), // Top arm left row (going up)
    Pair(0, 7),                                                 // Top center
    Pair(0, 8), Pair(1, 8), Pair(2, 8), Pair(3, 8), Pair(4, 8), Pair(5, 8), // Top arm right row (going down)
    Pair(6, 9), Pair(6, 10), Pair(6, 11), Pair(6, 12), Pair(6, 13), Pair(6, 14), // Right arm top row (going right)
    Pair(7, 14),                                                // Right center
    Pair(8, 14), Pair(8, 13), Pair(8, 12), Pair(8, 11), Pair(8, 10), Pair(8, 9), // Right arm bottom row (going left)
    Pair(9, 8), Pair(10, 8), Pair(11, 8), Pair(12, 8), Pair(13, 8), Pair(14, 8), // Bottom arm right row (going down)
    Pair(14, 7),                                                // Bottom center
    Pair(14, 6), Pair(13, 6), Pair(12, 6), Pair(11, 6), Pair(10, 6), Pair(9, 6), // Bottom arm left row (going up)
    Pair(8, 5), Pair(8, 4), Pair(8, 3), Pair(8, 2), Pair(8, 1), Pair(8, 0),    // Left arm bottom row (going left)
    Pair(7, 0),                                                 // Left center
    Pair(6, 0)                                                  // Left arm top start corner
)

// Safe cells by absolute path index
val ludoSafeIndices = setOf(0, 8, 13, 21, 26, 34, 39, 47)

// Home stretch relative to index
val redHomePath = listOf(Pair(7, 1), Pair(7, 2), Pair(7, 3), Pair(7, 4), Pair(7, 5))
val greenHomePath = listOf(Pair(1, 7), Pair(2, 7), Pair(3, 7), Pair(4, 7), Pair(5, 7))
val yellowHomePath = listOf(Pair(7, 13), Pair(7, 12), Pair(7, 11), Pair(7, 10), Pair(7, 9))
val blueHomePath = listOf(Pair(13, 7), Pair(12, 7), Pair(11, 7), Pair(10, 7), Pair(9, 7))

val ludoGoalPoint = Pair(7, 7)

// Yard piece display positions offset per color
fun getYardPositions(color: PlayerColor): List<Pair<Int, Int>> {
    return when (color) {
        PlayerColor.RED -> listOf(Pair(2, 2), Pair(2, 3), Pair(3, 2), Pair(3, 3))
        PlayerColor.GREEN -> listOf(Pair(2, 11), Pair(2, 12), Pair(3, 11), Pair(3, 12))
        PlayerColor.YELLOW -> listOf(Pair(11, 11), Pair(11, 12), Pair(12, 11), Pair(12, 12))
        PlayerColor.BLUE -> listOf(Pair(11, 2), Pair(11, 3), Pair(12, 2), Pair(12, 3))
    }
}

// Starting index on ludoCommonPath
fun getStartTrackIndex(color: PlayerColor): Int {
    return when (color) {
        PlayerColor.RED -> 0
        PlayerColor.GREEN -> 13
        PlayerColor.YELLOW -> 26
        PlayerColor.BLUE -> 39
    }
}

// Track index where home stretch starts
fun getHomeStretchStartIndex(color: PlayerColor): Int {
    return when (color) {
        PlayerColor.RED -> 50 // stepCounter 50 completes loop, enters stretch at 51
        PlayerColor.GREEN -> 50
        PlayerColor.YELLOW -> 50
        PlayerColor.BLUE -> 50
    }
}

fun getTokenGridPosition(token: LudoToken): Pair<Int, Int> {
    return when (token.state) {
        TokenState.YARD -> {
            getYardPositions(token.color)[token.id]
        }
        TokenState.TRACK -> {
            val startIndex = getStartTrackIndex(token.color)
            val absoluteIndex = (startIndex + token.stepCounter) % 52
            ludoCommonPath[absoluteIndex]
        }
        TokenState.HOME_STRETCH -> {
            val relativeIndex = token.stepCounter - 51 // 0..4
            when (token.color) {
                PlayerColor.RED -> redHomePath[relativeIndex]
                PlayerColor.GREEN -> greenHomePath[relativeIndex]
                PlayerColor.YELLOW -> yellowHomePath[relativeIndex]
                PlayerColor.BLUE -> blueHomePath[relativeIndex]
            }
        }
        TokenState.GOAL -> ludoGoalPoint
    }
}

data class ChatMessage(
    val sender: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEmojiOnly: Boolean = false
)

data class Particle(
    val id: Int,
    val x: Float,
    val y: Float,
    val color: Color,
    val velocityX: Float,
    val velocityY: Float,
    var alpha: Float = 1f
)

/**
 * 3D Interactive game announcements and celebration effects
 */
enum class InteractiveEffectType {
    ROLLED_SIX,
    TOKEN_UNLOCKED,
    CAPTURE,
    GOAL_SCORED,
    EXTRA_TURN
}

data class Interactive3DEffect(
    val type: InteractiveEffectType,
    val headline: String,
    val detail: String,
    val color: PlayerColor,
    val icon: String,
    val timestamp: Long = System.currentTimeMillis()
)

