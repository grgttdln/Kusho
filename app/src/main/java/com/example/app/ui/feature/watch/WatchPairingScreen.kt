package com.example.app.ui.feature.watch

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R
import com.example.app.ui.components.PrimaryButton

@Composable
fun WatchPairingScreen(
    modifier: Modifier = Modifier,
    pairedDevices: List<String> = listOf(
        "Galaxy Watch 8 Classic",
        "Galaxy Watch 7",
        "Galaxy Watch 6 Classic"
    ),
    onRefresh: () -> Unit = {},
    onProceedToDashboard: () -> Unit = {}
) {
    var selectedDeviceIndex by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.dis_pairing),
            contentDescription = "Watch pairing illustration",
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(horizontal = 32.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Connect your\nSmartwatch",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF49A9FF),
            textAlign = TextAlign.Center,
            lineHeight = 40.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Make sure that the smartwatch you want to add has Bluetooth turned on. Make sure that you have the Kusho' Wearable App installed.",
            fontSize = 14.sp,
            color = Color(0xFF2D2D2D),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Paired Devices",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF49A9FF)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onRefresh() }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Color(0xFF49A9FF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Refresh",
                    fontSize = 16.sp,
                    color = Color(0xFF2D2D2D)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        pairedDevices.forEachIndexed { index, deviceName ->
            DeviceListItem(
                deviceName = deviceName,
                isSelected = index == selectedDeviceIndex,
                onClick = { selectedDeviceIndex = index }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Proceed to Dashboard",
            onClick = onProceedToDashboard,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DeviceListItem(
    deviceName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_watch),
            contentDescription = "Watch",
            modifier = Modifier.size(40.dp),
            tint = if (isSelected) Color(0xFF49A9FF) else Color.Black
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = deviceName,
            fontSize = 18.sp,
            color = if (isSelected) Color(0xFF49A9FF) else Color(0xFF2D2D2D),
            fontWeight = FontWeight.Normal
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WatchPairingScreenPreview() {
    WatchPairingScreen()
}

