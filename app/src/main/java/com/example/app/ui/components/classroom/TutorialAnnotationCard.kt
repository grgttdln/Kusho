package com.example.app.ui.components.classroom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.DrawableRes
import com.example.app.R

@Composable
fun TutorialAnnotationCard(
    tags: List<String>,
    annotation: String,
    tutorialName: String,
    date: String,
    @DrawableRes iconRes: Int = R.drawable.ic_apple,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFEFBF2)
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFEDBB00))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Tags Row - only show if tags are not empty
            if (tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEDBB00), RoundedCornerShape(50.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
            }

            // Annotation Text
            Text(
                text = annotation,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333),
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(16.dp))

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFFEDBB00))
            )

            Spacer(Modifier.height(12.dp))

            // Bottom Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tutorial name with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = "Tutorial",
                        modifier = Modifier.size(28.dp)
                    )

                    Text(
                        text = tutorialName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFEDBB00)
                    )
                }

                Text(
                    text = date,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFEDBB00)
                )
            }
        }
    }
}
