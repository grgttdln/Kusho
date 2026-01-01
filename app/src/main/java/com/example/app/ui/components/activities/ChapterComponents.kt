package com.example.app.ui.components.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme

/**
 * Reusable component to display an added chapter pill
 *
 * @param number The chapter number
 * @param title The chapter title
 * @param itemCount The number of items in the chapter
 * @param onRemove Callback when remove button is clicked
 * @param modifier Optional modifier for customization
 */
@Composable
fun AddedChapterPill(
    number: Int,
    title: String,
    itemCount: Int = 0,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .border(
                width = 2.dp,
                color = Color(0xFF3FA9F8),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = number.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3FA9F8)
                )

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF3FA9F8)
                    )
                    Text(
                        text = "$itemCount items",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF808080)
                    )
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color(0xFF3FA9F8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Reusable card component for adding chapters
 *
 * @param modifier Optional modifier for customization
 * @param onClick Callback when the card is clicked
 */
@Composable
fun ChapterCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.8.dp))
            .border(
                width = 1.3.dp,
                color = Color(0xFF3FA9F8),
                shape = RoundedCornerShape(20.8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Chapter",
            tint = Color(0xFF3FA9F8),
            modifier = Modifier.size(24.dp)
        )
    }
}


@Preview(
    name = "AddedChapterPill Preview",
    showBackground = true,
    backgroundColor = 0xFFF0F4F8
)
@Composable
fun AddedChapterPillPreview() {
    MaterialTheme {
        AddedChapterPill(
            number = 1,
            title = "Introduction to Vocabulary",
            itemCount = 12,
            onRemove = {}
        )
    }
}