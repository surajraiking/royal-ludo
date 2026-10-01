package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

/**
 * Categorization of every cell on the 15x15 Ludo board grid.
 */
enum class LudoCellType {
    BASE_YARD,       // Player's 6x6 starting quadrant/base
    COMMON_TRACK,    // 52-cell outer running circuit
    SAFE_ZONE,       // Star safe cells where tokens cannot be captured
    START_CELL,      // Player's deployment tile onto the common path
    HOME_STRETCH,    // 5-tile colored home runway leading into the goal
    GOAL_CENTER      // 3x3 central victory triangle area
}

/**
 * Data model for an individual cell in the 15x15 Ludo board matrix.
 */
data class LudoGridCell(
    val row: Int,
    val col: Int,
    val type: LudoCellType,
    val ownerColor: PlayerColor? = null,
    val trackIndex: Int? = null,        // 0..51 for common path
    val isSafeZone: Boolean = false,
    val isStartCell: Boolean = false,
    val homeStepIndex: Int? = null,     // 0..4 for home stretch
    val yardSlotIndex: Int? = null      // 0..3 for base yard token positions
) {
    val coordinate: Pair<Int, Int> = Pair(row, col)
}

/**
 * Data model representing a player's base / yard (6x6 cells).
 */
data class PlayerBaseArea(
    val color: PlayerColor,
    val topLeftRow: Int,
    val topLeftCol: Int,
    val size: Int = 6,
    val yardSlots: List<Pair<Int, Int>>,
    val startTrackCell: Pair<Int, Int>,
    val homeStretchEntry: Pair<Int, Int>
)

/**
 * Full path trajectory for a player from start to finish.
 */
data class PlayerPathData(
    val color: PlayerColor,
    val startTrackIndex: Int,
    val commonCircuit: List<Pair<Int, Int>>,
    val homeStretch: List<Pair<Int, Int>>,
    val goalPoint: Pair<Int, Int> = Pair(7, 7)
) {
    /**
     * Resolves grid (row, col) for a given token step counter (0..56).
     * 0..50: common circuit loop
     * 51..55: home stretch (index 0..4)
     * 56: central goal
     */
    fun getCellForStep(stepCounter: Int): Pair<Int, Int> {
        return when {
            stepCounter in 0..50 -> {
                val absIndex = (startTrackIndex + stepCounter) % 52
                commonCircuit[absIndex]
            }
            stepCounter in 51..55 -> {
                homeStretch[stepCounter - 51]
            }
            else -> goalPoint
        }
    }
}

/**
 * Comprehensive 15x15 Ludo Board Grid Model holding full board structure,
 * cell definitions, 4-player paths, and home/base areas.
 */
class LudoBoardGrid {
    val rows: Int = 15
    val cols: Int = 15

    // 4 Player Base / Yard definitions
    val redBase = PlayerBaseArea(
        color = PlayerColor.RED,
        topLeftRow = 0,
        topLeftCol = 0,
        yardSlots = listOf(Pair(2, 2), Pair(2, 3), Pair(3, 2), Pair(3, 3)),
        startTrackCell = Pair(6, 1),
        homeStretchEntry = Pair(7, 1)
    )

    val greenBase = PlayerBaseArea(
        color = PlayerColor.GREEN,
        topLeftRow = 0,
        topLeftCol = 9,
        yardSlots = listOf(Pair(2, 11), Pair(2, 12), Pair(3, 11), Pair(3, 12)),
        startTrackCell = Pair(1, 8),
        homeStretchEntry = Pair(1, 7)
    )

    val yellowBase = PlayerBaseArea(
        color = PlayerColor.YELLOW,
        topLeftRow = 9,
        topLeftCol = 9,
        yardSlots = listOf(Pair(11, 11), Pair(11, 12), Pair(12, 11), Pair(12, 12)),
        startTrackCell = Pair(8, 13),
        homeStretchEntry = Pair(7, 13)
    )

    val blueBase = PlayerBaseArea(
        color = PlayerColor.BLUE,
        topLeftRow = 9,
        topLeftCol = 0,
        yardSlots = listOf(Pair(11, 2), Pair(11, 3), Pair(12, 2), Pair(12, 3)),
        startTrackCell = Pair(13, 6),
        homeStretchEntry = Pair(13, 7)
    )

    val playerBases: Map<PlayerColor, PlayerBaseArea> = mapOf(
        PlayerColor.RED to redBase,
        PlayerColor.GREEN to greenBase,
        PlayerColor.YELLOW to yellowBase,
        PlayerColor.BLUE to blueBase
    )

