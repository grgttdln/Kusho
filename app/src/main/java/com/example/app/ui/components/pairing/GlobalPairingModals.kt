package com.example.app.ui.components.pairing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.app.R
import com.example.app.service.WatchConnectionManager

@Composable
fun GlobalPairingModals() {
    val context = LocalContext.current
    val watchConnectionManager = remember { WatchConnectionManager.getInstance(context) }
    val pairingRequest by watchConnectionManager.pairingRequest.collectAsState()

    var showPairingSuccessModal by remember { mutableStateOf(false) }
    var pairedWatchName by remember { mutableStateOf("") }

    // Watch Pairing Request Modal
    pairingRequest?.let { request ->
        Dialog(
            onDismissRequest = { watchConnectionManager.declinePairing(request.nodeId) },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Main card content (positioned below the mascot)
                Column(
                    modifier = Modifier
                        .padding(top = 80.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                ) {
                    // Blue header section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .background(Color(0xFF49A9FF))
                    )

                    // White content section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 24.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Watch Pairing Request",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B0B0B),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "\"${request.watchName}\" is requesting to connect.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF555555),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Buttons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Accept button
                            Button(
                                onClick = {
                                    watchConnectionManager.acceptPairing(request.nodeId)
                                    pairedWatchName = request.watchName
                                    showPairingSuccessModal = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF49A9FF)
                                )
                            ) {
                                Text(
                                    text = "Accept",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            // Decline button
                            Button(
                                onClick = { watchConnectionManager.declinePairing(request.nodeId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .border(
                                        width = 0.dp,
                                        color = Color.Transparent,
                                        shape = RoundedCornerShape(28.dp)
                                    ),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD6EDFF)
                                )
                            ) {
                                Text(
                                    text = "Decline",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF49A9FF)
                                )
                            }
                        }
                    }
                }

                // Mascot image overlapping the top
                Image(
                    painter = painterResource(id = R.drawable.dis_question),
                    contentDescription = "Pairing request",
                    modifier = Modifier
                        .size(160.dp)
                        .offset(y = 0.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    // Pairing Success Modal
    if (showPairingSuccessModal) {
        Dialog(
            onDismissRequest = { showPairingSuccessModal = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Main card content (positioned below the mascot)
                Column(
                    modifier = Modifier
                        .padding(top = 80.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                ) {
                    // Blue header section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .background(Color(0xFF49A9FF))
                    )

                    // White content section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 24.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Paired Successfully!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B0B0B),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val pairingMessage = if (pairedWatchName == "Smartwatch") {
                            "Your smartwatch is now connected and ready to use."
                        } else {
                            "Your watch \"$pairedWatchName\" is now connected and ready to use."
                        }

                        Text(
                            text = pairingMessage,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF555555),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Continue button
                        Button(
                            onClick = { showPairingSuccessModal = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF49A9FF)
                            )
                        ) {
                            Text(
                                text = "Continue",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Mascot image overlapping the top
                Image(
                    painter = painterResource(id = R.drawable.dis_success),
                    contentDescription = "Pairing success",
                    modifier = Modifier
                        .size(160.dp)
                        .offset(y = 0.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
