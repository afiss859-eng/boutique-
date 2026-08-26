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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CashClosureEntity
import com.example.data.model.CashFloat
import com.example.data.model.FloatAdjustmentEntity
import com.example.data.model.PaymentProvider
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.service.tr
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MoovGreen
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WaveBlue
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AdminTab {
    Z_CLOSURE,
    USERS_MANAGEMENT,
    TREASURY_FLOAT,
    SETTINGS_BACKUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    currentUser: UserEntity?,
    cashFloat: CashFloat,
    transactions: List<TransactionEntity>,
    allUsers: List<UserEntity>,
    closures: List<CashClosureEntity>,
    floatAdjustments: List<FloatAdjustmentEntity>,
    onSaveUser: (UserEntity) -> Unit,
    onDeleteUser: (UserEntity) -> Unit,
    onPerformZClosure: (Long, Long, Long, Long, Long, String, (CashClosureEntity) -> Unit) -> Unit,
    onRecordFloatAdjustment: (String, PaymentProvider, Long, String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { NumberFormat.getNumberInstance(Locale.FRENCH) }
    var selectedAdminTab by remember { mutableStateOf(AdminTab.Z_CLOSURE) }

    // State for Add/Edit User Dialog
    var showUserDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }
    var inputFullName by remember { mutableStateOf("") }
    var inputUsername by remember { mutableStateOf("") }
    var inputPinCode by remember { mutableStateOf("") }
    var inputPhone by remember { mutableStateOf("") }
    var inputRole by remember { mutableStateOf(UserRole.CAISSIER) }

    // State for Float In/Out Dialog
    var showFloatDialog by remember { mutableStateOf(false) }
    var floatType by remember { mutableStateOf("APPROVISIONNEMENT") }
    var floatProvider by remember { mutableStateOf(PaymentProvider.ORANGE_MONEY) }
    var floatAmountInput by remember { mutableStateOf("") }
    var floatNoteInput by remember { mutableStateOf("") }

    // State for Z Closure Counting
    var count10k by remember { mutableStateOf(0) }
    var count5k by remember { mutableStateOf(0) }
    var count2k by remember { mutableStateOf(0) }
    var count1k by remember { mutableStateOf(0) }
    var count500 by remember { mutableStateOf(0) }
    var countCoins by remember { mutableStateOf(0) }
    var closureNotes by remember { mutableStateOf("") }
    var completedClosureResult by remember { mutableStateOf<CashClosureEntity?>(null) }

    val physicalCashCalculated = remember(count10k, count5k, count2k, count1k, count500, countCoins) {
        (count10k * 10000L) +
        (count5k * 5000L) +
        (count2k * 2000L) +
        (count1k * 1000L) +
        (count500 * 500L) +
        countCoins.toLong()
    }

    val theoreticalCash = cashFloat.cashOnHand
    val cashDifference = physicalCashCalculated - theoreticalCash

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Admin Header Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE11D48)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = currentUser?.fullName ?: "Administrateur",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(currentUser?.role?.badgeColor ?: 0xFFE11D48).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = currentUser?.role?.label ?: "Admin",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(currentUser?.role?.badgeColor ?: 0xFFE11D48),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Text(
                                    text = "• Session Active",
                                    style = MaterialTheme.typography.bodySmall.copy(color = EmeraldSecondary)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Déconnexion",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Sub-Navigation Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedAdminTab.ordinal,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedAdminTab == AdminTab.Z_CLOSURE,
                    onClick = { selectedAdminTab = AdminTab.Z_CLOSURE },
                    text = { Text("📊 Bilan Z & Caisse", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedAdminTab == AdminTab.USERS_MANAGEMENT,
                    onClick = { selectedAdminTab = AdminTab.USERS_MANAGEMENT },
                    text = { Text("👥 Caissiers (${allUsers.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedAdminTab == AdminTab.TREASURY_FLOAT,
                    onClick = { selectedAdminTab = AdminTab.TREASURY_FLOAT },
                    text = { Text("💼 Flotte & Trésorerie", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedAdminTab == AdminTab.SETTINGS_BACKUP,
                    onClick = { selectedAdminTab = AdminTab.SETTINGS_BACKUP },
                    text = { Text("⚙️ Paramètres", fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Tab Content
        when (selectedAdminTab) {
            AdminTab.Z_CLOSURE -> {
                item {
                    Text(
                        text = tr("z_closure_title"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Counting Table
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Comptage Physique des Espèces",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Denomination inputs
                            listOf(
                                "Billets 10 000 F" to (count10k to { v: Int -> count10k = v }),
                                "Billets 5 000 F" to (count5k to { v: Int -> count5k = v }),
                                "Billets 2 000 F" to (count2k to { v: Int -> count2k = v }),
                                "Billets 1 000 F" to (count1k to { v: Int -> count1k = v }),
                                "Billets 500 F" to (count500 to { v: Int -> count500 = v })
                            ).forEach { (label, pair) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = label, style = MaterialTheme.typography.bodyMedium)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { if (pair.first > 0) pair.second(pair.first - 1) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "-")
                                        }
                                        Text(
                                            text = "${pair.first}",
                                            modifier = Modifier.width(36.dp),
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = { pair.second(pair.first + 1) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "+")
                                        }
                                    }
                                }
                            }

                            // Pièces
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Total Pièces (FCFA)", style = MaterialTheme.typography.bodyMedium)
                                OutlinedTextField(
                                    value = if (countCoins == 0) "" else countCoins.toString(),
                                    onValueChange = { countCoins = it.toIntOrNull() ?: 0 },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(110.dp),
                                    singleLine = true,
                                    placeholder = { Text("0") }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(14.dp))

                            // Comparison Results
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = tr("physical_cash"), style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = "${formatter.format(physicalCashCalculated)} F",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Column {
                                    Text(text = tr("theoretical_cash"), style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = "${formatter.format(theoreticalCash)} F",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = tr("cash_difference"), style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = "${if (cashDifference > 0) "+" else ""}${formatter.format(cashDifference)} F",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                cashDifference == 0L -> EmeraldSecondary
                                                cashDifference > 0 -> GoldAccent
                                                else -> CrimsonRed
                                            }
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = closureNotes,
                                onValueChange = { closureNotes = it },
                                label = { Text("Observations / Justifications") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    onPerformZClosure(
                                        physicalCashCalculated,
                                        theoreticalCash,
                                        cashFloat.orangeMoneyBalance,
                                        cashFloat.moovMoneyBalance,
                                        cashFloat.waveBalance,
                                        closureNotes
                                    ) { result ->
                                        completedClosureResult = result
                                        Toast.makeText(context, "Rapport Z enregistré avec succès !", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.LockClock, contentDescription = null)
                                    Text("Certifier & Clôturer la Caisse (Rapport Z)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Recent Z Closures
                if (closures.isNotEmpty()) {
                    item {
                        Text(
                            text = "Historique des Clôtures Z",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    items(closures) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Rapport Z du ${item.dateString}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (item.cashDifference == 0L) EmeraldSecondary.copy(alpha = 0.15f) else CrimsonRed.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (item.cashDifference == 0L) "Conforme" else "Écart: ${formatter.format(item.cashDifference)} F",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (item.cashDifference == 0L) EmeraldSecondary else CrimsonRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Agent: ${item.closingAgent} • ${item.totalTransactionsCount} transactions • Comm: ${formatter.format(item.totalCommissions)} F",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                }
            }

            AdminTab.USERS_MANAGEMENT -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gestion des Utilisateurs & Caissiers",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Button(
                            onClick = {
                                editingUser = null
                                inputFullName = ""
                                inputUsername = ""
                                inputPinCode = ""
                                inputPhone = ""
                                inputRole = UserRole.CAISSIER
                                showUserDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ajouter", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(allUsers) { user ->
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(user.role.badgeColor).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (user.role) {
                                            UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                            UserRole.GERANT -> Icons.Default.SupervisedUserCircle
                                            UserRole.CAISSIER -> Icons.Default.Person
                                        },
                                        contentDescription = null,
                                        tint = Color(user.role.badgeColor),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = user.fullName,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Identifiant: ${user.username} • PIN: **** • ${user.role.label}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = {
                                    editingUser = user
                                    inputFullName = user.fullName
                                    inputUsername = user.username
                                    inputPinCode = user.pinCode
                                    inputPhone = user.phone
                                    inputRole = user.role
                                    showUserDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = OrangePrimary)
                                }
                                if (user.username != "admin") {
                                    IconButton(onClick = { onDeleteUser(user) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = CrimsonRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AdminTab.TREASURY_FLOAT -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mouvements de Flotte & Trésorerie",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Button(
                            onClick = { showFloatDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ajuster Flotte", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Balance Cards
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("Espèces en Caisse", cashFloat.cashOnHand, OrangePrimary),
                            Triple("Orange Money", cashFloat.orangeMoneyBalance, OrangePrimary),
                            Triple("Moov Money", cashFloat.moovMoneyBalance, MoovGreen),
                            Triple("Wave", cashFloat.waveBalance, WaveBlue)
                        ).forEach { (title, balance, color) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = title, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${formatter.format(balance)} FCFA",
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                }
                            }
                        }
                    }
                }

                if (floatAdjustments.isNotEmpty()) {
                    item {
                        Text(
                            text = "Historique des Approvisionnements",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    items(floatAdjustments) { adj ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${adj.type} (${adj.provider.label})",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Agent: ${adj.agentName} • ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(Date(adj.timestamp))}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                                Text(
                                    text = "${formatter.format(adj.amount)} F",
                                    fontWeight = FontWeight.Bold,
                                    color = if (adj.type == "APPROVISIONNEMENT") EmeraldSecondary else CrimsonRed
                                )
                            }
                        }
                    }
                }
            }

            AdminTab.SETTINGS_BACKUP -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Paramètres de l'Établissement",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = "Wend-Lamita Services",
                                onValueChange = {},
                                label = { Text("Nom Commercial du Kiosque") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = "Ouagadougou, Secteur 28, BF",
                                onValueChange = {},
                                label = { Text("Localisation / Adresse") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = "IFU: 00129482M / RCCM: BF-OUA-2023-A-102",
                                onValueChange = {},
                                label = { Text("N° IFU & RCCM (Légal)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    Toast.makeText(context, "Paramètres sauvegardés avec succès", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enregistrer les Modifications", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add/Edit User Dialog Modal
    if (showUserDialog) {
        Dialog(onDismissRequest = { showUserDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (editingUser == null) "Ajouter un Caissier" else "Modifier l'Agent",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedTextField(
                        value = inputFullName,
                        onValueChange = { inputFullName = it },
                        label = { Text("Nom complet") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = inputUsername,
                        onValueChange = { inputUsername = it },
                        label = { Text("Identifiant / Login") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = inputPinCode,
                        onValueChange = { if (it.length <= 4) inputPinCode = it },
                        label = { Text("Code PIN (4 chiffres)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = inputPhone,
                        onValueChange = { inputPhone = it },
                        label = { Text("Téléphone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Role Picker
                    Text(text = "Rôle de l'utilisateur :", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        UserRole.values().forEach { role ->
                            val isSelected = inputRole == role
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { inputRole = role },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(role.badgeColor).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(role.badgeColor) else Color.Transparent)
                            ) {
                                Text(
                                    text = role.label,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showUserDialog = false }) {
                            Text("Annuler")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputFullName.isNotBlank() && inputPinCode.length == 4) {
                                    val user = (editingUser ?: UserEntity(
                                        username = inputUsername.ifBlank { "user_${System.currentTimeMillis() % 1000}" },
                                        fullName = inputFullName,
                                        pinCode = inputPinCode,
                                        role = inputRole
                                    )).copy(
                                        fullName = inputFullName,
                                        username = inputUsername,
                                        pinCode = inputPinCode,
                                        role = inputRole,
                                        phone = inputPhone
                                    )
                                    onSaveUser(user)
                                    showUserDialog = false
                                    Toast.makeText(context, "Utilisateur enregistré", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Veuillez entrer un PIN à 4 chiffres", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Enregistrer")
                        }
                    }
                }
            }
        }
    }

    // Float In/Out Dialog Modal
    if (showFloatDialog) {
        Dialog(onDismissRequest = { showFloatDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Ajustement de Trésorerie",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    // Type
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("APPROVISIONNEMENT" to "Apport / Float In", "RETRAIT" to "Retrait / Float Out").forEach { (type, label) ->
                            val isSelected = floatType == type
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { floatType = type },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) OrangePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) OrangePrimary else Color.Transparent)
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Operator
                    Text(text = "Caisse / Opérateur concerné :", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentProvider.values().forEach { prov ->
                            val isSelected = floatProvider == prov
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { floatProvider = prov },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            ) {
                                Text(
                                    text = prov.label.take(8),
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = floatAmountInput,
                        onValueChange = { floatAmountInput = it },
                        label = { Text("Montant (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = floatNoteInput,
                        onValueChange = { floatNoteInput = it },
                        label = { Text("Motif / N° Référence bordereau") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showFloatDialog = false }) {
                            Text("Annuler")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = floatAmountInput.toLongOrNull() ?: 0L
                                if (amount > 0) {
                                    onRecordFloatAdjustment(floatType, floatProvider, amount, floatNoteInput)
                                    showFloatDialog = false
                                    Toast.makeText(context, "Trésorerie mise à jour", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Valider")
                        }
                    }
                }
            }
        }
    }
}
