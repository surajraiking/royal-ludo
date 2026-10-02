package com.example.model

/**
 * Result data class returned when a token move is executed by the Ludo Engine.
 */
data class MoveResult(
    val movedToken: LudoToken,
    val previousState: TokenState,
    val previousStep: Int,
    val isUnlockedFromYard: Boolean,
    val capturedTokens: List<LudoToken>,
    val reachedGoal: Boolean,
    val grantsExtraTurn: Boolean,
    val intermediateSteps: List<Pair<TokenState, Int>>
)

/**
 * Pure, rule-based, deterministic Ludo Engine implementing official international rules:
 * - 2, 3, and 4 player modes
 * - Accurate square-by-square pathing & home stretch entry
 * - Yard deployment on 6 (consumed for deployment without overshooting)
 * - Safe cells protection (starting cells 0, 13, 26, 39 and star cells 8, 21, 34, 47)
 * - Exact finish rule (cannot overshoot step 56)
 * - Bonus turns on: roll 6, capturing an opponent, or reaching Goal
 * - Three consecutive 6s penalty (turn forfeit)
 * - Automatic single-move detection & intelligent AI move scoring
 */
object LudoEngine {

    const val MAX_STEPS = 56
    const val TRACK_LOOP_STEPS = 50
    const val HOME_STRETCH_START = 51

    /**
     * Determines which token IDs (0..3) of [playerColor] can legally move given [diceValue].
     */
    fun getLegalMoves(
        playerColor: PlayerColor,
        diceValue: Int,
        tokens: List<LudoToken>,
        blockRulesEnabled: Boolean = false
    ): List<Int> {
        val playerTokens = tokens.filter { it.color == playerColor }
        val legalIds = mutableListOf<Int>()

        for (token in playerTokens) {
            when (token.state) {
                TokenState.YARD -> {
                    // Only a roll of 6 can release a token from the yard
                    if (diceValue == 6) {
                        legalIds.add(token.id)
                    }
                }
                TokenState.TRACK -> {
                    // Must not exceed the exact goal (step 56)
                    val targetStep = token.stepCounter + diceValue
                    if (targetStep <= MAX_STEPS) {
                        if (!blockRulesEnabled || !isMoveBlocked(token, diceValue, tokens)) {
                            legalIds.add(token.id)
                        }
                    }
                }
                TokenState.HOME_STRETCH -> {
                    // Must not exceed the exact goal (step 56)
                    val targetStep = token.stepCounter + diceValue
                    if (targetStep <= MAX_STEPS) {
                        legalIds.add(token.id)
                    }
                }
                TokenState.GOAL -> {
                    // Finished tokens can never move
                }
            }
        }

        return legalIds
    }

