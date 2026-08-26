package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CashFloat
import com.example.data.model.PaymentProvider
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.CashDenominationDialog
import com.example.ui.components.FeeSimulatorDialog
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WaveBlue
import com.example.ui.viewmodel.AppTab
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    cashFloat: CashFloat,
    transactions: List<TransactionEntity>,
    lowStockCount: Int,
    cniCount: Int,
    pendingDebtsCount: Int = 0,
    totalDebtAmount: Long = 0L,
    onNavigate: (AppTab) -> Unit,
    onViewTransaction: (TransactionEntity) -> Unit
) {
    var isBalanceVisible by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf<PaymentProvider?>(null) }
    var showFeeSimulator by remember { mutableStateOf(false) }
    var showCashDenomination by remember { mutableStateOf(false) }
    val fcfa = remember { NumberFormat.getNumberInstance(Locale.FRENCH) }

    val filteredTransactions = remember(transactions, selectedFilter) {
        if (selectedFilter == null) transactions else transactions.filter { it.provider == selectedFilter }
    }

    val totalVolumeToday = remember(transactions) {
        transactions.sumOf { it.amount }
    }
    val totalCommissionsToday = remember(transactions) {
        transactions.sumOf { it.commission }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card with Store Banner and Total Balance
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Hero Image Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.hero_wend_lamita),
                            contentDescription = "Bannière Wend-Lamita",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Agence Principale • Ouagadougou",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Total Assets Breakdown
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Fonds de Roulement Total",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isBalanceVisible) "${fcfa.format(cashFloat.totalAssets)} FCFA" else "•••••••• FCFA",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            IconButton(onClick = { isBalanceVisible = !isBalanceVisible }) {
                                Icon(
                                    imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Afficher/Masquer solde",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Individual Wallets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WalletItem(
                                title = "Espèces",
                                amount = if (isBalanceVisible) "${fcfa.format(cashFloat.cashOnHand)} F" else "•••",
                                color = GoldAccent,
                                modifier = Modifier.weight(1f)
                            )
                            WalletItem(
                                title = "Orange M.",
                                amount = if (isBalanceVisible) "${fcfa.format(cashFloat.orangeMoneyBalance)} F" else "•••",
                                color = OrangePrimary,
                                modifier = Modifier.weight(1f)
                            )
                            WalletItem(
                                title = "Moov M.",
                                amount = if (isBalanceVisible) "${fcfa.format(cashFloat.moovMoneyBalance)} F" else "•••",
                                color = EmeraldSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            WalletItem(
                                title = "Wave",
                                amount = if (isBalanceVisible) "${fcfa.format(cashFloat.waveBalance)} F" else "•••",
                                color = WaveBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Text(
                text = "Opérations Rapides",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.CallMade,
                    label = "Dépôt OM",
                    bgColor = OrangePrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppTab.MOBILE_MONEY) }
                )
                QuickActionButton(
                    icon = Icons.Default.CallReceived,
                    label = "Retrait OM",
                    bgColor = EmeraldSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppTab.MOBILE_MONEY) }
                )
                QuickActionButton(
                    icon = Icons.Default.QrCodeScanner,
                    label = "Scanner CNI",
                    bgColor = WaveBlue,
                    badge = "$cniCount",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppTab.CNI_REGISTRY) }
                )
                QuickActionButton(
                    icon = Icons.Default.ShoppingCart,
                    label = "Boutique",
                    bgColor = GoldAccent,
                    badge = if (lowStockCount > 0) "!$lowStockCount" else null,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppTab.BOUTIQUE_POS) }
                )
            }
        }

        // Secondary Utility Tools Row (Fee Simulator, Cash Denomination, Debts Book)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.3f)),
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(AppTab.DEBTS_BOOK) }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CrimsonRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Book, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Dettes", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = if (pendingDebtsCount > 0) "$pendingDebtsCount en cours" else "0 dette",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                            color = if (pendingDebtsCount > 0) CrimsonRed else EmeraldSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f)),
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showFeeSimulator = true }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GoldAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Calculate, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Frais", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        Text("Simulateur", style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, EmeraldSecondary.copy(alpha = 0.3f)),
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showCashDenomination = true }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(EmeraldSecondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.PriceCheck, contentDescription = null, tint = EmeraldSecondary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Billetage", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        Text("Comptage", style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Daily Financial Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Volume Total",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${fcfa.format(totalVolumeToday)} FCFA",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = EmeraldSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Commissions",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "+${fcfa.format(totalCommissionsToday)} FCFA",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSecondary
                            )
                        )
                    }
                }
            }
        }

        // Recent Transactions Section Header with Filters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dernières Activités (${filteredTransactions.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                TextButton(onClick = { onNavigate(AppTab.REPORTS) }) {
                    Text("Voir tout", color = OrangePrimary)
                }
            }

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("Tous") }
                )
                FilterChip(
                    selected = selectedFilter == PaymentProvider.ORANGE_MONEY,
                    onClick = { selectedFilter = PaymentProvider.ORANGE_MONEY },
                    label = { Text("Orange") }
                )
                FilterChip(
                    selected = selectedFilter == PaymentProvider.MOOV_MONEY,
                    onClick = { selectedFilter = PaymentProvider.MOOV_MONEY },
                    label = { Text("Moov") }
                )
                FilterChip(
                    selected = selectedFilter == PaymentProvider.ESPECES,
                    onClick = { selectedFilter = PaymentProvider.ESPECES },
                    label = { Text("Boutique") }
                )
            }
        }

        // Transaction List Items
        if (filteredTransactions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aucune transaction enregistrée pour ce filtre",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredTransactions.take(8), key = { it.id }) { tx ->
                TransactionRowItem(transaction = tx, onClick = { onViewTransaction(tx) })
            }
        }
    }

    if (showFeeSimulator) {
        FeeSimulatorDialog(
            onDismiss = { showFeeSimulator = false },
            onApplyToTransaction = { amount, type, provider ->
                onNavigate(AppTab.MOBILE_MONEY)
            }
        )
    }

    if (showCashDenomination) {
        CashDenominationDialog(
            onDismiss = { showCashDenomination = false }
        )
    }
}

