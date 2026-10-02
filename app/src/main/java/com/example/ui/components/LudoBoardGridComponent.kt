package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Premium 3D Ludo Board Component featuring:
 * - 3D Chess Pawn / Royal Knight style pieces with metallic gradients, pedestals, and specular highlights
 * - 3D parabolic hop movements during stepping and base deployment
 * - Extruded 3D text marquee in the central victory playing area
 * - Beveled 3D sunken tiles, raised platforms, and glowing circuit tracks
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
    center3DText: String = "ROYAL 3D LUDO",
    boardGrid: LudoBoardGrid = remember { LudoBoardGrid() }
) {
    // Pulse animation for movable token highlight & 3D levitation bobbing
    val infiniteTransition = rememberInfiniteTransition(label = "token_3d_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseHaloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobbing"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(24.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E1528), DeepDarkBg, Color(0xFF07040B))
                )
            )
            .border(2.5.dp, Brush.linearGradient(listOf(GoldPrimary, Color(0xFF5D481C), GoldPrimary)), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        val boardSideDp = min(maxWidth.value, maxHeight.value).dp
        val boardSidePx = with(LocalDensity.current) { boardSideDp.toPx() }
        val cellSizePx = boardSidePx / 15f

        // Freshly computed token groupings by grid coordinates (NO stale remember)
        val tokenClusters = tokens.groupBy { getTokenGridPosition(it) }

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

                        // 2. Check distance to each movable token on the board with generous touch target
                        var bestToken: LudoToken? = null
                        var bestDistSq = Float.MAX_VALUE
                        val maxRadiusPx = cellSizePx * 2.2f // Generous touch target: over 2 cells radius!

                        for (cand in currentMovableTokens) {
                            val pos = getTokenGridPosition(cand)
                            val candCenterPx = Offset((pos.second + 0.5f) * cellSizePx, (pos.first + 0.5f) * cellSizePx)
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
                            // If only 1 token is movable and user tapped on the board, execute that move
                            onTokenClicked(currentMovableTokens.first())
                        }
                    }
                }
        ) {
            // 1. Draw 3D Board Surface with metallic chiseled bevel border
            draw3DBoardFrame(size, cellSizePx)

            // 2. Draw 4-Player Corner Bases / Raised Palaces
            draw3DPlayerBases(cellSizePx, boardGrid)

            // 3. Draw 4-Player Home Stretches and Start Deployment Arrows with 3D depth
            draw3DHomeStretchesAndStarts(cellSizePx)

            // 4. Draw Central 4-Color Victory Goal (Center Triangles with depth)
            draw3DCenterVictoryGoal(cellSizePx)

            // 5. Draw Common Circuit Safe Zone 3D Golden Stars
            draw3DSafeZoneStars(cellSizePx)

            // 6. Draw 3D Sunken Track Borders & Grooves
            draw3DGridTrackBorders(cellSizePx)

            // 7. Draw 3D Chess Pieces with Pedestals, Waist, Collar & Specular Heads
            tokenClusters.forEach { (coord, tokensInCell) ->
                val centerPx = Offset(
                    (coord.second + 0.5f) * cellSizePx,
                    (coord.first + 0.5f) * cellSizePx
                )

                if (tokensInCell.size == 1) {
                    val singleToken = tokensInCell.first()
                    val isMovable = singleToken.color == currentTurnColor && movableTokenIds.contains(singleToken.id)
                    val isMoving = (movingTokenColor == null || singleToken.color == movingTokenColor) && singleToken.id == movingTokenId

                    draw3DChessPiece(
                        token = singleToken,
                        center = centerPx,
                        cellSize = cellSizePx,
                        isMovable = isMovable,
                        isMoving = isMoving,
                        pulseScale = pulseScale,
                        pulseHaloAlpha = pulseHaloAlpha,
                        bobbingY = if (isMovable) bobbingOffset else 0f
                    )
                } else {
                    // Stacking layout: distribute in miniature constellation around cell center
                    val count = tokensInCell.size
                    tokensInCell.forEachIndexed { idx, stackedToken ->
                        val angleDeg = (360f / count) * idx
                        val rad = Math.toRadians(angleDeg.toDouble())
                        val offsetDistance = cellSizePx * 0.22f
                        val miniCenter = Offset(
                            centerPx.x + (offsetDistance * cos(rad)).toFloat(),
                            centerPx.y + (offsetDistance * sin(rad)).toFloat()
                        )
                        val isMovable = stackedToken.color == currentTurnColor && movableTokenIds.contains(stackedToken.id)
                        val isMoving = (movingTokenColor == null || stackedToken.color == movingTokenColor) && stackedToken.id == movingTokenId

                        draw3DChessPiece(
                            token = stackedToken,
                            center = miniCenter,
                            cellSize = cellSizePx * 0.75f,
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

        // 8. 3D EXTRUDED TEXT MARQUEE IN CENTER PLAYING AREA
        // Centered inside the 3x3 Victory Hub (col 6..8, row 6..8)
        val centerBoxSizeDp = (boardSideDp / 15f) * 2.85f
        Box(
            modifier = Modifier
                .size(centerBoxSizeDp)
                .shadow(12.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF281C3C), Color(0xFF110B1E), Color(0xFF05030A))
                    )
                )
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(listOf(GoldPrimary, Color(0xFFFFA000), GoldPrimary))
                    ),
                    RoundedCornerShape(12.dp)
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "👑",
                    fontSize = (centerBoxSizeDp.value * 0.26f).sp
                )
                Spacer(modifier = Modifier.height(2.dp))

                // Multi-layered 3D Extruded Text effect
                Box(contentAlignment = Alignment.Center) {
                    // 3D Shadow Layers (extrusion depth)
                    Text(
                        text = center3DText,
                        color = Color.Black.copy(alpha = 0.85f),
                        fontSize = (centerBoxSizeDp.value * 0.12f).coerceIn(8f, 13f).sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = (centerBoxSizeDp.value * 0.14f).sp,
                        modifier = Modifier.offset(x = 2.dp, y = 2.dp)
                    )
                    Text(
                        text = center3DText,
                        color = Color(0xFF533B05),
                        fontSize = (centerBoxSizeDp.value * 0.12f).coerceIn(8f, 13f).sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = (centerBoxSizeDp.value * 0.14f).sp,
                        modifier = Modifier.offset(x = 1.dp, y = 1.dp)
                    )
                    // Gleaming Golden Front Face
                    Text(
                        text = center3DText,
                        color = GoldPrimary,
                        fontSize = (centerBoxSizeDp.value * 0.12f).coerceIn(8f, 13f).sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = (centerBoxSizeDp.value * 0.14f).sp
                    )
                }
            }
        }
    }
}

