package com.example.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavBarItem(
            icon = com.example.app.R.drawable.ic_home,
            label = "Home",
            isSelected = selectedTab == 0,
            onClick = { onTabSelected(0) }
        )

        NavBarItem(
            icon = com.example.app.R.drawable.ic_learn,
            label = "Learn",
            isSelected = selectedTab == 1,
            onClick = { onTabSelected(1) }
        )

        NavBarItem(
            icon = com.example.app.R.drawable.ic_class,
            label = "Class",
            isSelected = selectedTab == 2,
            onClick = { onTabSelected(2) }
        )

        NavBarItem(
            icon = com.example.app.R.drawable.ic_lesson,
            label = "Lesson",
            isSelected = selectedTab == 3,
            onClick = { onTabSelected(3) }
        )
    }
}

@Composable
private fun NavBarItem(
    icon: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFFD5F2FF) else Color.Transparent
    val textColor = if (isSelected) Color(0xFF2196F3) else Color(0xFF2196F3)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )

        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