@Composable
fun WalletItem(title: String, amount: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = color,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bgColor: Color,
    badge: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("action_${label.lowercase().replace(' ', '_')}")
    ) {
        Box(modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(bgColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = bgColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }

            if (badge != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: TransactionEntity,
    onClick: () -> Unit
) {
    val fcfa = remember { NumberFormat.getNumberInstance(Locale.FRENCH) }
    val timeStr = remember(transaction.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(transaction.timestamp))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("tx_item_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when (transaction.provider) {
                                PaymentProvider.ORANGE_MONEY -> OrangePrimary.copy(alpha = 0.15f)
                                PaymentProvider.MOOV_MONEY -> EmeraldSecondary.copy(alpha = 0.15f)
                                PaymentProvider.WAVE -> WaveBlue.copy(alpha = 0.15f)
                                PaymentProvider.ESPECES -> GoldAccent.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (transaction.type) {
                            TransactionType.DEPOT -> Icons.Default.ArrowDownward
                            TransactionType.RETRAIT -> Icons.Default.ArrowUpward
                            TransactionType.TRANSFERT -> Icons.Default.SwapHoriz
                            TransactionType.ACHAT_CREDIT -> Icons.Default.PhoneAndroid
                            TransactionType.VENTE_PRODUIT -> Icons.Default.ShoppingBag
                        },
                        contentDescription = null,
                        tint = when (transaction.provider) {
                            PaymentProvider.ORANGE_MONEY -> OrangePrimary
                            PaymentProvider.MOOV_MONEY -> EmeraldSecondary
                            PaymentProvider.WAVE -> WaveBlue
                            PaymentProvider.ESPECES -> GoldAccent
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${transaction.type.label} • ${transaction.provider.label}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (transaction.clientName.isNotBlank()) "${transaction.clientName} • $timeStr" else timeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (transaction.type == TransactionType.RETRAIT) "-" else "+"}${fcfa.format(transaction.amount)} F",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (transaction.type == TransactionType.RETRAIT) OrangePrimary else EmeraldSecondary
                    )
                )
                if (transaction.commission > 0) {
                    Text(
                        text = "+${fcfa.format(transaction.commission)} F comm.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = EmeraldSecondary
                    )
                }
            }
        }
    }
}
