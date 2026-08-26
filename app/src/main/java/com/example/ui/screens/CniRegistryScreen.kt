package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CniEntity
import com.example.data.model.IdDocumentType
import com.example.service.CniOcrService
import com.example.ui.components.CameraXScannerDialog
import com.example.ui.components.CniCardItem
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.OrangePrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CniRegistryScreen(
    cniRecords: List<CniEntity>,
    onSaveCni: (CniEntity) -> Unit,
    onDeleteCni: (CniEntity) -> Unit,
    onInitiateTransactionForCustomer: (CniEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Search & Filter State
    var searchQuery by remember { mutableStateOf("") }
    var selectedDocTypeFilter by remember { mutableStateOf<IdDocumentType?>(null) } // null = All
    var selectedDateFilter by remember { mutableStateOf<String?>(null) } // "ALL", "TODAY", or specific date string "dd/MM/yyyy"
    var customDateInput by remember { mutableStateOf("") }
    var showCustomDatePicker by remember { mutableStateOf(false) }
    var validityFilter by remember { mutableStateOf("ALL") } // ALL, VALID, EXPIRED
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showCameraXScanner by remember { mutableStateOf(false) }
    var isScanningMode by remember { mutableStateOf(false) }
    var isOcrProcessing by remember { mutableStateOf(false) }
    var ocrStatusMessage by remember { mutableStateOf<String?>(null) }
    var scannedEntityToEdit by remember { mutableStateOf<CniEntity?>(null) }

    val todayDateStr = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    // Camera Picture Capture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            isOcrProcessing = true
            ocrStatusMessage = "Analyse OCR ML Kit en cours..."
            coroutineScope.launch {
                val res = CniOcrService.processBitmapWithMlKit(bitmap)
                isOcrProcessing = false
                res.onSuccess { (entity, raw) ->
                    ocrStatusMessage = "Scan réussi !"
                    scannedEntityToEdit = entity
                }.onFailure { err ->
                    ocrStatusMessage = "Erreur de lecture : ${err.localizedMessage ?: "Image non reconnue"}"
                }
            }
        }
    }

    // Gallery Image Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isOcrProcessing = true
            ocrStatusMessage = "Analyse OCR ML Kit de l'image..."
            coroutineScope.launch {
                val res = CniOcrService.processUriWithMlKit(context, uri)
                isOcrProcessing = false
                res.onSuccess { (entity, raw) ->
                    ocrStatusMessage = "Scan réussi !"
                    scannedEntityToEdit = entity
                }.onFailure { err ->
                    ocrStatusMessage = "Erreur de lecture : ${err.localizedMessage ?: "Image non reconnue"}"
                }
            }
        }
    }

    val filteredList = remember(cniRecords, searchQuery, selectedDocTypeFilter, selectedDateFilter, customDateInput, validityFilter) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        cniRecords.filter { item ->
            val recordDateStr = dateFormat.format(Date(item.scanDate))
            
            // 1. Text match (CNI Number, First Name, Last Name, Phone, Place of Birth)
            val matchesQuery = searchQuery.isBlank() ||
                    item.cniNumber.contains(searchQuery, ignoreCase = true) ||
                    item.firstName.contains(searchQuery, ignoreCase = true) ||
                    item.lastName.contains(searchQuery, ignoreCase = true) ||
                    item.phone.contains(searchQuery) ||
                    item.placeOfBirth.contains(searchQuery, ignoreCase = true) ||
                    item.dateOfBirth.contains(searchQuery) ||
                    recordDateStr.contains(searchQuery)

            // 2. Document type filter
            val matchesDocType = selectedDocTypeFilter == null || item.idDocumentType == selectedDocTypeFilter

            // 3. Date filter (Today, Custom Date, Scan Date, Delivery Date)
            val matchesDate = when (selectedDateFilter) {
                "TODAY" -> recordDateStr == todayDateStr
                "CUSTOM" -> {
                    if (customDateInput.isNotBlank()) {
                        recordDateStr.contains(customDateInput.trim()) || 
                        item.dateOfBirth.contains(customDateInput.trim()) ||
                        item.deliveryDate.contains(customDateInput.trim()) ||
                        item.expiryDate.contains(customDateInput.trim())
                    } else true
                }
                else -> true
            }

            // 4. Validity filter
            val isExpired = CniOcrService.isDocumentExpired(item.expiryDate)
            val matchesValidity = when (validityFilter) {
                "VALID" -> !isExpired
                "EXPIRED" -> isExpired
                else -> true
            }

            matchesQuery && matchesDocType && matchesDate && matchesValidity
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cni_registry_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // KYC Regulatory Header Card
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
                                    .background(EmeraldSecondary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = EmeraldSecondary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Registre Légal des Pièces d'Identité",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${cniRecords.size} pièces enregistrées (CNIB, Passeport, Consulaire...)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Export Button
                        IconButton(
                            onClick = {
                                val reportText = buildString {
                                    appendLine("📋 REGISTRE DES PIÈCES D'IDENTITÉ (KYC) - WEND-LAMITA")
                                    appendLine("Date d'export : ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
                                    appendLine("Nombre d'enregistrements filtrés : ${filteredList.size} / ${cniRecords.size}")
                                    appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                    filteredList.forEachIndexed { index, c ->
                                        val scanDateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(c.scanDate))
                                        appendLine("${index + 1}. [${c.idDocumentType.badgeText}] N° ${c.cniNumber} | ${c.lastName.uppercase()} ${c.firstName}")
                                        appendLine("   Date enreg: $scanDateFormatted | Tél: ${c.phone} | Né(e) le: ${c.dateOfBirth} | Exp: ${c.expiryDate}")
                                    }
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, reportText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Exporter le Registre des Pièces"))
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Exporter", tint = OrangePrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Scanner Action Buttons (Camera / Gallery / Manual)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isScanningMode = !isScanningMode },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSecondary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scan_cni_btn")
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isScanningMode) "Fermer OCR" else "Scanner OCR", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_cni_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajout Manuel", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Live Scanner & Real OCR Card (ML Kit with CameraX)
        if (isScanningMode) {
            item {
                OcrRealScannerCard(
                    isProcessing = isOcrProcessing,
                    statusMessage = ocrStatusMessage,
                    onTakePhoto = { showCameraXScanner = true },
                    onPickFromGallery = { galleryLauncher.launch("image/*") },
                    onCardScanned = { scannedCni ->
                        scannedEntityToEdit = scannedCni
                    }
                )
            }
        }

        // Intelligent Search & Multi-criteria Filter Bar
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recherche Intelligente & Filtres",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (searchQuery.isNotEmpty() || selectedDocTypeFilter != null || selectedDateFilter != null || customDateInput.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedDocTypeFilter = null
                                    selectedDateFilter = null
                                    customDateInput = ""
                                    validityFilter = "ALL"
                                }
                            ) {
                                Text("Réinitialiser", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Main Text Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Nom, N° Pièce (ex: B128...), Tél, Date...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Effacer")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cni_search_input")
                    )

                    // 1. Date Filter Chips (Date précise)
                    Text(
                        text = "Filtrer par date précise :",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedDateFilter == null,
                                onClick = { selectedDateFilter = null; customDateInput = "" },
                                label = { Text("Toutes dates") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedDateFilter == "TODAY",
                                onClick = { selectedDateFilter = "TODAY"; customDateInput = "" },
                                label = { Text("Aujourd'hui ($todayDateStr)") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedDateFilter == "CUSTOM",
                                onClick = { 
                                    selectedDateFilter = "CUSTOM"
                                    showCustomDatePicker = !showCustomDatePicker
                                },
                                label = { Text(if (customDateInput.isBlank()) "Date précise..." else "Date: $customDateInput") },
                                trailingIcon = { Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }

                    // Custom Date Input Field if chosen
                    if (selectedDateFilter == "CUSTOM") {
                        OutlinedTextField(
                            value = customDateInput,
                            onValueChange = { customDateInput = it },
                            label = { Text("Saisir date exacte (ex: 26/08/2026)") },
                            placeholder = { Text("JJ/MM/AAAA") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Event, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 2. Document Type Chips (CNIB, Passeport, Carte consulaire, Carte militaire, Carte de réfugié)
                    Text(
                        text = "Type de pièce d'identité :",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedDocTypeFilter == null,
                                onClick = { selectedDocTypeFilter = null },
                                label = { Text("Toutes pièces") }
                            )
                        }
                        items(IdDocumentType.values()) { type ->
                            FilterChip(
                                selected = selectedDocTypeFilter == type,
                                onClick = { selectedDocTypeFilter = if (selectedDocTypeFilter == type) null else type },
                                label = { Text(type.badgeText) }
                            )
                        }
                    }

                    // 3. Validity Status Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = validityFilter == "ALL",
                            onClick = { validityFilter = "ALL" },
                            label = { Text("Tous (${filteredList.size})") }
                        )
                        FilterChip(
                            selected = validityFilter == "VALID",
                            onClick = { validityFilter = "VALID" },
                            label = { Text("Conformes") }
                        )
                        FilterChip(
                            selected = validityFilter == "EXPIRED",
                            onClick = { validityFilter = "EXPIRED" },
                            label = { Text("Expirés") }
                        )
                    }
                }
            }
        }

        // Records List
        if (filteredList.isEmpty()) {
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
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Aucune pièce trouvée pour ces critères de recherche.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedDateFilter != null || selectedDocTypeFilter != null || searchQuery.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    selectedDocTypeFilter = null
                                    selectedDateFilter = null
                                    customDateInput = ""
                                }
                            ) {
                                Text("Afficher toutes les pièces")
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { cni ->
                CniCardItem(
                    cni = cni,
                    onSelectForTransaction = { onInitiateTransactionForCustomer(cni) },
                    onDelete = { onDeleteCni(cni) }
                )
            }
        }
    }

    // Manual Add Dialog
    if (showAddDialog) {
        AddCniDialog(
            initialEntity = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { newCni ->
                onSaveCni(newCni)
                showAddDialog = false
            }
        )
    }

    // In-App CameraX Document Scanner Dialog
    if (showCameraXScanner) {
        CameraXScannerDialog(
            onDismiss = { showCameraXScanner = false },
            onImageCaptured = { bitmap ->
                showCameraXScanner = false
                isOcrProcessing = true
                ocrStatusMessage = "Traitement OCR CameraX en cours..."
                coroutineScope.launch {
                    val res = CniOcrService.processBitmapWithMlKit(bitmap)
                    isOcrProcessing = false
                    res.onSuccess { (entity, raw) ->
                        ocrStatusMessage = "Scan CameraX réussi !"
                        scannedEntityToEdit = entity
                    }.onFailure { err ->
                        ocrStatusMessage = "Erreur OCR : ${err.localizedMessage ?: "Image illisible"}"
                    }
                }
            }
        )
    }

    // Scanned OCR Entity Confirmation Dialog
    if (scannedEntityToEdit != null) {
        AddCniDialog(
            initialEntity = scannedEntityToEdit,
            onDismiss = { scannedEntityToEdit = null },
            onConfirm = { confirmedCni ->
                onSaveCni(confirmedCni)
                scannedEntityToEdit = null
                isScanningMode = false
            }
        )
    }
}

