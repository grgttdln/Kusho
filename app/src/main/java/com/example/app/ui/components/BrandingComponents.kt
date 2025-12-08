package com.example.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R

@Composable
fun KushoLogo(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.ic_kusho),
        contentDescription = "Kusho logo",
        modifier = modifier
            .width(300.dp)
            .height(80.dp),
        contentScale = ContentScale.Fit,
        alignment = Alignment.CenterStart
    )
}

@Composable
fun DescriptionText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = Color(0xFF2D2D2D),
        lineHeight = 24.sp,
        modifier = modifier
    )
}

