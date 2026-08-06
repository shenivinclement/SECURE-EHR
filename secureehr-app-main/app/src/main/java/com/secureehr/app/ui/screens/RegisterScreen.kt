package com.secureehr.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.secureehr.app.data.api.RegisterRequest
import com.secureehr.app.data.api.RetrofitClient
import com.secureehr.app.navigation.Screen
import com.secureehr.app.ui.components.BorderColorDim
import com.secureehr.app.ui.components.CardBgColor
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.CyanAccent
import com.secureehr.app.ui.theme.TealPrimary
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun RegisterScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("patient") }
    var isLoading by remember { mutableStateOf(false) }

    var fullNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var registerError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var valid = true
        if (fullName.isBlank()) { fullNameError = "Full name is required"; valid = false } else fullNameError = null
        if (email.isBlank()) { emailError = "Email is required"; valid = false }
        else if (!email.contains("@") || !email.contains(".")) { emailError = "Enter a valid email"; valid = false }
        else emailError = null
        if (password.isBlank()) { passwordError = "Password is required"; valid = false }
        else if (password.length < 6) { passwordError = "Min. 6 characters"; valid = false }
        else passwordError = null
        if (confirmPassword.isBlank()) { confirmPasswordError = "Please confirm password"; valid = false }
        else if (confirmPassword != password) { confirmPasswordError = "Passwords do not match"; valid = false }
        else confirmPasswordError = null
        return valid
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = CardBgColor,
        unfocusedContainerColor = CardBgColor,
        focusedBorderColor = TealPrimary,
        unfocusedBorderColor = BorderColorDim
    )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E1A))) {
        // Background accent
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.TopEnd)
                .offset(80.dp, (-80).dp)
                .background(CyanAccent.copy(alpha = 0.04f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(TealPrimary, CyanAccent),
                            start = Offset(0f, 0f),
                            end = Offset(64f, 64f)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Create Account", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text("Join SecureEHR", fontSize = 14.sp, color = TextSecondaryColor)
            Spacer(Modifier.height(32.dp))

            // Input card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PremiumTextField("Full Name", fullName, { fullName = it; fullNameError = null }, Icons.Default.Person, fullNameError, fieldColors)
                PremiumTextField("Email Address", email, { email = it; emailError = null }, Icons.Default.Email, emailError, fieldColors, KeyboardType.Email)
                PasswordField("Password", password, { password = it; passwordError = null }, passwordError, fieldColors)
                PasswordField("Confirm Password", confirmPassword, { confirmPassword = it; confirmPasswordError = null }, confirmPasswordError, fieldColors)

                // Role selection
                Column {
                    Text("Account Type", fontSize = 12.sp, color = TextSecondaryColor, modifier = Modifier.padding(bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("patient" to "Patient", "doctor" to "Doctor").forEach { (value, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (role == value) TealPrimary.copy(alpha = 0.15f) else CardBgColor,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (role == value) TealPrimary else BorderColorDim,
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                FilterChip(
                                    selected = role == value,
                                    onClick = { role = value },
                                    label = { Text(label, fontWeight = FontWeight.Medium) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.Transparent,
                                        selectedLabelColor = TealPrimary,
                                        containerColor = Color.Transparent,
                                        labelColor = TextSecondaryColor
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }
            }

            if (registerError != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(registerError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Gradient register button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        Brush.linearGradient(
                            if (!isLoading) listOf(TealPrimary, CyanAccent)
                            else listOf(Color.Gray, Color.Gray),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, 0f)
                        ),
                        RoundedCornerShape(27.dp)
                    )
                    .then(
                        if (!isLoading)
                            Modifier.border(0.dp, Color.Transparent, RoundedCornerShape(27.dp))
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        if (validate()) {
                            isLoading = true; registerError = null
                            scope.launch {
                                try {
                                    RetrofitClient.apiService.register(
                                        RegisterRequest(fullName, email, password, role)
                                    )
                                    Toast.makeText(context, "Registration successful! Please login.", Toast.LENGTH_LONG).show()
                                    navController.navigate(Screen.Login.routeWithEmail(email)) {
                                        popUpTo(Screen.Register.route) { inclusive = true }
                                    }
                                } catch (e: HttpException) {
                                    registerError = e.response()?.errorBody()?.string()?.ifBlank { null }
                                        ?: "Registration failed. Try again."
                                    isLoading = false
                                } catch (e: Exception) {
                                    registerError = "Network error. Check your connection."
                                    isLoading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    if (isLoading)
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    else
                        Text("Create Account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))
            TextButton(onClick = { navController.navigate(Screen.Login.route) }) {
                Text("Already have an account? ", color = TextSecondaryColor, fontSize = 14.sp)
                Text("Sign In", color = TealPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PremiumTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    error: String?,
    colors: TextFieldColors,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = { Icon(icon, null, tint = TealPrimary) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            isError = error != null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = colors
        )
        if (error != null)
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, top = 3.dp))
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    colors: TextFieldColors
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = TealPrimary) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = error != null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = colors
        )
        if (error != null)
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, top = 3.dp))
    }
}
