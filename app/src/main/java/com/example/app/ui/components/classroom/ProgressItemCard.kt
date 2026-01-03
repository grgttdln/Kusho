package com.example.app.ui.components.classroom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ProgressStatus {
    COMPLETED,
    IN_PROGRESS,
    NOT_STARTED
}

@Composable
fun ProgressItemCard(
    iconRes: Int,
    title: String,
    status: ProgressStatus,
    progress: Float = 0f,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = Color(0xFFE3F2FD),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in circle
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Title and status
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3FA9F8)
                )

                Spacer(Modifier.height(8.dp))

                // Status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (status) {
                        ProgressStatus.COMPLETED -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Completed",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        ProgressStatus.IN_PROGRESS -> {
                            Icon(
                                painter = painterResource(id = com.example.app.R.drawable.ic_time),
                                contentDescription = "In Progress",
                                tint = Color(0xFF3FA9F8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "In Progress",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF3FA9F8)
                            )
                        }
                        ProgressStatus.NOT_STARTED -> {
                            Icon(
                                painter = painterResource(id = com.example.app.R.drawable.ic_time),
                                contentDescription = "Not Started",
                                tint = Color(0xFF999999),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Not Started",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF999999)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when (status) {
                        ProgressStatus.COMPLETED -> Color(0xFF4CAF50)
                        ProgressStatus.IN_PROGRESS -> Color(0xFF3FA9F8)
                        ProgressStatus.NOT_STARTED -> Color(0xFFE0E0E0)
                    },
                    trackColor = Color(0xFFE0E0E0)
                )
            }
        }
    }
}
