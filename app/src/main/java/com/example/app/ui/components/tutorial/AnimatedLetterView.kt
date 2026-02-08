package com.example.app.ui.components.tutorial

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val TAG = "AnimatedLetterView"

@Composable
fun AnimatedLetterView(
    letter: Char,
    modifier: Modifier = Modifier,
    isUpperCase: Boolean = true,
    strokeColor: Color = Color.Black,
    numberColor: Color = Color.White,
    circleColor: Color = Color.Black,
    loopAnimation: Boolean = true,
    loopDelay: Int = 1500,
    showGuidingHand: Boolean = true
) {
    var currentStroke by remember { mutableIntStateOf(0) }
    var animProgress by remember { mutableFloatStateOf(0f) }
    var animationFailed by remember { mutableStateOf(false) }
    var handPosition by remember { mutableStateOf<Offset?>(null) }
    var canvasMinDimension by remember { mutableFloatStateOf(0f) }
    
    val targetLetter = if (isUpperCase) letter.uppercaseChar() else letter.lowercaseChar()
    
    // Main animation loop
    LaunchedEffect(targetLetter) {
        // Reset all state when letter changes
        currentStroke = 0
        animProgress = 0f
        animationFailed = false
        handPosition = null
        
        try {
            val strokes = getSimpleLetterStrokes(targetLetter)
            if (strokes.isEmpty()) {
                Log.w(TAG, "No strokes found for letter: $targetLetter")
                animationFailed = true
                handPosition = null
                return@LaunchedEffect
            }
            
            // Calculate dynamic timing to ensure exactly 3 seconds total per letter
            val totalTimeMs = 3000L
            val strokeCount = strokes.size
            val delayBetweenStrokesMs = 150L // Short pause between strokes
            val totalDelayTime = (strokeCount - 1) * delayBetweenStrokesMs
            val strokeDurationMs = (totalTimeMs - totalDelayTime) / strokeCount
            
            Log.d(TAG, "Starting animation for $targetLetter with $strokeCount strokes, strokeDuration=${strokeDurationMs}ms")
            
            while (isActive) {
                // Animate all strokes
                for (strokeIndex in strokes.indices) {
                    if (!isActive) break
                    
                    currentStroke = strokeIndex
                    
                    // Animate current stroke from 0 to 1
                    val steps = 60 // More steps for smoother animation, especially for straight lines
                    repeat(steps) { step ->
                        if (!isActive) return@LaunchedEffect
                        animProgress = (step + 1f) / steps
                        delay((strokeDurationMs / steps).toLong())
                    }
                    
                    // Keep stroke complete before moving to next
                    animProgress = 1f
                    if (strokeIndex < strokes.size - 1) {
                        handPosition = null // Hide hand between strokes
                        delay(delayBetweenStrokesMs)
                    }
                }
                
                // All strokes complete
                currentStroke = strokes.size
                handPosition = null // Hide hand when complete
                
                // Loop or stop
                if (loopAnimation && isActive) {
                    delay(loopDelay.toLong())
                    currentStroke = 0
                    animProgress = 0f
                    handPosition = null
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Animation error for $targetLetter", e)
            animationFailed = true
            currentStroke = 0
            animProgress = 0f
            handPosition = null
        }
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (animationFailed) {
                // Fallback: just show letter outline
                Log.w(TAG, "Animation failed, showing static letter")
                handPosition = null
                return@Canvas
            }
            
            try {
                val strokes = getSimpleLetterStrokes(targetLetter)
                if (strokes.isEmpty()) {
                    handPosition = null
                    return@Canvas
                }
                
                // Use the smaller dimension to maintain aspect ratio and center the letter
                val canvasWidth = size.width
                val canvasHeight = size.height
                val minDimension = minOf(canvasWidth, canvasHeight)
                
                if (minDimension <= 0f) {
                    handPosition = null
                    return@Canvas
                }
                
                // Center the letter in the available space
                val offsetX = (canvasWidth - minDimension) / 2f
                val offsetY = (canvasHeight - minDimension) / 2f
                
                // Store minDimension for hand sizing
                canvasMinDimension = minDimension
                
                val strokeWidth = minDimension * 0.08f
            
            // Draw completed strokes
            for (i in 0 until currentStroke.coerceIn(0, strokes.size)) {
                try {
                    val points = strokes[i]
                    val path = Path()
                    
                    if (points.isNotEmpty()) {
                        path.moveTo(offsetX + points[0].x * minDimension, offsetY + points[0].y * minDimension)
                        for (j in 1 until points.size) {
                            path.lineTo(offsetX + points[j].x * minDimension, offsetY + points[j].y * minDimension)
                        }
                        
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error drawing completed stroke $i", e)
                }
            }
            
            // Draw current animating stroke
            if (currentStroke < strokes.size && animProgress > 0f) {
                try {
                    val points = strokes[currentStroke]
                    if (points.isNotEmpty()) {
                        val path = Path()
                        path.moveTo(offsetX + points[0].x * minDimension, offsetY + points[0].y * minDimension)
                        
                        // Calculate how many points to draw based on progress
                        val totalSegments = points.size - 1
                        val segmentsToShow = (totalSegments * animProgress).toInt()
                        val partialSegment = (totalSegments * animProgress) - segmentsToShow
                        
                        // Calculate hand position at the current animation point
                        if (showGuidingHand) {
                            val currentPointIndex = segmentsToShow.coerceIn(0, points.size - 1)
                            if (currentPointIndex < points.size - 1) {
                                val startPoint = points[currentPointIndex]
                                val endPoint = points[currentPointIndex + 1]
                                // Position the hand so finger tip is at the stroke point
                                val strokeX = offsetX + (startPoint.x + (endPoint.x - startPoint.x) * partialSegment) * minDimension
                                val strokeY = offsetY + (startPoint.y + (endPoint.y - startPoint.y) * partialSegment) * minDimension
                                handPosition = Offset(strokeX, strokeY)
                            } else if (currentPointIndex == points.size - 1) {
                                // At the last point
                                val lastPoint = points[currentPointIndex]
                                handPosition = Offset(
                                    offsetX + lastPoint.x * minDimension, 
                                    offsetY + lastPoint.y * minDimension
                                )
                            }
                        } else {
                            handPosition = null
                        }
                        
                        // Draw complete segments
                        for (j in 1..segmentsToShow.coerceAtMost(points.size - 1)) {
                            path.lineTo(offsetX + points[j].x * minDimension, offsetY + points[j].y * minDimension)
                        }
                        
                        // Draw partial segment
                        if (segmentsToShow < totalSegments && segmentsToShow + 1 < points.size) {
                            val startPoint = points[segmentsToShow]
                            val endPoint = points[segmentsToShow + 1]
                            val partialX = startPoint.x + (endPoint.x - startPoint.x) * partialSegment
                            val partialY = startPoint.y + (endPoint.y - startPoint.y) * partialSegment
                            path.lineTo(offsetX + partialX * minDimension, offsetY + partialY * minDimension)
                        }
                        
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        
                        // Draw number indicator at start of current stroke
                        if (points.isNotEmpty()) {
                            val startX = offsetX + points[0].x * minDimension
                            val startY = offsetY + points[0].y * minDimension
                            val radius = minDimension * 0.06f
                            
                            drawCircle(
                                color = circleColor,
                                radius = radius,
                                center = Offset(startX, startY)
                            )
                            
                            drawContext.canvas.nativeCanvas.apply {
                                val paint = android.graphics.Paint().apply {
                                    color = numberColor.toArgb()
                                    textSize = radius * 1.2f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                    isFakeBoldText = true
                                }
                                drawText(
                                    (currentStroke + 1).toString(),
                                    startX,
                                    startY + (paint.textSize / 3),
                                    paint
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error drawing animated stroke", e)
                }
            } else {
                // No stroke animating, hide hand
                handPosition = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Canvas drawing error", e)
            handPosition = null
        }
        }
        
        // Guiding hand overlay - scaled relative to letter size
        handPosition?.let { pos ->
            if (showGuidingHand && canvasMinDimension > 0f) {
                val density = LocalDensity.current
                // Scale hand size to 150% of the letter drawing area for very prominent close-up perspective
                val handSizePx = canvasMinDimension * 1.5f
                val handSize = with(density) { handSizePx.toDp() }
                
                // Offset to position finger tip (finger tip is at top-left of hand image)
                val fingerTipOffsetX = handSizePx * 0.15f // Finger tip is 15% from left edge
                val fingerTipOffsetY = handSizePx * 0.15f // Finger tip is 15% from top edge
                
                Image(
                    painter = painterResource(id = R.drawable.ic_kusho_hand),
                    contentDescription = "Guiding hand",
                    modifier = Modifier
                        .size(handSize)
                        .offset(
                            x = with(density) { (pos.x - fingerTipOffsetX).toDp() },
                            y = with(density) { (pos.y - fingerTipOffsetY).toDp() }
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

// Helper to convert bezier curves to pre-calculated smooth points
private data class Point(val x: Float, val y: Float)

private fun calculateCubicBezierPoints(
    start: Point, 
    control1: Point, 
    control2: Point, 
    end: Point, 
    segments: Int = 25
): List<Point> {
    val points = mutableListOf<Point>()
    for (i in 0..segments) {
        val t = i.toFloat() / segments
        val oneMinusT = 1f - t
        
        // Cubic bezier formula
        val x = oneMinusT * oneMinusT * oneMinusT * start.x +
                3f * oneMinusT * oneMinusT * t * control1.x +
                3f * oneMinusT * t * t * control2.x +
                t * t * t * end.x
                
        val y = oneMinusT * oneMinusT * oneMinusT * start.y +
                3f * oneMinusT * oneMinusT * t * control1.y +
                3f * oneMinusT * t * t * control2.y +
                t * t * t * end.y
                
        points.add(Point(x, y))
    }
    return points
}

private fun calculateLinePoints(start: Point, end: Point, segments: Int = 30): List<Point> {
    // Interpolate points for smooth animation of straight lines
    val points = mutableListOf<Point>()
    for (i in 0..segments) {
        val t = i.toFloat() / segments
        val x = start.x + (end.x - start.x) * t
        val y = start.y + (end.y - start.y) * t
        points.add(Point(x, y))
    }
    return points
}

// Pre-calculated smooth letter strokes - using pre-calculated bezier points for smooth curves
private fun getSimpleLetterStrokes(letter: Char): List<List<Point>> {
    return when (letter) {
        'A' -> listOf(
            calculateLinePoints(Point(0.5f, 0.15f), Point(0.2f, 0.85f)),
            calculateLinePoints(Point(0.5f, 0.15f), Point(0.8f, 0.85f)),
            calculateLinePoints(Point(0.32f, 0.58f), Point(0.68f, 0.58f))
        )
        'B' -> listOf(
            calculateLinePoints(Point(0.25f, 0.15f), Point(0.25f, 0.85f)),
            calculateCubicBezierPoints(Point(0.25f, 0.15f), Point(0.6f, 0.15f), Point(0.7f, 0.25f), Point(0.7f, 0.35f)) +
            calculateCubicBezierPoints(Point(0.7f, 0.35f), Point(0.7f, 0.45f), Point(0.6f, 0.5f), Point(0.25f, 0.5f)),
            calculateCubicBezierPoints(Point(0.25f, 0.5f), Point(0.65f, 0.5f), Point(0.75f, 0.6f), Point(0.75f, 0.7f)) +
            calculateCubicBezierPoints(Point(0.75f, 0.7f), Point(0.75f, 0.8f), Point(0.65f, 0.85f), Point(0.25f, 0.85f))
        )
        'C' -> listOf(
            calculateCubicBezierPoints(Point(0.75f, 0.25f), Point(0.35f, 0.1f), Point(0.25f, 0.4f), Point(0.25f, 0.5f)) +
            calculateCubicBezierPoints(Point(0.25f, 0.5f), Point(0.25f, 0.75f), Point(0.4f, 0.9f), Point(0.75f, 0.75f))
        )
        'D' -> listOf(
            calculateLinePoints(Point(0.25f, 0.15f), Point(0.25f, 0.85f)),
            calculateCubicBezierPoints(Point(0.25f, 0.15f), Point(0.7f, 0.15f), Point(0.7f, 0.85f), Point(0.25f, 0.85f))
        )
        'E' -> listOf(
            calculateLinePoints(Point(0.3f, 0.15f), Point(0.3f, 0.85f)),
            calculateLinePoints(Point(0.3f, 0.15f), Point(0.75f, 0.15f)),
            calculateLinePoints(Point(0.3f, 0.5f), Point(0.65f, 0.5f)),
            calculateLinePoints(Point(0.3f, 0.85f), Point(0.75f, 0.85f))
        )
        'F' -> listOf(
            calculateLinePoints(Point(0.3f, 0.15f), Point(0.3f, 0.85f)),
            calculateLinePoints(Point(0.3f, 0.15f), Point(0.75f, 0.15f)),
            calculateLinePoints(Point(0.3f, 0.5f), Point(0.65f, 0.5f))
        )
        'G' -> listOf(
            calculateCubicBezierPoints(Point(0.75f, 0.25f), Point(0.35f, 0.1f), Point(0.25f, 0.4f), Point(0.25f, 0.5f)) +
            calculateCubicBezierPoints(Point(0.25f, 0.5f), Point(0.25f, 0.75f), Point(0.4f, 0.9f), Point(0.7f, 0.75f)) +
            calculateCubicBezierPoints(Point(0.7f, 0.75f), Point(0.75f, 0.68f), Point(0.75f, 0.55f), Point(0.75f, 0.5f)),
            calculateLinePoints(Point(0.55f, 0.5f), Point(0.75f, 0.5f))
        )
        'H' -> listOf(
            calculateLinePoints(Point(0.25f, 0.15f), Point(0.25f, 0.85f)),
            calculateLinePoints(Point(0.75f, 0.15f), Point(0.75f, 0.85f)),
            calculateLinePoints(Point(0.25f, 0.5f), Point(0.75f, 0.5f))
        )
        'I' -> listOf(
            calculateLinePoints(Point(0.5f, 0.15f), Point(0.5f, 0.85f)),
            calculateLinePoints(Point(0.3f, 0.15f), Point(0.7f, 0.15f)),
            calculateLinePoints(Point(0.3f, 0.85f), Point(0.7f, 0.85f))
        )
        'J' -> listOf(
            calculateLinePoints(Point(0.6f, 0.15f), Point(0.6f, 0.65f)) +
            calculateCubicBezierPoints(Point(0.6f, 0.65f), Point(0.6f, 0.85f), Point(0.45f, 0.9f), Point(0.3f, 0.75f)),
            calculateLinePoints(Point(0.4f, 0.15f), Point(0.75f, 0.15f))
        )
        'K' -> listOf(
            calculateLinePoints(Point(0.25f, 0.15f), Point(0.25f, 0.85f)),
            calculateLinePoints(Point(0.75f, 0.15f), Point(0.25f, 0.5f)),
            calculateLinePoints(Point(0.4f, 0.42f), Point(0.75f, 0.85f))
        )
        'L' -> listOf(
            calculateLinePoints(Point(0.3f, 0.15f), Point(0.3f, 0.85f)),
            calculateLinePoints(Point(0.3f, 0.85f), Point(0.75f, 0.85f))
        )
        'M' -> listOf(
            calculateLinePoints(Point(0.2f, 0.15f), Point(0.2f, 0.85f)),
            calculateLinePoints(Point(0.2f, 0.15f), Point(0.5f, 0.6f)),
            calculateLinePoints(Point(0.5f, 0.6f), Point(0.8f, 0.15f)),
            calculateLinePoints(Point(0.8f, 0.15f), Point(0.8f, 0.85f))
        )
        'N' -> listOf(
            calculateLinePoints(Point(0.25f, 0.15f), Point(0.25f, 0.85f)),
            calculateLinePoints(Point(0.25f, 0.15f), Point(0.75f, 0.85f)),
            calculateLinePoints(Point(0.75f, 0.85f), Point(0.75f, 0.15f))
        )
        'O' -> listOf(
            calculateCubicBezierPoints(Point(0.5f, 0.12f), Point(0.12f, 0.12f), Point(0.12f, 0.88f), Point(0.5f, 0.88f)) + 
            calculateCubicBezierPoints(Point(0.5f, 0.88f), Point(0.88f, 0.88f), Point(0.88f, 0.12f), Point(0.5f, 0.12f))
        )
        'P' -> listOf(
            calculateLinePoints(Point(0.25f, 0.15f), Point(0.25f, 0.85f)),
            calculateCubicBezierPoints(Point(0.25f, 0.15f), Point(0.72f, 0.15f), Point(0.72f, 0.45f), Point(0.25f, 0.5f))
        )
        'Q' -> listOf(
            calculateCubicBezierPoints(Point(0.5f, 0.15f), Point(0.2f, 0.15f), Point(0.2f, 0.8f), Point(0.5f, 0.8f)) +
            calculateCubicBezierPoints(Point(0.5f, 0.8f), Point(0.8f, 0.8f), Point(0.8f, 0.15f), Point(0.5f, 0.15f)),
            calculateLinePoints(Point(0.62f, 0.65f), Point(0.7435f, 0.821f))
        )
        'R' -> listOf(
            calculateLinePoints(Point(0.25f, 0.15f), Point(0.25f, 0.85f)),
            calculateCubicBezierPoints(Point(0.25f, 0.15f), Point(0.65f, 0.15f), Point(0.65f, 0.45f), Point(0.25f, 0.5f)),
            calculateLinePoints(Point(0.45f, 0.5f), Point(0.6875f, 0.8325f))
        )
        'S' -> listOf(
            calculateCubicBezierPoints(Point(0.7f, 0.25f), Point(0.5f, 0.1f), Point(0.3f, 0.15f), Point(0.35f, 0.35f)) +
            calculateCubicBezierPoints(Point(0.35f, 0.35f), Point(0.4f, 0.5f), Point(0.6f, 0.5f), Point(0.65f, 0.65f)) +
            calculateCubicBezierPoints(Point(0.65f, 0.65f), Point(0.7f, 0.85f), Point(0.5f, 0.9f), Point(0.3f, 0.75f))
        )
        'T' -> listOf(
            calculateLinePoints(Point(0.5f, 0.15f), Point(0.5f, 0.85f)),
            calculateLinePoints(Point(0.2f, 0.15f), Point(0.8f, 0.15f))
        )
        'U' -> listOf(
            calculateLinePoints(Point(0.25f, 0.15f), Point(0.25f, 0.65f)) +
            calculateCubicBezierPoints(Point(0.25f, 0.65f), Point(0.25f, 0.88f), Point(0.75f, 0.88f), Point(0.75f, 0.65f)) +
            calculateLinePoints(Point(0.75f, 0.65f), Point(0.75f, 0.15f))
        )
        'V' -> listOf(
            calculateLinePoints(Point(0.2f, 0.15f), Point(0.5f, 0.85f)),
            calculateLinePoints(Point(0.5f, 0.85f), Point(0.8f, 0.15f))
        )
        'W' -> listOf(
            calculateLinePoints(Point(0.15f, 0.15f), Point(0.3f, 0.85f)),
            calculateLinePoints(Point(0.3f, 0.85f), Point(0.5f, 0.45f)),
            calculateLinePoints(Point(0.5f, 0.45f), Point(0.7f, 0.85f)),
            calculateLinePoints(Point(0.7f, 0.85f), Point(0.85f, 0.15f))
        )
        'X' -> listOf(
            calculateLinePoints(Point(0.2f, 0.15f), Point(0.8f, 0.85f)),
            calculateLinePoints(Point(0.8f, 0.15f), Point(0.2f, 0.85f))
        )
        'Y' -> listOf(
            calculateLinePoints(Point(0.2f, 0.15f), Point(0.5f, 0.5f)),
            calculateLinePoints(Point(0.8f, 0.15f), Point(0.5f, 0.5f)),
            calculateLinePoints(Point(0.5f, 0.5f), Point(0.5f, 0.85f))
        )
        'Z' -> listOf(
            calculateLinePoints(Point(0.25f, 0.15f), Point(0.75f, 0.15f)),
            calculateLinePoints(Point(0.75f, 0.15f), Point(0.25f, 0.85f)),
            calculateLinePoints(Point(0.25f, 0.85f), Point(0.75f, 0.85f))
        )
        
        // Lowercase letters
        'a' -> listOf(
            calculateCubicBezierPoints(Point(0.58f, 0.38f), Point(0.22f, 0.42f), Point(0.22f, 0.82f), Point(0.58f, 0.82f)) +
            calculateCubicBezierPoints(Point(0.58f, 0.82f), Point(0.68f, 0.78f), Point(0.68f, 0.42f), Point(0.58f, 0.38f)),
            calculateLinePoints(Point(0.65f, 0.35f), Point(0.65f, 0.85f))
        )
        'b' -> listOf(
            calculateLinePoints(Point(0.3f, 0.1f), Point(0.3f, 0.85f)),
            calculateCubicBezierPoints(Point(0.3f, 0.5f), Point(0.3f, 0.3f), Point(0.7f, 0.3f), Point(0.7f, 0.6f)) +
            calculateCubicBezierPoints(Point(0.7f, 0.6f), Point(0.7f, 0.88f), Point(0.3f, 0.88f), Point(0.3f, 0.85f))
        )
        'c' -> listOf(
            calculateCubicBezierPoints(Point(0.7f, 0.38f), Point(0.35f, 0.28f), Point(0.28f, 0.8f), Point(0.7f, 0.75f))
        )
        'd' -> listOf(
            calculateCubicBezierPoints(Point(0.7f, 0.5f), Point(0.7f, 0.3f), Point(0.3f, 0.3f), Point(0.3f, 0.6f)) +
            calculateCubicBezierPoints(Point(0.3f, 0.6f), Point(0.3f, 0.88f), Point(0.7f, 0.88f), Point(0.7f, 0.7f)),
            calculateLinePoints(Point(0.7f, 0.1f), Point(0.7f, 0.85f))
        )
        'e' -> listOf(
            calculateLinePoints(Point(0.25f, 0.55f), Point(0.70f, 0.55f)),
            calculateCubicBezierPoints(Point(0.70f, 0.55f), Point(0.75f, 0.28f), Point(0.25f, 0.25f), Point(0.25f, 0.55f)) +
            calculateCubicBezierPoints(Point(0.25f, 0.55f), Point(0.25f, 0.82f), Point(0.6f, 0.88f), Point(0.70f, 0.75f))
        )
        'f' -> listOf(
            calculateCubicBezierPoints(Point(0.6f, 0.12f), Point(0.52f, 0.08f), Point(0.45f, 0.1f), Point(0.45f, 0.2f)) +
            calculateLinePoints(Point(0.45f, 0.2f), Point(0.45f, 0.85f)),
            calculateLinePoints(Point(0.28f, 0.35f), Point(0.65f, 0.35f))
        )
        'g' -> listOf(
            calculateCubicBezierPoints(Point(0.7f, 0.4f), Point(0.3f, 0.35f), Point(0.3f, 0.75f), Point(0.7f, 0.7f)),
            calculateLinePoints(Point(0.7f, 0.4f), Point(0.7f, 0.9f)) +
            calculateCubicBezierPoints(Point(0.7f, 0.9f), Point(0.7f, 1.05f), Point(0.45f, 1.08f), Point(0.28f, 0.95f))
        )
        'h' -> listOf(
            calculateLinePoints(Point(0.3f, 0.1f), Point(0.3f, 0.85f)),
            calculateCubicBezierPoints(Point(0.3f, 0.4f), Point(0.33f, 0.3f), Point(0.62f, 0.3f), Point(0.67f, 0.45f)) +
            calculateLinePoints(Point(0.67f, 0.45f), Point(0.67f, 0.85f))
        )
        'i' -> listOf(
            calculateLinePoints(Point(0.5f, 0.35f), Point(0.5f, 0.85f)),
            calculateLinePoints(Point(0.5f, 0.18f), Point(0.5f, 0.22f))
        )
        'j' -> listOf(
            calculateLinePoints(Point(0.55f, 0.35f), Point(0.55f, 0.88f)) +
            calculateCubicBezierPoints(Point(0.55f, 0.88f), Point(0.55f, 1.02f), Point(0.38f, 1.05f), Point(0.25f, 0.95f)),
            calculateLinePoints(Point(0.55f, 0.18f), Point(0.55f, 0.22f))
        )
        'k' -> listOf(
            calculateLinePoints(Point(0.3f, 0.1f), Point(0.3f, 0.85f)),
            calculateLinePoints(Point(0.7f, 0.35f), Point(0.35f, 0.6f)),
            calculateLinePoints(Point(0.45f, 0.52f), Point(0.7f, 0.85f))
        )
        'l' -> listOf(
            calculateLinePoints(Point(0.5f, 0.1f), Point(0.5f, 0.85f))
        )
        'm' -> listOf(
            calculateLinePoints(Point(0.15f, 0.35f), Point(0.15f, 0.85f)),
            calculateCubicBezierPoints(Point(0.15f, 0.35f), Point(0.17f, 0.25f), Point(0.35f, 0.25f), Point(0.41f, 0.4f)) +
            calculateLinePoints(Point(0.41f, 0.4f), Point(0.41f, 0.85f)),
            calculateCubicBezierPoints(Point(0.41f, 0.4f), Point(0.43f, 0.25f), Point(0.62f, 0.25f), Point(0.68f, 0.4f)) +
            calculateLinePoints(Point(0.68f, 0.4f), Point(0.68f, 0.85f))
        )
        'n' -> listOf(
            calculateLinePoints(Point(0.3f, 0.35f), Point(0.3f, 0.85f)),
            calculateCubicBezierPoints(Point(0.3f, 0.41f), Point(0.33f, 0.28f), Point(0.62f, 0.28f), Point(0.67f, 0.4f)) +
            calculateLinePoints(Point(0.67f, 0.4f), Point(0.67f, 0.85f))
        )
        'o' -> listOf(
            calculateCubicBezierPoints(Point(0.5f, 0.25f), Point(0.18f, 0.25f), Point(0.18f, 0.90f), Point(0.5f, 0.90f)) +
            calculateCubicBezierPoints(Point(0.5f, 0.90f), Point(0.82f, 0.90f), Point(0.82f, 0.25f), Point(0.5f, 0.25f))
        )
        'p' -> listOf(
            calculateLinePoints(Point(0.3f, 0.35f), Point(0.3f, 1.0f)),
            calculateCubicBezierPoints(Point(0.3f, 0.41f), Point(0.3f, 0.28f), Point(0.7f, 0.28f), Point(0.7f, 0.55f)) +
            calculateCubicBezierPoints(Point(0.7f, 0.55f), Point(0.7f, 0.82f), Point(0.3f, 0.82f), Point(0.3f, 0.7f))
        )
        'q' -> listOf(
            calculateCubicBezierPoints(Point(0.7f, 0.35f), Point(0.3f, 0.28f), Point(0.3f, 0.82f), Point(0.7f, 0.75f)),
            calculateLinePoints(Point(0.7f, 0.35f), Point(0.7f, 0.95f)) +
            calculateCubicBezierPoints(Point(0.7f, 0.95f), Point(0.75f, 1.0f), Point(0.82f, 0.98f), Point(0.85f, 0.92f))
        )
        'r' -> listOf(
            calculateLinePoints(Point(0.32f, 0.35f), Point(0.32f, 0.85f)),
            calculateCubicBezierPoints(Point(0.32f, 0.41f), Point(0.35f, 0.28f), Point(0.6f, 0.28f), Point(0.68f, 0.38f))
        )
        's' -> listOf(
            calculateCubicBezierPoints(Point(0.68f, 0.38f), Point(0.48f, 0.28f), Point(0.32f, 0.35f), Point(0.38f, 0.52f)) +
            calculateCubicBezierPoints(Point(0.38f, 0.52f), Point(0.45f, 0.65f), Point(0.62f, 0.65f), Point(0.68f, 0.82f)) +
            calculateCubicBezierPoints(Point(0.68f, 0.82f), Point(0.62f, 0.9f), Point(0.42f, 0.88f), Point(0.32f, 0.75f))
        )
        't' -> listOf(
            calculateLinePoints(Point(0.45f, 0.15f), Point(0.45f, 0.80f)),
            calculateLinePoints(Point(0.25f, 0.35f), Point(0.65f, 0.35f))
        )
        'u' -> listOf(
            calculateLinePoints(Point(0.3f, 0.35f), Point(0.3f, 0.72f)) +
            calculateCubicBezierPoints(Point(0.3f, 0.72f), Point(0.3f, 0.88f), Point(0.48f, 0.92f), Point(0.67f, 0.78f)),
            calculateLinePoints(Point(0.67f, 0.78f), Point(0.67f, 0.35f)) +
            calculateCubicBezierPoints(Point(0.67f, 0.85f), Point(0.72f, 0.88f), Point(0.78f, 0.88f), Point(0.8f, 0.85f))
        )
        'v' -> listOf(
            calculateLinePoints(Point(0.25f, 0.35f), Point(0.5f, 0.85f)),
            calculateLinePoints(Point(0.5f, 0.85f), Point(0.75f, 0.35f))
        )
        'w' -> listOf(
            calculateLinePoints(Point(0.15f, 0.35f), Point(0.3f, 0.85f)),
            calculateLinePoints(Point(0.3f, 0.85f), Point(0.5f, 0.5f)),
            calculateLinePoints(Point(0.5f, 0.5f), Point(0.7f, 0.85f)),
            calculateLinePoints(Point(0.7f, 0.85f), Point(0.85f, 0.35f))
        )
        'x' -> listOf(
            calculateLinePoints(Point(0.28f, 0.35f), Point(0.72f, 0.85f)),
            calculateLinePoints(Point(0.72f, 0.35f), Point(0.28f, 0.85f))
        )
        'y' -> listOf(
            calculateLinePoints(Point(0.28f, 0.35f), Point(0.5f, 0.75f)),
            calculateLinePoints(Point(0.72f, 0.35f), Point(0.32f, 1.0f))
        )
        'z' -> listOf(
            calculateLinePoints(Point(0.28f, 0.35f), Point(0.72f, 0.35f)),
            calculateLinePoints(Point(0.72f, 0.35f), Point(0.28f, 0.85f)),
            calculateLinePoints(Point(0.28f, 0.85f), Point(0.72f, 0.85f))
        )
        
        else -> emptyList()
    }
}
