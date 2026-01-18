package com.example.app.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun AnalyticsCard(
    icon: ImageVector,
    number: String,
    label: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null
) {
    val kushoBlue = Color(0xFF3FA9F8)
    val bg = Color(0xFFE9FCFF)
    val iconBg = Color.White

    Card(
        modifier = modifier
            .height(128.dp)
            .width(168.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Start)
                    .size(36.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = kushoBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            androidx.compose.material3.Text(
                text = number,
                modifier = Modifier.align(Alignment.Start),
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = kushoBlue,
                lineHeight = 30.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            androidx.compose.material3.Text(
                text = label,
                modifier = Modifier.align(Alignment.Start),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = kushoBlue.copy(alpha = 0.80f),
                letterSpacing = 0.6.sp,
                lineHeight = 14.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (!badgeText.isNullOrBlank()) {
                androidx.compose.material3.Text(
                    text = badgeText,
                    modifier = Modifier.align(Alignment.Start),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = kushoBlue.copy(alpha = 0.65f),
                    lineHeight = 14.sp,
                    maxLines = 1
                )
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