    /**
     * Executes the move for [token] using [diceValue].
     * Computes intermediate stepping frames for realistic 3D hopping animations,
     * checks safe cells, performs captures, and checks goal attainment.
     */
    fun applyMove(
        token: LudoToken,
        diceValue: Int,
        allTokens: MutableList<LudoToken>
    ): MoveResult {
        val prevState = token.state
        val prevStep = token.stepCounter
        val intermediateSteps = mutableListOf<Pair<TokenState, Int>>()
        val capturedTokens = mutableListOf<LudoToken>()
        var isUnlockedFromYard = false
        var reachedGoal = false

        when (token.state) {
            TokenState.YARD -> {
                // Deployment on 6: token enters the starting square (stepCounter = 0 on TRACK)
                token.state = TokenState.TRACK
                token.stepCounter = 0
                isUnlockedFromYard = true
                intermediateSteps.add(Pair(TokenState.TRACK, 0))

                // Check capture on player's starting cell (if enemy present and not in safe state)
                checkCapturesAtCurrentPosition(token, allTokens, capturedTokens)
            }
            TokenState.TRACK, TokenState.HOME_STRETCH -> {
                // Progressive step traversal
                for (s in 1..diceValue) {
                    when {
                        token.stepCounter < TRACK_LOOP_STEPS -> {
                            token.stepCounter += 1
                        }
                        token.stepCounter == TRACK_LOOP_STEPS -> {
                            token.state = TokenState.HOME_STRETCH
                            token.stepCounter = HOME_STRETCH_START
                        }
                        token.stepCounter in HOME_STRETCH_START until MAX_STEPS -> {
                            token.stepCounter += 1
                            if (token.stepCounter == MAX_STEPS) {
                                token.state = TokenState.GOAL
                                reachedGoal = true
                            }
                        }
                    }
                    intermediateSteps.add(Pair(token.state, token.stepCounter))
                }

                // Check captures on final landing position
                if (token.state == TokenState.TRACK) {
                    checkCapturesAtCurrentPosition(token, allTokens, capturedTokens)
                }
            }
            TokenState.GOAL -> {
                // Already finished
            }
        }

        // Extra turn awarded if: rolled 6, captured enemy, or scored a goal
        val grantsExtraTurn = (diceValue == 6) || (capturedTokens.isNotEmpty()) || reachedGoal

        return MoveResult(
            movedToken = token,
            previousState = prevState,
            previousStep = prevStep,
            isUnlockedFromYard = isUnlockedFromYard,
            capturedTokens = capturedTokens,
            reachedGoal = reachedGoal,
            grantsExtraTurn = grantsExtraTurn,
            intermediateSteps = intermediateSteps
        )
    }

    /**
     * Checks if any opponent tokens on the same cell can be captured.
     * Safe cells (0, 8, 13, 21, 26, 34, 39, 47) strictly protect tokens.
     */
    private fun checkCapturesAtCurrentPosition(
        mover: LudoToken,
        allTokens: MutableList<LudoToken>,
        capturedOut: MutableList<LudoToken>
    ) {
        if (mover.state != TokenState.TRACK) return

        val startIndex = getStartTrackIndex(mover.color)
        val absIndex = (startIndex + mover.stepCounter) % 52

        // Safe cells are immune to captures
        if (ludoSafeIndices.contains(absIndex)) return

        val moverGridPos = getTokenGridPosition(mover)

        allTokens.forEachIndexed { idx, other ->
            if (other.color != mover.color && other.state == TokenState.TRACK) {
                val otherGridPos = getTokenGridPosition(other)
                if (otherGridPos == moverGridPos) {
                    // Knock back to yard
                    other.state = TokenState.YARD
                    other.stepCounter = 0
                    allTokens[idx] = other.copy()
                    capturedOut.add(other)
                }
            }
        }
    }

