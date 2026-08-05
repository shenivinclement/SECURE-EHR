package com.secureehr.app.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.secureehr.app.R
import com.secureehr.app.data.api.ChangePasswordRequest
import com.secureehr.app.data.api.RetrofitClient
import com.secureehr.app.data.local.TokenManager
import com.secureehr.app.data.local.db.AppDatabase
import com.secureehr.app.navigation.Screen
import com.secureehr.app.ui.theme.TealPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val db = remember { AppDatabase.getDatabase(context) }

    var darkModeEnabled by remember { mutableStateOf(true) }
    var biometricEnabled by remember { mutableStateOf(false) }
    var faceIdEnabled by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("Hindi") }

    var showChangePassword by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showVersion by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showFaceIdDialog by remember { mutableStateOf(false) }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordSuccess by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        tokenManager.darkMode.collect { darkModeEnabled = it }
    }
    LaunchedEffect(Unit) {
        tokenManager.biometricEnabled.collect { biometricEnabled = it }
    }
    LaunchedEffect(Unit) {
        tokenManager.language.collect { selectedLanguage = it }
    }

    val languages = listOf("English", "Hindi")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // General
            item {
                SettingsSectionHeader(stringResource(R.string.section_general))
                SettingsToggleRow(stringResource(R.string.dark_mode), darkModeEnabled) { enabled ->
                    darkModeEnabled = enabled
                    scope.launch { tokenManager.setDarkMode(enabled) }
                    AppCompatDelegate.setDefaultNightMode(
                        if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                    )
                }
                SettingsToggleRow(stringResource(R.string.fingerprint_auth), biometricEnabled) { enabled ->
                    biometricEnabled = enabled
                    scope.launch { tokenManager.setBiometricEnabled(enabled) }
                }
            }

            // Security
            item {
                Spacer(Modifier.height(16.dp))
                SettingsSectionHeader(stringResource(R.string.section_security))

                SettingsExpandableRow(
                    stringResource(R.string.change_password), showChangePassword,
                    { showChangePassword = !showChangePassword }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = { currentPassword = it; passwordError = null },
                            label = { Text(stringResource(R.string.current_password_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it; passwordError = null },
                            label = { Text(stringResource(R.string.new_password_label)) },
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(
                                        if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = { confirmNewPassword = it; passwordError = null },
                            label = { Text(stringResource(R.string.confirm_new_password_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            isError = passwordError != null,
                            singleLine = true
                        )
                        if (passwordError != null)
                            Text(passwordError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        if (passwordSuccess)
                            Text(stringResource(R.string.password_changed_success), color = Color(0xFF4CAF50), fontSize = 12.sp)
                        Button(
                            onClick = {
                                if (newPassword.length < 6) { passwordError = context.getString(R.string.error_password_min_length); return@Button }
                                if (newPassword != confirmNewPassword) { passwordError = context.getString(R.string.error_passwords_mismatch); return@Button }
                                scope.launch {
                                    try {
                                        val token = tokenManager.token.first() ?: return@launch
                                        RetrofitClient.apiService.changePassword(
                                            "Bearer $token",
                                            ChangePasswordRequest(currentPassword, newPassword)
                                        )
                                        passwordSuccess = true
                                        currentPassword = ""; newPassword = ""; confirmNewPassword = ""
                                    } catch (e: Exception) {
                                        passwordError = context.getString(R.string.error_change_password_failed)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) { Text(stringResource(R.string.update_password)) }
                    }
                }

                // Face ID
                SettingsInfoRow(
                    stringResource(R.string.face_id_auth),
                    if (faceIdEnabled) stringResource(R.string.face_id_enabled_subtitle) else stringResource(R.string.face_id_setup_subtitle)
                ) {
                    showFaceIdDialog = true
                }

                // NFC
                SettingsInfoRow(stringResource(R.string.nfc_card_reader), stringResource(R.string.nfc_setup_subtitle)) {
                    Toast.makeText(context, context.getString(R.string.nfc_instructions_toast), Toast.LENGTH_LONG).show()
                }
            }

            // Account
            item {
                Spacer(Modifier.height(16.dp))
                SettingsSectionHeader(stringResource(R.string.section_account))

                // Language — each option always shows its own native name, regardless of the active UI language
                Box {
                    val selectedLanguageDisplay = if (selectedLanguage == "Hindi") "हिन्दी" else "English"
                    SettingsInfoRow(stringResource(R.string.language), selectedLanguageDisplay) { showLanguageMenu = true }
                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false }
                    ) {
                        languages.forEach { lang ->
                            val displayName = if (lang == "Hindi") "हिन्दी" else "English"
                            DropdownMenuItem(
                                text = { Text(displayName) },
                                onClick = {
                                    showLanguageMenu = false
                                    selectedLanguage = lang
                                    scope.launch { tokenManager.setLanguage(lang) }
                                    val localeTag = if (lang == "Hindi") "hi" else "en"
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(localeTag)
                                    )
                                    Toast.makeText(context, context.getString(R.string.language_set_to, displayName), Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                // Data Export (fixed)
                SettingsInfoRow(stringResource(R.string.export_health_records), stringResource(R.string.export_health_records_subtitle)) {
                    scope.launch {
                        try {
                            val token = tokenManager.token.first()
                            val records = try {
                                if (token != null) RetrofitClient.apiService.getRecords("Bearer $token")
                                else emptyList()
                            } catch (_: Exception) { emptyList() }
                            val json = Gson().toJson(records)
                            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val fileName = "SecureEHR_export_$dateStr.json"

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val resolver = context.contentResolver
                                val cv = ContentValues().apply {
                                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                                    put(MediaStore.Downloads.IS_PENDING, 1)
                                }
                                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                                uri?.let {
                                    resolver.openOutputStream(it)?.use { os -> os.write(json.toByteArray()) }
                                    cv.clear(); cv.put(MediaStore.Downloads.IS_PENDING, 0)
                                    resolver.update(it, cv, null, null)
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, it)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(share, context.getString(R.string.share_health_records_chooser)))
                                }
                            } else {
                                withContext(Dispatchers.IO) {
                                    val file = java.io.File(context.getExternalFilesDir(null), fileName)
                                    file.writeText(json)
                                }
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.exported_to_toast, "${context.getExternalFilesDir(null)?.path}/$fileName"),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.export_failed_toast, e.message ?: ""), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // About
            item {
                Spacer(Modifier.height(16.dp))
                SettingsSectionHeader(stringResource(R.string.section_about))

                SettingsExpandableRow(stringResource(R.string.terms_of_service), showTerms, { showTerms = !showTerms }) {
                    Text(
                        stringResource(R.string.terms_of_service_body),
                        fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SettingsExpandableRow(stringResource(R.string.privacy_policy), showPrivacy, { showPrivacy = !showPrivacy }) {
                    Text(
                        stringResource(R.string.privacy_policy_body),
                        fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SettingsExpandableRow(stringResource(R.string.version_info), showVersion, { showVersion = !showVersion }) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        VersionRow(stringResource(R.string.version_label), "1.0.0")
                        VersionRow(stringResource(R.string.build_date_label), stringResource(R.string.build_date_value))
                        VersionRow(stringResource(R.string.min_sdk_label), "26 (Android 8.0)")
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.version_changelog_v1), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Logout
            item {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            db.medicalRecordDao().deleteAll()
                            db.consentAuditDao().deleteAll()
                            tokenManager.clearHealthWallet()
                            tokenManager.clearUserRole()
                            tokenManager.clearToken()
                            navController.navigate(Screen.Login.route) { popUpTo(0) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.logout), fontWeight = FontWeight.SemiBold)
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showFaceIdDialog) {
        FaceIdSetupDialog(
            onSuccess = {
                faceIdEnabled = true
                showFaceIdDialog = false
                Toast.makeText(context, context.getString(R.string.face_id_setup_success_toast), Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showFaceIdDialog = false }
        )
    }
}

@Composable
fun FaceIdSetupDialog(onSuccess: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var faceDetected by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) cameraError = context.getString(R.string.camera_permission_denied)
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    // Trigger success after face detected
    LaunchedEffect(faceDetected) {
        if (faceDetected) {
            delay(700)
            onSuccess()
        }
    }

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            cameraProvider = provider

            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val detector = FaceDetection.getClient(
                FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setMinFaceSize(0.15f)
                    .build()
            )

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also { ia ->
                    ia.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && !faceDetected) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            detector.process(image)
                                .addOnSuccessListener { faces ->
                                    if (faces.isNotEmpty()) faceDetected = true
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview, analysis
                )
            } catch (e: Exception) {
                cameraError = context.getString(R.string.camera_unavailable, e.message ?: "")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            executor.shutdown()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.set_up_face_id_title), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.face_id_position_instruction),
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))

                // Camera preview in circle
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(
                            3.dp,
                            if (faceDetected) Color(0xFF4CAF50) else TealPrimary,
                            CircleShape
                        )
                ) {
                    if (hasPermission) {
                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier.fillMaxSize()
                        )
                        if (faceDetected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    modifier = Modifier.size(56.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.NoPhotography, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                when {
                    faceDetected -> Text(
                        stringResource(R.string.face_recognised), color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                    )
                    cameraError != null -> Text(
                        cameraError!!, color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp, textAlign = TextAlign.Center
                    )
                    hasPermission -> {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(0.7f),
                            color = TealPrimary,
                            trackColor = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.looking_for_face), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> Text(stringResource(R.string.awaiting_camera_permission), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = TealPrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(vertical = 10.dp)
    )
}

@Composable
fun SettingsToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TealPrimary)
        )
    }
    Spacer(Modifier.height(6.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsInfoRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle.isNotBlank())
                    Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(6.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsExpandableRow(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, modifier = Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))
                content()
            }
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
fun VersionRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
