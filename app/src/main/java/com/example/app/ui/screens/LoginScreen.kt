package com.example.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R
import com.example.app.ui.components.PrimaryButton
import com.example.app.ui.theme.KushoTheme

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLoginSuccess: () -> Unit = {},
    onNavigateToSignUp: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var staySignedIn by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Scrollable content area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
                .padding(bottom = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Character illustration at top
            Image(
                painter = painterResource(id = R.drawable.dis_study),
                contentDescription = null,
                modifier = Modifier
                    .size(280.dp)
                    .padding(bottom = 16.dp),
                contentScale = ContentScale.Fit
            )

            // Login to Kusho title image
            Image(
                painter = painterResource(id = R.drawable.title_login),
                contentDescription = "Login to Kusho",
                modifier = Modifier
                    .width(300.dp)
                    .height(80.dp)
                    .padding(bottom = 32.dp),
                contentScale = ContentScale.Fit
            )

            // Username TextField
            TextField(
                value = username,
                onValueChange = { username = it },
                placeholder = { Text("Username") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF49A9FF),
                    unfocusedIndicatorColor = Color(0xFF49A9FF),
                    focusedTextColor = Color(0xFF2D2D2D),
                    unfocusedTextColor = Color(0xFF2D2D2D)
                ),
                singleLine = true
            )

            // Password TextField with visibility toggle
            TextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color(0xFF49A9FF)
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF49A9FF),
                    unfocusedIndicatorColor = Color(0xFF49A9FF),
                    focusedTextColor = Color(0xFF2D2D2D),
                    unfocusedTextColor = Color(0xFF2D2D2D)
                ),
                singleLine = true
            )

            // Stay Signed in checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = staySignedIn,
                    onCheckedChange = { staySignedIn = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF49A9FF),
                        uncheckedColor = Color(0xFF49A9FF)
                    )
                )
                Text(
                    text = "Stay Signed in",
                    color = Color(0xFF2D2D2D),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Fixed bottom section with button and sign-up link
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Login Button
            PrimaryButton(
                text = "Log in",
                onClick = {
                    // TODO: Add actual login logic
                    onLoginSuccess()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sign Up link at bottom
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    color = Color(0xFF2D2D2D),
                    fontSize = 16.sp
                )
                TextButton(
                    onClick = onNavigateToSignUp,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Sign up here.",
                        color = Color(0xFF49A9FF),
                        fontSize = 16.sp,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    KushoTheme {
        LoginScreen()
    }
}