/**
 * Draws the 3D chiseled board surface frame and corner rivets.
 */
private fun DrawScope.draw3DBoardFrame(size: Size, cellSize: Float) {
    // Deep obsidian background
    drawRect(color = DeepDarkBg, size = size)

    // Outer chiseled bevel border
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF3B2E15), Color(0xFF1E170B), Color(0xFF4C3B1A)),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        ),
        topLeft = Offset.Zero,
        size = size,
        style = Stroke(width = cellSize * 0.15f)
    )

    // Golden metallic corner accents
    val rivetRadius = cellSize * 0.12f
    val margin = cellSize * 0.25f
    val rivets = listOf(
        Offset(margin, margin),
        Offset(size.width - margin, margin),
        Offset(margin, size.height - margin),
        Offset(size.width - margin, size.height - margin)
    )
    rivets.forEach { rivet ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, GoldPrimary, Color(0xFF6B5218)),
                center = rivet - Offset(rivetRadius * 0.3f, rivetRadius * 0.3f),
                radius = rivetRadius
            ),
            radius = rivetRadius,
            center = rivet
        )
    }
}

/**
 * Draws the four 6x6 raised 3D palace bases for Red, Green, Yellow, and Blue.
 */
private fun DrawScope.draw3DPlayerBases(cellSize: Float, grid: LudoBoardGrid) {
    val baseConfigs = listOf(
        Triple(grid.redBase, LudoRed, Color(0xFF700018)),
        Triple(grid.greenBase, LudoGreen, Color(0xFF004D40)),
        Triple(grid.yellowBase, LudoYellow, Color(0xFF8C7100)),
        Triple(grid.blueBase, LudoBlueVibrant, Color(0xFF094183))
    )

    baseConfigs.forEach { (base, primaryColor, darkColor) ->
        val topLeftOffset = Offset(base.topLeftCol * cellSize, base.topLeftRow * cellSize)
        val quadrantSize = cellSize * 6

        // 3D Cast drop shadow around base perimeter
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = topLeftOffset + Offset(4f, 4f),
            size = Size(quadrantSize, quadrantSize),
            cornerRadius = CornerRadius(14f, 14f)
        )

        // Raised 3D platform with rich royal gradient
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor, darkColor),
                center = Offset(topLeftOffset.x + quadrantSize / 2f, topLeftOffset.y + quadrantSize / 2f),
                radius = quadrantSize * 0.85f
            ),
            topLeft = topLeftOffset,
            size = Size(quadrantSize, quadrantSize),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // 3D Top Bevel light highlight line
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                start = topLeftOffset,
                end = Offset(topLeftOffset.x + quadrantSize, topLeftOffset.y + quadrantSize * 0.3f)
            ),
            topLeft = topLeftOffset,
            size = Size(quadrantSize, quadrantSize),
            cornerRadius = CornerRadius(12f, 12f),
            style = Stroke(width = 3f)
        )

        // Inner palace courtyard card
        val innerPad = cellSize * 0.75f
        val innerSize = cellSize * 4.5f
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(topLeftOffset.x + innerPad, topLeftOffset.y + innerPad),
            size = Size(innerSize, innerSize),
            cornerRadius = CornerRadius(14f, 14f)
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(GlassCard, Color(0x33100B1A))
            ),
            topLeft = Offset(topLeftOffset.x + innerPad, topLeftOffset.y + innerPad),
            size = Size(innerSize, innerSize),
            cornerRadius = CornerRadius(14f, 14f)
        )

        // 4 Token spawn docks with 3D sunken well effect
        base.yardSlots.forEach { (r, c) ->
            val slotCenter = Offset((c + 0.5f) * cellSize, (r + 0.5f) * cellSize)
            val dockRadius = cellSize * 0.34f

            // 3D sunken well shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.75f),
                radius = dockRadius,
                center = slotCenter + Offset(2f, 2f)
            )
            // Outer golden glow ring
            drawCircle(
                color = primaryColor,
                radius = dockRadius,
                center = slotCenter,
                style = Stroke(width = 3.5f)
            )
            // Inner dock pad with metallic finish
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF282035), Color(0xFF0F0B17)),
                    center = slotCenter,
                    radius = dockRadius
                ),
                radius = dockRadius - 2f,
                center = slotCenter
            )
        }
    }
}

