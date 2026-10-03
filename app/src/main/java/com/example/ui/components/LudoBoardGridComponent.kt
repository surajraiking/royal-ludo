package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.model.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Classic Authentic Ludo Colors (matching official traditional board exactly)
private val BoardRed = Color(0xFFE52521)
private val BoardGreen = Color(0xFF00A859)
private val BoardYellow = Color(0xFFFFC20E)
private val BoardBlue = Color(0xFF0080FF)
private val CellBorderColor = Color(0xFFD4D4D4)
private val StarOutlineColor = Color(0xFF888888)

/**
 * Authentic Classic Ludo Board Component matching traditional Ludo King format:
 * - 15x15 crisp white grid with 4 solid colored corner yards
 * - Inner white courtyards with 4 circular token sockets per player
 * - 4 center victory triangles meeting at the center point (Red, Green, Yellow, Blue)
 * - Authentic teardrop / pin goti tokens with silver metallic rim, colored dome, and 3D drop shadow
 * - 8 safe zone stars (⭐) and directional entry arrows
 */
@Composable
fun LudoBoardGridComponent(
    tokens: List<LudoToken>,
    movableTokenIds: List<Int>,
    currentTurnColor: PlayerColor,
    onTokenClicked: (LudoToken) -> Unit,
    modifier: Modifier = Modifier,
    movingTokenId: Int? = null,
    movingTokenColor: PlayerColor? = null,
    center3DText: String = "ROYAL LUDO",
    boardGrid: LudoBoardGrid = remember { LudoBoardGrid() }
) {
    // Pulse animation for movable tokens
    val infiniteTransition = rememberInfiniteTransition(label = "token_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseHaloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobbing"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(16.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, CellBorderColor, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        val boardSideDp = min(maxWidth.value, maxHeight.value).dp
        val boardSidePx = with(LocalDensity.current) { boardSideDp.toPx() }
        val cellSizePx = boardSidePx / 15f

        // Separate yard tokens and track tokens
        val yardTokens = tokens.filter { it.state == TokenState.YARD }
        val trackTokens = tokens.filter { it.state != TokenState.YARD }
        val trackClusters = trackTokens.groupBy { getTokenGridPosition(it) }

        Canvas(
            modifier = Modifier
                .size(boardSideDp)
                .pointerInput(tokens.map { "${it.color}_${it.id}_${it.state}_${it.stepCounter}" }, movableTokenIds.toList(), currentTurnColor) {
                    detectTapGestures { tapOffset ->
                        val col = (tapOffset.x / cellSizePx).toInt().coerceIn(0, 14)
                        val row = (tapOffset.y / cellSizePx).toInt().coerceIn(0, 14)

                        val currentMovableTokens = tokens.filter { it.color == currentTurnColor && movableTokenIds.contains(it.id) }
                        if (currentMovableTokens.isEmpty()) return@detectTapGestures

                        // 1. Check if user tapped inside current player's yard base
                        val isTappingBase = when (currentTurnColor) {
                            PlayerColor.RED -> row in 0..5 && col in 0..5
                            PlayerColor.GREEN -> row in 0..5 && col in 9..14
                            PlayerColor.YELLOW -> row in 9..14 && col in 9..14
                            PlayerColor.BLUE -> row in 9..14 && col in 0..5
                        }

                        if (isTappingBase) {
                            val yardMovable = currentMovableTokens.firstOrNull { it.state == TokenState.YARD }
                            if (yardMovable != null) {
                                onTokenClicked(yardMovable)
                                return@detectTapGestures
                            }
                        }

                        // 2. Check distance to each movable token on the board
                        var bestToken: LudoToken? = null
                        var bestDistSq = Float.MAX_VALUE
                        val maxRadiusPx = cellSizePx * 2.2f

                        for (cand in currentMovableTokens) {
                            val candCenterPx = if (cand.state == TokenState.YARD) {
                                getYardSocketCenter(cand.color, cand.id, cellSizePx)
                            } else {
                                val pos = getTokenGridPosition(cand)
                                Offset((pos.second + 0.5f) * cellSizePx, (pos.first + 0.5f) * cellSizePx)
                            }
                            val distSq = (tapOffset.x - candCenterPx.x) * (tapOffset.x - candCenterPx.x) +
                                         (tapOffset.y - candCenterPx.y) * (tapOffset.y - candCenterPx.y)
                            if (distSq < bestDistSq && distSq <= maxRadiusPx * maxRadiusPx) {
                                bestDistSq = distSq
                                bestToken = cand
                            }
                        }

                        if (bestToken != null) {
                            onTokenClicked(bestToken)
                        } else if (currentMovableTokens.size == 1) {
                            onTokenClicked(currentMovableTokens.first())
                        }
                    }
                }
        ) {
            // 1. Draw Board Grid Background (White cells)
            drawRect(color = Color.White, size = size)

            // 2. Draw 4 Corner Yards (Home Bases)
            drawClassicPlayerYards(cellSizePx)

            // 3. Draw Home Stretches and Start Squares
            drawClassicHomeStretchesAndStarts(cellSizePx)

            // 4. Draw Center 4-Color Victory Triangles
            drawClassicCenterTriangles(cellSizePx)

            // 5. Draw 8 Safe Zone Stars (⭐)
            drawClassicSafeZoneStars(cellSizePx)

            // 6. Draw Track Grid Border Lines & Directional Arrows
            drawClassicTrackBordersAndArrows(cellSizePx)

            // 7. Draw Tokens in Yard Sockets
            yardTokens.forEach { token ->
                val socketCenter = getYardSocketCenter(token.color, token.id, cellSizePx)
                val isMovable = token.color == currentTurnColor && movableTokenIds.contains(token.id)
                val isMoving = (movingTokenColor == null || token.color == movingTokenColor) && token.id == movingTokenId

                drawAuthenticLudoToken(
                    token = token,
                    center = socketCenter,
                    cellSize = cellSizePx,
                    isMovable = isMovable,
                    isMoving = isMoving,
                    pulseScale = pulseScale,
                    pulseHaloAlpha = pulseHaloAlpha,
                    bobbingY = if (isMovable) bobbingOffset else 0f
                )
            }

            // 8. Draw Tokens on Common Track / Home Stretch
            trackClusters.forEach { (coord, tokensInCell) ->
                val cellCenterPx = Offset(
                    (coord.second + 0.5f) * cellSizePx,
                    (coord.first + 0.5f) * cellSizePx
                )

                if (tokensInCell.size == 1) {
                    val singleToken = tokensInCell.first()
                    val isMovable = singleToken.color == currentTurnColor && movableTokenIds.contains(singleToken.id)
                    val isMoving = (movingTokenColor == null || singleToken.color == movingTokenColor) && singleToken.id == movingTokenId

                    drawAuthenticLudoToken(
                        token = singleToken,
                        center = cellCenterPx,
                        cellSize = cellSizePx,
                        isMovable = isMovable,
                        isMoving = isMoving,
                        pulseScale = pulseScale,
                        pulseHaloAlpha = pulseHaloAlpha,
                        bobbingY = if (isMovable) bobbingOffset else 0f
                    )
                } else {
                    // Stacking layout: distribute tokens in cluster
                    val count = tokensInCell.size
                    tokensInCell.forEachIndexed { idx, stackedToken ->
                        val angleDeg = (360f / count) * idx
                        val rad = Math.toRadians(angleDeg.toDouble())
                        val offsetDistance = cellSizePx * 0.22f
                        val miniCenter = Offset(
                            cellCenterPx.x + (offsetDistance * cos(rad)).toFloat(),
                            cellCenterPx.y + (offsetDistance * sin(rad)).toFloat()
                        )
                        val isMovable = stackedToken.color == currentTurnColor && movableTokenIds.contains(stackedToken.id)
                        val isMoving = (movingTokenColor == null || stackedToken.color == movingTokenColor) && stackedToken.id == movingTokenId

                        drawAuthenticLudoToken(
                            token = stackedToken,
                            center = miniCenter,
                            cellSize = cellSizePx * 0.82f,
                            isMovable = isMovable,
                            isMoving = isMoving,
                            pulseScale = pulseScale,
                            pulseHaloAlpha = pulseHaloAlpha,
                            bobbingY = if (isMovable) bobbingOffset * 0.7f else 0f
                        )
                    }
                }
            }
        }
    }
}

/**
 * Returns exact pixel center for the 4 circular sockets inside each yard.
 */
private fun getYardSocketCenter(color: PlayerColor, id: Int, cellSize: Float): Offset {
    val offsets = listOf(
        Pair(1.85f, 1.85f),
        Pair(4.15f, 1.85f),
        Pair(1.85f, 4.15f),
        Pair(4.15f, 4.15f)
    )
    val (relC, relR) = offsets[id.coerceIn(0, 3)]

    val baseTopLeft = when (color) {
        PlayerColor.RED -> Offset(0f, 0f)
        PlayerColor.GREEN -> Offset(9f * cellSize, 0f)
        PlayerColor.YELLOW -> Offset(9f * cellSize, 9f * cellSize)
        PlayerColor.BLUE -> Offset(0f, 9f * cellSize)
    }

    return Offset(baseTopLeft.x + relC * cellSize, baseTopLeft.y + relR * cellSize)
}

/**
 * Draws the 4 authentic player yards (6x6 solid color with rounded white courtyard and 4 sockets).
 */
private fun DrawScope.drawClassicPlayerYards(cellSize: Float) {
    val yards = listOf(
        Triple(Offset(0f, 0f), BoardRed, PlayerColor.RED),
        Triple(Offset(9f * cellSize, 0f), BoardGreen, PlayerColor.GREEN),
        Triple(Offset(9f * cellSize, 9f * cellSize), BoardYellow, PlayerColor.YELLOW),
        Triple(Offset(0f, 9f * cellSize), BoardBlue, PlayerColor.BLUE)
    )

    val yardSize = cellSize * 6f
    val innerPad = cellSize * 0.9f
    val innerSize = cellSize * 4.2f
    val cornerRad = CornerRadius(14f, 14f)

    yards.forEach { (topLeft, color, pColor) ->
        // 1. Solid Outer 6x6 Yard Square
        drawRect(color = color, topLeft = topLeft, size = Size(yardSize, yardSize))

        // 2. Inner White Courtyard with rounded corners
        val innerTopLeft = Offset(topLeft.x + innerPad, topLeft.y + innerPad)
        drawRoundRect(
            color = Color.White,
            topLeft = innerTopLeft,
            size = Size(innerSize, innerSize),
            cornerRadius = cornerRad
        )
        // Subtle crisp border around inner courtyard
        drawRoundRect(
            color = CellBorderColor,
            topLeft = innerTopLeft,
            size = Size(innerSize, innerSize),
            cornerRadius = cornerRad,
            style = Stroke(width = 1.5f)
        )

        // 3. Four Circular Sockets with colored border & circular well
        val socketRadius = cellSize * 0.45f
        val innerWellRadius = cellSize * 0.35f

        for (slotId in 0..3) {
            val center = getYardSocketCenter(pColor, slotId, cellSize)

            // Outer colored concentric ring
            drawCircle(
                color = color,
                radius = socketRadius,
                center = center,
                style = Stroke(width = 3.5f)
            )

            // Inner colored solid well
            drawCircle(
                color = color,
                radius = innerWellRadius,
                center = center
            )

            // Subtle dark inner bevel ring
            drawCircle(
                color = Color.Black.copy(alpha = 0.18f),
                radius = innerWellRadius,
                center = center,
                style = Stroke(width = 2f)
            )
        }
    }
}

/**
 * Draws colored home runways and starting deployment tiles matching classic Ludo.
 */
private fun DrawScope.drawClassicHomeStretchesAndStarts(cellSize: Float) {
    // Red Home Runway: Row 7, Cols 1..5
    for (col in 1..5) {
        drawRect(color = BoardRed, topLeft = Offset(col * cellSize, 7 * cellSize), size = Size(cellSize, cellSize))
    }
    // Green Home Runway: Col 7, Rows 1..5
    for (row in 1..5) {
        drawRect(color = BoardGreen, topLeft = Offset(7 * cellSize, row * cellSize), size = Size(cellSize, cellSize))
    }
    // Yellow Home Runway: Row 7, Cols 9..13
    for (col in 9..13) {
        drawRect(color = BoardYellow, topLeft = Offset(col * cellSize, 7 * cellSize), size = Size(cellSize, cellSize))
    }
    // Blue Home Runway: Col 7, Rows 9..13
    for (row in 9..13) {
        drawRect(color = BoardBlue, topLeft = Offset(7 * cellSize, row * cellSize), size = Size(cellSize, cellSize))
    }

    // Start Cells (where tokens enter the common running track)
    // Red Start: Row 6, Col 1
    drawRect(color = BoardRed, topLeft = Offset(1 * cellSize, 6 * cellSize), size = Size(cellSize, cellSize))
    // Green Start: Row 1, Col 8
    drawRect(color = BoardGreen, topLeft = Offset(8 * cellSize, 1 * cellSize), size = Size(cellSize, cellSize))
    // Yellow Start: Row 8, Col 13
    drawRect(color = BoardYellow, topLeft = Offset(13 * cellSize, 8 * cellSize), size = Size(cellSize, cellSize))
    // Blue Start: Row 13, Col 6
    drawRect(color = BoardBlue, topLeft = Offset(6 * cellSize, 13 * cellSize), size = Size(cellSize, cellSize))
}

/**
 * Draws the authentic 3x3 central victory area with 4 colored triangles meeting at the exact center.
 */
private fun DrawScope.drawClassicCenterTriangles(cellSize: Float) {
    val centerTopLeft = Offset(6 * cellSize, 6 * cellSize)
    val centerSide = cellSize * 3
    val midPoint = Offset(centerTopLeft.x + centerSide / 2f, centerTopLeft.y + centerSide / 2f)

    // Red Left Triangle
    val pathRed = Path().apply {
        moveTo(centerTopLeft.x, centerTopLeft.y)
        lineTo(midPoint.x, midPoint.y)
        lineTo(centerTopLeft.x, centerTopLeft.y + centerSide)
        close()
    }
    // Green Top Triangle
    val pathGreen = Path().apply {
        moveTo(centerTopLeft.x, centerTopLeft.y)
        lineTo(midPoint.x, midPoint.y)
        lineTo(centerTopLeft.x + centerSide, centerTopLeft.y)
        close()
    }
    // Yellow Right Triangle
    val pathYellow = Path().apply {
        moveTo(centerTopLeft.x + centerSide, centerTopLeft.y)
        lineTo(midPoint.x, midPoint.y)
        lineTo(centerTopLeft.x + centerSide, centerTopLeft.y + centerSide)
        close()
    }
    // Blue Bottom Triangle
    val pathBlue = Path().apply {
        moveTo(centerTopLeft.x, centerTopLeft.y + centerSide)
        lineTo(midPoint.x, midPoint.y)
        lineTo(centerTopLeft.x + centerSide, centerTopLeft.y + centerSide)
        close()
    }

    drawPath(path = pathRed, color = BoardRed)
    drawPath(path = pathGreen, color = BoardGreen)
    drawPath(path = pathYellow, color = BoardYellow)
    drawPath(path = pathBlue, color = BoardBlue)

    // Dividing lines between center triangles
    drawLine(color = CellBorderColor, start = Offset(centerTopLeft.x, centerTopLeft.y), end = Offset(centerTopLeft.x + centerSide, centerTopLeft.y + centerSide), strokeWidth = 1.5f)
    drawLine(color = CellBorderColor, start = Offset(centerTopLeft.x + centerSide, centerTopLeft.y), end = Offset(centerTopLeft.x, centerTopLeft.y + centerSide), strokeWidth = 1.5f)
}

/**
 * Draws the 8 authentic safe zone stars (⭐).
 */
private fun DrawScope.drawClassicSafeZoneStars(cellSize: Float) {
    val starCoords = listOf(
        Pair(6, 1),  // Red Start (Row 6, Col 1)
        Pair(8, 2),  // Red Track Safe Spot (Row 8, Col 2)
        Pair(1, 8),  // Green Start (Row 1, Col 8)
        Pair(2, 6),  // Green Track Safe Spot (Row 2, Col 6)
        Pair(8, 13), // Yellow Start (Row 8, Col 13)
        Pair(6, 12), // Yellow Track Safe Spot (Row 6, Col 12)
        Pair(13, 6), // Blue Start (Row 13, Col 6)
        Pair(12, 8)  // Blue Track Safe Spot (Row 12, Col 8)
    )

    starCoords.forEach { (r, c) ->
        val cX = (c + 0.5f) * cellSize
        val cY = (r + 0.5f) * cellSize

        // In starts (colored squares), draw white outline star; in white track cells, draw dark gray outline star
        val isColoredTile = (r == 6 && c == 1) || (r == 1 && c == 8) || (r == 8 && c == 13) || (r == 13 && c == 6)
        val starColor = if (isColoredTile) Color.White else StarOutlineColor

        drawOutlinedStar(cX, cY, cellSize * 0.28f, starColor)
    }
}

private fun DrawScope.drawOutlinedStar(cX: Float, cY: Float, r: Float, color: Color) {
    val path = Path()
    val points = 5
    var angle = -90.0
    val angleInc = 360.0 / points
    for (i in 0 until points) {
        val radOuter = Math.toRadians(angle)
        val radInner = Math.toRadians(angle + angleInc / 2)
        val oX = cX + r * cos(radOuter).toFloat()
        val oY = cY + r * sin(radOuter).toFloat()
        if (i == 0) path.moveTo(oX, oY) else path.lineTo(oX, oY)

        val iX = cX + (r / 2.3f) * cos(radInner).toFloat()
        val iY = cY + (r / 2.3f) * sin(radInner).toFloat()
        path.lineTo(iX, iY)
        angle += angleInc
    }
    path.close()
    drawPath(path = path, color = color, style = Stroke(width = 2f))
}

/**
 * Draws fine grid lines and directional track entry arrows.
 */
private fun DrawScope.drawClassicTrackBordersAndArrows(cellSize: Float) {
    // 1. Grid Lines around tracks
    for (row in 6..9) {
        drawLine(color = CellBorderColor, start = Offset(0f, row * cellSize), end = Offset(15 * cellSize, row * cellSize), strokeWidth = 1.2f)
    }
    for (col in 6..9) {
        drawLine(color = CellBorderColor, start = Offset(col * cellSize, 0f), end = Offset(col * cellSize, 15 * cellSize), strokeWidth = 1.2f)
    }

    for (row in 0..14) {
        if (row in 6..8) {
            for (col in 0..14) {
                drawLine(color = CellBorderColor, start = Offset(col * cellSize, row * cellSize), end = Offset(col * cellSize, (row + 1) * cellSize), strokeWidth = 1.2f)
            }
        } else {
            for (col in 6..8) {
                drawLine(color = CellBorderColor, start = Offset(col * cellSize, row * cellSize), end = Offset((col + 1) * cellSize, row * cellSize), strokeWidth = 1.2f)
                drawLine(color = CellBorderColor, start = Offset(col * cellSize, row * cellSize), end = Offset(col * cellSize, (row + 1) * cellSize), strokeWidth = 1.2f)
                drawLine(color = CellBorderColor, start = Offset((col + 1) * cellSize, row * cellSize), end = Offset((col + 1) * cellSize, (row + 1) * cellSize), strokeWidth = 1.2f)
            }
        }
    }

    // 2. Directional Entry Arrows
    // Red Arrow pointing right at (row 7, col 0)
    drawDirectionalArrow(center = Offset(0.5f * cellSize, 7.5f * cellSize), radius = cellSize * 0.22f, direction = ArrowDirection.RIGHT, color = BoardRed)
    // Green Arrow pointing down at (row 0, col 7)
    drawDirectionalArrow(center = Offset(7.5f * cellSize, 0.5f * cellSize), radius = cellSize * 0.22f, direction = ArrowDirection.DOWN, color = BoardGreen)
    // Yellow Arrow pointing left at (row 7, col 14)
    drawDirectionalArrow(center = Offset(14.5f * cellSize, 7.5f * cellSize), radius = cellSize * 0.22f, direction = ArrowDirection.LEFT, color = BoardYellow)
    // Blue Arrow pointing up at (row 14, col 7)
    drawDirectionalArrow(center = Offset(7.5f * cellSize, 14.5f * cellSize), radius = cellSize * 0.22f, direction = ArrowDirection.UP, color = BoardBlue)
}

private enum class ArrowDirection { UP, DOWN, LEFT, RIGHT }

private fun DrawScope.drawDirectionalArrow(center: Offset, radius: Float, direction: ArrowDirection, color: Color) {
    val path = Path()
    when (direction) {
        ArrowDirection.RIGHT -> {
            path.moveTo(center.x - radius, center.y - radius * 0.6f)
            path.lineTo(center.x + radius, center.y)
            path.lineTo(center.x - radius, center.y + radius * 0.6f)
            path.lineTo(center.x - radius * 0.3f, center.y)
            path.close()
        }
        ArrowDirection.DOWN -> {
            path.moveTo(center.x - radius * 0.6f, center.y - radius)
            path.lineTo(center.x, center.y + radius)
            path.lineTo(center.x + radius * 0.6f, center.y - radius)
            path.lineTo(center.x, center.y - radius * 0.3f)
            path.close()
        }
        ArrowDirection.LEFT -> {
            path.moveTo(center.x + radius, center.y - radius * 0.6f)
            path.lineTo(center.x - radius, center.y)
            path.lineTo(center.x + radius, center.y + radius * 0.6f)
            path.lineTo(center.x + radius * 0.3f, center.y)
            path.close()
        }
        ArrowDirection.UP -> {
            path.moveTo(center.x - radius * 0.6f, center.y + radius)
            path.lineTo(center.x, center.y - radius)
            path.lineTo(center.x + radius * 0.6f, center.y + radius)
            path.lineTo(center.x, center.y + radius * 0.3f)
            path.close()
        }
    }
    drawPath(path = path, color = color)
}

/**
 * Draws the authentic Ludo King Teardrop / Map-Pin Pawn (Goti) matching the user's uploaded image:
 * - Oval drop shadow on the board
 * - Silver/White metallic 3D outer rim
 * - Vibrant colored circular dome in the head
 * - Tapered conical stem pointing to the tile
 * - Pulsing highlight aura when movable
 */
private fun DrawScope.drawAuthenticLudoToken(
    token: LudoToken,
    center: Offset,
    cellSize: Float,
    isMovable: Boolean,
    isMoving: Boolean,
    pulseScale: Float,
    pulseHaloAlpha: Float,
    bobbingY: Float
) {
    val pieceColor = when (token.color) {
        PlayerColor.RED -> BoardRed
        PlayerColor.GREEN -> BoardGreen
        PlayerColor.YELLOW -> BoardYellow
        PlayerColor.BLUE -> BoardBlue
    }

    val currentScale = if (isMoving) 1.28f else if (isMovable) pulseScale else 1.0f
    val jumpElevationY = if (isMoving) -cellSize * 0.45f else bobbingY
    val effectiveCenter = center + Offset(0f, jumpElevationY)

    val pinRadius = cellSize * 0.34f * currentScale
    val stemLength = cellSize * 0.42f * currentScale

    // 1. Drop shadow onto the board
    val shadowWidth = pinRadius * 1.8f
    val shadowHeight = pinRadius * 0.9f
    drawOval(
        color = Color.Black.copy(alpha = if (isMoving) 0.2f else 0.35f),
        topLeft = Offset(center.x - shadowWidth / 2f + 2f, center.y + cellSize * 0.15f - shadowHeight / 2f),
        size = Size(shadowWidth, shadowHeight)
    )

    // 2. Movable Halo Ring
    if (isMovable && pulseHaloAlpha > 0f) {
        drawCircle(
            color = pieceColor.copy(alpha = pulseHaloAlpha * 0.65f),
            radius = pinRadius * 1.6f,
            center = center,
            style = Stroke(width = 3.5f)
        )
    }

    // 3. Teardrop / Map-Pin Outer Body (Silver / White metallic rim)
    val headCenter = effectiveCenter - Offset(0f, stemLength * 0.25f)
    val tipBottom = effectiveCenter + Offset(0f, stemLength * 0.85f)

    val pinPath = Path().apply {
        // Start at left tangent of the head circle
        moveTo(headCenter.x - pinRadius, headCenter.y)
        // Top circular arc
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                headCenter.x - pinRadius,
                headCenter.y - pinRadius,
                headCenter.x + pinRadius,
                headCenter.y + pinRadius
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        // Right side tapering down to bottom tip
        quadraticTo(
            headCenter.x + pinRadius * 0.85f, headCenter.y + stemLength * 0.45f,
            tipBottom.x, tipBottom.y
        )
        // Left side tapering back up to left tangent
        quadraticTo(
            headCenter.x - pinRadius * 0.85f, headCenter.y + stemLength * 0.45f,
            headCenter.x - pinRadius, headCenter.y
        )
        close()
    }

    // Fill outer body with silver / metallic 3D gradient
    drawPath(
        path = pinPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFECEFF1), Color(0xFFCFD8DC), Color(0xFF90A4AE)),
            start = Offset(headCenter.x - pinRadius, headCenter.y - pinRadius),
            end = Offset(tipBottom.x + pinRadius, tipBottom.y)
        )
    )

    // Outer crisp dark rim outline
    drawPath(
        path = pinPath,
        color = Color(0x66000000),
        style = Stroke(width = 1.8f)
    )

    // 4. Inner Colored Circle Dome (The vibrant head of the goti)
    val innerRadius = pinRadius * 0.65f
    drawCircle(
        color = pieceColor,
        radius = innerRadius,
        center = headCenter
    )

    // Inner 3D specular highlight glint (gives shiny plastic/glass dome appearance)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
            center = headCenter - Offset(innerRadius * 0.35f, innerRadius * 0.35f),
            radius = innerRadius * 0.7f
        ),
        radius = innerRadius * 0.7f,
        center = headCenter - Offset(innerRadius * 0.35f, innerRadius * 0.35f)
    )

    // Thin dark outline separating inner circle from silver rim
    drawCircle(
        color = Color.Black.copy(alpha = 0.25f),
        radius = innerRadius,
        center = headCenter,
        style = Stroke(width = 1.2f)
    )
}