    /**
     * Block rule verification: returns true if an enemy block (2+ tokens) obstructs the path.
     */
    private fun isMoveBlocked(
        token: LudoToken,
        diceValue: Int,
        allTokens: List<LudoToken>
    ): Boolean {
        if (token.state != TokenState.TRACK) return false
        val startIdx = getStartTrackIndex(token.color)

        for (step in 1..diceValue) {
            val checkStep = token.stepCounter + step
            if (checkStep <= TRACK_LOOP_STEPS) {
                val absIdx = (startIdx + checkStep) % 52
                // Check if any opponent has a block (2 or more tokens on this cell)
                PlayerColor.values().filter { it != token.color }.forEach { enemyCol ->
                    val enemyStart = getStartTrackIndex(enemyCol)
                    val enemyTokensOnCell = allTokens.filter {
                        it.color == enemyCol && it.state == TokenState.TRACK &&
                                ((enemyStart + it.stepCounter) % 52 == absIdx)
                    }
                    if (enemyTokensOnCell.size >= 2) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Checks if [playerColor] has won by completing all 4 tokens.
     */
    fun checkVictory(playerColor: PlayerColor, tokens: List<LudoToken>): Boolean {
        val pTokens = tokens.filter { it.color == playerColor }
        return pTokens.isNotEmpty() && pTokens.all { it.state == TokenState.GOAL }
    }

    /**
     * Seamlessly advances turn to the next player, supporting 2, 3, or 4 active players.
     */
    fun getNextTurnColor(currentColor: PlayerColor, activePlayers: List<LudoPlayer>): PlayerColor {
        if (activePlayers.isEmpty()) return PlayerColor.RED
        val curIndex = activePlayers.indexOfFirst { it.color == currentColor }
        val nextIndex = if (curIndex == -1) 0 else (curIndex + 1) % activePlayers.size
        return activePlayers[nextIndex].color
    }

    /**
     * Tracks consecutive sixes. Returns (newCount, isThreeSixesPenalty).
     */
    fun handleConsecutiveSix(currentCount: Int, diceValue: Int): Pair<Int, Boolean> {
        return if (diceValue == 6) {
            val next = currentCount + 1
            Pair(next, next >= 3)
        } else {
            Pair(0, false)
        }
    }

    /**
     * High-intelligence heuristic AI decision engine:
     * Evaluates all legal moves for [playerColor] and chooses the optimal token.
     */
    fun selectBestAiMove(
        playerColor: PlayerColor,
        diceValue: Int,
        legalTokenIds: List<Int>,
        allTokens: List<LudoToken>
    ): Int? {
        if (legalTokenIds.isEmpty()) return null
        if (legalTokenIds.size == 1) return legalTokenIds.first()

        val playerTokens = allTokens.filter { it.color == playerColor && legalTokenIds.contains(it.id) }

        var bestScore = Int.MIN_VALUE
        var bestTokenId = legalTokenIds.first()

        val enemyTokens = allTokens.filter { it.color != playerColor && it.state == TokenState.TRACK }

        for (token in playerTokens) {
            var score = 0
            val targetStep = token.stepCounter + diceValue

            when (token.state) {
                TokenState.YARD -> {
                    // High priority: Deploy pawn to the active board
                    score += 650
                    // Bonus if enemy sits on player start cell (spawn kill)
                    val startCell = ludoCommonPath[getStartTrackIndex(playerColor)]
                    if (enemyTokens.any { getTokenGridPosition(it) == startCell }) {
                        score += 300
                    }
                }
                TokenState.TRACK -> {
                    // 1. Scoring Goal
                    if (targetStep == MAX_STEPS) {
                        score += 1500
                    } else if (targetStep in HOME_STRETCH_START..55) {
                        // Entering safe home stretch
                        score += 500 + targetStep * 2
                    } else {
                        // 2. Capture potential
                        val startIdx = getStartTrackIndex(playerColor)
                        val targetAbsIdx = (startIdx + targetStep) % 52
                        val isSafe = ludoSafeIndices.contains(targetAbsIdx)

                        if (!isSafe) {
                            val targetGrid = ludoCommonPath[targetAbsIdx]
                            if (enemyTokens.any { getTokenGridPosition(it) == targetGrid }) {
                                score += 1100 // High reward for capturing
                            }
                        } else {
                            score += 350 // Reward for landing on safe star
                        }

                        // 3. Danger evasion: check if currently in danger from enemies behind
                        val curAbsIdx = (startIdx + token.stepCounter) % 52
                        val isCurrentlySafe = ludoSafeIndices.contains(curAbsIdx)
                        if (!isCurrentlySafe) {
                            enemyTokens.forEach { enemy ->
                                val enemyStart = getStartTrackIndex(enemy.color)
                                val enemyAbs = (enemyStart + enemy.stepCounter) % 52
                                val dist = (curAbsIdx - enemyAbs + 52) % 52
                                if (dist in 1..6) {
                                    score += 400 // Escape danger
                                }
                            }
                        }

                        // 4. Progress along track
                        score += token.stepCounter * 3
                    }
                }
                TokenState.HOME_STRETCH -> {
                    if (targetStep == MAX_STEPS) {
                        score += 1500 // Score goal
                    } else {
                        score += 600 + targetStep * 2
                    }
                }
                TokenState.GOAL -> {}
            }

            if (score > bestScore) {
                bestScore = score
                bestTokenId = token.id
            }
        }

        return bestTokenId
    }
}