/**
 * Draws colored home runways and starting deployment tiles with 3D lighting.
 */
private fun DrawScope.draw3DHomeStretchesAndStarts(cellSize: Float) {
    // Red Home Runway: Row 7, Col 1..5
    for (col in 1..5) {
        val rectTopLeft = Offset(col * cellSize, 7 * cellSize)
        draw3DTile(rectTopLeft, cellSize, LudoRed)
    }
    // Green Home Runway: Col 7, Row 1..5
    for (row in 1..5) {
        val rectTopLeft = Offset(7 * cellSize, row * cellSize)
        draw3DTile(rectTopLeft, cellSize, LudoGreen)
    }
    // Yellow Home Runway: Row 7, Col 9..13
    for (col in 9..13) {
        val rectTopLeft = Offset(col * cellSize, 7 * cellSize)
        draw3DTile(rectTopLeft, cellSize, LudoYellow)
    }
    // Blue Home Runway: Col 7, Row 9..13
    for (row in 9..13) {
        val rectTopLeft = Offset(7 * cellSize, row * cellSize)
        draw3DTile(rectTopLeft, cellSize, LudoBlueVibrant)
    }

    // Start Cells (where tokens spawn onto the board with 3D embossed border)
    val startCells = listOf(
        Pair(Offset(1 * cellSize, 6 * cellSize), LudoRed),
        Pair(Offset(8 * cellSize, 1 * cellSize), LudoGreen),
        Pair(Offset(13 * cellSize, 8 * cellSize), LudoYellow),
        Pair(Offset(6 * cellSize, 13 * cellSize), LudoBlueVibrant)
    )

    startCells.forEach { (offset, col) ->
        draw3DTile(offset, cellSize, col, isStartCell = true)
    }
}

