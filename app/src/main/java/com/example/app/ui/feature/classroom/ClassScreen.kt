package com.example.app.ui.feature.classroom

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Edit
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
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.PrimaryButton
import com.example.app.ui.components.classroom.StudentCard
import com.example.app.ui.components.classroom.RemoveStudentDialog
import androidx.compose.ui.text.style.TextAlign


@Suppress("UNUSED_PARAMETER", "DEPRECATION")
@Composable
fun ClassScreen(
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToCreateClass: () -> Unit = {},
    onNavigateToClassDetails: (String) -> Unit = {},
    onNavigateToStudentDetails: (String, String, String) -> Unit = { _, _, _ -> },
    onNavigateToAddStudent: () -> Unit = {},
    vm: ClassroomViewModel = viewModel<ClassroomViewModel>()
) {
    val classUiState by vm.classListUiState.collectAsState()
    val students by vm.allStudents.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf("Name (A-Z)") }
    var isRemovalMode by remember { mutableStateOf(false) }
    var studentToRemove by remember { mutableStateOf<RosterStudent?>(null) }

    val accentBlue = Color(0xFF3FA9F8)
    val textDark = Color(0xFF0B0B0B)
    val subtleText = Color(0xFF6B7280)
    val borderColor = Color(0xFFE6EEF6)
    val surfaceWhite = Color.White

    // Filter and sort students
    val displayedStudents = remember(students, searchQuery, sortOption) {
        val filtered = if (searchQuery.isBlank()) students else students.filter {
            it.fullName.contains(searchQuery, ignoreCase = true)
        }
        when (sortOption) {
            "Name (A-Z)" -> filtered.sortedBy { it.fullName }
            "Name (Z-A)" -> filtered.sortedByDescending { it.fullName }
            else -> filtered
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 170.dp),
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(36.dp))

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your Students",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textDark,
                        textAlign = TextAlign.Center
                    )
                }

                IconButton(
                    onClick = { isRemovalMode = !isRemovalMode },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = accentBlue
                    )
                }
            }


            Spacer(Modifier.height(16.dp))

            val pillShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = surfaceWhite,
                    tonalElevation = 0.dp,
                    shadowElevation = 2.dp,
                    shape = pillShape,
                    modifier = Modifier
                        .weight(1f)
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
                            contentDescription = "Search",
                            tint = accentBlue
                        )

                        Spacer(Modifier.width(10.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
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
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
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

                Spacer(Modifier.width(12.dp))

                Surface(
                    color = surfaceWhite,
                    tonalElevation = 0.dp,
                    shadowElevation = 2.dp,
                    shape = pillShape,
                    modifier = Modifier
                        .height(56.dp)
                        .clip(pillShape)
                        .border(1.dp, borderColor, pillShape)
                        .clickable { isSortMenuExpanded = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = accentBlue
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when (sortOption) {
                                "Name (A-Z)" -> "A–Z"
                                "Name (Z-A)" -> "Z–A"
                                else -> "Sort"
                            },
                            color = textDark,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                DropdownMenu(
                    expanded = isSortMenuExpanded,
                    onDismissRequest = { isSortMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Name (A-Z)") },
                        onClick = {
                            sortOption = "Name (A-Z)"
                            isSortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Name (Z-A)") },
                        onClick = {
                            sortOption = "Name (Z-A)"
                            isSortMenuExpanded = false
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (classUiState.isLoading) {
                Spacer(Modifier.height(40.dp))
                CircularProgressIndicator(color = accentBlue)
                Spacer(Modifier.height(40.dp))
            } else if (classUiState.error != null) {
                Spacer(Modifier.height(40.dp))
                Text(
                    text = classUiState.error!!,
                    fontSize = 16.sp,
                    color = Color.Red,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(40.dp))
            } else if (displayedStudents.isEmpty()) {
                Spacer(Modifier.height(40.dp))

                Text(
                    text = "No students found",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Add students to get started. Use the search to find existing students.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = subtleText,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )


                Spacer(Modifier.height(40.dp))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    displayedStudents.chunked(2).forEach { rowStudents ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowStudents.forEach { student ->
                                StudentCard(
                                    studentName = student.fullName,
                                    profileImageRes = R.drawable.dis_default_pfp,
                                    profileImagePath = student.pfpPath,
                                    onClick = {
                                        onNavigateToStudentDetails(
                                            student.studentId.toString(),
                                            student.fullName,
                                            ""
                                        )
                                    },
                                    isRemovalMode = isRemovalMode,
                                    onRemove = { studentToRemove = student },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (rowStudents.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Pinned bottom actions (always visible)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // Button pinned ABOVE the BottomNavBar
            Surface(
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp, top = 10.dp)
                ) {
                    PrimaryButton(
                        text = "Add a Student",
                        onClick = onNavigateToAddStudent,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            BottomNavBar(
                selectedTab = 2,
                onTabSelected = { onNavigate(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        studentToRemove?.let { student ->
            RemoveStudentDialog(
                studentName = student.fullName,
                onConfirm = {
                    vm.deleteStudent(
                              studentId = student.studentId,
                              onSuccess = {
                                  studentToRemove = null
                                  isRemovalMode = false
                                  // refresh lists
                                 vm.loadClasses()
                              },
                             onError = { _errorMsg: String ->
                                  // TODO: show snackbar/toast. For now just dismiss the dialog.
                                  studentToRemove = null
                              }
                          )
                },
                onDismiss = { studentToRemove = null }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ClassScreenPreview() {
    ClassScreen(onNavigate = {})
}
