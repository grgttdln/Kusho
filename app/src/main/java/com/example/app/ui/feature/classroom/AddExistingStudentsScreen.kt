package com.example.app.ui.feature.classroom

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.app.ui.components.PrimaryButton

/**
 * AddExistingStudentsScreen
 * - Shows global student directory
 * - Disables students already in the class (passed via `existingStudentIds`)
 */
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Add existing student",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onBack) { Text("Done") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search directory") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        val filtered = remember(directory, query) {
            if (query.isBlank()) directory else directory.filter { it.fullName.contains(query, ignoreCase = true) }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered) { student ->
                val isAdded = existingStudentIds.contains(student.studentId)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.dis_default_pfp),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = student.fullName, fontWeight = FontWeight.SemiBold)
                        Text(text = "Grade: -", color = Color.Gray, fontSize = 12.sp)
                    }

                    if (isAdded) {
                        Text(text = "Added", color = Color.Gray)
                    } else {
                        PrimaryButton(text = "Add", onClick = { onAdd(student.studentId) })
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddExistingStudentsPreview() {
    AddExistingStudentsScreen(classId = 0L, existingStudentIds = emptySet(), onAdd = {}, onBack = {})
}
