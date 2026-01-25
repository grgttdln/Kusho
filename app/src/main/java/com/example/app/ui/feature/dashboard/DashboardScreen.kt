package com.example.app.ui.feature.dashboard

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.data.SessionManager
import com.example.app.service.ConnectionState
import com.example.app.service.WatchConnectionManager
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.dashboard.AnalyticsCard
import com.example.app.ui.components.dashboard.BatteryIcon
import com.example.app.ui.components.dashboard.getWatchImageResource
import com.example.app.ui.components.dashboard.ActivityProgressSection
import com.example.app.ui.components.dashboard.ActivityProgress

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onNavigate: (Int) -> Unit,
    onLogout: () -> Unit,
    onNavigateToWatchPairing: () -> Unit = {},
    onNavigateToClassDetails: (String, String, String) -> Unit = { _, _, _ -> },
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val watchDevice = uiState.watchDevice
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val watchConnectionManager = remember { WatchConnectionManager.getInstance(context) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showInstallInstructionsDialog by remember { mutableStateOf(false) }
    val greeting = viewModel.getGreeting()
    
    // Bluetooth enable request launcher (must be declared first)
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Bluetooth was enabled, check connection again
            viewModel.checkWatchConnection()
        }
    }
    
    // Bluetooth permission request launcher (Android 12+)
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, now launch Bluetooth enable dialog
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableBtIntent)
        }
    }
    
    // Request battery update every time Dashboard appears/resumes
    LaunchedEffect(Unit) {
        viewModel.requestBatteryUpdate()
        viewModel.refreshAnalytics()
    }

    LaunchedEffect(watchDevice.isConnected) {
        if (watchDevice.isConnected) {
            viewModel.requestBatteryUpdate()
        }
    }

    // Collect activities from ViewModel and map into ActivityProgress UI model.
    val activities by viewModel.activities.collectAsState()

    val activityProgressList: List<ActivityProgress> = remember(activities) {
        if (activities.isEmpty()) {
            // Return empty list to trigger the empty state in ActivityProgressSection
            emptyList()
        } else {
            // prepare same icon pool as LearnModeActivitySelectionScreen for consistent assignment
            val allIcons = listOf(
                R.drawable.ic_activity_1, R.drawable.ic_activity_2, R.drawable.ic_activity_3, R.drawable.ic_activity_4,
                R.drawable.ic_activity_5, R.drawable.ic_activity_6, R.drawable.ic_activity_7, R.drawable.ic_activity_8,
                R.drawable.ic_activity_9, R.drawable.ic_activity_10, R.drawable.ic_activity_11, R.drawable.ic_activity_12,
                R.drawable.ic_activity_13, R.drawable.ic_activity_14, R.drawable.ic_activity_15, R.drawable.ic_activity_16,
                R.drawable.ic_activity_17, R.drawable.ic_activity_18, R.drawable.ic_activity_19, R.drawable.ic_activity_20,
                R.drawable.ic_activity_21, R.drawable.ic_activity_22
            )

            fun getIconForActivity(activityId: Long): Int {
                val iconIndex = ((activityId - 1) % allIcons.size).toInt()
                return allIcons[iconIndex]
            }

            activities.map { act ->
                 // If the activity doesn't have a coverImagePath, try to resolve a drawable named ic_<slugified title>
                 val resolvedCover = act.coverImagePath?.takeIf { it.isNotBlank() } ?: run {
                     val slug = act.title
                         .lowercase()
                         .replace(Regex("[^a-z0-9]+"), "_")
                         .trim('_')
                     val candidate = "ic_$slug"
                     val resId = context.resources.getIdentifier(candidate, "drawable", context.packageName)
                     if (resId != 0) candidate else null
                 }

                ActivityProgress(
                    activityId = act.id.toString(),
                    activityName = act.title,
                    coverImagePath = resolvedCover,
                    iconRes = getIconForActivity(act.id),
                    accuracyDeltaPercent = 0,
                    timeDeltaSeconds = 0,
                    masteryPercent = 0f,
                    masteryLabel = "Beginner",
                    avgAttempts = 0f,
                    avgAccuracyPercent = 0,
                    avgScoreText = "",
                    avgTimeSeconds = 0
                )
             }
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
            
            // Install Instructions Dialog
            if (showInstallInstructionsDialog) {
                AlertDialog(
                    onDismissRequest = { showInstallInstructionsDialog = false },
                    title = { Text(text = "Install Kusho Watch App", fontWeight = FontWeight.Bold) },
                    text = { 
                        Text(
                            text = "To use Kusho with your watch:\n\n" +
                                   "1. Open Play Store on your watch\n" +
                                   "2. Search for \"Kusho\"\n" +
                                   "3. Install the app\n\n" +
                                   "Once installed, your watch will connect automatically."
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showInstallInstructionsDialog = false }) {
                            Text(text = "Got it", color = Color(0xFF2196F3))
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
                    .clickable {
                        when (watchDevice.connectionState) {
                            ConnectionState.BLUETOOTH_OFF -> {
                                // Check if we need to request permission (Android 12+)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    // Android 12+ requires BLUETOOTH_CONNECT permission
                                    when (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    )) {
                                        PackageManager.PERMISSION_GRANTED -> {
                                            // Permission already granted, launch Bluetooth enable
                                            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                            bluetoothEnableLauncher.launch(enableBtIntent)
                                        }
                                        else -> {
                                            // Request permission first
                                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                        }
                                    }
                                } else {
                                    // Android 11 and below - no runtime permission needed
                                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                    bluetoothEnableLauncher.launch(enableBtIntent)
                                }
                            }
                            ConnectionState.NO_WATCH -> {
                                // Navigate to watch pairing screen
                                onNavigateToWatchPairing()
                            }
                            ConnectionState.WATCH_PAIRED_NO_APP -> {
                                // Watch paired but app not installed - open Play Store on watch
                                watchConnectionManager.openPlayStoreOnWatch()
                                Toast.makeText(
                                    context,
                                    "Opening Play Store on your watch...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                // Other states: do nothing or handle future cases
                            }
                        }
                    },
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
                    label = if (uiState.totalStudents == 1) "Total Student" else "Total Students",
                    badgeText = null
                )

                AnalyticsCard(
                    icon = Icons.Filled.BarChart,
                    number = uiState.totalActivities.toString(),
                    label = if (uiState.totalActivities == 1) "Activity" else "Activities",
                    badgeText = null
                )

                AnalyticsCard(
                    icon = Icons.Filled.TypeSpecimen,
                    number = uiState.totalWords.toString(),
                    label = if (uiState.totalWords == 1) "Total Word" else "Total Words",
                    badgeText = null
                )

                AnalyticsCard(
                    icon = Icons.Filled.CheckCircle,
                    number = "6",
                    label = if (6 == 1) "Student at Mastery" else "Students at Mastery",
                    badgeText = "≥ 90% accuracy"
                )

                AnalyticsCard(
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    number = "5",
                    label = if (5 == 1) "Student Below Target" else "Students Below Target",
                    badgeText = "< 70% accuracy"
                )

                Spacer(modifier = Modifier.width(30.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Activity Progress Section (previous accordion design)
            ActivityProgressSection(
                activities = activityProgressList,
                onActivityClick = { /* TODO */ }
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

// ---------------------- Preview ----------------------

@Preview(showBackground = true)
@Composable
fun ActivityProgressSectionPreview() {
    val sample = listOf(
        ActivityProgress(
            activityId = "a1",
            activityName = "Alphabet Gestures",
            coverImagePath = "ic_apple",
            accuracyDeltaPercent = 12,
            timeDeltaSeconds = 0,
            masteryPercent = 0.78f,
            masteryLabel = "Intermediate",
            avgAttempts = 1.9f,
            avgAccuracyPercent = 79,
            avgScoreText = "8/10",
            avgTimeSeconds = 124
        ),
        ActivityProgress(
            activityId = "a2",
            activityName = "Word Practice",
            coverImagePath = "ic_apple",
            accuracyDeltaPercent = 0,
            timeDeltaSeconds = -20,
            masteryPercent = 0.92f,
            masteryLabel = "Advanced",
            avgAttempts = 1.3f,
            avgAccuracyPercent = 92,
            avgScoreText = "9/10",
            avgTimeSeconds = 80
        ),
        ActivityProgress(
            activityId = "a3",
            activityName = "Shape Matching",
            coverImagePath = "ic_apple",
            accuracyDeltaPercent = 0,
            timeDeltaSeconds = 0,
            masteryPercent = 0.45f,
            masteryLabel = "Beginner",
            avgAttempts = 2.4f,
            avgAccuracyPercent = 45,
            avgScoreText = "5/10",
            avgTimeSeconds = 165
        )
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ActivityProgressSection(activities = sample, onActivityClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    DashboardScreen(onNavigate = {}, onLogout = {})
}