package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CniEntity
import com.example.data.model.PaymentProvider
import com.example.data.model.TransactionType
import com.example.service.MobileMoneyService
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WaveBlue
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileMoneyScreen(
    cniRecords: List<CniEntity>,
    initialCustomer: CniEntity? = null,
    onClearInitialCustomer: () -> Unit = {},
    onExecuteTransaction: (
        type: TransactionType,
        provider: PaymentProvider,
        amount: Long,
        clientPhone: String,
        clientName: String,
        clientCniNumber: String,
        note: String
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedProvider by remember { mutableStateOf(PaymentProvider.ORANGE_MONEY) }
    var selectedType by remember { mutableStateOf(TransactionType.DEPOT) }
    var amountInput by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var clientCniNumber by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showCustomerPicker by remember { mutableStateOf(false) }

    LaunchedEffect(initialCustomer) {
        if (initialCustomer != null) {
            clientName = "${initialCustomer.lastName} ${initialCustomer.firstName}".trim()
            clientPhone = initialCustomer.phone
            clientCniNumber = initialCustomer.cniNumber
            onClearInitialCustomer()
        }
    }

    val fcfa = remember { NumberFormat.getNumberInstance(Locale.FRENCH) }
    val parsedAmount = amountInput.toLongOrNull() ?: 0L

    val customerFee = remember(parsedAmount, selectedType, selectedProvider) {
        MobileMoneyService.calculateCustomerFee(parsedAmount, selectedType, selectedProvider)
    }

    val agentCommission = remember(parsedAmount, selectedType, selectedProvider) {
        MobileMoneyService.calculateAgentCommission(parsedAmount, selectedType, selectedProvider)
    }

    val ussdCode = remember(selectedProvider, selectedType, clientPhone, parsedAmount) {
        MobileMoneyService.buildUssdCode(selectedProvider, selectedType, clientPhone, parsedAmount)
    }

    val quickAmounts = listOf(1000L, 2000L, 5000L, 10000L, 25000L, 50000L, 100000L)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("mobile_money_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Provider Selection
        item {
            Text(
                text = "Opérateur Mobile",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProviderChip(
                    name = "Orange Money",
                    isSelected = selectedProvider == PaymentProvider.ORANGE_MONEY,
                    brandColor = OrangePrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedProvider = PaymentProvider.ORANGE_MONEY }
                )
                ProviderChip(
                    name = "Moov Money",
                    isSelected = selectedProvider == PaymentProvider.MOOV_MONEY,
                    brandColor = EmeraldSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedProvider = PaymentProvider.MOOV_MONEY }
                )
                ProviderChip(
                    name = "Wave",
                    isSelected = selectedProvider == PaymentProvider.WAVE,
                    brandColor = WaveBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedProvider = PaymentProvider.WAVE }
                )
            }
        }

        // Operation Type Tabs
        item {
            Text(
                text = "Type d'Opération",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    TransactionType.DEPOT to "Dépôt",
                    TransactionType.RETRAIT to "Retrait",
                    TransactionType.TRANSFERT to "Transfert",
                    TransactionType.ACHAT_CREDIT to "Crédit"
                ).forEach { (type, label) ->
                    val isSelected = selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangePrimary.copy(alpha = 0.2f),
                            selectedLabelColor = OrangePrimary
                        )
                    )
                }
            }
        }

        // Amount Input & Quick Chips
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Montant de l'opération (FCFA)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { ch -> ch.isDigit() } },
                        placeholder = { Text("Ex: 25000") },
                        trailingIcon = { Text("FCFA", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("om_amount_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickAmounts) { amount ->
                            SuggestionChip(
                                onClick = { amountInput = amount.toString() },
                                label = { Text("${fcfa.format(amount)} F") }
                            )
                        }
                    }
                }
            }
        }

        // Client Details Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
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
                        Text(
                            text = "Informations Client",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (cniRecords.isNotEmpty()) {
                            TextButton(onClick = { showCustomerPicker = !showCustomerPicker }) {
                                Icon(imageVector = Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Choisir du registre CNI", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Dropdown for quick fill from CNI records
                    if (showCustomerPicker && cniRecords.isNotEmpty()) {
                        var pickerSearch by remember { mutableStateOf("") }
                        val pickerFiltered = remember(cniRecords, pickerSearch) {
                            if (pickerSearch.isBlank()) cniRecords.take(6)
                            else cniRecords.filter {
                                it.lastName.contains(pickerSearch, ignoreCase = true) ||
                                it.firstName.contains(pickerSearch, ignoreCase = true) ||
                                it.cniNumber.contains(pickerSearch, ignoreCase = true) ||
                                it.phone.contains(pickerSearch)
                            }.take(8)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Sélectionner une pièce d'identité enregistrée :",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = OrangePrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = pickerSearch,
                                    onValueChange = { pickerSearch = it },
                                    placeholder = { Text("Rechercher par nom, N° pièce...", fontSize = 12.sp) },
                                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                pickerFiltered.forEach { cni ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                clientName = "${cni.lastName} ${cni.firstName}".trim()
                                                clientPhone = cni.phone
                                                clientCniNumber = cni.cniNumber
                                                showCustomerPicker = false
                                            }
                                            .padding(vertical = 6.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${cni.lastName} ${cni.firstName}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "[${cni.idDocumentType.badgeText}] N° ${cni.cniNumber}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (cni.phone.isNotBlank()) {
                                            Text(
                                                text = cni.phone,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = EmeraldSecondary
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Numéro Téléphone Client") },
                        placeholder = { Text("70123456 ou +226 76543210") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("om_phone_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nom & Prénom (Facultatif)") },
                        placeholder = { Text("Ex: Ouedraogo Moussa") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("om_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = clientCniNumber,
                        onValueChange = { clientCniNumber = it.uppercase() },
                        label = { Text("N° Pièce d'Identité / CNI (KYC)") },
                        placeholder = { Text("Ex: B12894732") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Badge, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("om_cni_input")
                    )
                }
            }
        }

        // Live Fees & Commission Estimation
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Frais Client", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = if (customerFee == 0L) "GRATUIT" else "${fcfa.format(customerFee)} FCFA",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (customerFee == 0L) EmeraldSecondary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Votre Commission Nette", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "+${fcfa.format(agentCommission)} FCFA",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldSecondary
                            )
                        )
                    }
                }
            }
        }

        // USSD Launcher Card
        if (ussdCode.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Code USSD Express :",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = OrangePrimary
                            )
                            Text(
                                text = ussdCode,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }

                        Button(
                            onClick = {
                                val encoded = Uri.encode(ussdCode)
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$encoded"))
                                context.startActivity(dialIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSecondary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("dial_ussd_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Composer")
                        }
                    }
                }
            }
        }

        // Execute / Save Transaction Button
        item {
            Button(
                onClick = {
                    if (parsedAmount > 0) {
                        onExecuteTransaction(
                            selectedType,
                            selectedProvider,
                            parsedAmount,
                            clientPhone,
                            clientName,
                            clientCniNumber,
                            note
                        )
                        // Reset inputs
                        amountInput = ""
                        clientPhone = ""
                        clientName = ""
                        clientCniNumber = ""
                        note = ""
                    }
                },
                enabled = parsedAmount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("validate_transaction_btn")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Valider & Enregistrer (${fcfa.format(parsedAmount)} F)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun ProviderChip(
    name: String,
    isSelected: Boolean,
    brandColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) brandColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) brandColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(brandColor)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isSelected) brandColor else MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1
            )
        }
    }
}
