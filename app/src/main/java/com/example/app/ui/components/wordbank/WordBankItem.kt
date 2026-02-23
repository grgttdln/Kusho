package com.example.app.ui.components.wordbank

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Individual word item in the Word Bank grid.
 */
@Composable
fun WordBankItem(
    word: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    isSelected: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isEditMode && isSelected) {
                    Modifier
                        .background(Color(0x203FA9F8), RoundedCornerShape(16.dp))
                        .border(
                            width = 3.dp,
                            color = Color(0xFF3FA9F8),
                            shape = RoundedCornerShape(16.dp)
                        )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = Color(0xFF49A9FF),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = word,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF49A9FF),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WordBankItemPreview() {
    WordBankItem(
        word = "Example",
        onClick = {}
    )
}

