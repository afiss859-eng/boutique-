package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CniEntity
import com.example.data.model.PaymentProvider
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.service.CniOcrService
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WaveBlue
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TopFintechBar(
    storeName: String = "Wend-Lamita Services",
    agentName: String = "Caisse Principale",
    userRole: com.example.data.model.UserRole? = null,
    currentLanguage: com.example.service.AppLanguage = com.example.service.AppLanguage.FRANCAIS,
    onSelectLanguage: (com.example.service.AppLanguage) -> Unit = {},
    onOpenSubscription: () -> Unit = {},
    onOpenAdmin: () -> Unit = {},
    onLogout: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    var showLangMenu by remember { mutableStateOf(false) }
    val currentTime = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(OrangePrimary, GoldAccent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = storeName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(EmeraldSecondary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$agentName • $currentTime",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Language Dropdown Selector Button
                Box {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { showLangMenu = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${currentLanguage.flag} ${currentLanguage.code.uppercase()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Langues",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showLangMenu,
                        onDismissRequest = { showLangMenu = false }
                    ) {
                        com.example.service.AppLanguage.values().forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Text("${lang.flag} ${lang.nativeName} (${lang.code.uppercase()})")
                                },
                                onClick = {
                                    onSelectLanguage(lang)
                                    showLangMenu = false
                                },
                                leadingIcon = {
                                    if (lang == currentLanguage) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = OrangePrimary)
                                    }
                                }
                            )
                        }
                    }
                }

                // Subscription Badge Button
                IconButton(
                    onClick = onOpenSubscription,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "Abonnement",
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Admin Button
                IconButton(
                    onClick = onOpenAdmin,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin",
                        tint = OrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiptDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val fcfa = remember { NumberFormat.getNumberInstance(Locale.FRENCH) }
    val dateStr = remember(transaction.timestamp) {
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(transaction.timestamp))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(EmeraldSecondary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Succès",
                        tint = EmeraldSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "REÇU DE TRANSACTION",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Établissement Wend-Lamita",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Amount
                Text(
                    text = "${fcfa.format(transaction.amount)} FCFA",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = OrangePrimary
                    )
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (transaction.provider) {
                        PaymentProvider.ORANGE_MONEY -> OrangePrimary.copy(alpha = 0.12f)
                        PaymentProvider.MOOV_MONEY -> EmeraldSecondary.copy(alpha = 0.12f)
                        PaymentProvider.WAVE -> WaveBlue.copy(alpha = 0.12f)
                        PaymentProvider.ESPECES -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = "${transaction.type.label} • ${transaction.provider.label}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = when (transaction.provider) {
                                PaymentProvider.ORANGE_MONEY -> OrangePrimary
                                PaymentProvider.MOOV_MONEY -> EmeraldSecondary
                                PaymentProvider.WAVE -> WaveBlue
                                PaymentProvider.ESPECES -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Details List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReceiptRow(label = "Réf. Transaction", value = transaction.referenceCode)
                    ReceiptRow(label = "Date & Heure", value = dateStr)
                    if (transaction.clientName.isNotBlank()) {
                        ReceiptRow(label = "Client", value = transaction.clientName)
                    }
                    if (transaction.clientPhone.isNotBlank()) {
                        ReceiptRow(label = "Téléphone", value = transaction.clientPhone)
                    }
                    if (transaction.clientCniNumber.isNotBlank()) {
                        ReceiptRow(label = "N° CNI Client", value = transaction.clientCniNumber)
                    }
                    if (transaction.fee > 0) {
                        ReceiptRow(label = "Frais de service", value = "${fcfa.format(transaction.fee)} FCFA")
                    }
                    if (transaction.commission > 0) {
                        ReceiptRow(label = "Marge / Commission", value = "+${fcfa.format(transaction.commission)} FCFA")
                    }
                    if (transaction.note.isNotBlank()) {
                        ReceiptRow(label = "Détails", value = transaction.note)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareBody = """
                                🧾 *REÇU WEND-LAMITA*
                                ━━━━━━━━━━━━━━━━
                                • Type: ${transaction.type.label} (${transaction.provider.label})
                                • Montant: ${fcfa.format(transaction.amount)} FCFA
                                • Réf: ${transaction.referenceCode}
                                • Client: ${transaction.clientName} (${transaction.clientPhone})
                                • Date: $dateStr
                                ━━━━━━━━━━━━━━━━
                                Merci de votre confiance !
                            """.trimIndent()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareBody)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Partager le reçu")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Partager")
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("close_receipt_btn")
                    ) {
                        Text("Terminer", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun CniCardItem(
    cni: CniEntity,
    onSelectForTransaction: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val isExpired = CniOcrService.isDocumentExpired(cni.expiryDate)
    val docType = cni.idDocumentType

    val (badgeBg, badgeFg, docTitle) = when (docType) {
        com.example.data.model.IdDocumentType.CNIB -> Triple(EmeraldSecondary.copy(alpha = 0.15f), EmeraldSecondary, "BURKINA FASO • CNIB")
        com.example.data.model.IdDocumentType.PASSEPORT -> Triple(WaveBlue.copy(alpha = 0.15f), WaveBlue, "PASSEPORT INTERNATIONAL")
        com.example.data.model.IdDocumentType.CARTE_CONSULAIRE -> Triple(GoldAccent.copy(alpha = 0.2f), OrangePrimary, "CARTE CONSULAIRE")
        com.example.data.model.IdDocumentType.CARTE_MILITAIRE -> Triple(Color(0xFF4B5563).copy(alpha = 0.2f), Color(0xFF1E293B), "CARTE MILITAIRE / FAN")
        com.example.data.model.IdDocumentType.CARTE_REFUGIE -> Triple(Color(0xFF8B5CF6).copy(alpha = 0.15f), Color(0xFF7C3AED), "CARTE DE RÉFUGIÉ / HCR")
        com.example.data.model.IdDocumentType.AUTRE -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "PIÈCE D'IDENTITÉ")
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isExpired) CrimsonRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cni_card_${cni.cniNumber}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // CNI Card Header like Official Document Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (docType) {
                                com.example.data.model.IdDocumentType.PASSEPORT -> Icons.Default.Public
                                com.example.data.model.IdDocumentType.CARTE_MILITAIRE -> Icons.Default.Shield
                                com.example.data.model.IdDocumentType.CARTE_CONSULAIRE -> Icons.Default.LocationCity
                                else -> Icons.Default.Badge
                            },
                            contentDescription = docType.label,
                            tint = badgeFg,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = docTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = badgeFg,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = cni.cniNumber,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isExpired) CrimsonRed.copy(alpha = 0.12f) else EmeraldSecondary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isExpired) "Expiré" else docType.badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isExpired) CrimsonRed else EmeraldSecondary
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer Identity Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${cni.lastName.uppercase()} ${cni.firstName}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Né(e) le ${cni.dateOfBirth} à ${cni.placeOfBirth}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (cni.profession.isNotBlank()) {
                        Text(
                            text = "Profession : ${cni.profession}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (cni.phone.isNotBlank()) {
                        Text(
                            text = "Tél : +226 ${cni.phone}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = OrangePrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Valable jusqu'au ${cni.expiryDate.ifBlank { "N/A" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isExpired) CrimsonRed else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = onSelectForTransaction,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Opérer OM", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
