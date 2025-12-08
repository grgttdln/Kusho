package com.example.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.app.R

@Composable
fun CharacterIllustration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(460.dp),
        contentAlignment = Alignment.CenterEnd
    ) {

        Image(
            painter = painterResource(id = R.drawable.dis_home),
            contentDescription = null,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = 2.35f
                    scaleY = 2.35f
                    clip = false
                }
                .offset(x = 15.dp)
                .fillMaxHeight(),
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterEnd
        )
    }
}
