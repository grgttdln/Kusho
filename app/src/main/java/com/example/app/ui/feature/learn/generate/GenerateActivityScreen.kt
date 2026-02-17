package com.example.app.ui.feature.learn.generate

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
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
import com.example.app.ui.components.BottomNavBar

/**
 * Screen for displaying AI-generated activity JSON result.
 * Simply displays the raw JSON output from the AI generation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateActivityScreen(
    onNavigate: (Int) -> Unit,
    onBackClick: () -> Unit,
    generatedJson: String,
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
                .padding(bottom = 100.dp)
        ) {
            // Top Bar
            TopAppBar(
                title = { Text("AI Generated Activity") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // AI Mascot
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dis_wand_sit),
                    contentDescription = "AI Assistant",
                    modifier = Modifier.size(120.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "Generated Activity JSON",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B0B0B),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Here is the AI-generated activity in JSON format:",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // JSON Display
            JsonDisplaySection(json = generatedJson)
        }

        // Bottom Navigation
        BottomNavBar(
            selectedTab = 3,
            onTabSelected = { onNavigate(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Section for displaying the JSON result.
 */
@Composable
private fun JsonDisplaySection(json: String) {
    Column {
        Text(
            text = "JSON Output:",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0B0B0B)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                text = json,
                fontSize = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = Color(0xFF333333),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GenerateActivityScreenPreview() {
    val sampleJson = """
    {
      "activity": {
        "title": "Animals Story",
        "description": "A fun story about animals for beginner readers"
      },
      "sets": [
        {
          "title": "Set 1",
          "description": "First part of the story",
          "words": [
            {"word": "cat", "configurationType": "name the picture"},
            {"word": "dog", "configurationType": "fill in the blanks", "selectedLetterIndex": 1}
          ]
        }
      ]
    }
    """.trimIndent()
    
    GenerateActivityScreen(
        onNavigate = {},
        onBackClick = {},
        generatedJson = sampleJson
    )
}