/**
 * Draws a 3D bevelled track tile.
 */
private fun DrawScope.draw3DTile(topLeft: Offset, cellSize: Float, color: Color, isStartCell: Boolean = false) {
    val size = Size(cellSize, cellSize)
    // 3D sunken tile background
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0.5f)),
            center = Offset(topLeft.x + cellSize / 2f, topLeft.y + cellSize / 2f),
            radius = cellSize * 0.7f
        ),
        topLeft = topLeft,
        size = size
    )
    // Top-left highlight
    drawLine(
        color = Color.White.copy(alpha = 0.4f),
        start = topLeft,
        end = Offset(topLeft.x + cellSize, topLeft.y),
        strokeWidth = 2f
    )
    drawLine(
        color = Color.White.copy(alpha = 0.4f),
        start = topLeft,
        end = Offset(topLeft.x, topLeft.y + cellSize),
        strokeWidth = 2f
    )
    // Bottom-right shadow
    drawLine(
        color = Color.Black.copy(alpha = 0.6f),
        start = Offset(topLeft.x, topLeft.y + cellSize),
        end = Offset(topLeft.x + cellSize, topLeft.y + cellSize),
        strokeWidth = 2.5f
    )
    drawLine(
        color = Color.Black.copy(alpha = 0.6f),
        start = Offset(topLeft.x + cellSize, topLeft.y),
        end = Offset(topLeft.x + cellSize, topLeft.y + cellSize),
        strokeWidth = 2.5f
    )

    if (isStartCell) {
        // Start Arrow / Emblem indicator
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = cellSize * 0.15f,
            center = Offset(topLeft.x + cellSize / 2f, topLeft.y + cellSize / 2f)
        )
    }
}

/**
 * Draws the 3x3 central victory triangle where all four colors meet with 3D depth.
 */
private fun DrawScope.draw3DCenterVictoryGoal(cellSize: Float) {
    val centerTopLeft = Offset(6 * cellSize, 6 * cellSize)
    val centerSide = cellSize * 3

    val pathRed = Path().apply {
        moveTo(centerTopLeft.x, centerTopLeft.y)
        lineTo(centerTopLeft.x + centerSide / 2f, centerTopLeft.y + centerSide / 2f)
        lineTo(centerTopLeft.x, centerTopLeft.y + centerSide)
        close()
    }
    val pathGreen = Path().apply {
        moveTo(centerTopLeft.x, centerTopLeft.y)
        lineTo(centerTopLeft.x + centerSide / 2f, centerTopLeft.y + centerSide / 2f)
        lineTo(centerTopLeft.x + centerSide, centerTopLeft.y)
        close()
    }
    val pathYellow = Path().apply {
        moveTo(centerTopLeft.x + centerSide, centerTopLeft.y)
        lineTo(centerTopLeft.x + centerSide / 2f, centerTopLeft.y + centerSide / 2f)
        lineTo(centerTopLeft.x + centerSide, centerTopLeft.y + centerSide)
        close()
    }
    val pathBlue = Path().apply {
        moveTo(centerTopLeft.x, centerTopLeft.y + centerSide)
        lineTo(centerTopLeft.x + centerSide / 2f, centerTopLeft.y + centerSide / 2f)
        lineTo(centerTopLeft.x + centerSide, centerTopLeft.y + centerSide)
        close()
    }

    drawPath(path = pathRed, color = LudoRed.copy(alpha = 0.9f))
    drawPath(path = pathGreen, color = LudoGreen.copy(alpha = 0.9f))
    drawPath(path = pathYellow, color = LudoYellow.copy(alpha = 0.9f))
    drawPath(path = pathBlue, color = LudoBlueVibrant.copy(alpha = 0.9f))

    // 3D Golden Victory Ring
    drawCircle(
        color = GoldPrimary,
        radius = cellSize * 0.5f,
        center = Offset(7.5f * cellSize, 7.5f * cellSize),
        style = Stroke(width = 5f)
    )
}