    // 4 Player Path trajectories
    val playerPaths: Map<PlayerColor, PlayerPathData> = mapOf(
        PlayerColor.RED to PlayerPathData(
            color = PlayerColor.RED,
            startTrackIndex = 0,
            commonCircuit = ludoCommonPath,
            homeStretch = redHomePath
        ),
        PlayerColor.GREEN to PlayerPathData(
            color = PlayerColor.GREEN,
            startTrackIndex = 13,
            commonCircuit = ludoCommonPath,
            homeStretch = greenHomePath
        ),
        PlayerColor.YELLOW to PlayerPathData(
            color = PlayerColor.YELLOW,
            startTrackIndex = 26,
            commonCircuit = ludoCommonPath,
            homeStretch = yellowHomePath
        ),
        PlayerColor.BLUE to PlayerPathData(
            color = PlayerColor.BLUE,
            startTrackIndex = 39,
            commonCircuit = ludoCommonPath,
            homeStretch = blueHomePath
        )
    )

    // Complete 15x15 grid cells matrix
    val matrix: List<List<LudoGridCell>> = (0 until rows).map { r ->
        (0 until cols).map { c ->
            buildCell(r, c)
        }
    }

    private fun buildCell(r: Int, c: Int): LudoGridCell {
        val coord = Pair(r, c)

        // 1. Center Goal (3x3 core at [6..8, 6..8])
        if (r in 6..8 && c in 6..8) {
            val owner = when {
                r == 7 && c == 6 -> PlayerColor.RED
                r == 6 && c == 7 -> PlayerColor.GREEN
                r == 7 && c == 8 -> PlayerColor.YELLOW
                r == 8 && c == 7 -> PlayerColor.BLUE
                else -> null
            }
            return LudoGridCell(r, c, LudoCellType.GOAL_CENTER, ownerColor = owner)
        }

        // 2. Base Yards (Corner 6x6 blocks)
        if (r < 6 && c < 6) {
            val slotIdx = redBase.yardSlots.indexOf(coord).takeIf { it >= 0 }
            return LudoGridCell(r, c, LudoCellType.BASE_YARD, PlayerColor.RED, yardSlotIndex = slotIdx)
        }
        if (r < 6 && c >= 9) {
            val slotIdx = greenBase.yardSlots.indexOf(coord).takeIf { it >= 0 }
            return LudoGridCell(r, c, LudoCellType.BASE_YARD, PlayerColor.GREEN, yardSlotIndex = slotIdx)
        }
        if (r >= 9 && c >= 9) {
            val slotIdx = yellowBase.yardSlots.indexOf(coord).takeIf { it >= 0 }
            return LudoGridCell(r, c, LudoCellType.BASE_YARD, PlayerColor.YELLOW, yardSlotIndex = slotIdx)
        }
        if (r >= 9 && c < 6) {
            val slotIdx = blueBase.yardSlots.indexOf(coord).takeIf { it >= 0 }
            return LudoGridCell(r, c, LudoCellType.BASE_YARD, PlayerColor.BLUE, yardSlotIndex = slotIdx)
        }

        // 3. Home Stretches
        redHomePath.indexOf(coord).takeIf { it >= 0 }?.let { idx ->
            return LudoGridCell(r, c, LudoCellType.HOME_STRETCH, PlayerColor.RED, homeStepIndex = idx)
        }
        greenHomePath.indexOf(coord).takeIf { it >= 0 }?.let { idx ->
            return LudoGridCell(r, c, LudoCellType.HOME_STRETCH, PlayerColor.GREEN, homeStepIndex = idx)
        }
        yellowHomePath.indexOf(coord).takeIf { it >= 0 }?.let { idx ->
            return LudoGridCell(r, c, LudoCellType.HOME_STRETCH, PlayerColor.YELLOW, homeStepIndex = idx)
        }
        blueHomePath.indexOf(coord).takeIf { it >= 0 }?.let { idx ->
            return LudoGridCell(r, c, LudoCellType.HOME_STRETCH, PlayerColor.BLUE, homeStepIndex = idx)
        }

        // 4. Common Track Cells
        val trackIdx = ludoCommonPath.indexOf(coord)
        val isSafe = trackIdx >= 0 && ludoSafeIndices.contains(trackIdx)
        val isStart = when (coord) {
            Pair(6, 1) -> true // Red start
            Pair(1, 8) -> true // Green start
            Pair(8, 13) -> true // Yellow start
            Pair(13, 6) -> true // Blue start
            else -> false
        }
        val owner = when (coord) {
            Pair(6, 1) -> PlayerColor.RED
            Pair(1, 8) -> PlayerColor.GREEN
            Pair(8, 13) -> PlayerColor.YELLOW
            Pair(13, 6) -> PlayerColor.BLUE
            else -> null
        }

        return LudoGridCell(
            row = r,
            col = c,
            type = if (isSafe) LudoCellType.SAFE_ZONE else if (isStart) LudoCellType.START_CELL else LudoCellType.COMMON_TRACK,
            ownerColor = owner,
            trackIndex = if (trackIdx >= 0) trackIdx else null,
            isSafeZone = isSafe,
            isStartCell = isStart
        )
    }

    fun getCell(row: Int, col: Int): LudoGridCell? {
        if (row in 0 until rows && col in 0 until cols) {
            return matrix[row][col]
        }
        return null
    }

    fun getBase(color: PlayerColor): PlayerBaseArea = playerBases[color]!!
    fun getPath(color: PlayerColor): PlayerPathData = playerPaths[color]!!
}
