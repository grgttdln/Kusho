package com.example.app.ui.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.data.SessionManager
import com.example.app.service.ConnectionState
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.ClassCard


@Composable
fun DashboardScreen(
    onNavigate: (Int) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val watchDevice = uiState.watchDevice
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }

    // State for logout confirmation dialog
    var showLogoutDialog by remember { mutableStateOf(false) }
    val greeting = viewModel.getGreeting()
    
    // Request battery update every time Dashboard appears/resumes
    LaunchedEffect(Unit) {
        viewModel.requestBatteryUpdate()
    }
    
    // Also request when watch connection state changes to connected
    LaunchedEffect(watchDevice.isConnected) {
        if (watchDevice.isConnected) {
            viewModel.requestBatteryUpdate()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Kusho Logo - Centered
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

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Confirmation Dialog
            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = {
                        Text(
                            text = "Log Out",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text("Are you sure you want to log out?")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showLogoutDialog = false
                                sessionManager.clearSession()
                                onLogout()
                            }
                        ) {
                            Text(
                                text = "Log Out",
                                color = Color(0xFFE53935)
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showLogoutDialog = false }
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color(0xFF2196F3)
                            )
                        }
                    }
                )
            }

            // User Profile Section - 45dp circle, blue background (clickable for logout)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .clickable { showLogoutDialog = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1769C2)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_profile_placeholder),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.size(30.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }

                Spacer(modifier = Modifier.width(13.dp))

                Text(
                    text = uiState.userName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    lineHeight = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(29.dp))

            // Greeting - 18sp, Medium weight
            Text(
                text = greeting,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                lineHeight = 27.sp,
                modifier = Modifier.padding(horizontal = 30.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Device Card - #E9FCFF background, 145dp height, 24dp radius
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .height(145.dp)
                    .clickable {
                        if (!watchDevice.isConnected) {
                            // Navigate to watch pairing screen
                            // onNavigate(Screen.WatchPairing.route)
                        }
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (watchDevice.isConnected) Color(0xFFE9FCFF) else Color(0xFFF5F5F5)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Refresh button
                    if (watchDevice.isConnected) {
                        IconButton(
                            onClick = { 
                                viewModel.checkWatchConnection()
                                viewModel.requestBatteryUpdate() // Also request fresh battery data
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(24.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF3FA9F8)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh connection",
                                    tint = Color(0xFF3FA9F8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            // Display name based on connection state
                            Text(
                                text = when (watchDevice.connectionState) {
                                    ConnectionState.WATCH_CONNECTED -> watchDevice.name
                                    ConnectionState.WATCH_PAIRED_NO_APP -> watchDevice.name
                                    ConnectionState.BLUETOOTH_OFF -> "Bluetooth Off"
                                    ConnectionState.NO_WATCH -> "No Watch Connected"
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (watchDevice.isConnected) Color(0xFF3FA9F8) else Color(0xFF888888),
                                lineHeight = 27.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Display status based on connection state
                            Text(
                                text = when (watchDevice.connectionState) {
                                    ConnectionState.WATCH_CONNECTED -> "Connected"
                                    ConnectionState.WATCH_PAIRED_NO_APP -> "Install Kusho app on watch"
                                    ConnectionState.BLUETOOTH_OFF -> "Turn on Bluetooth"
                                    ConnectionState.NO_WATCH -> "Tap to connect"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = when (watchDevice.connectionState) {
                                    ConnectionState.WATCH_CONNECTED -> Color(0xFF3FA9F8)
                                    ConnectionState.WATCH_PAIRED_NO_APP -> Color(0xFFFF9800) // Orange for warning
                                    else -> Color(0xFF888888)
                                },
                                lineHeight = 21.sp
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Only show battery if fully connected with app
                            if (watchDevice.connectionState == ConnectionState.WATCH_CONNECTED) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BatteryIcon(percentage = watchDevice.batteryPercentage)

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = watchDevice.batteryPercentage?.let { "$it%" } ?: "Loading...",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color(0xFF3FA9F8),
                                        lineHeight = 21.sp
                                    )
                                }
                            }
                        }

                        Image(
                            painter = painterResource(id = R.drawable.ic_watch7),
                            contentDescription = "Watch",
                            modifier = Modifier.size(111.dp),
                            contentScale = ContentScale.Fit,
                            alpha = if (watchDevice.isConnected) 1f else 0.4f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Quick Analytics - 20sp, Medium weight
            Text(
                text = "Quick Analytics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                lineHeight = 30.sp,
                modifier = Modifier.padding(horizontal = 30.dp)
            )

            Spacer(modifier = Modifier.height(7.dp))

            // Analytics Cards - 168dp wide, 128dp tall, 18dp radius
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                AnalyticsCard(
                    number = uiState.totalStudents.toString(),
                    label = "Total Students",
                    modifier = Modifier.weight(1f)
                )

                AnalyticsCard(
                    number = uiState.totalClassrooms.toString(),
                    label = "Classrooms",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Your Recent Class - 20sp, Medium weight
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Recent Class",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    lineHeight = 30.sp
                )

                TextButton(onClick = { /* TODO */ }) {
                    Text(
                        text = "View More",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF3FA9F8),
                        lineHeight = 18.sp,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            // Class Card
            ClassCard(
                classCode = "G1-YB",
                className = "Grade 1 Young Builders",
                imageRes = R.drawable.ic_class_abc,
                onClick = { /* TODO: Navigate to class details */ },
                modifier = Modifier.padding(horizontal = 30.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        BottomNavBar(
            selectedTab = 0,
            onTabSelected = { onNavigate(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun BatteryIcon(
    percentage: Int?,
    modifier: Modifier = Modifier
) {
    val batteryColor = Color(0xFF14FF1E) // Green color from design

    Box(
        modifier = modifier.size(24.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bodyWidth = size.width * 0.75f
            val bodyHeight = size.height * 0.65f
            val strokeWidth = 1.dp.toPx()

            // Battery border with opacity 0.35
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(0f, (size.height - bodyHeight) / 2),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(4.3.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )

            // Battery cap with opacity 0.4
            drawRect(
                color = Color.Black.copy(alpha = 0.4f),
                topLeft = Offset(bodyWidth, size.height * 0.35f),
                size = Size(size.width * 0.25f, size.height * 0.3f)
            )

            // Battery capacity (fill) - only if percentage is not null
            percentage?.let {
                val fillWidth = (bodyWidth - strokeWidth * 2) * (it / 100f)
                val fillHeight = bodyHeight - strokeWidth * 2
                drawRoundRect(
                    color = batteryColor,
                    topLeft = Offset(strokeWidth, (size.height - bodyHeight) / 2 + strokeWidth),
                    size = Size(fillWidth, fillHeight),
                    cornerRadius = CornerRadius(2.5.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun AnalyticsCard(
    number: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(128.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE9FCFF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = number,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3FA9F8),
                    lineHeight = 27.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = label,
                    fontSize = 16.sp,
                    color = Color(0xFF3FA9F8),
                    fontWeight = FontWeight.Medium,
                    lineHeight = 27.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    DashboardScreen(onNavigate = {}, onLogout = {})
}