@Composable
fun OcrRealScannerCard(
    isProcessing: Boolean,
    statusMessage: String?,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onCardScanned: (CniEntity) -> Unit
) {
    val presets = remember { CniOcrService.getPresetScanSamples() }
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanProgress"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, EmeraldSecondary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Scanner de Pièce d'Identité Hors Ligne (OCR)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = EmeraldSecondary
            )
            Text(
                text = "Reconnaissance CNIB, Passeport, Carte Consulaire, Militaire, Réfugié",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Real Camera & Gallery Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onTakePhoto,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSecondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Caméra CameraX", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onPickFromGallery,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galerie Photo", fontSize = 12.sp)
                }
            }

            if (isProcessing) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = EmeraldSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(statusMessage ?: "Traitement OCR en cours...", style = MaterialTheme.typography.bodySmall, color = EmeraldSecondary)
                }
            } else if (statusMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Visual Card Frame Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F172A))
                    .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("BURKINA FASO • PIÈCE LÉGALE", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("KYC CONFORME", color = Color(0xFF38BDF8), fontSize = 10.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(45.dp, 55.dp)
                                .background(Color.DarkGray, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.LightGray)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("NOM : SAWADOGO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("PRENOM : IBRAHIM", color = Color.White, fontSize = 11.sp)
                            Text("N° : B12948201 / P01492014", color = Color(0xFF38BDF8), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Text("<<< B12948201<<<<<BFA950918<<<<<<<<<<<", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }

                // Laser Scanning Line Animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (scanProgress * 150).dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color(0xFF00FF66), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Ou testez avec un spécimen officiel :",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { (label, cni) ->
                    OutlinedButton(
                        onClick = { onCardScanned(cni) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = when (cni.idDocumentType) {
                                IdDocumentType.PASSEPORT -> Icons.Default.Public
                                IdDocumentType.CARTE_MILITAIRE -> Icons.Default.Shield
                                IdDocumentType.CARTE_CONSULAIRE -> Icons.Default.LocationCity
                                else -> Icons.Default.Badge
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reconnaître $label (${cni.cniNumber})", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun AddCniDialog(
    initialEntity: CniEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (CniEntity) -> Unit
) {
    var docType by remember { mutableStateOf(initialEntity?.idDocumentType ?: IdDocumentType.CNIB) }
    var cniNumber by remember { mutableStateOf(initialEntity?.cniNumber ?: "") }
    var lastName by remember { mutableStateOf(initialEntity?.lastName ?: "") }
    var firstName by remember { mutableStateOf(initialEntity?.firstName ?: "") }
    var phone by remember { mutableStateOf(initialEntity?.phone ?: "") }
    var dob by remember { mutableStateOf(initialEntity?.dateOfBirth ?: "15/06/1992") }
    var pob by remember { mutableStateOf(initialEntity?.placeOfBirth ?: "Ouagadougou") }
    var profession by remember { mutableStateOf(initialEntity?.profession ?: "Commerçant") }
    var expiryDate by remember { mutableStateOf(initialEntity?.expiryDate ?: "15/06/2032") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = if (initialEntity != null) "Vérifier la pièce scannée (OCR)" else "Enregistrer une Pièce d'Identité / KYC",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (errorMessage != null) {
                    item {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Type de pièce selector
                item {
                    Text(
                        text = "Type de pièce d'identité :",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(IdDocumentType.values()) { type ->
                            FilterChip(
                                selected = docType == type,
                                onClick = { docType = type },
                                label = { Text(type.badgeText) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = cniNumber,
                        onValueChange = { cniNumber = it.uppercase() },
                        label = { Text("Numéro / Réf de pièce (ex: B12894732 / P01294)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it.uppercase() },
                            label = { Text("Nom") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("Prénom") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Numéro Téléphone (optionnel)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { dob = it },
                            label = { Text("Date Naiss.") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pob,
                            onValueChange = { pob = it },
                            label = { Text("Lieu Naiss.") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = profession,
                            onValueChange = { profession = it },
                            label = { Text("Profession") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { expiryDate = it },
                            label = { Text("Date Expiration") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Annuler")
                        }

                        Button(
                            onClick = {
                                val validation = CniOcrService.validateDocumentNumber(docType, cniNumber)
                                if (!validation.isValid) {
                                    errorMessage = validation.message
                                    return@Button
                                }
                                if (lastName.isBlank() || firstName.isBlank()) {
                                    errorMessage = "Veuillez renseigner le nom et le prénom."
                                    return@Button
                                }

                                onConfirm(
                                    CniEntity(
                                        id = initialEntity?.id ?: 0L,
                                        idDocumentType = docType,
                                        cniNumber = cniNumber.trim(),
                                        firstName = firstName.trim(),
                                        lastName = lastName.trim(),
                                        dateOfBirth = dob.trim(),
                                        placeOfBirth = pob.trim(),
                                        deliveryDate = initialEntity?.deliveryDate ?: "10/01/2022",
                                        expiryDate = expiryDate.trim(),
                                        phone = phone.trim(),
                                        profession = profession.trim(),
                                        nationality = "Burkinabè"
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSecondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sauvegarder", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

