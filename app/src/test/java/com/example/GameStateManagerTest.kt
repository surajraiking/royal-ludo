package com.example

import com.example.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GameStateManagerTest {

    private lateinit var stateManager: GameStateManager
    private val testPlayers = listOf(
        LudoPlayer(PlayerColor.RED, "Player 1", PlayerType.LOCAL_HUMAN, "avatar_1"),
        LudoPlayer(PlayerColor.GREEN, "Player 2", PlayerType.LOCAL_HUMAN, "avatar_2"),
        LudoPlayer(PlayerColor.YELLOW, "Player 3", PlayerType.LOCAL_HUMAN, "avatar_3"),
        LudoPlayer(PlayerColor.BLUE, "Player 4", PlayerType.LOCAL_HUMAN, "avatar_4")
    )

    @Before
    fun setUp() = runBlocking {
        stateManager = GameStateManager()
        stateManager.startNewGame(testPlayers, PlayerColor.RED)
    }

    @Test
    fun testInitialStateFollowsLudoRules() {
        val state = stateManager.currentState
        assertEquals(4, state.players.size)
        assertEquals(16, state.tokens.size)
        assertEquals(PlayerColor.RED, state.currentTurnColor)
        assertEquals(TurnPhase.ROLL_DICE, state.turnPhase)
        assertFalse(state.hasRolled)
        assertFalse(state.isRolling)
        assertFalse(state.isMovingPiece)
        assertFalse(state.isGameOver)
        assertEquals(0, state.consecutiveSixCount)
        assertTrue(state.movableTokenIds.isEmpty())

        // All 16 tokens should start in YARD with stepCounter = 0
        assertTrue(state.tokens.all { it.state == TokenState.YARD && it.stepCounter == 0 })
    }

    @Test
    fun testNonSixRollWhenAllInYardProducesNoMoves() = runBlocking {
        assertTrue(stateManager.canRollDice(PlayerColor.RED))
        assertFalse(stateManager.canRollDice(PlayerColor.GREEN)) // Wrong color

        val result = stateManager.rollDice(forcedValue = 3)
        assertTrue(result is RollResult.NoMovesAvailable)
        assertEquals(3, (result as RollResult.NoMovesAvailable).diceValue)
        assertTrue(stateManager.hasRolled)
        assertEquals(TurnPhase.NO_MOVES, stateManager.turnPhase)
        assertTrue(stateManager.movableTokenIds.isEmpty())

        // Cannot roll again without advancing turn
        val duplicateRoll = stateManager.rollDice(forcedValue = 4)
        assertTrue(duplicateRoll is RollResult.IllegalRoll)

        // Advance turn should move to GREEN
        val nextColor = stateManager.advanceTurn()
        assertEquals(PlayerColor.GREEN, nextColor)
        assertEquals(PlayerColor.GREEN, stateManager.currentTurnColor)
        assertEquals(TurnPhase.ROLL_DICE, stateManager.turnPhase)
        assertFalse(stateManager.hasRolled)
    }

    @Test
    fun testRollingSixDeploysYardTokenAndAwardsBonusTurn() = runBlocking {
        val rollResult = stateManager.rollDice(forcedValue = 6)
        assertTrue(rollResult is RollResult.Success)
        val success = rollResult as RollResult.Success
        assertEquals(6, success.diceValue)
        assertEquals(listOf(0, 1, 2, 3), success.legalMoves)
        assertEquals(1, success.consecutiveSixes)
        assertEquals(TurnPhase.SELECT_PIECE, stateManager.turnPhase)

        // Illegal move: wrong player
        val wrongPlayerMove = stateManager.executeMove(PlayerColor.GREEN, 0)
        assertTrue(wrongPlayerMove is MoveExecutionResult.IllegalMove)

        // Illegal move: non-existent token id
        val invalidIdMove = stateManager.executeMove(PlayerColor.RED, 99)
        assertTrue(invalidIdMove is MoveExecutionResult.IllegalMove)

        // Legal move: token 0 of RED
        val moveResult = stateManager.executeMove(PlayerColor.RED, 0)
        assertTrue(moveResult is MoveExecutionResult.Success)
        val moveSuccess = moveResult as MoveExecutionResult.Success

        // Token 0 should now be on TRACK at step 0
        val movedTok = stateManager.tokens.first { it.color == PlayerColor.RED && it.id == 0 }
        assertEquals(TokenState.TRACK, movedTok.state)
        assertEquals(0, movedTok.stepCounter)

        // Rolling 6 grants extra turn
        assertTrue(moveSuccess.grantsExtraTurn)
        assertEquals(PlayerColor.RED, moveSuccess.nextTurnColor)
        assertEquals(TurnPhase.EXTRA_TURN, stateManager.turnPhase)
        assertFalse(stateManager.hasRolled)
    }

    @Test
    fun testThreeConsecutiveSixesPenalizesAndForfeitsTurn() = runBlocking {
        // Roll 1: 6
        val roll1 = stateManager.rollDice(forcedValue = 6)
        assertTrue(roll1 is RollResult.Success)
        assertEquals(1, stateManager.consecutiveSixCount)
        stateManager.executeMove(PlayerColor.RED, 0)

        // Roll 2: 6
        val roll2 = stateManager.rollDice(forcedValue = 6)
        assertTrue(roll2 is RollResult.Success)
        assertEquals(2, stateManager.consecutiveSixCount)
        stateManager.executeMove(PlayerColor.RED, 1)

        // Roll 3: 6 -> 3 consecutive sixes penalty!
        val roll3 = stateManager.rollDice(forcedValue = 6)
        assertTrue(roll3 is RollResult.TurnForfeitedThreeSixes)
        val penalty = roll3 as RollResult.TurnForfeitedThreeSixes
        assertEquals(PlayerColor.GREEN, penalty.nextTurnColor)
        assertEquals(0, stateManager.consecutiveSixCount)
        assertEquals(TurnPhase.NO_MOVES, stateManager.turnPhase)

        // Cannot move any tokens on forfeiture
        assertFalse(stateManager.canMoveToken(PlayerColor.RED, 0))
    }

    @Test
    fun testOpponentCaptureKnocksBackToYardAndAwardsBonusTurn() = runBlocking {
        // Deploy RED token 0 to TRACK (step 0)
        stateManager.rollDice(forcedValue = 6)
        stateManager.executeMove(PlayerColor.RED, 0)

        // Move RED token 0 forward 4 steps (step 4)
        stateManager.rollDice(forcedValue = 4)
        stateManager.executeMove(PlayerColor.RED, 0)

        // RED token 0 is now at step 4. Absolute index = 0 + 4 = 4 (non-safe cell)
        val redTok = stateManager.tokens.first { it.color == PlayerColor.RED && it.id == 0 }
        assertEquals(4, redTok.stepCounter)
        assertEquals(TokenState.TRACK, redTok.state)

        // After moving 4 steps, RED's turn ended without bonus -> turn automatically advanced to GREEN!
        assertEquals(PlayerColor.GREEN, stateManager.currentTurnColor)

        // Deploy GREEN token 0 and set its position right before RED
        stateManager.rollDice(forcedValue = 6)
        stateManager.executeMove(PlayerColor.GREEN, 0)

        // Position GREEN token 0 at step 40
        stateManager.setIntermediateTokenStep(PlayerColor.GREEN, 0, TokenState.TRACK, 40)

        // Roll a 3 for GREEN: target step = 43 -> lands on absolute cell 4!
        val greenRoll = stateManager.rollDice(forcedValue = 3)
        assertTrue(greenRoll is RollResult.Success)

        val greenMove = stateManager.executeMove(PlayerColor.GREEN, 0)
        assertTrue(greenMove is MoveExecutionResult.Success)
        val greenSuccess = greenMove as MoveExecutionResult.Success

        // Verify RED token was captured and returned to YARD!
        val capturedRed = stateManager.tokens.first { it.color == PlayerColor.RED && it.id == 0 }
        assertEquals(TokenState.YARD, capturedRed.state)
        assertEquals(0, capturedRed.stepCounter)

        // Verify GREEN received bonus turn for capture!
        assertTrue(greenSuccess.grantsExtraTurn)
        assertEquals(1, greenSuccess.moveResult.capturedTokens.size)
        assertEquals(PlayerColor.GREEN, stateManager.currentTurnColor)
    }

    @Test
    fun testVictoryWhenAllTokensReachGoal() = runBlocking {
        // Position 3 of RED's tokens already in GOAL, and 1 at step 55 (1 step away from GOAL)
        stateManager.setIntermediateTokenStep(PlayerColor.RED, 0, TokenState.GOAL, 56)
        stateManager.setIntermediateTokenStep(PlayerColor.RED, 1, TokenState.GOAL, 56)
        stateManager.setIntermediateTokenStep(PlayerColor.RED, 2, TokenState.GOAL, 56)
        stateManager.setIntermediateTokenStep(PlayerColor.RED, 3, TokenState.HOME_STRETCH, 55)

        // Roll 1 for RED
        val roll = stateManager.rollDice(forcedValue = 1)
        assertTrue(roll is RollResult.Success)
        assertTrue(stateManager.movableTokenIds.contains(3))

        val move = stateManager.executeMove(PlayerColor.RED, 3)
        assertTrue(move is MoveExecutionResult.Success)
        val success = move as MoveExecutionResult.Success

        assertTrue(success.isVictory)
        assertEquals("Player 1", success.winner?.name)
        assertEquals(TurnPhase.GAME_OVER, stateManager.turnPhase)
        assertTrue(stateManager.isGameOver)

        // Further actions should be rejected
        assertFalse(stateManager.canRollDice(PlayerColor.RED))
        val afterOverRoll = stateManager.rollDice(forcedValue = 6)
        assertTrue(afterOverRoll is RollResult.IllegalRoll)
    }
}
