package com.example.app.ui.feature.learn

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme

/**
 * Reusable confirmation screen for various features and flows.
 *
 * @param onContinueClick Callback invoked when the continue button is clicked
 * @param modifier Modifier for the screen
 * @param title The main confirmation message (e.g., "Activity Created!" or "Set Created!")
 * @param subtitle Optional subtitle with name/title (e.g., activity or set name)
 * @param buttonText Text displayed on the continue button
 */
@Composable
fun ConfirmationScreen(
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Created!",
    subtitle: String = "",
    buttonText: String = "Continue"
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Kusho Logo at top - matching LearnScreen style
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

            // Center content vertically in available space
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Celebration Icon - larger size
                    Image(
                        painter = painterResource(id = R.drawable.ic_confirmation),
                        contentDescription = "Confirmation",
                        modifier = Modifier
                            .size(300.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Main Title - bold and italic
                    Text(
                        text = title,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF000000),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 30.dp)
                    )
                }
            }

            // Space for button
            Spacer(modifier = Modifier.height(120.dp))
        }

        // Continue Button
        Button(
            onClick = onContinueClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
                .fillMaxWidth(0.86f)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3FA9F8)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = buttonText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}


@Preview(
    name = "ConfirmationScreen Preview",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun ConfirmationScreenPreview() {
    MaterialTheme {
        ConfirmationScreen(
            title = "Set Created!",
            subtitle = "Meet the Vowels",
            onContinueClick = {}
        )
    }
}
