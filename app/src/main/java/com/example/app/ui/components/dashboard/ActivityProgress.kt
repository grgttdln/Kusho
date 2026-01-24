package com.example.app.ui.components.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.app.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Data model representing per-activity progress insights
 */
data class ActivityProgress(
    val activityId: String,
    val activityName: String,
    // optional path to a cover image file saved by the activity
    val coverImagePath: String? = null,
    // optional drawable resource id (preferred display over coverImagePath)
    val iconRes: Int? = null,
    val accuracyDeltaPercent: Int,
    val timeDeltaSeconds: Int,
    val masteryPercent: Float,
    val masteryLabel: String,

    // Use non-null defaults so UI can safely display zero values when backend doesn't provide them
    val avgAttempts: Float = 0f,
    val avgAccuracyPercent: Int = 0,
    val avgScoreText: String = "",
    val avgTimeSeconds: Int = 0
)

@Composable
fun ActivityProgressSection(
    activities: List<ActivityProgress>,
    onActivityClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp)
    ) {
        Text(
            text = "Activity Progress",
            fontSize = 20.sp,
            color = Color.Black
        )



        Spacer(modifier = Modifier.height(12.dp))

        if (activities.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dis_none),
                    contentDescription = "No activities mascot",
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No Activities Yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4A4A4A)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Create your first activity in the Learn tab\nto start tracking student progress.",
                    fontSize = 12.sp,
                    color = Color(0xFF7A7A7A),
                    lineHeight = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            return
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                activities.forEachIndexed { index, activity ->
                    val expanded = expandedMap[activity.activityId] ?: false

                    ActivityProgressAccordionRow(
                        progress = activity,
                        expanded = expanded,
                        onToggleExpand = { expandedMap[activity.activityId] = !expanded },
                        onOpenDetails = { onActivityClick(activity.activityId) }
                    )

                    if (index != activities.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFF0F0F0))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityProgressAccordionRow(
    progress: ActivityProgress,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryBlue = Color(0xFF3FA9F8)
    val muted = Color(0xFF7A7A7A)
    val chipBg = Color(0xFFF2F6FF)

    val positive = Color(0xFF2E7D32)
    val negative = Color(0xFFD32F2F)
    val neutral = Color(0xFF6F6F6F)

    val masteryPercent = progress.masteryPercent.coerceIn(0f, 1f)
    val masteryInt = (masteryPercent * 100).toInt()

    val (improvementText, improvementColor) = remember(progress) {
        when {
            progress.accuracyDeltaPercent != 0 -> {
                val sign = if (progress.accuracyDeltaPercent > 0) "+" else ""
                val text = "${sign}${progress.accuracyDeltaPercent}% accuracy"
                val color = if (progress.accuracyDeltaPercent > 0) positive else negative
                text to color
            }
            progress.timeDeltaSeconds != 0 -> {
                val s = kotlin.math.abs(progress.timeDeltaSeconds)
                if (progress.timeDeltaSeconds < 0) "-${s}s faster" to positive else "+${s}s slower" to negative
            }
            else -> "No change" to neutral
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Row header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Activity icon: prefer explicit drawable resource, otherwise use coverImagePath via ActivityIcon
            if (progress.iconRes != null) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE9FCFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = progress.iconRes),
                        contentDescription = progress.activityName,
                        modifier = Modifier
                            .size(28.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                ActivityIcon(
                    coverPath = progress.coverImagePath,
                    name = progress.activityName
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = progress.activityName.ifBlank { "—" },
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Mastery: $masteryInt%",
                    fontSize = 12.sp,
                    color = muted
                )
            }

            // Right improvement chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(chipBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = improvementText,
                    fontSize = 12.sp,
                    color = improvementColor
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = Color(0xFFBDBDBD)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(120)) + expandVertically(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(180))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAFAFA))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Chip row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricChip(
                        icon = Icons.Default.Refresh,
                        label = "Improvement",
                        value = improvementText,
                        valueColor = improvementColor,
                        background = chipBg
                    )

                    MetricChip(
                        icon = Icons.Default.CheckCircle,
                        label = "Mastery",
                        value = progress.masteryLabel,
                        valueColor = Color.Black,
                        background = chipBg
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mastery progress header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mastery Progress",
                        fontSize = 12.sp,
                        color = muted,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$masteryInt%",
                        fontSize = 12.sp,
                        color = muted
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { masteryPercent },
                    color = primaryBlue,
                    trackColor = Color(0xFFEFF2F5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Averages (Attempts, Accuracy, Score, Time)
                Text(
                    text = "Averages",
                    fontSize = 12.sp,
                    color = muted
                )

                Spacer(modifier = Modifier.height(8.dp))

                val avgAttemptsText = String.format(java.util.Locale.US, "%.1f", progress.avgAttempts)
                val avgAccuracyText = "${progress.avgAccuracyPercent}%"
                val avgScoreText = progress.avgScoreText.takeIf { it.isNotBlank() } ?: "—"
                val avgTimeText = formatSecondsToMinSec(progress.avgTimeSeconds)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatPill("Attempts", avgAttemptsText, Modifier.weight(1f))
                    StatPill("Accuracy", avgAccuracyText, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatPill("Score", avgScoreText, Modifier.weight(1f))
                    StatPill("Time", avgTimeText, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "View details",
                        fontSize = 13.sp,
                        color = primaryBlue,
                        modifier = Modifier.clickable { onOpenDetails() }
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF3FA9F8),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(text = label, fontSize = 11.sp, color = Color(0xFF7A7A7A))
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                color = valueColor
            )
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFE8E8E8)), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF7A7A7A))
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.Black
        )
    }
}

