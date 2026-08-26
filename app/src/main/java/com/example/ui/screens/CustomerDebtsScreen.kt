package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.CustomerDebtEntity
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.OrangePrimary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerDebtsScreen(
    debts: List<CustomerDebtEntity>,
    onSaveDebt: (CustomerDebtEntity) -> Unit,
    onMakePayment: (debt: CustomerDebtEntity, amount: Long) -> Unit,
    onDeleteDebt: (CustomerDebtEntity) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDebtForPayment by remember { mutableStateOf<CustomerDebtEntity?>(null) }
    var filterStatus by remember { mutableStateOf("ALL") } // ALL, PENDING, SETTLED

    val formatter = NumberFormat.getNumberInstance(Locale.FRENCH)

    val filteredDebts = remember(debts, searchQuery, filterStatus) {
        debts.filter { d ->
            val matchesQuery = searchQuery.isBlank() ||
                    d.customerName.contains(searchQuery, ignoreCase = true) ||
                    d.customerPhone.contains(searchQuery) ||
                    d.description.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (filterStatus) {
                "PENDING" -> !d.isSettled
                "SETTLED" -> d.isSettled
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    val totalOutstanding = remember(debts) {
        debts.filter { !it.isSettled }.sumOf { it.remainingAmount }
    }
    val pendingCount = remember(debts) { debts.count { !it.isSettled } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_debts_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Outstanding Total Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CrimsonRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    tint = CrimsonRed,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Cahier de Dettes & Crédits",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$pendingCount créances en attente",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Add Debt Button
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                            modifier = Modifier.testTag("add_debt_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nouveau Crédit", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CrimsonRed.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.25f)),
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
                                Text("Total Créances Impayées", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${formatter.format(totalOutstanding)} FCFA",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = CrimsonRed
                                )
                            }
                            Icon(imageVector = Icons.Default.PendingActions, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }

        // Filter and Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher client, téléphone, motif...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Effacer")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filterStatus == "ALL",
                        onClick = { filterStatus = "ALL" },
                        label = { Text("Toutes (${debts.size})") }
                    )
                    FilterChip(
                        selected = filterStatus == "PENDING",
                        onClick = { filterStatus = "PENDING" },
                        label = { Text("En cours ($pendingCount)") }
                    )
                    FilterChip(
                        selected = filterStatus == "SETTLED",
                        onClick = { filterStatus = "SETTLED" },
                        label = { Text("Réglées (${debts.size - pendingCount})") }
                    )
                }
            }
        }

        // Debt Items List
        if (filteredDebts.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "Aucune dette enregistrée." else "Aucun résultat trouvé.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredDebts, key = { it.id }) { debt ->
                DebtItemCard(
                    debt = debt,
                    onPay = { selectedDebtForPayment = debt },
                    onDelete = { onDeleteDebt(debt) },
                    onSendReminder = {
                        val message = "Bonjour ${debt.customerName}, nous vous rappelons amicalement votre crédit de ${formatter.format(debt.remainingAmount)} FCFA chez Wend-Lamita Services. Merci de passer régler dès que possible."
                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=226${debt.customerPhone}&text=${Uri.encode(message)}")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to SMS
                            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:${debt.customerPhone}")
                                putExtra("sms_body", message)
                            }
                            context.startActivity(smsIntent)
                        }
                    }
                )
            }
        }
    }

    // Add Debt Dialog
    if (showAddDialog) {
        AddDebtDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newDebt ->
                onSaveDebt(newDebt)
                showAddDialog = false
            }
        )
    }

    // Make Payment Dialog
    if (selectedDebtForPayment != null) {
        val currentDebt = selectedDebtForPayment!!
        PaymentDialog(
            debt = currentDebt,
            onDismiss = { selectedDebtForPayment = null },
            onConfirmPayment = { amt ->
                onMakePayment(currentDebt, amt)
                selectedDebtForPayment = null
            }
        )
    }
}

@Composable
fun DebtItemCard(
    debt: CustomerDebtEntity,
    onPay: () -> Unit,
    onDelete: () -> Unit,
    onSendReminder: () -> Unit
) {
    val formatter = NumberFormat.getNumberInstance(Locale.FRENCH)
    val dateStr = remember(debt.dateCreated) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(debt.dateCreated))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (debt.isSettled) EmeraldSecondary.copy(alpha = 0.4f) else CrimsonRed.copy(alpha = 0.3f)
        ),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debt.customerName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (debt.customerPhone.isNotBlank()) {
                        Text(
                            text = "Tél: ${debt.customerPhone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (debt.isSettled) EmeraldSecondary.copy(alpha = 0.15f) else CrimsonRed.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (debt.isSettled) "RÉGLÉE" else "IMPAYÉE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (debt.isSettled) EmeraldSecondary else CrimsonRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (debt.description.isNotBlank()) {
                Text(
                    text = "Motif : ${debt.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Montant initial : ${formatter.format(debt.totalAmount)} F", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (debt.paidAmount > 0) {
                        Text("Déjà payé : ${formatter.format(debt.paidAmount)} F", style = MaterialTheme.typography.labelSmall, color = EmeraldSecondary)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Reste à payer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${formatter.format(debt.remainingAmount)} FCFA",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (debt.isSettled) EmeraldSecondary else CrimsonRed
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date : $dateStr ${if (debt.dueDate.isNotBlank()) "• Échéance: ${debt.dueDate}" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!debt.isSettled && debt.customerPhone.isNotBlank()) {
                        IconButton(onClick = onSendReminder, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.Chat, contentDescription = "Rappel WhatsApp/SMS", tint = EmeraldSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    if (!debt.isSettled) {
                        Button(
                            onClick = onPay,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSecondary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Encaisser", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddDebtDialog(
    onDismiss: () -> Unit,
    onConfirm: (CustomerDebtEntity) -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("Fin de mois") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Accorder un Crédit Client",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (errorMessage != null) {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Nom du client *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Numéro Téléphone (8 chiffres)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Montant du crédit (FCFA) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Articles / Motif (ex: 2 packs eau + 1 carte OM)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Date ou Délai d'échéance") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Annuler")
                    }

                    Button(
                        onClick = {
                            val amt = amountInput.toLongOrNull() ?: 0L
                            if (customerName.isBlank()) {
                                errorMessage = "Veuillez entrer le nom du client."
                                return@Button
                            }
                            if (amt <= 0) {
                                errorMessage = "Veuillez saisir un montant supérieur à 0."
                                return@Button
                            }

                            onConfirm(
                                CustomerDebtEntity(
                                    customerName = customerName.trim(),
                                    customerPhone = customerPhone.trim(),
                                    totalAmount = amt,
                                    paidAmount = 0,
                                    description = description.trim(),
                                    dueDate = dueDate.trim(),
                                    isSettled = false
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Enregistrer", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentDialog(
    debt: CustomerDebtEntity,
    onDismiss: () -> Unit,
    onConfirmPayment: (Long) -> Unit
) {
    var amountInput by remember { mutableStateOf(debt.remainingAmount.toString()) }
    val formatter = NumberFormat.getNumberInstance(Locale.FRENCH)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Encaisser un Remboursement",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Client : ${debt.customerName}\nReste dû : ${formatter.format(debt.remainingAmount)} FCFA",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Montant versé aujourd'hui (FCFA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Annuler")
                    }

                    Button(
                        onClick = {
                            val amt = amountInput.toLongOrNull() ?: 0L
                            if (amt > 0) {
                                onConfirmPayment(amt)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSecondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Confirmer", color = Color.White)
                    }
                }
            }
        }
    }
}
