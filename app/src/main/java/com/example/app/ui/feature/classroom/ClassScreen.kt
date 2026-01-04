package com.example.app.ui.feature.classroom

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.app.R
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.classroom.ClassCard
import com.example.app.ui.components.PrimaryButton

@Composable
fun ClassScreen(
    onNavigate: (Int) -> Unit,
    onNavigateToCreateClass: () -> Unit = {},
    onNavigateToClassDetails: (String) -> Unit = {},
    viewModel: ClassroomViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.classListUiState.collectAsState()
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

            // Show loading indicator
            if (uiState.isLoading) {
                Spacer(Modifier.height(40.dp))
                CircularProgressIndicator(color = Color(0xFF3FA9F8))
                Spacer(Modifier.height(40.dp))
            }
            // Show error message
            else if (uiState.error != null) {
                Spacer(Modifier.height(40.dp))
                Text(
                    text = uiState.error!!,
                    fontSize = 16.sp,
                    color = Color.Red,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(40.dp))
            }
            // Show empty state
            else if (uiState.classes.isEmpty()) {
                Spacer(Modifier.height(40.dp))
                
                Text(
                    text = "Start Building Your Classroom!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3FA9F8),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = "Create your first class to begin managing students and tracking their progress.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    lineHeight = 24.sp
                )
                
                Spacer(Modifier.height(40.dp))
            }
            // Show class list
            else {
                uiState.classes.forEach { classWithCount ->
                    val classEntity = classWithCount.classEntity
                    ClassCard(
                        classCode = classEntity.classCode,
                        className = classEntity.className,
                        imageRes = R.drawable.ic_class_abc,
                        imagePath = classEntity.bannerPath,
                        onClick = { onNavigateToClassDetails(classEntity.classId.toString()) }
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }

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

