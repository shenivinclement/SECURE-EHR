package com.secureehr.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.secureehr.app.data.api.ChatRequest
import com.secureehr.app.data.api.RetrofitClient
import com.secureehr.app.data.local.TokenManager
import com.secureehr.app.ui.components.BorderColorDim
import com.secureehr.app.ui.components.CardBgColor
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.CyanAccent
import com.secureehr.app.ui.theme.TealPrimary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val listState = rememberLazyListState()

    var messageText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf(
            ChatMessage("Hello! I'm your SecureEHR AI assistant. How can I help you today?", false)
        )
    }

    fun sendMessage() {
        val text = messageText.trim()
        if (text.isBlank()) return
        messages.add(ChatMessage(text, true))
        messageText = ""
        isTyping = true
        scope.launch {
            try { listState.animateScrollToItem(messages.size - 1) } catch (_: Exception) {}
            try {
                val token = tokenManager.token.first() ?: ""
                val response = RetrofitClient.apiService.chat("Bearer $token", ChatRequest(text))
                isTyping = false
                messages.add(ChatMessage(response.response.ifBlank { "I received your message. How can I assist further?" }, false))
            } catch (e: Exception) {
                isTyping = false
                messages.add(ChatMessage("I'm having trouble connecting right now. Please try again later.", false))
            }
            try { listState.animateScrollToItem(messages.size - 1) } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(TealPrimary, CyanAccent),
                                        Offset(0f, 0f), Offset(32f, 32f)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("AI Health Assistant", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            Text("Powered by SecureEHR", fontSize = 10.sp, color = TextSecondaryColor)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141824))
            )
        },
        containerColor = Color(0xFF0A0E1A)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }
                if (isTyping) {
                    item { TypingIndicator() }
                }
            }

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141824))
                    .border(width = 0.5.dp, color = BorderColorDim, shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about your health…", color = TextSecondaryColor, fontSize = 14.sp) },
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBgColor,
                        unfocusedContainerColor = CardBgColor,
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = BorderColorDim,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (messageText.isNotBlank() && !isTyping)
                                Brush.linearGradient(listOf(TealPrimary, CyanAccent), Offset(0f, 0f), Offset(44f, 44f))
                            else
                                Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF37474F))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { sendMessage() },
                        enabled = messageText.isNotBlank() && !isTyping,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        Brush.linearGradient(listOf(TealPrimary, CyanAccent), Offset(0f, 0f), Offset(28f, 28f)),
                        CircleShape
                    )
                    .align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier.widthIn(max = 280.dp),
            contentAlignment = alignment
        ) {
            Box(
                modifier = Modifier
                    .background(
                        if (message.isUser)
                            Brush.linearGradient(listOf(TealPrimary, CyanAccent.copy(alpha = 0.8f)))
                        else
                            Brush.linearGradient(listOf(CardBgColor, CardBgColor)),
                        RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = if (message.isUser) 16.dp else 4.dp,
                            bottomEnd = if (message.isUser) 4.dp else 16.dp
                        )
                    )
                    .border(
                        if (!message.isUser) 1.dp else 0.dp,
                        if (!message.isUser) BorderColorDim else Color.Transparent,
                        RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = if (message.isUser) 16.dp else 4.dp,
                            bottomEnd = if (message.isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        if (message.isUser) Spacer(Modifier.width(8.dp))
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    Brush.linearGradient(listOf(TealPrimary, CyanAccent), Offset(0f, 0f), Offset(28f, 28f)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SmartToy, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .background(CardBgColor, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .border(1.dp, BorderColorDim, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { index ->
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500, delayMillis = index * 150),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(TealPrimary.copy(alpha = dotAlpha), CircleShape)
                    )
                }
            }
        }
    }
}
