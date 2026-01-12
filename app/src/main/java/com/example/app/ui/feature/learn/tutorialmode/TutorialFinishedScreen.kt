package com.example.app.ui.feature.learn.tutorialmode

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R

private val BlueButtonColor = Color(0xFF3FA9F8)

@Composable
fun TutorialFinishedScreen(
    onEndSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 40.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Spacer(Modifier.weight(0.5f))

        // Hooray Avatar Image
        // Note: Replace with R.drawable.dis_hooray when the image is added
        Image(
            painter = painterResource(id = R.drawable.dis_hooray),
            contentDescription = "Hooray",
            modifier = Modifier
                .size(380.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(32.dp))

        // Tutorial Finished Text
        Text(
            text = "Tutorial Finished!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(Modifier.height(12.dp))

        // Subtitle
        Text(
            text = "Way to go!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black
        )

        Text(
            text = "You learned so much.",
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black
        )

        Spacer(Modifier.weight(1f))

        // End Session Button
        Button(
            onClick = onEndSession,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BlueButtonColor
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "End Session",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TutorialFinishedScreenPreview() {
    TutorialFinishedScreen(
        onEndSession = {}
    )
}

