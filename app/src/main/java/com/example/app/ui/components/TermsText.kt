package com.example.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TermsText(
    modifier: Modifier = Modifier
) {
    Text(
        text = "By continuing, you agree to Kusho's Terms of Service and Privacy Policy.",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF666666),
        textAlign = TextAlign.Center,
        modifier = modifier
            .padding(horizontal = 8.dp),
        lineHeight = 18.sp
    )
}

