package com.example.app.ui.components.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.data.entity.VideoTutorial

@Composable
fun VideoTutorialSection(
    tutorials: List<VideoTutorial>,
    onTutorialClick: (VideoTutorial) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Getting Started with Kusho'",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            lineHeight = 27.sp,
            modifier = Modifier.padding(horizontal = 30.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 30.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(tutorials, key = { it.id }) { tutorial ->
                VideoTutorialCard(
                    tutorial = tutorial,
                    onClick = { onTutorialClick(tutorial) }
                )
            }
        }
    }
}

@Composable
fun VideoTutorialCard(
    tutorial: VideoTutorial,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(320.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Gradient thumbnail area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFE91E63), // Pink
                                Color(0xFF9C27B0)  // Purple
                            )
                        )
                    )
            ) {
                // Play button — centered
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    // White triangle play icon
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.2f, 0f)
                            lineTo(size.width, size.height / 2f)
                            lineTo(size.width * 0.2f, size.height)
                            close()
                        }
                        drawPath(path, Color.White)
                    }
                }

                // Category + duration badge — bottom-left
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                        .background(
                            Color.Black.copy(alpha = 0.3f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${tutorial.category} \u2022 ${tutorial.durationMinutes} MIN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Text content below thumbnail
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = tutorial.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A),
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = tutorial.description,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF7A7A7A),
                    lineHeight = 20.sp,
                    minLines = 3,
                    maxLines = 3
                )
            }
        }
    }
}

// ---------------------- Preview ----------------------

@Preview(showBackground = true)
@Composable
fun VideoTutorialSectionPreview() {
    val sampleTutorials = listOf(
        VideoTutorial(
            id = 1,
            title = "1. Welcome to Kusho'!",
            description = "A quick overview of what Kusho' can do for your classroom.",
            category = "GUIDE",
            durationMinutes = 2,
            sortOrder = 1,
            userId = 1
        ),
        VideoTutorial(
            id = 2,
            title = "2. Setting Up Your Class",
            description = "Learn how to create a class and add students.",
            category = "GUIDE",
            durationMinutes = 3,
            sortOrder = 2,
            userId = 1
        ),
        VideoTutorial(
            id = 3,
            title = "3. Creating Activities",
            description = "Build your first word activity with AI assistance.",
            category = "GUIDE",
            durationMinutes = 4,
            sortOrder = 3,
            userId = 1
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp)
    ) {
        VideoTutorialSection(
            tutorials = sampleTutorials,
            onTutorialClick = {}
        )
    }
}
