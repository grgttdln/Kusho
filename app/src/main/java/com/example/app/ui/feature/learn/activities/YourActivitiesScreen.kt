package com.example.app.ui.feature.learn.activities

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R
import com.example.app.ui.components.BottomNavBar

@Composable
fun YourActivitiesScreen(
    onNavigate: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Back Button and Kusho Logo - Same Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.offset(x = (-12).dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF3FA9F8)
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.ic_kusho),
                    contentDescription = "Kusho Logo",
                    modifier = Modifier
                        .height(54.dp)
                        .weight(1f)
                        .padding(horizontal = 30.dp)
                        .offset(x = 10.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )

                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Your Activities",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B0B0B)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Activity Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Vowels Activity Card
                ActivityItemCard(
                    title = "Vowels",
                    iconRes = R.drawable.ic_apple,
                    backgroundColor = Color(0xFF3FA9F8),
                    iconBackgroundColor = Color(0xFF3FA9F8),
                    onClick = { /* Navigate to Vowels activity */ }
                )

                // Consonants Activity Card
                ActivityItemCard(
                    title = "Consonants",
                    iconRes = R.drawable.ic_ball,
                    backgroundColor = Color(0xFF3FA9F8),
                    iconBackgroundColor = Color(0xFF3FA9F8),
                    onClick = { /* Navigate to Consonants activity */ }
                )

                // Stops Activity Card
                ActivityItemCard(
                    title = "Stops",
                    iconRes = R.drawable.ic_flower,
                    backgroundColor = Color(0xFF3FA9F8),
                    iconBackgroundColor = Color(0xFF3FA9F8),
                    onClick = { /* Navigate to Stops activity */ }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Floating Add Activity Button
        Button(
            onClick = { /* Add new activity */ },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .width(207.dp)
                .height(75.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3FA9F8)
            ),
            shape = RoundedCornerShape(28.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add Activity",
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White
            )
        }

        // Bottom Navigation Bar
        BottomNavBar(
            selectedTab = 3,
            onTabSelected = { onNavigate(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ActivityItemCard(
    title: String,
    iconRes: Int,
    backgroundColor: Color,
    iconBackgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(175.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Transparent)
            .clickable { onClick() }
            .padding(3.dp)
    ) {
        // Border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Transparent)
                .then(
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = androidx.compose.ui.graphics.Color(0xFFC5E5FD),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 9f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx())
                        )
                    }
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Icon Circle with Border
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .then(
                        Modifier.drawBehind {
                            drawCircle(
                                color = androidx.compose.ui.graphics.Color(0x803FA9F8),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 14f)
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3FA9F8)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        modifier = Modifier.size(52.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = title,
                fontSize = 21.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF3FA9F8),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun YourActivitiesScreenPreview() {
    YourActivitiesScreen(
        onNavigate = {},
        onBackClick = {}
    )
}
