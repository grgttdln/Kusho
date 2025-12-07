package com.example.kusho.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.input.pointer.pointerInput
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.kusho.R
import com.example.kusho.common.MessageService
import com.google.android.gms.wearable.NodeClient
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.navigation.NavHostController
import android.graphics.Paint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        setContent {
            WearApp(messageService = null, nodeClient = null)
        }
    }
}

// Navigation Routes
private object Routes {
    const val HOME = "home"
    const val PRACTICE_MODE = "practice_mode"
    const val TUTORIAL_MODE = "tutorial_mode"
    const val LEARN_MODE = "learn_mode"
}

// Constants
private const val ARC_STROKE_WIDTH = 26f
private const val ARC_STROKE_WIDTH_DP = 16f
private const val ARC_SWEEP_ANGLE = 70f
private const val LETTER_SPACING_FACTOR = 1.15f
private const val TEXT_SIZE_SP = 13f
private const val LOGO_SIZE_DP = 120f
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
        val navController = rememberSwipeDismissableNavController()

        SwipeDismissableNavHost(
            navController = navController,
            startDestination = Routes.HOME
        ) {
            // Home screen with mode arcs
            composable(Routes.HOME) {
                HomeScreen(navController = navController)
            }

            // Practice Mode screen
            composable(Routes.PRACTICE_MODE) {
                ModeScreen(
                    modeName = "Practice Mode",
                    modeColor = PracticeModeColor
                )
            }

            // Tutorial Mode screen
            composable(Routes.TUTORIAL_MODE) {
                ModeScreen(
                    modeName = "Tutorial Mode",
                    modeColor = TutorialModeColor
                )
            }

            // Learn Mode screen
            composable(Routes.LEARN_MODE) {
                ModeScreen(
                    modeName = "Learn Mode",
                    modeColor = LearnModeColor
                )
            }
        }
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val radius = size.width / RADIUS_FACTOR

                    // Calculate which arc was tapped
                    val tappedMode = getTappedMode(
                        tapX = offset.x,
                        tapY = offset.y,
                        centerX = centerX,
                        centerY = centerY,
                        radius = radius
                    )

                    // Navigate to the appropriate mode screen
                    when (tappedMode) {
                        "practice" -> navController.navigate(Routes.PRACTICE_MODE)
                        "tutorial" -> navController.navigate(Routes.TUTORIAL_MODE)
                        "learn" -> navController.navigate(Routes.LEARN_MODE)
                    }
                }
            },
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

@Composable
fun ModeScreen(modeName: String, modeColor: Color) {
    Scaffold(
        timeText = {
            TimeText()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = modeName,
                color = modeColor,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Swipe right to go back",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

/**
 * Determines which arc mode was tapped based on tap coordinates
 */
private fun getTappedMode(
    tapX: Float,
    tapY: Float,
    centerX: Float,
    centerY: Float,
    radius: Float
): String? {
    // Calculate distance from center
    val dx = tapX - centerX
    val dy = tapY - centerY
    val distance = sqrt(dx * dx + dy * dy)

    // Check if tap is within the arc ring area
    val innerRadius = radius - (ARC_STROKE_WIDTH * 2)
    val outerRadius = radius + (ARC_STROKE_WIDTH * 2)

    if (distance < innerRadius || distance > outerRadius) {
        return null // Tap outside arc area
    }

    // Calculate angle of tap
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (angle < 0) angle += 360f

    // Check which arc was tapped based on angle ranges
    // Practice Mode: bottom (around 90°)
    val practiceStart = PRACTICE_MODE_ANGLE - (ARC_SWEEP_ANGLE / 2f)
    val practiceEnd = PRACTICE_MODE_ANGLE + (ARC_SWEEP_ANGLE / 2f)
    if (angle >= practiceStart && angle <= practiceEnd) {
        return "practice"
    }

    // Tutorial Mode: left (around 180°)
    val tutorialStart = TUTORIAL_MODE_ANGLE - (ARC_SWEEP_ANGLE / 2f)
    val tutorialEnd = TUTORIAL_MODE_ANGLE + (ARC_SWEEP_ANGLE / 2f)
    if (angle >= tutorialStart && angle <= tutorialEnd) {
        return "tutorial"
    }

    // Learn Mode: right (around 0°/360°)
    val learnStart = LEARN_MODE_ANGLE - (ARC_SWEEP_ANGLE / 2f)
    val learnEnd = LEARN_MODE_ANGLE + (ARC_SWEEP_ANGLE / 2f)
    if ((angle >= 360f + learnStart || angle <= learnEnd) ||
        (angle >= learnStart && angle <= learnEnd)) {
        return "learn"
    }

    return null
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
        isFakeBoldText = true
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
