package com.example.app.ui.feature.classroom

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.ClassCard
import com.example.app.ui.components.PrimaryButton

@Composable
fun ClassScreen(
    onNavigate: (Int) -> Unit,
    onNavigateToCreateClass: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

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

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Your Classes",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0B0B0B)
            )

            Spacer(Modifier.height(28.dp))

            // Class Cards
            ClassCard(
                classCode = "G1-YB",
                className = "Grade 1 Young Builders",
                imageRes = R.drawable.ic_class_abc,
                onClick = { /* TODO: Navigate to class details */ }
            )

            Spacer(Modifier.height(20.dp))

            ClassCard(
                classCode = "G1-BS",
                className = "Grade 1 Bright Sparks",
                imageRes = R.drawable.ic_class_stars,
                onClick = { /* TODO: Navigate to class details */ }
            )

            Spacer(Modifier.height(28.dp))

            // Create New Class Button
            PrimaryButton(
                text = "Create a New Class",
                onClick = onNavigateToCreateClass,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(24.dp))
        }

        BottomNavBar(
            selectedTab = 2,
            onTabSelected = { onNavigate(it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ClassScreenPreview() {
    ClassScreen(onNavigate = {})
}

