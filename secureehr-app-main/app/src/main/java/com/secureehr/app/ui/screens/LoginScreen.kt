package com.secureehr.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.secureehr.app.R
import com.secureehr.app.data.api.LoginRequest
import com.secureehr.app.data.api.RetrofitClient
import com.secureehr.app.data.local.SavedCredential
import com.secureehr.app.data.local.SavedCredentialsManager
import com.secureehr.app.data.local.TokenManager
import com.secureehr.app.data.local.db.AppDatabase
import com.secureehr.app.data.local.deriveDisplayName
import com.secureehr.app.navigation.Screen
import com.secureehr.app.ui.components.RoleBadge
import com.secureehr.app.ui.theme.CyanAccent
import com.secureehr.app.ui.theme.TealPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, prefillEmail: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val credentialsManager = remember { SavedCredentialsManager(context) }
    val db = remember { AppDatabase.getDatabase(context) }

    var email by remember { mutableStateOf(prefillEmail ?: "") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }

    var savedCredentials by remember { mutableStateOf<List<SavedCredential>>(emptyList()) }
    var emailDropdownExpanded by remember { mutableStateOf(false) }
    var emailFieldWidthPx by remember { mutableStateOf(0) }

    val errorEmailRequired = stringResource(R.string.error_email_required)
    val errorEmailInvalid = stringResource(R.string.error_email_invalid)
    val errorPasswordRequired = stringResource(R.string.error_password_required)
    val errorPasswordMinLength = stringResource(R.string.error_password_min_length)
    val errorInvalidCredentials = stringResource(R.string.error_invalid_credentials)

    // Logo entrance animation
    var logoVisible by remember { mutableStateOf(false) }
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "logoAlpha"
    )

    LaunchedEffect(Unit) {
        delay(100)
        logoVisible = true
        savedCredentials = credentialsManager.getAll()
    }

    fun validate(): Boolean {
        var valid = true
        if (email.isBlank()) { emailError = errorEmailRequired; valid = false }
        else if (!email.contains("@") || !email.contains(".")) { emailError = errorEmailInvalid; valid = false }
        else emailError = null
        if (password.isBlank()) { passwordError = errorPasswordRequired; valid = false }
        else if (password.length < 6) { passwordError = errorPasswordMinLength; valid = false }
        else passwordError = null
        return valid
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background gradient blobs
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset((-60).dp, (-60).dp)
                .background(
                    TealPrimary.copy(alpha = 0.06f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(60.dp, 60.dp)
                .background(CyanAccent.copy(alpha = 0.04f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated logo
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .size(80.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(TealPrimary, CyanAccent),
                            start = Offset(0f, 0f),
                            end = Offset(80f, 80f)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.app_name),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.login_tagline),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(40.dp))

            // Glassmorphism input card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { emailFieldWidthPx = it.size.width }
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; emailError = null; loginError = null },
                            label = { Text(stringResource(R.string.email_address)) },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = TealPrimary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused && savedCredentials.isNotEmpty()) {
                                        emailDropdownExpanded = true
                                    }
                                },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            isError = emailError != null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        DropdownMenu(
                            expanded = emailDropdownExpanded && savedCredentials.isNotEmpty(),
                            onDismissRequest = { emailDropdownExpanded = false },
                            modifier = Modifier
                                .width(with(LocalDensity.current) { emailFieldWidthPx.toDp() })
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            savedCredentials.forEach { credential ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    credential.displayName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    credential.email,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            RoleBadge(credential.role)
                                        }
                                    },
                                    onClick = {
                                        email = credential.email
                                        password = credential.password
                                        emailError = null
                                        passwordError = null
                                        loginError = null
                                        emailDropdownExpanded = false
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                credentialsManager.delete(credential.email)
                                                savedCredentials = credentialsManager.getAll()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                    if (emailError != null)
                        Text(emailError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, top = 3.dp))
                }

                Column {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; passwordError = null; loginError = null },
                        label = { Text(stringResource(R.string.password)) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = TealPrimary) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = passwordError != null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    if (passwordError != null)
                        Text(passwordError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, top = 3.dp))
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                    )
                    Text(
                        stringResource(R.string.remember_me),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                    )
                }
            }

            if (loginError != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(loginError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Gradient login button
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
                    .clickable(enabled = !isLoading) {
                        if (validate()) {
                            isLoading = true
                            loginError = null
                            scope.launch {
                                try {
                                    val response = RetrofitClient.apiService.login(LoginRequest(email, password))
                                    db.medicalRecordDao().deleteAll()
                                    db.consentAuditDao().deleteAll()
                                    tokenManager.saveToken(response.access_token)
                                    val profile = try {
                                        RetrofitClient.apiService.getMyProfile("Bearer ${response.access_token}")
                                    } catch (_: Exception) { null }
                                    val role = profile?.role ?: "patient"
                                    tokenManager.saveUserRole(role)
                                    if (rememberMe) {
                                        val displayName = profile?.name?.takeIf { it.isNotBlank() }
                                            ?: deriveDisplayName(email)
                                        credentialsManager.upsert(email, password, displayName, role)
                                    }
                                    val destination = if (role == "doctor") Screen.DoctorDashboard.route else Screen.Dashboard.route
                                    navController.navigate(destination) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    loginError = errorInvalidCredentials
                                    isLoading = false
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading)
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else
                    Text(stringResource(R.string.sign_in), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }

            Spacer(Modifier.height(20.dp))
            TextButton(onClick = { navController.navigate(Screen.Register.route) }) {
                Text(
                    stringResource(R.string.no_account_prompt),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Text(stringResource(R.string.register), color = TealPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
