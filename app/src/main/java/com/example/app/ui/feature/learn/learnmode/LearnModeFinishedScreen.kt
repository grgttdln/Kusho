package com.example.app.ui.feature.learn.learnmode

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R

private val BlueButtonColor = Color(0xFF3FA9F8)

@Composable
fun LearnModeFinishedScreen(
    onEndSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 10.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_kusho),
            contentDescription = "Kusho Logo",
            modifier = Modifier.size(120.dp),
            contentScale = ContentScale.Fit
        )

        // Champion/Hooray Image for Learn Mode
        Image(
            painter = painterResource(id = R.drawable.dis_hooray),
            contentDescription = "Champion",
            modifier = Modifier.size(380.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Learn Mode Finished!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(Modifier.height(12.dp))

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
fun LearnModeFinishedScreenPreview() {
    LearnModeFinishedScreen(
        onEndSession = {}
    )
}