@Composable
private fun ActivityIcon(coverPath: String?, name: String, modifier: Modifier = Modifier) {
    val size = 46.dp
    val primaryBlue = Color(0xFF3FA9F8)
    val context = LocalContext.current

    // Resolve model for Coil's AsyncImage: can be Uri, File path string, or resource id (Int)
    val model = remember(coverPath) {
        if (coverPath.isNullOrBlank()) return@remember null
        try {
            when {
                coverPath.startsWith("content://") || coverPath.startsWith("file://") || coverPath.startsWith("http") -> android.net.Uri.parse(coverPath)
                coverPath.startsWith("android.resource://") -> android.net.Uri.parse(coverPath)
                coverPath.startsWith("R.drawable.") -> {
                    val name = coverPath.substringAfter("R.drawable.")
                    val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
                    if (resId != 0) resId else null
                }
                coverPath.matches(Regex("^\\d+")) -> coverPath.toIntOrNull()
                else -> {
                    // try as absolute path
                    val file = java.io.File(coverPath)
                    if (file.exists()) file.absolutePath
                    else {
                        // try drawable resource name
                        val resId = context.resources.getIdentifier(coverPath, "drawable", context.packageName)
                        if (resId != 0) resId else null
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (model != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(model).crossfade(true).build(),
                contentDescription = name,
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_profile_placeholder),
                error = painterResource(id = R.drawable.ic_profile_placeholder)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_profile_placeholder),
                    contentDescription = name,
                    tint = primaryBlue
                )
            }
        }
    }
}

private fun formatSecondsToMinSec(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val m = s / 60
    val r = s % 60
    return "${m}m ${r}s"
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ActivityIconPreview_DrawableName() {
    Column(modifier = Modifier.padding(16.dp)) {
        ActivityIcon(coverPath = "ic_apple", name = "Apple")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Drawable name: ic_apple", fontSize = 12.sp, color = Color.Black)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ActivityIconPreview_RDrawable() {
    Column(modifier = Modifier.padding(16.dp)) {
        ActivityIcon(coverPath = "R.drawable.ic_apple", name = "Apple R.drawable")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "R.drawable.ic_apple", fontSize = 12.sp, color = Color.Black)
    }
}