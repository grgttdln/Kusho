package com.example.app.ui.feature.learn.learnmode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R
import com.example.app.ui.components.learnmode.LearnModeStatusCard

data class ActivitySetStatus(
    val setId: Long,
    val title: String,
    val status: String
)

@Composable
fun LearnModeSetStatusScreen(
    activityIconRes: Int,
    activityTitle: String,
    sets: List<ActivitySetStatus>,
    onBack: () -> Unit,
    onStartSet: (ActivitySetStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    var selectedSet by remember { mutableStateOf<ActivitySetStatus?>(null) }

    Box(modifier = modifier.fillMaxSize()) {

        // ✅ Back button stays fixed at the top
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 25.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF2196F3)
            )
        }

        // ✅ Scrollable content using LazyVerticalGrid (good for MANY cards)
        // We include the header as a "full span" item, then the cards as 2 columns.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                // Push content down below the back button area
                .padding(top = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = 110.dp // ✅ so last row isn't covered by Start button
            )
        ) {
            // Header takes full width (spans both columns)
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(Color(0xFFEDE6FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = activityIconRes),
                            contentDescription = "Activity Icon",
                            modifier = Modifier.size(80.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = activityTitle,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBA9BFF),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))
                }
            }

            // Cards (2 columns)
            items(sets) { set ->
                val isSelected = set == selectedSet
                LearnModeStatusCard(
                    title = set.title,
                    status = set.status,
                    isSelected = isSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clickable { selectedSet = set }
                )
            }
        }

        // ✅ Fixed bottom button (does NOT scroll)
        Button(
            onClick = { selectedSet?.let { onStartSet(it) } },
            enabled = selectedSet != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FA9F8)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Start Set",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LearnModeSetStatusScreenPreview() {
    LearnModeSetStatusScreen(
        activityIconRes = R.drawable.ic_apple,
        activityTitle = "Vowels",
        sets = List(25) { index ->
            ActivitySetStatus(
                setId = index.toLong(),
                title = "Set ${index + 1}",
                status = if (index % 3 == 0) "Completed" else if (index % 3 == 1) "25% Complete" else "Not Started"
            )
        },
        onBack = {},
        onStartSet = {}
    )
}