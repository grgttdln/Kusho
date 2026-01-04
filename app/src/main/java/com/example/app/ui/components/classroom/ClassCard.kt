package com.example.app.ui.components.classroom

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.app.R

@Composable
fun ClassCard(
    classCode: String,
    className: String,
    imageRes: Int = R.drawable.ic_class_abc,
    imagePath: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(247.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF6F6F8)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                imagePath?.startsWith("drawable://") == true -> {
                    val resName = imagePath.removePrefix("drawable://")
                    val drawableRes = when (resName) {
                        "ic_class_abc" -> R.drawable.ic_class_abc
                        "ic_class_stars" -> R.drawable.ic_class_stars
                        else -> imageRes
                    }
                    Image(
                        painter = painterResource(id = drawableRes),
                        contentDescription = className,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(183.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                imagePath != null -> {
                    AsyncImage(
                        model = java.io.File(imagePath),
                        contentDescription = className,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(183.dp),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = imageRes)
                    )
                }
                else -> {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = className,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(183.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 12.dp)
            ) {
                Text(
                    text = classCode,
                    fontSize = 14.sp,
                    color = Color(0xFF3FA9F8),
                    fontWeight = FontWeight.Normal,
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(0.dp))

                Text(
                    text = className,
                    fontSize = 20.57.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    lineHeight = 31.sp
                )
            }
        }
    }
}
