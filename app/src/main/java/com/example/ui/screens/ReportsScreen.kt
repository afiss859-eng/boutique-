package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashFloat
import com.example.data.model.PaymentProvider
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WaveBlue
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    cashFloat: CashFloat,
    transactions: List<TransactionEntity>,
    onUpdateCashFloat: (cash: Long, om: Long, moov: Long, wave: Long) -> Unit
) {
    val context = LocalContext.current
    val fcfa = remember { NumberFormat.getNumberInstance(Locale.FRENCH) }
    var showClosingWizard by remember { mutableStateOf(false) }

    // Physical Cash Count Inputs for Closing Wizard
    var physicalCashInput by remember { mutableStateOf(cashFloat.cashOnHand.toString()) }
    var omRealInput by remember { mutableStateOf(cashFloat.orangeMoneyBalance.toString()) }
    var moovRealInput by remember { mutableStateOf(cashFloat.moovMoneyBalance.toString()) }
    var waveRealInput by remember { mutableStateOf(cashFloat.waveBalance.toString()) }
    var closingDone by remember { mutableStateOf(false) }

    val totalDeposits = remember(transactions) {
        transactions.filter { it.type == TransactionType.DEPOT }.sumOf { it.amount }
    }
    val totalWithdrawals = remember(transactions) {
        transactions.filter { it.type == TransactionType.RETRAIT }.sumOf { it.amount }
    }
    val totalStoreSales = remember(transactions) {
        transactions.filter { it.type == TransactionType.VENTE_PRODUIT }.sumOf { it.amount }
    }
    val totalCommissions = remember(transactions) {
        transactions.sumOf { it.commission }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Header Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Bilan Financier & Clôture",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Rapport généré le ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                val reportBody = buildString {
                                    appendLine("📊 *RAPPORT JOURNALIER D'ACTIVITÉ - WEND-LAMITA*")
                                    appendLine("Date : ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
                                    appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                    appendLine("• Total Dépôts OM/Moov : ${fcfa.format(totalDeposits)} FCFA")
                                    appendLine("• Total Retraits : ${fcfa.format(totalWithdrawals)} FCFA")
                                    appendLine("• Ventes Boutique : ${fcfa.format(totalStoreSales)} FCFA")
                                    appendLine("• Commissions Nettes : +${fcfa.format(totalCommissions)} FCFA")
                                    appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                    appendLine("• Solde Espèces : ${fcfa.format(cashFloat.cashOnHand)} FCFA")
                                    appendLine("• Solde Orange Money : ${fcfa.format(cashFloat.orangeMoneyBalance)} FCFA")
                                    appendLine("• Solde Moov Money : ${fcfa.format(cashFloat.moovMoneyBalance)} FCFA")
                                    appendLine("• Solde Wave : ${fcfa.format(cashFloat.waveBalance)} FCFA")
                                    appendLine("• ACTIFS TOTAUX : ${fcfa.format(cashFloat.totalAssets)} FCFA")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, reportBody)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Partager le Bilan Journalier"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            modifier = Modifier.testTag("export_report_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Partager")
                        }
                    }
                }
            }
        }

        // Financial KPI Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReportMetricCard(
                    title = "Dépôts Mobile",
                    amount = "${fcfa.format(totalDeposits)} F",
                    icon = Icons.Default.ArrowDownward,
                    color = OrangePrimary,
                    modifier = Modifier.weight(1f)
                )
                ReportMetricCard(
                    title = "Retraits Espèces",
                    amount = "${fcfa.format(totalWithdrawals)} F",
                    icon = Icons.Default.ArrowUpward,
                    color = EmeraldSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReportMetricCard(
                    title = "Ventes Boutique",
                    amount = "${fcfa.format(totalStoreSales)} F",
                    icon = Icons.Default.Storefront,
                    color = GoldAccent,
                    modifier = Modifier.weight(1f)
                )
                ReportMetricCard(
                    title = "Bénéfice Net",
                    amount = "+${fcfa.format(totalCommissions)} F",
                    icon = Icons.Default.AttachMoney,
                    color = EmeraldSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Clôture de Caisse Wizard Section
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.LockClock, contentDescription = null, tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Clôture de Caisse Quotidienne",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        IconButton(onClick = { showClosingWizard = !showClosingWizard }) {
                            Icon(
                                imageVector = if (showClosingWizard) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }

                    AnimatedVisibility(visible = showClosingWizard) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Saisissez les montants physiques et réels comptés en fin de journée pour détecter d'éventuels écarts :",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = physicalCashInput,
                                onValueChange = { physicalCashInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Espèces Physiques en Caisse (FCFA)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = omRealInput,
                                    onValueChange = { omRealInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Solde Orange Money") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = moovRealInput,
                                    onValueChange = { moovRealInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Solde Moov Money") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = waveRealInput,
                                onValueChange = { waveRealInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Solde Wave (FCFA)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val cash = physicalCashInput.toLongOrNull() ?: cashFloat.cashOnHand
                                    val om = omRealInput.toLongOrNull() ?: cashFloat.orangeMoneyBalance
                                    val moov = moovRealInput.toLongOrNull() ?: cashFloat.moovMoneyBalance
                                    val wave = waveRealInput.toLongOrNull() ?: cashFloat.waveBalance
                                    onUpdateCashFloat(cash, om, moov, wave)
                                    closingDone = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSecondary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_closing_btn")
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Valider la Clôture et Mettre à Jour les Soldes")
                            }

                            if (closingDone) {
                                Text(
                                    text = "✅ Clôture de caisse enregistrée avec succès !",
                                    color = EmeraldSecondary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportMetricCard(
    title: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = amount, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
        }
    }
}
