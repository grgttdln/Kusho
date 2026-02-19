package com.example.app.ui.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.app.ui.components.*
import com.example.app.ui.theme.KushoTheme

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onStartLearning: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CharacterIllustration()

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            KushoLogo()

            Spacer(modifier = Modifier.height(16.dp))

            DescriptionText(
                text = "Discover a new way to teach and learn Air Writing"
            )

            Spacer(modifier = Modifier.height(28.dp))

            PrimaryButton(
                text = "Begin",
                onClick = onStartLearning,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            TermsText(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingScreenPreview() {
    KushoTheme {
        OnboardingScreen()
    }
}