/**
 * Draws the 3D star safe zone markers where tokens cannot be captured.
 */
private fun DrawScope.draw3DSafeZoneStars(cellSize: Float) {
    val starCoords = listOf(
        Pair(6, 2), Pair(8, 2),    // Red / Blue side stars
        Pair(2, 6), Pair(2, 8),    // Red / Green top stars
        Pair(6, 12), Pair(8, 12),  // Green / Yellow side stars
        Pair(12, 6), Pair(12, 8)   // Blue / Yellow bottom stars
    )

    starCoords.forEach { (r, c) ->
        val cX = (c + 0.5f) * cellSize
        val cY = (r + 0.5f) * cellSize
        // 3D Drop shadow for star
        draw3DStarPath(cX + 2f, cY + 2f, cellSize * 0.22f, Color.Black.copy(alpha = 0.7f))
        // Foreground 3D Golden Star
        draw3DStarPath(cX, cY, cellSize * 0.22f, GoldPrimary)
    }
}

private fun DrawScope.draw3DStarPath(cX: Float, cY: Float, r: Float, starColor: Color) {
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

        val iX = cX + (r / 2.2f) * cos(radInner).toFloat()
        val iY = cY + (r / 2.2f) * sin(radInner).toFloat()
        path.lineTo(iX, iY)
        angle += angleInc
    }
    path.close()
    drawPath(path = path, color = starColor)
}

/**
 * Draws delicate grid lines with 3D chiseled groove styling.
 */
private fun DrawScope.draw3DGridTrackBorders(cellSize: Float) {
    for (row in 6..9) {
        drawLine(color = Color.Black.copy(alpha = 0.5f), start = Offset(0f, row * cellSize + 1f), end = Offset(15 * cellSize, row * cellSize + 1f), strokeWidth = 2.5f)
        drawLine(color = GlassCardBorder, start = Offset(0f, row * cellSize), end = Offset(15 * cellSize, row * cellSize), strokeWidth = 1.5f)
    }
    for (col in 6..9) {
        drawLine(color = Color.Black.copy(alpha = 0.5f), start = Offset(col * cellSize + 1f, 0f), end = Offset(col * cellSize + 1f, 15 * cellSize), strokeWidth = 2.5f)
        drawLine(color = GlassCardBorder, start = Offset(col * cellSize, 0f), end = Offset(col * cellSize, 15 * cellSize), strokeWidth = 1.5f)
    }

    for (row in 0..14) {
        if (row in 6..8) {
            for (col in 0..14) {
                drawLine(color = GlassCardBorder.copy(alpha = 0.6f), start = Offset(col * cellSize, row * cellSize), end = Offset(col * cellSize, (row + 1) * cellSize), strokeWidth = 1.2f)
            }
        } else {
            for (col in 6..8) {
                drawLine(color = GlassCardBorder.copy(alpha = 0.6f), start = Offset(col * cellSize, row * cellSize), end = Offset((col + 1) * cellSize, row * cellSize), strokeWidth = 1.2f)
                drawLine(color = GlassCardBorder.copy(alpha = 0.6f), start = Offset(col * cellSize, row * cellSize), end = Offset(col * cellSize, (row + 1) * cellSize), strokeWidth = 1.2f)
                drawLine(color = GlassCardBorder.copy(alpha = 0.6f), start = Offset((col + 1) * cellSize, row * cellSize), end = Offset((col + 1) * cellSize, (row + 1) * cellSize), strokeWidth = 1.2f)
            }
        }
    }
}

/**
 * Draws an authentic 3D CHESS PIECE (Staunton-style Royal Pawn / Knight Finial).
 * 
 * Features:
 * 1. Board Drop Shadow: Cast onto board tile, enlarges & detaches when hopping/moving in 3D
 * 2. Stepped Pedestal Base: Multi-tiered rounded base with specular highlight
 * 3. Graceful Tapered Waist (Body): Curving stem with 3D cylindrical lighting & reflection streak
 * 4. Collar Bead Ring: Golden torus ring around the neck
 * 5. Spherical Head (Finial Ball): 3D sphere with radial gradient and specular glint
 * 6. Royal Crown Crest: Topped with a golden crown finial
 * 7. 3D Hop Animation: Vertical elevation (-Y) and dynamic scale during stepping
 */
