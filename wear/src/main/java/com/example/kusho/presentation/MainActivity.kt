package com.example.kusho.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.kusho.R
import com.example.kusho.common.MessageService
import com.google.android.gms.wearable.NodeClient
import androidx.wear.compose.material.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import android.graphics.Paint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        setContent {
            WearApp(messageService = null, nodeClient = null)
        }
    }
}

// Constants
private const val ARC_STROKE_WIDTH = 24f
private const val ARC_STROKE_WIDTH_DP = 16f
private const val ARC_SWEEP_ANGLE = 70f
private const val LETTER_SPACING_FACTOR = 1.15f
private const val TEXT_SIZE_SP = 13f
private const val LOGO_SIZE_DP = 100f
private const val RADIUS_FACTOR = 2.3f

// Arc positions (in degrees)
private const val PRACTICE_MODE_ANGLE = 90f  // Bottom
private const val TUTORIAL_MODE_ANGLE = 180f // Left
private const val LEARN_MODE_ANGLE = 0f      // Right

// Arc colors
private val PracticeModeColor = Color(0xFF42A5F5)
private val TutorialModeColor = Color(0xFFFFC107)
private val LearnModeColor = Color(0xFFB388FF)

@Suppress("UNUSED_PARAMETER")
@Composable
fun WearApp(messageService: MessageService?, nodeClient: NodeClient?) {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Center logo
            Image(
                painter = painterResource(id = R.drawable.ic_kusho),
                contentDescription = "Kusho Logo",
                modifier = Modifier.size(LOGO_SIZE_DP.dp),
                contentScale = ContentScale.Fit
            )

            // Draw mode arcs
            val density = LocalDensity.current
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val radius = size.width / RADIUS_FACTOR

                // Draw all three mode arcs
                drawModeArc(
                    centerX = centerX,
                    centerY = centerY,
                    radius = radius,
                    centerAngle = PRACTICE_MODE_ANGLE,
                    text = "Practice Mode",
                    color = PracticeModeColor,
                    density = density
                )

                drawModeArc(
                    centerX = centerX,
                    centerY = centerY,
                    radius = radius,
                    centerAngle = TUTORIAL_MODE_ANGLE,
                    text = "Tutorial Mode",
                    color = TutorialModeColor,
                    density = density
                )

                drawModeArc(
                    centerX = centerX,
                    centerY = centerY,
                    radius = radius,
                    centerAngle = LEARN_MODE_ANGLE,
                    text = "Learn Mode",
                    color = LearnModeColor,
                    density = density
                )
            }
        }
    }
}

/**
 * Draws a circular arc pill with curved text for a mode button
 */
private fun DrawScope.drawModeArc(
    centerX: Float,
    centerY: Float,
    radius: Float,
    centerAngle: Float,
    text: String,
    color: Color,
    density: Density
) {
    val startAngle = centerAngle - (ARC_SWEEP_ANGLE / 2f)

    // Draw the arc background
    val arcPath = Path()
    arcPath.addArc(
        oval = androidx.compose.ui.geometry.Rect(
            left = centerX - radius,
            top = centerY - radius,
            right = centerX + radius,
            bottom = centerY + radius
        ),
        startAngleDegrees = startAngle,
        sweepAngleDegrees = ARC_SWEEP_ANGLE
    )

    drawPath(
        path = arcPath,
        color = color,
        style = Stroke(
            width = ARC_STROKE_WIDTH.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    )

    // Draw curved text along the arc
    drawCurvedText(
        text = text.reversed(),
        centerX = centerX,
        centerY = centerY,
        radius = radius,
        startAngle = startAngle,
        sweepAngle = ARC_SWEEP_ANGLE,
        density = density
    )
}

/**
 * Draws text curved along a circular arc path
 */
private fun DrawScope.drawCurvedText(
    text: String,
    centerX: Float,
    centerY: Float,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    density: Density
) {
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = with(density) { TEXT_SIZE_SP.sp.toPx() }
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    val arcStrokeWidth = ARC_STROKE_WIDTH_DP.dp.toPx()
    val textRadius = radius + (arcStrokeWidth / 4f)

    // Calculate character widths and spacing
    val charWidths = FloatArray(text.length)
    textPaint.getTextWidths(text, charWidths)
    val totalTextWidth = charWidths.sum() * LETTER_SPACING_FACTOR
    val textArcAngle = (totalTextWidth / (2f * PI.toFloat() * textRadius)) * 360f
    val textStartAngle = startAngle + sweepAngle - (sweepAngle - textArcAngle) / 2f

    // Draw each character along the curve
    var currentAngle = textStartAngle
    for (i in text.indices.reversed()) {
        val char = text[i].toString()
        val charWidth = charWidths[i] * LETTER_SPACING_FACTOR
        val charAngle = (charWidth / (2f * PI.toFloat() * textRadius)) * 360f

        currentAngle -= charAngle / 2f

        val angleInRadians = Math.toRadians(currentAngle.toDouble())
        val x = centerX + textRadius * cos(angleInRadians).toFloat()
        val y = centerY + textRadius * sin(angleInRadians).toFloat()

        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.translate(x, y)
        drawContext.canvas.nativeCanvas.rotate(currentAngle - 90f)
        drawContext.canvas.nativeCanvas.drawText(char, 0f, 0f, textPaint)
        drawContext.canvas.nativeCanvas.restore()

        currentAngle -= charAngle / 2f
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(
        messageService = null,
        nodeClient = null
    )
}
