package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldAccent
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CashDenominationDialog(
    onDismiss: () -> Unit,
    onApplyCountedTotal: ((Long) -> Unit)? = null
) {
    // Banknotes and Coins in FCFA (BCEAO / UEMOA)
    val denominations = remember {
        listOf(
            10000L to "Billet 10 000 F",
            5000L to "Billet 5 000 F",
            2000L to "Billet 2 000 F",
            1000L to "Billet 1 000 F",
            500L to "Billet 500 F",
            500L to "Pièce 500 F",
            250L to "Pièce 250 F",
            200L to "Pièce 200 F",
            100L to "Pièce 100 F",
            50L to "Pièce 50 F",
            25L to "Pièce 25 F"
        )
    }

    val counts = remember { mutableStateMapOf<Int, Int>() }
    val formatter = NumberFormat.getNumberInstance(Locale.FRENCH)

    val grandTotal = remember(counts.toMap()) {
        denominations.indices.sumOf { index ->
            val value = denominations[index].first
            val qty = counts[index] ?: 0
            value * qty
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("cash_denomination_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldSecondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PriceCheck,
                                contentDescription = null,
                                tint = EmeraldSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Calculateur de Billetage",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Comptage physique des espèces",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { counts.clear() }) {
                            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Réinitialiser", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }
                }

                // Total Bar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EmeraldSecondary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, EmeraldSecondary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Compté en Espèces", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${formatter.format(grandTotal)} FCFA",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = EmeraldSecondary
                            )
                        }
                        Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, tint = EmeraldSecondary, modifier = Modifier.size(32.dp))
                    }
                }

                // Denomination List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(denominations.indices.toList()) { index ->
                        val (denomValue, denomLabel) = denominations[index]
                        val count = counts[index] ?: 0
                        val subTotal = denomValue * count

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(denomLabel, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        "${formatter.format(subTotal)} F",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (subTotal > 0) EmeraldSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilledIconButton(
                                        onClick = {
                                            if (count > 0) counts[index] = count - 1
                                        },
                                        modifier = Modifier.size(30.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Diminuer", modifier = Modifier.size(16.dp))
                                    }

                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.widthIn(min = 28.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )

                                    FilledIconButton(
                                        onClick = {
                                            counts[index] = count + 1
                                        },
                                        modifier = Modifier.size(30.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Ajouter", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Action
                if (onApplyCountedTotal != null) {
                    Button(
                        onClick = {
                            onApplyCountedTotal(grandTotal)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSecondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Transférer au Bilan (${formatter.format(grandTotal)} F)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
