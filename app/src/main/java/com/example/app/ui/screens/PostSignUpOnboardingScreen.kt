package com.example.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
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
import com.example.app.ui.components.PrimaryButton
import com.example.app.ui.screens.data.OnboardingPage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostSignUpOnboardingScreen(
    modifier: Modifier = Modifier,
    onOnboardingComplete: () -> Unit = {}
) {
    val pages = listOf(
        OnboardingPage(
            imageRes = R.drawable.dis_onb1,
            title = "Welcome to Kusho",
            description = "Learn to write by moving your hands in the air. Fun, playful, and magical!"
        ),
        OnboardingPage(
            imageRes = R.drawable.dis_onb2,
            title = "Move, Write, and Learn!",
            description = "Trace letters with your hand. Kusho tracks your movements and helps you learn to write!"
        ),
        OnboardingPage(
            imageRes = R.drawable.dis_onb3,
            title = "Every Learner Grows Differently",
            description = "Kusho adapts to the child's pace, making learning gentle, encouraging, and fun."
        ),
        OnboardingPage(
            imageRes = R.drawable.dis_onb4,
            title = "For Teachers and Parents",
            description = "View learning progress, favorite words, and air writing accuracy anytime."
        ),
        OnboardingPage(
            imageRes = R.drawable.dis_onb5,
            title = "Ready to Begin?",
            description = "Join Kuu and start your air writing journey!"
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Horizontal Pager for swiping
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            // Page Indicators (hide on last page)
            if (pagerState.currentPage < pages.size - 1) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(if (index == pagerState.currentPage) 12.dp else 8.dp)
                                .background(
                                    color = if (index == pagerState.currentPage)
                                        Color(0xFF49A9FF)
                                    else
                                        Color(0xFFCCCCCC),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Get Started Button (only show on last page)
            if (pagerState.currentPage == pages.size - 1) {
                PrimaryButton(
                    text = "Let's Go!",
                    onClick = onOnboardingComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Skip button in upper right corner (hide on last page)
        if (pagerState.currentPage < pages.size - 1) {
            TextButton(
                onClick = onOnboardingComplete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 16.dp)
            ) {
                Text(
                    text = "Skip",
                    color = Color(0xFF49A9FF),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        // Illustration with weight to adapt to screen height
        Image(
            painter = painterResource(id = page.imageRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = page.title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF49A9FF),
            textAlign = TextAlign.Start,
            lineHeight = 40.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = page.description,
            fontSize = 18.sp,
            color = Color(0xFF2D2D2D),
            textAlign = TextAlign.Start,
            lineHeight = 26.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PostSignUpOnboardingScreenPreview() {
    PostSignUpOnboardingScreen()
}
