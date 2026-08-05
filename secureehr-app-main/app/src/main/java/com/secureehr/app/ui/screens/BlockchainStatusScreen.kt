package com.secureehr.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.secureehr.app.data.blockchain.BlockchainOpState
import com.secureehr.app.data.blockchain.BlockchainService
import com.secureehr.app.data.blockchain.BlockchainViewModel
import com.secureehr.app.data.blockchain.IPFSService
import com.secureehr.app.data.local.db.AppDatabase
import com.secureehr.app.data.local.db.MedicalRecordEntity
import com.secureehr.app.ui.components.BorderColorDim
import com.secureehr.app.ui.components.CardBgColor
import com.secureehr.app.ui.components.EmptyState
import com.secureehr.app.ui.components.TextSecondaryColor
import com.secureehr.app.ui.theme.CyanAccent
import com.secureehr.app.ui.theme.TealPrimary

private const val USER_SEED = "default_user"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockchainStatusScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val blockchainVm: BlockchainViewModel = viewModel()

    val chainRecords by db.medicalRecordDao().observeBlockchainRecords()
        .collectAsState(initial = emptyList())
    val opState by blockchainVm.state.collectAsState()

    val walletAddress = remember { BlockchainService.getWalletAddress(USER_SEED) }
    val ipfsCount = chainRecords.count { !it.ipfsCid.isNullOrBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blockchain Activity", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141824))
            )
        },
        containerColor = Color(0xFF0A0E1A)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Wallet card ────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0D3B35), Color(0xFF0A1A30)),
                                Offset(0f, 0f), Offset(Float.MAX_VALUE, Float.MAX_VALUE)
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .border(1.dp, TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(TealPrimary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, null, tint = TealPrimary, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Sepolia Testnet Wallet", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 15.sp)
                            Text(
                                "${walletAddress.take(10)}…${walletAddress.takeLast(6)}",
                                fontSize = 12.sp, color = TealPrimary, fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://sepolia.etherscan.io/address/$walletAddress"))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.OpenInNew, null, tint = TextSecondaryColor, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BlockchainStatChip("Records Secured", chainRecords.size.toString(), Icons.Default.Shield, Modifier.weight(1f))
                        BlockchainStatChip("IPFS Docs", ipfsCount.toString(), Icons.Default.Cloud, Modifier.weight(1f))
                        BlockchainStatChip("Network", "Sepolia", Icons.Default.Hub, Modifier.weight(1f))
                    }
                }
            }

            // ── Op state banner ────────────────────────────────────────────
            if (opState is BlockchainOpState.Loading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TealPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .border(1.dp, TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text((opState as BlockchainOpState.Loading).message, color = TealPrimary, fontSize = 13.sp)
                    }
                }
            }

            // ── Records list ───────────────────────────────────────────────
            if (chainRecords.isEmpty()) {
                item {
                    EmptyState(
                        title = "No Records Secured",
                        subtitle = "Records you secure from Medical Records will appear here",
                        icon = Icons.Default.Shield
                    )
                }
            } else {
                item {
                    Text(
                        "SECURED RECORDS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondaryColor,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
                items(chainRecords, key = { it.id }) { record ->
                    BlockchainRecordCard(record = record, blockchainVm = blockchainVm)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun BlockchainStatChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CardBgColor, RoundedCornerShape(12.dp))
            .border(1.dp, BorderColorDim, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = TealPrimary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 9.sp, color = TextSecondaryColor)
    }
}

@Composable
private fun BlockchainRecordCard(record: MedicalRecordEntity, blockchainVm: BlockchainViewModel) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var verifyState by remember { mutableStateOf<String?>(null) }    // null | "verifying" | "true" | "false"
    var txStatus by remember { mutableStateOf(record.chainTimestamp?.let { "Confirmed" } ?: "Pending") }

    // Auto-refresh tx status once
    LaunchedEffect(record.txHash) {
        if (!record.txHash.isNullOrBlank()) {
            blockchainVm.refreshTxStatus(record.txHash) { txStatus = it }
        }
    }

    val statusColor = when (txStatus) {
        "Confirmed" -> Color(0xFF4CAF50)
        "Failed" -> MaterialTheme.colorScheme.error
        else -> Color(0xFFFF9800)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBgColor, RoundedCornerShape(16.dp))
            .border(1.dp, TealPrimary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Shield badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(TealPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, null, tint = TealPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(record.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                if (!record.txHash.isNullOrBlank()) {
                    Text(
                        "TX: ${record.txHash.take(10)}…${record.txHash.takeLast(6)}",
                        fontSize = 11.sp, color = TextSecondaryColor,
                        overflow = TextOverflow.Ellipsis, maxLines = 1
                    )
                }
                if (!record.chainTimestamp.isNullOrBlank()) {
                    Text(record.chainTimestamp, fontSize = 10.sp, color = TextSecondaryColor)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(txStatus, fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = TextSecondaryColor, modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring()) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
        ) {
            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                HorizontalDivider(color = BorderColorDim, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))

                if (!record.recordHash.isNullOrBlank()) {
                    ChainDetailRow("Content Hash", "${record.recordHash.take(12)}…${record.recordHash.takeLast(8)}")
                }
                if (!record.txHash.isNullOrBlank()) {
                    ChainDetailRow("TX Hash", "${record.txHash.take(12)}…${record.txHash.takeLast(8)}")
                }
                if (!record.ipfsCid.isNullOrBlank()) {
                    ChainDetailRow("IPFS CID", record.ipfsCid)
                }

                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Etherscan button
                    if (!record.txHash.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                val url = "https://sepolia.etherscan.io/tx/${record.txHash}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Etherscan", fontSize = 12.sp)
                        }
                    }
                    // IPFS button
                    if (!record.ipfsCid.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                val url = IPFSService.getIPFSUrl(record.ipfsCid)
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Cloud, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("IPFS", fontSize = 12.sp)
                        }
                    }
                    // Verify button
                    if (!record.recordHash.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                verifyState = "verifying"
                                blockchainVm.verifyRecord(record.recordHash) {
                                    verifyState = it.toString()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (verifyState == "true") Color(0xFF4CAF50) else TextSecondaryColor
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (verifyState == "true") Color(0xFF4CAF50).copy(0.5f) else BorderColorDim
                            )
                        ) {
                            if (verifyState == "verifying") {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = TealPrimary)
                            } else {
                                Icon(
                                    if (verifyState == "true") Icons.Default.Verified else Icons.Default.VerifiedUser,
                                    null, modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                when (verifyState) {
                                    "verifying" -> "…"
                                    "true" -> "Verified"
                                    "false" -> "Not found"
                                    else -> "Verify"
                                },
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChainDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label: ", fontSize = 11.sp, color = TextSecondaryColor, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(90.dp))
        Text(value, fontSize = 11.sp, color = Color.White, modifier = Modifier.weight(1f),
            overflow = TextOverflow.Ellipsis, maxLines = 1)
    }
}
