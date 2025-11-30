package com.example.vibeai

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.vibeai.ui.theme.VibeAITheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure Firebase is initialised (if not already)
        FirebaseApp.initializeApp(this)

        setContent {
            VibeAITheme {
                SignUpScreen(
                    onSignUpSuccess = {
                        // After successful signup, go to Home
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    },
                    onLoginClick = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Title
                Text(
                    text = "Create Account",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Sign up to continue with VibeAI",
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Full Name") },
                    singleLine = true
                )

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    singleLine = true
                )

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                // Confirm Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                // Error message (if any)
                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sign Up Button (Firebase)
                Button(
                    onClick = {
                        // Basic validation
                        if (name.isBlank() || email.isBlank() ||
                            password.isBlank() || confirmPassword.isBlank()
                        ) {
                            Toast.makeText(
                                context,
                                "Please fill in all fields",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        if (password != confirmPassword) {
                            errorMessage = "Passwords do not match"
                            return@Button
                        }

                        if (password.length < 6) {
                            errorMessage = "Password must be at least 6 characters"
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null

                        val auth = FirebaseAuth.getInstance()
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    Toast.makeText(
                                        context,
                                        "Account created successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // You could also save 'name' to Firestore later
                                    onSignUpSuccess()
                                } else {
                                    errorMessage = task.exception?.localizedMessage
                                        ?: "Sign up failed. Please try again."
                                }
                            }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Sign Up")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Already have an account?
                TextButton(
                    onClick = { onLoginClick() }
                ) {
                    Text("Already have an account? Log In")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpPreview() {
    VibeAITheme {
        SignUpScreen()
    }
}
