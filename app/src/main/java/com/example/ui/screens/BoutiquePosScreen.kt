package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.*
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
import com.example.data.model.CartItem
import com.example.data.model.PaymentProvider
import com.example.data.model.ProductEntity
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WaveBlue
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoutiquePosScreen(
    products: List<ProductEntity>,
    cart: List<CartItem>,
    onAddToCart: (ProductEntity) -> Unit,
    onRemoveFromCart: (Long) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: (clientName: String, clientPhone: String, paymentProvider: PaymentProvider) -> Unit,
    onSaveProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tous") }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    val fcfa = remember { NumberFormat.getNumberInstance(Locale.FRENCH) }
    val categories = listOf("Tous", "Télécom", "Boissons", "Alimentation", "Hygiène", "Accessoires")

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { product ->
            val matchesCategory = selectedCategory == "Tous" || product.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() || product.name.contains(searchQuery, ignoreCase = true) || product.barcode.contains(searchQuery)
            matchesCategory && matchesSearch
        }
    }

    val cartTotal = remember(cart) { cart.sumOf { it.totalPrice } }
    val cartCount = remember(cart) { cart.sumOf { it.quantity } }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("boutique_pos_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header & Add Product Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Gestion de Stock & Caisse",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${products.size} articles référencés",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showAddProductDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        modifier = Modifier.testTag("add_product_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nouvel Article")
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher un produit, code barre...") },
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
                        .testTag("product_search_input")
                )
            }

            // Categories Filter
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            // Products List
            if (filteredProducts.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Aucun article trouvé dans cette catégorie", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    val inCartCount = cart.find { it.product.id == product.id }?.quantity ?: 0
                    ProductItemRow(
                        product = product,
                        inCartCount = inCartCount,
                        onAdd = { onAddToCart(product) },
                        onRemove = { onRemoveFromCart(product.id) },
                        onDelete = { onDeleteProduct(product) }
                    )
                }
            }
        }

        // Floating Cart Summary Bar
        AnimatedVisibility(
            visible = cart.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = OrangePrimary,
                tonalElevation = 8.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCheckoutDialog = true }
                    .testTag("floating_cart_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$cartCount",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Color.White)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Panier de Vente", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.9f)))
                            Text("${fcfa.format(cartTotal)} FCFA", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = Color.White))
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Encaisser", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }

    // Add Product Modal
    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onConfirm = { newProduct ->
                onSaveProduct(newProduct)
                showAddProductDialog = false
            }
        )
    }

    // Checkout POS Dialog
    if (showCheckoutDialog) {
        PosCheckoutDialog(
            cart = cart,
            cartTotal = cartTotal,
            onDismiss = { showCheckoutDialog = false },
            onConfirmSale = { clientName, clientPhone, provider ->
                onCheckout(clientName, clientPhone, provider)
                showCheckoutDialog = false
            }
        )
    }
}

@Composable
fun ProductItemRow(
    product: ProductEntity,
    inCartCount: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit
) {
    val fcfa = remember { NumberFormat.getNumberInstance(Locale.FRENCH) }
    val isLowStock = product.stockQuantity <= product.minStockAlert

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_row_${product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${fcfa.format(product.salePrice)} FCFA / ${product.unit} (Achat: ${fcfa.format(product.purchasePrice)} F)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isLowStock) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else EmeraldSecondary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (product.stockQuantity == 0) "Rupture" else "Stock: ${product.stockQuantity} ${product.unit}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isLowStock) MaterialTheme.colorScheme.error else EmeraldSecondary
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Cart Quantity Controller
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (inCartCount > 0) {
                    FilledTonalIconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Moins", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "$inCartCount",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                IconButton(
                    onClick = onAdd,
                    enabled = product.stockQuantity > inCartCount,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = OrangePrimary),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Alimentation") }
    var barcode by remember { mutableStateOf("") }
    var purchasePriceInput by remember { mutableStateOf("") }
    var salePriceInput by remember { mutableStateOf("") }
    var stockInput by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pièce") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ajouter un Produit au Stock",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du produit") },
                    placeholder = { Text("Ex: Huile Dinor 1L") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Catégorie") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unité") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchasePriceInput,
                        onValueChange = { purchasePriceInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Prix Achat (F)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = salePriceInput,
                        onValueChange = { salePriceInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Prix Vente (F)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = stockInput,
                    onValueChange = { stockInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Quantité en stock initial") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(
                                    ProductEntity(
                                        name = name.trim(),
                                        category = category.trim(),
                                        barcode = barcode.trim(),
                                        purchasePrice = purchasePriceInput.toLongOrNull() ?: 0L,
                                        salePrice = salePriceInput.toLongOrNull() ?: 0L,
                                        stockQuantity = stockInput.toIntOrNull() ?: 0,
                                        unit = unit.trim()
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
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
fun PosCheckoutDialog(
    cart: List<CartItem>,
    cartTotal: Long,
    onDismiss: () -> Unit,
    onConfirmSale: (clientName: String, clientPhone: String, provider: PaymentProvider) -> Unit
) {
    val fcfa = remember { NumberFormat.getNumberInstance(Locale.FRENCH) }
    var clientName by remember { mutableStateOf("Client Comptoir") }
    var clientPhone by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf(PaymentProvider.ESPECES) }
    var cashReceivedInput by remember { mutableStateOf("") }

    val cashReceived = cashReceivedInput.toLongOrNull() ?: cartTotal
    val changeDue = if (cashReceived >= cartTotal) cashReceived - cartTotal else 0L

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Encaissement Panier",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Total Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OrangePrimary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total à Payer", style = MaterialTheme.typography.labelSmall, color = OrangePrimary)
                        Text(
                            text = "${fcfa.format(cartTotal)} FCFA",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = OrangePrimary
                            )
                        )
                    }
                }

                // Payment Method Selector
                Text("Mode de Paiement :", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        PaymentProvider.ESPECES to "Espèces",
                        PaymentProvider.ORANGE_MONEY to "OM",
                        PaymentProvider.MOOV_MONEY to "Moov",
                        PaymentProvider.WAVE to "Wave"
                    ).forEach { (provider, label) ->
                        FilterChip(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (selectedProvider == PaymentProvider.ESPECES) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = cashReceivedInput,
                            onValueChange = { cashReceivedInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Espèces reçues") },
                            placeholder = { Text("${fcfa.format(cartTotal)}") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldSecondary.copy(alpha = 0.15f),
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Monnaie à rendre", style = MaterialTheme.typography.labelSmall, color = EmeraldSecondary)
                                Text(
                                    text = "${fcfa.format(changeDue)} F",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldSecondary)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Nom du Client (optionnel)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = { onConfirmSale(clientName, clientPhone, selectedProvider) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSecondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Valider Vente", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
