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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R
import com.example.app.ui.feature.classroom.ConfettiAnimationStudent

private val BlueButtonColor = Color(0xFF3FA9F8)

@Composable
fun LearnModeFinishedScreen(
    onEndSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        ConfettiAnimationStudent()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_kusho),
                contentDescription = "Kusho Logo",
                modifier = Modifier
                    .height(54.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .offset(x = 10.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
                    .offset(y = (-24).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dis_hooray),
                    contentDescription = "Champion",
                    modifier = Modifier.size(380.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Learn Mode Finished!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Way to go!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "You learned so much.",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onEndSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BlueButtonColor),
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
}

@Preview(showBackground = true)
@Composable
fun LearnModeFinishedScreenPreview() {
    LearnModeFinishedScreen(onEndSession = {})
}
