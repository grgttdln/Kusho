package com.example.app.ui.feature.auth.login

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.ui.components.PrimaryButton
import com.example.app.ui.theme.KushoTheme

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit = {},
    onNavigateToSignUp: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var staySignedIn by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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

                Image(
                    painter = painterResource(id = R.drawable.dis_study),
                    contentDescription = null,
                    modifier = Modifier
                        .size(280.dp)
                        .padding(bottom = 16.dp),
                    contentScale = ContentScale.Fit
                )

                Image(
                    painter = painterResource(id = R.drawable.title_login),
                    contentDescription = "Login to Kusho",
                    modifier = Modifier
                        .width(300.dp)
                        .height(80.dp)
                        .padding(bottom = 32.dp),
                    contentScale = ContentScale.Fit
                )

                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email") },
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
                    singleLine = true,
                    enabled = !uiState.isLoading
                )

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
                    singleLine = true,
                    enabled = !uiState.isLoading
                )

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
                        ),
                        enabled = !uiState.isLoading
                    )
                    Text(
                        text = "Stay Signed in",
                        color = Color(0xFF2D2D2D),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF49A9FF)
                    )
                } else {
                    PrimaryButton(
                        text = "Log in",
                        onClick = {
                            viewModel.login(
                                email = email,
                                password = password,
                                staySignedIn = staySignedIn,
                                onSuccess = { onLoginSuccess() }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    KushoTheme {
        LoginScreen()
    }
}

