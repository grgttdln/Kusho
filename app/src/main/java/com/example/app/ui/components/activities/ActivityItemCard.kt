package com.example.app.ui.components.activities

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import com.example.app.R

/**
 * Component for displaying a single activity card with icon and title.
 * Used in YourActivitiesScreen and similar screens.
 * 
 * @param title The activity title
 * @param description Optional activity description
 * @param iconRes The resource ID of the icon image
 * @param onClick Callback when card is clicked
 * @param modifier Optional modifier for the card
 */
@Composable
fun ActivityItemCard(
    title: String,
    iconRes: Int,
    description: String? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(175.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Transparent)
            .clickable { onClick() }
            .padding(3.dp)
    ) {
        // Border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Transparent)
                .then(
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = androidx.compose.ui.graphics.Color(0xFFC5E5FD),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 9f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx())
                        )
                    }
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Icon Circle with Border
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .then(
                        Modifier.drawBehind {
                            drawCircle(
                                color = androidx.compose.ui.graphics.Color(0x803FA9F8),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 14f)
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3FA9F8)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        modifier = Modifier.size(52.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = title,
                fontSize = 21.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF3FA9F8),
                textAlign = TextAlign.Center
            )

            // Description (optional)
            if (!description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF808080),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
