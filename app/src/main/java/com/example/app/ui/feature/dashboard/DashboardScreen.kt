package com.example.app.ui.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TypeSpecimen
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.data.SessionManager
import com.example.app.service.ConnectionState
import com.example.app.ui.components.BottomNavBar

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onNavigate: (Int) -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val watchDevice = uiState.watchDevice
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    val greeting = viewModel.getGreeting()

    LaunchedEffect(Unit) {
        viewModel.requestBatteryUpdate()
        viewModel.refreshAnalytics()
    }

    LaunchedEffect(watchDevice.isConnected) {
        if (watchDevice.isConnected) {
            viewModel.requestBatteryUpdate()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
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
                    title = { Text(text = "Log Out", fontWeight = FontWeight.Bold) },
                    text = { Text("Are you sure you want to log out?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showLogoutDialog = false
                                sessionManager.clearSession()
                                onLogout()
                            }
                        ) { Text(text = "Log Out", color = Color(0xFFE53935)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text(text = "Cancel", color = Color(0xFF2196F3))
                        }
                    }
                )
            }

            // User Profile Section
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

            // Greeting
            Text(
                text = greeting,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                lineHeight = 27.sp,
                modifier = Modifier.padding(horizontal = 30.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Device Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .height(145.dp)
                    .clickable { },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (watchDevice.isConnected) Color(0xFFE9FCFF) else Color(0xFFF5F5F5)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Refresh button
                    if (watchDevice.isConnected) {
                        IconButton(
                            onClick = {
                                viewModel.checkWatchConnection()
                                viewModel.requestBatteryUpdate()
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
                        Column(modifier = Modifier.weight(1f)) {
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
                                    ConnectionState.WATCH_PAIRED_NO_APP -> Color(0xFFFF9800)
                                    else -> Color(0xFF888888)
                                },
                                lineHeight = 21.sp
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            if (watchDevice.connectionState == ConnectionState.WATCH_CONNECTED) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            painter = painterResource(id = getWatchImageResource(watchDevice.name)),
                            contentDescription = "Watch",
                            modifier = Modifier.size(111.dp),
                            contentScale = ContentScale.Fit,
                            alpha = if (watchDevice.isConnected) 1f else 0.4f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Quick Analytics
            Text(
                text = "Quick Analytics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                lineHeight = 30.sp,
                modifier = Modifier.padding(horizontal = 30.dp)
            )

            Spacer(modifier = Modifier.height(7.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 30.dp, top = 4.dp, bottom = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                AnalyticsCard(
                    icon = Icons.Filled.Group,
                    number = uiState.totalStudents.toString(),
                    label = "Total Students",
                    badgeText = null // e.g. "vs 1,114 last month"
                )

                AnalyticsCard(
                    icon = Icons.Filled.BarChart,
                    number = uiState.totalActivities.toString(),
                    label = "Activities",
                    badgeText = null
                )

                AnalyticsCard(
                    icon = Icons.Filled.TypeSpecimen,
                    number = "50",
                    label = "Total Words",
                    badgeText = null
                )

                AnalyticsCard(
                    icon = Icons.Filled.CheckCircle,
                    number = "6",
                    label = "Students at Mastery",
                    badgeText = "≥ 90% accuracy"
                )

                AnalyticsCard(
                    icon = Icons.Filled.TrendingDown,
                    number = "5",
                    label = "Students Below Target",
                    badgeText = "< 70% accuracy"
                )

                Spacer(modifier = Modifier.width(30.dp))
            }

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
    val batteryColor = Color(0xFF14FF1E)

    Box(modifier = modifier.size(24.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bodyWidth = size.width * 0.75f
            val bodyHeight = size.height * 0.65f
            val strokeWidth = 1.dp.toPx()

            drawRoundRect(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(0f, (size.height - bodyHeight) / 2),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(4.3.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )

            drawRect(
                color = Color.Black.copy(alpha = 0.4f),
                topLeft = Offset(bodyWidth, size.height * 0.35f),
                size = Size(size.width * 0.25f, size.height * 0.3f)
            )

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
    icon: ImageVector,
    number: String,
    label: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null
) {
    val kushoBlue = Color(0xFF3FA9F8)
    val bg = Color(0xFFE9FCFF)
    val border = kushoBlue.copy(alpha = 0.22f)
    val iconBg = Color.White

    Card(
        modifier = modifier
            .height(128.dp)
            .width(168.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Start)
                    .size(36.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = kushoBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))


            Text(
                text = number,
                modifier = Modifier.align(Alignment.Start),
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = kushoBlue,
                lineHeight = 30.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = label,
                modifier = Modifier.align(Alignment.Start),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = kushoBlue.copy(alpha = 0.80f),
                letterSpacing = 0.6.sp,
                lineHeight = 14.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (!badgeText.isNullOrBlank()) {
                Text(
                    text = badgeText,
                    modifier = Modifier.align(Alignment.Start),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = kushoBlue.copy(alpha = 0.65f),
                    lineHeight = 14.sp,
                    maxLines = 1
                )
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}


/**
 * Map watch model name to corresponding drawable resource
 */
fun getWatchImageResource(watchName: String): Int {
    return when {
        watchName.contains("Watch8", ignoreCase = true) && watchName.contains("Classic", ignoreCase = true) -> R.drawable.ic_galaxy_watch_8_classic
        watchName.contains("Watch 8", ignoreCase = true) && watchName.contains("Classic", ignoreCase = true) -> R.drawable.ic_galaxy_watch_8_classic
        watchName.contains("Watch8", ignoreCase = true) -> R.drawable.ic_galaxy_watch_8
        watchName.contains("Watch 8", ignoreCase = true) -> R.drawable.ic_galaxy_watch_8
        watchName.contains("Watch7", ignoreCase = true) -> R.drawable.ic_galaxy_watch_7
        watchName.contains("Watch 7", ignoreCase = true) -> R.drawable.ic_galaxy_watch_7
        watchName.contains("Watch6", ignoreCase = true) && watchName.contains("Classic", ignoreCase = true) -> R.drawable.ic_galaxy_watch_6_classic
        watchName.contains("Watch 6", ignoreCase = true) && watchName.contains("Classic", ignoreCase = true) -> R.drawable.ic_galaxy_watch_6_classic
        watchName.contains("Watch6", ignoreCase = true) -> R.drawable.ic_galaxy_watch_6
        watchName.contains("Watch 6", ignoreCase = true) -> R.drawable.ic_galaxy_watch_6
        watchName.contains("Watch5", ignoreCase = true) && watchName.contains("Pro", ignoreCase = true) -> R.drawable.ic_galaxy_watch_5_pro
        watchName.contains("Watch 5", ignoreCase = true) && watchName.contains("Pro", ignoreCase = true) -> R.drawable.ic_galaxy_watch_5_pro
        watchName.contains("Watch5", ignoreCase = true) -> R.drawable.ic_galaxy_watch_5
        watchName.contains("Watch 5", ignoreCase = true) -> R.drawable.ic_galaxy_watch_5
        watchName.contains("Watch4", ignoreCase = true) && watchName.contains("Classic", ignoreCase = true) -> R.drawable.ic_galaxy_watch_4_classic
        watchName.contains("Watch 4", ignoreCase = true) && watchName.contains("Classic", ignoreCase = true) -> R.drawable.ic_galaxy_watch_4_classic
        watchName.contains("Watch4", ignoreCase = true) -> R.drawable.ic_galaxy_watch_4
        watchName.contains("Watch 4", ignoreCase = true) -> R.drawable.ic_galaxy_watch_4
        watchName.contains("Ultra", ignoreCase = true) -> R.drawable.ic_galaxy_ultra
        else -> R.drawable.ic_watch_generic
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    DashboardScreen(onNavigate = {}, onLogout = {})
}
