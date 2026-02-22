package com.example.app.ui.feature.learn

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R
import com.example.app.data.entity.Word
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.wordbank.WordBankItem

@Composable
fun LessonScreen(
    onNavigate: (Int) -> Unit,
    onNavigateToWordBank: () -> Unit = {},
    onNavigateToActivities: () -> Unit = {},
    onNavigateToSets: () -> Unit = {},
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
                text = "Air Writing Activities",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0B0B0B)
            )

            Spacer(Modifier.height(32.dp))

            NavigationTile(
                title = "Word Bank",
                backgroundColor = Color(0xFF5DB7FF),
                imageRes = R.drawable.ic_wordbank,
                onClick = onNavigateToWordBank
            )

            Spacer(Modifier.height(20.dp))

            NavigationTile(
                title = "My Activities",
                backgroundColor = Color(0xFF5DB7FF),
                imageRes = R.drawable.ic_activities,
                onClick = onNavigateToSets
            )

            Spacer(Modifier.height(20.dp))

            NavigationTile(
                title = "My Activity Sets",
                backgroundColor = Color(0xFF5DB7FF),
                imageRes = R.drawable.ic_activity_set,
                onClick = onNavigateToActivities
            )
        }

        BottomNavBar(
            selectedTab = 3,
            onTabSelected = { onNavigate(it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun NavigationTile(
    title: String,
    backgroundColor: Color,
    imageRes: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "$title image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Reusable Word Bank List component with scrollable grid layout.
 * Only this component scrolls, keeping the rest of the UI fixed.
 */
@Composable
fun WordBankList(
    words: List<Word>,
    onWordClick: (Word) -> Unit,
    modifier: Modifier = Modifier
) {
    if (words.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dis_none),
                    contentDescription = "No words mascot",
                    modifier = Modifier.size(140.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "No Words Yet",
                    color = Color(0xFF4A4A4A),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap the button below to add\nyour first word to the bank.",
                    color = Color(0xFF7A7A7A),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(words, key = { it.id }) { word ->
                WordBankItem(
                    word = word.word,
                    onClick = { onWordClick(word) }
                )
            }
        }
    }
}


/**
 * Add Word Bank button component.
 */
@Composable
fun AddWordBankButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(75.dp)
            .width(207.dp)
            .widthIn(min = 200.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF3FA9F8)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Add a Word",
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LessonScreenPreview() {
    LessonScreen(onNavigate = {})
}
