package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun LudoBoardComponent(
    tokens: List<LudoToken>,
    movableTokenIds: List<Int>,
    currentTurnColor: PlayerColor,
    onTokenClicked: (LudoToken) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation for active clickable tokens
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(16.dp, RoundedCornerShape(12.dp))
            .background(GlassCard, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        val boardSize = min(maxWidth.value, maxHeight.value).dp
        val boardSizePx = with(LocalDensity.current) { boardSize.toPx() }
        val cellSizePx = boardSizePx / 15f

        // Group tokens by grid positions for stacking
        val positionGroups = remember(tokens) {
            tokens.groupBy { getTokenGridPosition(it) }
        }

        Canvas(
            modifier = Modifier
                .size(boardSize)
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(tokens, movableTokenIds) {
                    detectTapGestures { offset ->
                        val col = (offset.x / cellSizePx).toInt()
                        val row = (offset.y / cellSizePx).toInt()
                        val tappedPos = Pair(row, col)

                        // Find tokens at tapped position
                        val tokensAtPos = positionGroups[tappedPos] ?: emptyList()
                        // Filter for current player's token that is movable
                        val targetToken = tokensAtPos.firstOrNull { token ->
                            token.color == currentTurnColor && movableTokenIds.contains(token.id)
                        }
                        if (targetToken != null) {
                            onTokenClicked(targetToken)
                        }
                    }
                }
        ) {
            // 1. Draw Background & Paths
            drawLudoDecorations(cellSizePx)

            // 2. Draw Safe Zone Stars
            drawSafeStars(cellSizePx)

            // 3. Draw Grid Lines with Glassmorphism Border Colors
            drawGridBorders(cellSizePx)

            // 4. Draw Tokens (Stacked if multiple reside on the same coordinates)
            positionGroups.forEach { (gridPos, tokenList) ->
                val centerOffset = Offset(
                    (gridPos.second + 0.5f) * cellSizePx,
                    (gridPos.first + 0.5f) * cellSizePx
                )

                if (tokenList.size == 1) {
                    val token = tokenList.first()
                    val isMovable = token.color == currentTurnColor && movableTokenIds.contains(token.id)
                    drawTokenCircle(
                        token = token,
                        center = centerOffset,
                        radius = cellSizePx * 0.38f,
                        scale = if (isMovable) pulseScale else 1.0f,
                        haloAlpha = if (isMovable) pulseAlpha else 0f
                    )
                } else {
                    // Stacking layout: draw mini-spheres offset around the center cell
                    val subRadius = cellSizePx * 0.22f
                    val count = tokenList.size
                    tokenList.forEachIndexed { index, token ->
                        val angle = (360f / count) * index
                        val rad = Math.toRadians(angle.toDouble())
                        val offsetDistance = cellSizePx * 0.20f
                        val miniCenter = Offset(
                            centerOffset.x + (offsetDistance * cos(rad)).toFloat(),
                            centerOffset.y + (offsetDistance * sin(rad)).toFloat()
                        )
                        val isMovable = token.color == currentTurnColor && movableTokenIds.contains(token.id)

                        drawTokenCircle(
                            token = token,
                            center = miniCenter,
                            radius = subRadius,
                            scale = if (isMovable) pulseScale else 1.0f,
                            haloAlpha = if (isMovable) pulseAlpha else 0f
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawLudoDecorations(cellSize: Float) {
    // Fill deep dark background
    drawRect(color = DeepDarkBg, size = size)

    // Base Quadrants Colors
    val redPaint = Brush.radialGradient(colors = listOf(LudoRed, Color(0xFF8B0024)), center = Offset(cellSize * 3, cellSize * 3), radius = cellSize * 5)
    val greenPaint = Brush.radialGradient(colors = listOf(LudoGreen, Color(0xFF006D60)), center = Offset(cellSize * 12, cellSize * 3), radius = cellSize * 5)
    val yellowPaint = Brush.radialGradient(colors = listOf(LudoYellow, Color(0xFFB59300)), center = Offset(cellSize * 12, cellSize * 12), radius = cellSize * 5)
    val bluePaint = Brush.radialGradient(colors = listOf(LudoBlueVibrant, Color(0xFF0D5EBA)), center = Offset(cellSize * 3, cellSize * 12), radius = cellSize * 5)

    // Draw Corner Yards (6x6 cells each)
    drawRect(brush = redPaint, topLeft = Offset(0f, 0f), size = Size(cellSize * 6, cellSize * 6))
    drawRect(brush = greenPaint, topLeft = Offset(cellSize * 9, 0f), size = Size(cellSize * 6, cellSize * 6))
    drawRect(brush = yellowPaint, topLeft = Offset(cellSize * 9, cellSize * 9), size = Size(cellSize * 6, cellSize * 6))
    drawRect(brush = bluePaint, topLeft = Offset(0f, cellSize * 9), size = Size(cellSize * 6, cellSize * 6))

    // Draw inner neon-colored bases within yards
    val innerYOffset = cellSize * 0.75f
    val innerSize = cellSize * 4.5f
    drawRoundRect(color = GlassCard, topLeft = Offset(innerYOffset, innerYOffset), size = Size(innerSize, innerSize), cornerRadius = CornerRadius(16f, 16f))
    drawRoundRect(color = GlassCard, topLeft = Offset(cellSize * 9 + innerYOffset, innerYOffset), size = Size(innerSize, innerSize), cornerRadius = CornerRadius(16f, 16f))
    drawRoundRect(color = GlassCard, topLeft = Offset(cellSize * 9 + innerYOffset, cellSize * 9 + innerYOffset), size = Size(innerSize, innerSize), cornerRadius = CornerRadius(16f, 16f))
    drawRoundRect(color = GlassCard, topLeft = Offset(innerYOffset, cellSize * 9 + innerYOffset), size = Size(innerSize, innerSize), cornerRadius = CornerRadius(16f, 16f))

    // Yard token circle outlines to represent slots
    val redSlotBr = Brush.linearGradient(colors = listOf(LudoRed, GoldPrimary))
    for (i in 0..3) {
        val rPos = getYardPositions(PlayerColor.RED)[i]
        val gPos = getYardPositions(PlayerColor.GREEN)[i]
        val yPos = getYardPositions(PlayerColor.YELLOW)[i]
        val bPos = getYardPositions(PlayerColor.BLUE)[i]

        val rim = cellSize * 0.33f
        drawCircle(brush = redSlotBr, radius = rim, center = Offset((rPos.second + 0.5f) * cellSize, (rPos.first + 0.5f) * cellSize), style = Stroke(width = 4f))
        drawCircle(color = LudoGreen, radius = rim, center = Offset((gPos.second + 0.5f) * cellSize, (gPos.first + 0.5f) * cellSize), style = Stroke(width = 4f))
        drawCircle(color = LudoYellow, radius = rim, center = Offset((yPos.second + 0.5f) * cellSize, (yPos.first + 0.5f) * cellSize), style = Stroke(width = 4f))
        drawCircle(color = LudoBlueVibrant, radius = rim, center = Offset((bPos.second + 0.5f) * cellSize, (bPos.first + 0.5f) * cellSize), style = Stroke(width = 4f))
    }

    // 2. Draw Home Runs (Colored Paths)
    // Red Home Run: Row 7, Col 1..5
    for (col in 1..5) {
        drawRect(color = LudoRed.copy(alpha = 0.85f), topLeft = Offset(col * cellSize, 7 * cellSize), size = Size(cellSize, cellSize))
    }
    // Green Home Run: Row 1..5, Col 7
    for (row in 1..5) {
        drawRect(color = LudoGreen.copy(alpha = 0.85f), topLeft = Offset(7 * cellSize, row * cellSize), size = Size(cellSize, cellSize))
    }
    // Yellow Home Run: Row 7, Col 9..13
    for (col in 9..13) {
        drawRect(color = LudoYellow.copy(alpha = 0.85f), topLeft = Offset(col * cellSize, 7 * cellSize), size = Size(cellSize, cellSize))
    }
    // Blue Home Run: Row 9..13, Col 7
    for (row in 9..13) {
        drawRect(color = LudoBlueVibrant.copy(alpha = 0.85f), topLeft = Offset(7 * cellSize, row * cellSize), size = Size(cellSize, cellSize))
    }

    // 3. Draw Start cells
    drawRect(color = LudoRed, topLeft = Offset(1 * cellSize, 6 * cellSize), size = Size(cellSize, cellSize))
    drawRect(color = LudoGreen, topLeft = Offset(8 * cellSize, 1 * cellSize), size = Size(cellSize, cellSize))
    drawRect(color = LudoYellow, topLeft = Offset(13 * cellSize, 8 * cellSize), size = Size(cellSize, cellSize))
    drawRect(color = LudoBlueVibrant, topLeft = Offset(6 * cellSize, 13 * cellSize), size = Size(cellSize, cellSize))

    // 4. Center Home Triangles (Goal area at grid 6..8, 6..8)
    val centerTopLeft = Offset(6 * cellSize, 6 * cellSize)
    val centerSide = cellSize * 3

    // Triangle paths meeting at the core
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

    drawPath(path = pathRed, color = LudoRed)
    drawPath(path = pathGreen, color = LudoGreen)
    drawPath(path = pathYellow, color = LudoYellow)
    drawPath(path = pathBlue, color = LudoBlueVibrant)

    // Gold Center Core Border
    drawCircle(color = GoldPrimary, radius = cellSize * 0.4f, center = Offset(7.5f * cellSize, 7.5f * cellSize), style = Stroke(width = 6f))
}

private fun DrawScope.drawSafeStars(cellSize: Float) {
    // Coordinates of stars (safe tiles indices)
    val starsColRows = listOf(
        Pair(6, 2), Pair(8, 2),    // Left zones
        Pair(2, 6), Pair(2, 8),    // Top zones
        Pair(6, 12), Pair(8, 12),  // Right zones
        Pair(12, 6), Pair(12, 8)   // Bottom zones
    )

    starsColRows.forEach { (row, col) ->
        val cX = (col + 0.5f) * cellSize
        val cY = (row + 0.5f) * cellSize
        drawStarSign(cX, cY, cellSize * 0.22f)
    }
}

private fun DrawScope.drawStarSign(cX: Float, cY: Float, r: Float) {
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
    drawPath(path = path, color = GoldPrimary)
}

private fun DrawScope.drawGridBorders(cellSize: Float) {
    // Horizontal tracking paths borders within cols 0..15, rows 6,7,8
    for (row in 6..9) {
        drawLine(color = GlassCardBorder, start = Offset(0f, row * cellSize), end = Offset(15 * cellSize, row * cellSize), strokeWidth = 2.5f)
    }
    for (col in 6..9) {
        drawLine(color = GlassCardBorder, start = Offset(col * cellSize, 0f), end = Offset(col * cellSize, 15 * cellSize), strokeWidth = 2.5f)
    }

    // Vertical tracks individual boxes borders
    for (row in 0..14) {
        if (row in 6..8) {
            for (col in 0..14) {
                drawLine(color = GlassCardBorder, start = Offset(col * cellSize, row * cellSize), end = Offset(col * cellSize, (row + 1) * cellSize), strokeWidth = 1.5f)
            }
        } else {
            for (col in 6..8) {
                drawLine(color = GlassCardBorder, start = Offset(col * cellSize, row * cellSize), end = Offset((col + 1) * cellSize, row * cellSize), strokeWidth = 1.5f)
                drawLine(color = GlassCardBorder, start = Offset(col * cellSize, row * cellSize), end = Offset(col * cellSize, (row + 1) * cellSize), strokeWidth = 1.5f)
                drawLine(color = GlassCardBorder, start = Offset((col + 1) * cellSize, row * cellSize), end = Offset((col + 1) * cellSize, (row + 1) * cellSize), strokeWidth = 1.5f)
            }
        }
    }

    // Yard outer glows
    drawRoundRect(color = GoldPrimary.copy(alpha = 0.3f), topLeft = Offset(0f, 0f), size = Size(cellSize * 6, cellSize * 6), style = Stroke(width = 4f), cornerRadius = CornerRadius(8f, 8f))
    drawRoundRect(color = NeonCyan.copy(alpha = 0.3f), topLeft = Offset(cellSize * 9, 0f), size = Size(cellSize * 6, cellSize * 6), style = Stroke(width = 4f), cornerRadius = CornerRadius(8f, 8f))
    drawRoundRect(color = LudoYellow.copy(alpha = 0.3f), topLeft = Offset(cellSize * 9, cellSize * 9), size = Size(cellSize * 6, cellSize * 6), style = Stroke(width = 4f), cornerRadius = CornerRadius(8f, 8f))
    drawRoundRect(color = LudoBlueVibrant.copy(alpha = 0.3f), topLeft = Offset(0f, cellSize * 9), size = Size(cellSize * 6, cellSize * 6), style = Stroke(width = 4f), cornerRadius = CornerRadius(8f, 8f))
}

private fun DrawScope.drawTokenCircle(
    token: LudoToken,
    center: Offset,
    radius: Float,
    scale: Float,
    haloAlpha: Float
) {
    val activeRadius = radius * scale

    // Movable Halo highlight
    if (haloAlpha > 0f) {
        drawCircle(
            color = GoldPrimary.copy(alpha = haloAlpha),
            radius = activeRadius + 12f,
            center = center
        )
    }

    // Outer gold rims
    drawCircle(
        color = GoldPrimary,
        radius = activeRadius + 2f,
        center = center,
        style = Stroke(width = 4f)
    )

    // Inner filled color
    val colorBrush = Brush.radialGradient(
        colors = listOf(token.color.color, token.color.color.copy(alpha = 0.7f)),
        center = center,
        radius = activeRadius
    )
    drawCircle(
        brush = colorBrush,
        radius = activeRadius,
        center = center
    )

    // Inner visual concentric shine
    drawCircle(
        color = Color.White.copy(alpha = 0.61f),
        radius = activeRadius * 0.45f,
        center = center - Offset(activeRadius * 0.15f, activeRadius * 0.15f)
    )

    // Visual Crown Core for premium 'Royal' branding
    val crownR = activeRadius * 0.3f
    val path = Path().apply {
        moveTo(center.x - crownR, center.y + crownR * 0.5f)
        lineTo(center.x - crownR * 0.8f, center.y - crownR * 0.5f)
        lineTo(center.x - crownR * 0.3f, center.y)
        lineTo(center.x, center.y - crownR * 0.8f) // Middle spike
        lineTo(center.x + crownR * 0.3f, center.y)
        lineTo(center.x + crownR * 0.8f, center.y - crownR * 0.5f)
        lineTo(center.x + crownR, center.y + crownR * 0.5f)
        close()
    }
    drawPath(path = path, color = Color.White)
}
