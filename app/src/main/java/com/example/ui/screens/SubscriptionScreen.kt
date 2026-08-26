package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentProvider
import com.example.data.model.SubscriptionEntity
import com.example.data.model.SubscriptionPlan
import com.example.service.tr
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MoovGreen
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WaveBlue
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    activeSubscription: SubscriptionEntity?,
    allSubscriptions: List<SubscriptionEntity>,
    onActivateSubscription: (SubscriptionPlan, String, PaymentProvider, Long, Int, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { NumberFormat.getNumberInstance(Locale.FRENCH) }

    var selectedPlan by remember { mutableStateOf(SubscriptionPlan.PRO_KIOSQUE) }
    var selectedDurationMonths by remember { mutableStateOf(1) } // 1, 3, or 12
    var selectedOperator by remember { mutableStateOf(PaymentProvider.ORANGE_MONEY) }
    var transactionIdInput by remember { mutableStateOf("") }
    var showActivationSuccessDialog by remember { mutableStateOf(false) }

    val calculatedPrice = remember(selectedPlan, selectedDurationMonths) {
        when (selectedPlan) {
            SubscriptionPlan.FREE -> 0L
            SubscriptionPlan.PRO_KIOSQUE -> when (selectedDurationMonths) {
                1 -> 3500L
                3 -> 9000L
                else -> 30000L
            }
            SubscriptionPlan.ENTERPRISE -> when (selectedDurationMonths) {
                1 -> 7500L
                3 -> 20000L
                else -> 70000L
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Active Subscription Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_subscription_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(
                    1.5.dp,
                    Brush.horizontalGradient(listOf(OrangePrimary, GoldAccent))
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(GoldAccent, OrangePrimary))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "Premium Badge",
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = activeSubscription?.plan?.title ?: "Forfait Pro Kiosque",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (activeSubscription != null && !activeSubscription.isExpired) {
                                        "Actif jusqu'au ${SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(Date(activeSubscription.expiryDate))}"
                                    } else {
                                        "Licence Kiosque Vérifiée"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldSecondary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, EmeraldSecondary)
                        ) {
                            Text(
                                text = tr("active_badge"),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = EmeraldSecondary,
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "N° Transaction Validée",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = activeSubscription?.transactionId ?: "OM260826.0945.A18",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Jours Restants",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = "${activeSubscription?.daysRemaining ?: 28} jours",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = GoldAccent
                                )
                            )
                        }
                    }
                }
            }
        }

        // Plans Comparison
        item {
            Text(
                text = "Choisir votre formule Wend-Lamita",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pro Kiosque Plan Card
                val isProSelected = selectedPlan == SubscriptionPlan.PRO_KIOSQUE
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedPlan = SubscriptionPlan.PRO_KIOSQUE },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProSelected) OrangePrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        if (isProSelected) 2.dp else 1.dp,
                        if (isProSelected) OrangePrimary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "PRO KIOSQUE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = OrangePrimary
                                )
                            )
                            if (isProSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "3 500 F / mois",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "• OCR CNI Illimité", fontSize = 11.sp)
                        Text(text = "• Bilan Z & Export", fontSize = 11.sp)
                        Text(text = "• Carnet Dettes WhatsApp", fontSize = 11.sp)
                        Text(text = "• Calculateur Frais Officiel", fontSize = 11.sp)
                    }
                }

                // Enterprise VIP Plan Card
                val isEnterpriseSelected = selectedPlan == SubscriptionPlan.ENTERPRISE
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedPlan = SubscriptionPlan.ENTERPRISE },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEnterpriseSelected) GoldAccent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        if (isEnterpriseSelected) 2.dp else 1.dp,
                        if (isEnterpriseSelected) GoldAccent else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ENTREPRISE VIP",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = GoldAccent
                                )
                            )
                            if (isEnterpriseSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "7 500 F / mois",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "• Tout le pack Pro", fontSize = 11.sp)
                        Text(text = "• Multi-Caissiers illimité", fontSize = 11.sp)
                        Text(text = "• Assistant IA Dédié", fontSize = 11.sp)
                        Text(text = "• Sauvegarde Cloud VIP", fontSize = 11.sp)
                    }
                }
            }
        }

        // Duration Selection
        item {
            Text(
                text = "Durée de l'abonnement :",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1 to "1 Mois", 3 to "3 Mois (-15%)", 12 to "1 An (-30%)").forEach { (months, label) ->
                    val isSelected = selectedDurationMonths == months
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedDurationMonths = months },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        // Real Activation by Transaction ID Form
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activation_by_tx_form"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr("activate_by_tx"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "1. Transférez ${formatter.format(calculatedPrice)} FCFA vers notre compte marchand :",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    // Operator Selector with official number +226 56060976
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            PaymentProvider.ORANGE_MONEY to ("Orange Money\n+226 56060976" to OrangePrimary),
                            PaymentProvider.MOOV_MONEY to ("Moov Money\n+226 56060976" to MoovGreen),
                            PaymentProvider.WAVE to ("Wave\n+226 56060976" to WaveBlue)
                        ).forEach { (prov, info) ->
                            val isSelected = selectedOperator == prov
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedOperator = prov },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) info.second.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) info.second else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Text(
                                    text = info.first,
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) info.second else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "2. Entrez l'ID / Référence de transaction reçu par SMS :",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = transactionIdInput,
                        onValueChange = { transactionIdInput = it.uppercase() },
                        placeholder = { Text(tr("tx_id_hint")) },
                        leadingIcon = {
                            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = OrangePrimary)
                        },
                        trailingIcon = {
                            if (transactionIdInput.isNotEmpty()) {
                                IconButton(onClick = { transactionIdInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_transaction_id")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (transactionIdInput.isBlank()) {
                                Toast.makeText(context, "Veuillez saisir le N° de transaction", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val durationDays = when (selectedDurationMonths) {
                                1 -> 30
                                3 -> 90
                                else -> 365
                            }
                            onActivateSubscription(
                                selectedPlan,
                                transactionIdInput,
                                selectedOperator,
                                calculatedPrice,
                                durationDays
                            ) {
                                showActivationSuccessDialog = true
                                transactionIdInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_validate_subscription")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null)
                            Text(
                                text = "Vérifier & Activer la Licence",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Subscriptions History
        if (allSubscriptions.isNotEmpty()) {
            item {
                Text(
                    text = "Historique des Activations de Licence",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(allSubscriptions) { sub ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${sub.plan.title} (${sub.operator.label})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Réf: ${sub.transactionId}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "Activé le ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(Date(sub.activationDate))}",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${formatter.format(sub.amountPaid)} F",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSecondary
                                )
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (sub.isExpired) MaterialTheme.colorScheme.errorContainer else EmeraldSecondary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (sub.isExpired) "Expiré" else "Actif",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (sub.isExpired) MaterialTheme.colorScheme.error else EmeraldSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Success Dialog
    if (showActivationSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showActivationSuccessDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Succès",
                    tint = EmeraldSecondary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Abonnement Activé avec Succès !",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Votre forfait ${selectedPlan.title} est maintenant actif et vérifié. Toutes les fonctionnalités premium et la conformité sont débloquées pour votre kiosque.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showActivationSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Parfait, continuer")
                }
            }
        )
    }
}