private fun DrawScope.draw3DChessPiece(
    token: LudoToken,
    center: Offset,
    cellSize: Float,
    isMovable: Boolean,
    isMoving: Boolean,
    pulseScale: Float,
    pulseHaloAlpha: Float,
    bobbingY: Float
) {
    val pieceHeight = cellSize * 0.95f
    val currentScale = if (isMoving) 1.35f else if (isMovable) pulseScale else 1.0f

    // 3D Parabolic Jump Elevation
    val jumpElevationY = if (isMoving) -pieceHeight * 0.55f else bobbingY
    val effectiveCenter = center + Offset(0f, jumpElevationY)

    val baseWidth = cellSize * 0.72f * currentScale
    val baseHeight = cellSize * 0.22f * currentScale
    val waistWidth = cellSize * 0.28f * currentScale
    val neckWidth = cellSize * 0.22f * currentScale
    val headRadius = cellSize * 0.22f * currentScale

    // 1. BOARD DROP SHADOW (Stays on board plane, grows larger & softer when jumping)
    val shadowAlpha = if (isMoving) 0.22f else if (isMovable) 0.35f else 0.55f
    val shadowWidth = if (isMoving) baseWidth * 1.4f else baseWidth * 1.05f
    val shadowHeight = if (isMoving) baseHeight * 1.5f else baseHeight
    val shadowCenter = Offset(center.x + 3f, center.y + pieceHeight * 0.28f)

    drawOval(
        color = Color.Black.copy(alpha = shadowAlpha),
        topLeft = Offset(shadowCenter.x - shadowWidth / 2f, shadowCenter.y - shadowHeight / 2f),
        size = Size(shadowWidth, shadowHeight)
    )

    // Movable Golden Aura beacon disc on board tile
    if (isMovable && pulseHaloAlpha > 0f) {
        drawCircle(
            color = GoldPrimary.copy(alpha = pulseHaloAlpha * 0.7f),
            radius = baseWidth * 0.85f,
            center = center + Offset(0f, pieceHeight * 0.28f),
            style = Stroke(width = 3.5f)
        )
    }

    // 2. STEPPED PEDESTAL BASE (Bottom-most disc)
    val baseY = effectiveCenter.y + pieceHeight * 0.22f
    val baseTopLeft = Offset(effectiveCenter.x - baseWidth / 2f, baseY - baseHeight / 2f)

    // Dark under-rim of base
    drawOval(
        color = Color(0xFF100C16),
        topLeft = baseTopLeft + Offset(0f, 3f),
        size = Size(baseWidth, baseHeight)
    )

    // Base upper platform with radial metallic lighting
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(token.color.color, token.color.color.copy(alpha = 0.75f), Color(0xFF1C1424)),
            center = Offset(effectiveCenter.x - baseWidth * 0.2f, baseY - baseHeight * 0.2f),
            radius = baseWidth * 0.7f
        ),
        topLeft = baseTopLeft,
        size = Size(baseWidth, baseHeight)
    )

    // Golden rim line along base edge
    drawOval(
        color = GoldPrimary,
        topLeft = baseTopLeft,
        size = Size(baseWidth, baseHeight),
        style = Stroke(width = 2.5f)
    )

    // 3. SECOND PEDESTAL TIER (Collar of base)
    val tier2Width = baseWidth * 0.72f
    val tier2Height = baseHeight * 0.65f
    val tier2Y = baseY - baseHeight * 0.45f
    drawOval(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFE0C068), GoldPrimary, Color(0xFF7A5816)),
            start = Offset(effectiveCenter.x - tier2Width / 2f, tier2Y),
            end = Offset(effectiveCenter.x + tier2Width / 2f, tier2Y)
        ),
        topLeft = Offset(effectiveCenter.x - tier2Width / 2f, tier2Y - tier2Height / 2f),
        size = Size(tier2Width, tier2Height)
    )

    // 4. CONCAVE TAPERED WAIST (Pawn Trunk Body)
    val bodyBottomY = tier2Y
    val bodyTopY = effectiveCenter.y - pieceHeight * 0.16f

    val bodyPath = Path().apply {
        moveTo(effectiveCenter.x - tier2Width * 0.42f, bodyBottomY)
        quadraticBezierTo(
            effectiveCenter.x - waistWidth * 0.35f,
            (bodyBottomY + bodyTopY) / 2f,
            effectiveCenter.x - neckWidth / 2f,
            bodyTopY
        )
        lineTo(effectiveCenter.x + neckWidth / 2f, bodyTopY)
        quadraticBezierTo(
            effectiveCenter.x + waistWidth * 0.35f,
            (bodyBottomY + bodyTopY) / 2f,
            effectiveCenter.x + tier2Width * 0.42f,
            bodyBottomY
        )
        close()
    }

    // Cylindrical 3D lighting gradient: dark on right, jewel in center, specular highlight on left
    drawPath(
        path = bodyPath,
        brush = Brush.linearGradient(
            0.0f to Color.White.copy(alpha = 0.6f),
            0.25f to token.color.color,
            0.7f to token.color.color.copy(alpha = 0.85f),
            1.0f to Color(0xFF150E1E),
            start = Offset(effectiveCenter.x - baseWidth / 2f, 0f),
            end = Offset(effectiveCenter.x + baseWidth / 2f, 0f)
        )
    )

    // 5. NECK COLLAR BEAD RING
    val collarWidth = neckWidth * 1.3f
    val collarHeight = baseHeight * 0.55f
    drawOval(
        brush = Brush.linearGradient(
            colors = listOf(Color.White, GoldPrimary, Color(0xFF6E5014)),
            start = Offset(effectiveCenter.x - collarWidth / 2f, bodyTopY),
            end = Offset(effectiveCenter.x + collarWidth / 2f, bodyTopY)
        ),
        topLeft = Offset(effectiveCenter.x - collarWidth / 2f, bodyTopY - collarHeight / 2f),
        size = Size(collarWidth, collarHeight)
    )

    // 6. SPHERICAL HEAD (Pawn Finial Ball with 3D Specular Highlight)
    val headCenter = Offset(effectiveCenter.x, bodyTopY - headRadius * 0.85f)

    // Head Sphere with 3D Radial Lighting
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f),
                token.color.color,
                token.color.color.copy(alpha = 0.8f),
                Color(0xFF140D1C)
            ),
            center = headCenter - Offset(headRadius * 0.32f, headRadius * 0.35f),
            radius = headRadius * 1.1f
        ),
        radius = headRadius,
        center = headCenter
    )

    // Golden Halo rim on head
    drawCircle(
        color = GoldPrimary.copy(alpha = 0.85f),
        radius = headRadius,
        center = headCenter,
        style = Stroke(width = 2.2f)
    )

    // White specular reflection dot (glass glint)
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = headRadius * 0.28f,
        center = headCenter - Offset(headRadius * 0.35f, headRadius * 0.38f)
    )

    // 7. ROYAL CROWN FINIAL ON TOP OF HEAD
    val crownY = headCenter.y - headRadius * 0.95f
    val crownW = headRadius * 0.8f
    val crownH = headRadius * 0.55f
    val crownPath = Path().apply {
        moveTo(headCenter.x - crownW / 2f, crownY + crownH * 0.5f)
        lineTo(headCenter.x - crownW * 0.45f, crownY - crownH * 0.5f)
        lineTo(headCenter.x - crownW * 0.15f, crownY)
        lineTo(headCenter.x, crownY - crownH * 0.9f) // Middle Crown Spire
        lineTo(headCenter.x + crownW * 0.15f, crownY)
        lineTo(headCenter.x + crownW * 0.45f, crownY - crownH * 0.5f)
        lineTo(headCenter.x + crownW / 2f, crownY + crownH * 0.5f)
        close()
    }
    drawPath(path = crownPath, color = GoldPrimary)
}
