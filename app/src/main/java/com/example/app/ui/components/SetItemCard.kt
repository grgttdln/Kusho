package com.example.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color constants
private val CHECKMARK_BACKGROUND_COLOR = Color(0xFF14FF1E)

/**
 * Reusable Set Item Card component for displaying sets with an icon.
 * Used in Your Sets screen to show different phonics sets.
 *
 * @param title The title text to display (e.g., "Meet the Vowels")
 * @param iconRes The drawable resource ID for the icon (e.g., R.drawable.ic_pencil)
 * @param onClick Callback when the card is clicked
 * @param isSelected Whether the card is selected (shows blue highlight and checkmark)
 * @param modifier Optional modifier for customization
 */
@Composable
fun SetItemCard(
    title: String,
    iconRes: Int,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(if (isSelected) Color(0xFFD5F2FF) else Color.White)
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFFC5E5FD),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 9f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                )
            }
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title text on the left
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B0B0B),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp)
            )

            // Icon on the right - fills the height and extends to fill container
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(200.dp)
                    .offset(x = 30.dp)
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier
                        .size(300.dp)
                        .offset(x = (-20).dp, y = (-20).dp)
                        .rotate(-5f),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                )
            }
        }

        // Checkmark if selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(28.dp)
                    .background(CHECKMARK_BACKGROUND_COLOR, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
