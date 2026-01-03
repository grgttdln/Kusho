package com.example.app.ui.feature.classroom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R
import com.example.app.ui.components.classroom.AnalyticsCard
import com.example.app.ui.components.classroom.ProgressItemCard
import com.example.app.ui.components.classroom.ProgressStatus
import com.example.app.ui.components.classroom.TipCard

@Composable
fun StudentDetailsScreen(
    studentId: String,
    studentName: String,
    className: String,
    onNavigateBack: () -> Unit,
    onEditStudent: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Mock data - will be replaced with database data later
    val profileImageRes = R.drawable.dis_default_pfp
    val practiceTime = "48 mins"
    val sessionsCompleted = "5"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(45.dp))

        // Back Button
        Icon(
            imageVector = Icons.Default.KeyboardArrowLeft,
            contentDescription = "Back",
            tint = Color(0xFF3FA9F8),
            modifier = Modifier
                .size(32.dp)
                .offset(x = 10.dp)
                .clickable { onNavigateBack() }
        )

        Spacer(Modifier.height(28.dp))

        // Student Name with Edit Icon - Centered in Blue
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = studentName,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF3FA9F8)
            )
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Student",
                tint = Color(0xFF3FA9F8),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onEditStudent() }
            )
        }

        Spacer(Modifier.height(12.dp))

        // Class Name - Centered
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = className,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF0B0B0B)
            )
        }

        Spacer(Modifier.height(28.dp))

        // Profile Picture with Edit Icon - Centered
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box {
                Image(
                    painter = painterResource(id = profileImageRes),
                    contentDescription = "Student Profile",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                // Edit icon overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(40.dp)
                        .background(Color(0xFF3FA9F8), CircleShape)
                        .clickable { onEditStudent() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile Picture",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Analytics Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_analytics),
                contentDescription = "Analytics",
                tint = Color(0xFF3FA9F8),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Analytics",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3FA9F8)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Analytics Cards
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AnalyticsCard(
                iconRes = R.drawable.ic_time,
                value = practiceTime,
                label = "Total Practice Time",
                modifier = Modifier.weight(1f)
            )
            AnalyticsCard(
                iconRes = R.drawable.ic_sessions,
                value = sessionsCompleted,
                label = "Sessions Completed",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(32.dp))

        // Progress Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_progress),
                contentDescription = "Progress",
                tint = Color(0xFF3FA9F8),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Progress",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3FA9F8)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Progress Items
        ProgressItemCard(
            iconRes = R.drawable.ic_apple,
            title = "Vowels",
            status = ProgressStatus.COMPLETED,
            progress = 1f
        )

        Spacer(Modifier.height(12.dp))

        ProgressItemCard(
            iconRes = R.drawable.ic_ball,
            title = "Consonants",
            status = ProgressStatus.IN_PROGRESS,
            progress = 0.6f
        )

        Spacer(Modifier.height(12.dp))

        ProgressItemCard(
            iconRes = R.drawable.ic_flower,
            title = "Stops",
            status = ProgressStatus.NOT_STARTED,
            progress = 0f
        )

        Spacer(Modifier.height(32.dp))

        // Kuu's Tips Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_bulb),
                contentDescription = "Tips",
                tint = Color(0xFF3FA9F8),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Kuu's Tips",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3FA9F8)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Tip Cards
        TipCard(
            title = "Areas to Focus",
            description = "You're amazing at vowel tracing! Let's try consonants next.",
            subtitle = "AI-Generated Suggestion",
            backgroundColor = Color(0xFFEDBB00)
        )

        Spacer(Modifier.height(12.dp))

        TipCard(
            title = "Keep Growing",
            description = "Practice writing 'B' and 'D' slowly, it helps build smoother air writing motion!",
            subtitle = "Based on your recent session",
            backgroundColor = Color(0xFF9067F7)
        )

        Spacer(Modifier.height(100.dp))
    }
}
