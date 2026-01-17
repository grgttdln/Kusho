package com.example.app.ui.feature.classroom

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExistingStudentsScreen(
    classId: Long,
    existingStudentIds: Set<Long>,
    onAdd: (Long) -> Unit,
    onBack: () -> Unit,
    vm: ClassroomViewModel = viewModel()
) {
    val directory by vm.studentDirectory.collectAsState()
    var query by remember { mutableStateOf("") }

    val accentBlue = Color(0xFF3FA9F8)
    val textDark = Color(0xFF0B0B0B)
    val subtleText = Color(0xFF6B7280)
    val borderColor = Color(0xFFE6EEF6)
    val surfaceWhite = Color.White
    val pillShape = RoundedCornerShape(16.dp)

    val filtered = remember(directory, query) {
        val base = if (query.isBlank()) directory else directory.filter {
            it.fullName.contains(query, ignoreCase = true)
        }
        base.sortedBy { it.fullName }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Add existing students",
                        fontWeight = FontWeight.ExtraBold,
                        color = textDark
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = accentBlue, fontWeight = FontWeight.SemiBold)
                    }
                },
                actions = {
                    TextButton(onClick = onBack) {
                        Text("Done", color = accentBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            // Pill Search (matches your ClassScreen style)
            Surface(
                color = surfaceWhite,
                tonalElevation = 0.dp,
                shadowElevation = 2.dp,
                shape = pillShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, borderColor, pillShape)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = accentBlue
                    )

                    Spacer(Modifier.width(10.dp))

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                text = "Search students",
                                color = subtleText,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            color = textDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = subtleText
                                    )
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = textDark,
                            unfocusedTextColor = textDark,
                            focusedContainerColor = surfaceWhite,
                            unfocusedContainerColor = surfaceWhite,
                            disabledContainerColor = surfaceWhite,
                            cursorColor = accentBlue,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (filtered.isEmpty()) {
                Spacer(Modifier.height(36.dp))
                Text(
                    text = "No students found",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentBlue,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Try a different name or clear the search.",
                    fontSize = 14.sp,
                    color = subtleText,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(filtered) { student ->
                        val isAdded = existingStudentIds.contains(student.studentId)

                        StudentDirectoryRow(
                            name = student.fullName,
                            gradeText = "Grade: -", // swap when you have grade
                            isAdded = isAdded,
                            accentBlue = accentBlue,
                            subtleText = subtleText,
                            borderColor = borderColor,
                            onAdd = { onAdd(student.studentId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentDirectoryRow(
    name: String,
    gradeText: String,
    isAdded: Boolean,
    accentBlue: Color,
    subtleText: Color,
    borderColor: Color,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.dis_default_pfp),
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFF0B0B0B)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = gradeText,
                    color = subtleText,
                    fontSize = 13.sp
                )
            }

            if (isAdded) {
                AssistChip(
                    onClick = { /* no-op */ },
                    label = { Text("Added") },
                    enabled = false,
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = Color(0xFFF3F4F6),
                        disabledLabelColor = Color(0xFF9CA3AF)
                    )
                )
            } else {
                TextButton(onClick = onAdd) {
                    Text(
                        text = "Add",
                        color = accentBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddExistingStudentsPreview() {
    AddExistingStudentsScreen(
        classId = 0L,
        existingStudentIds = setOf(1L, 2L),
        onAdd = {},
        onBack = {}
    )
}
