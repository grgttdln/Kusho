package com.example.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import com.example.app.R


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
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFFD5F2FF) else Color(0xFFF8FBFF))
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFFB8DDF8),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
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
                lineHeight = 28.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 24.dp, end = 8.dp)
            )

            // Icon on the right - fills the height and extends to fill container
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier
                        .requiredSize(500.dp)
                        .offset(x = 80.dp, y = -110.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )
            }
        }
    }
}

@Preview(
    name = "SetItemCard Preview",
    showBackground = true,
    backgroundColor = 0xFFF0F4F8
)
@Composable
fun SetItemCardPreview() {
    MaterialTheme {
        SetItemCard(
            title = "Meet the Vowels",
            iconRes = R.drawable.ic_pencil,
            onClick = {}
        )
    }
}

