package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CartItem
import com.example.data.model.CashClosureEntity
import com.example.data.model.CashFloat
import com.example.data.model.CniEntity
import com.example.data.model.CustomerDebtEntity
import com.example.data.model.FloatAdjustmentEntity
import com.example.data.model.PaymentProvider
import com.example.data.model.ProductEntity
import com.example.data.model.SubscriptionEntity
import com.example.data.model.SubscriptionPlan
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.WendLamitaRepository
import com.example.service.AppLanguage
import com.example.service.CniOcrService
import com.example.service.GeminiAiService
import com.example.service.MobileMoneyService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab(val title: String) {
    DASHBOARD("Accueil"),
    MOBILE_MONEY("Mobile Money"),
    CNI_REGISTRY("Registre CNI"),
    BOUTIQUE_POS("Boutique"),
    DEBTS_BOOK("Dettes"),
    AI_ASSISTANT("Assistant IA"),
    REPORTS("Bilan"),
    ADMIN_PANEL("Admin & Z"),
    SUBSCRIPTION("Abonnement Pro")
}

data class ChatMessage(
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class WendLamitaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WendLamitaRepository
    
    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = WendLamitaRepository(
            db.transactionDao(),
            db.cniDao(),
            db.productDao(),
            db.debtDao(),
            db.userDao(),
            db.subscriptionDao(),
            db.cashClosureDao(),
            db.floatAdjustmentDao()
        )
    }

    // Language State
    private val _currentLanguage = MutableStateFlow(AppLanguage.FRANCAIS)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }

    // Authentication State
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    fun loginWithPin(pin: String) {
        viewModelScope.launch {
            _authErrorMessage.value = null
            val user = repository.getUserByPin(pin)
            if (user != null && user.isActive) {
                _currentUser.value = user
                _isLoggedIn.value = true
            } else {
                // fallback check for default 1234 / 0000 / 2222
                if (pin == "1234") {
                    val adminUser = UserEntity(username = "admin", fullName = "Sawadogo Afis (Admin)", pinCode = "1234", role = UserRole.ADMIN)
                    _currentUser.value = adminUser
                    _isLoggedIn.value = true
                } else if (pin == "0000") {
                    val cashier = UserEntity(username = "caissier1", fullName = "Aminata KABORE (Caissière)", pinCode = "0000", role = UserRole.CAISSIER)
                    _currentUser.value = cashier
                    _isLoggedIn.value = true
                } else if (pin == "2222") {
                    val gerant = UserEntity(username = "gerant1", fullName = "Ouedraogo Moussa (Gérant)", pinCode = "2222", role = UserRole.GERANT)
                    _currentUser.value = gerant
                    _isLoggedIn.value = true
                } else {
                    _authErrorMessage.value = "Code PIN incorrect ou compte désactivé"
                }
            }
        }
    }

    fun loginWithPassword(username: String, password: String) {
        viewModelScope.launch {
            _authErrorMessage.value = null
            val user = repository.getUserByUsername(username.trim())
            if (user != null && user.password == password.trim() && user.isActive) {
                _currentUser.value = user
                _isLoggedIn.value = true
            } else {
                if (username.equals("admin", ignoreCase = true) && password == "admin") {
                    val adminUser = UserEntity(username = "admin", fullName = "Sawadogo Afis (Admin)", pinCode = "1234", role = UserRole.ADMIN)
                    _currentUser.value = adminUser
                    _isLoggedIn.value = true
                } else {
                    _authErrorMessage.value = "Identifiants invalides"
                }
            }
        }
    }

    fun loginWithBiometric() {
        val adminUser = UserEntity(
            username = "admin",
            fullName = "Sawadogo Afis (Admin Biométrique)",
            pinCode = "1234",
            role = UserRole.ADMIN
        )
        _currentUser.value = adminUser
        _isLoggedIn.value = true
    }

    fun activateSubscription(
        plan: SubscriptionPlan,
        transactionId: String,
        operator: PaymentProvider,
        amount: Long,
        durationDays: Int,
        onSuccess: () -> Unit
    ) {
        activateSubscriptionWithTransactionId(plan, transactionId, operator, amount, durationDays, onSuccess)
    }

    fun performCashClosure(
        physicalCash: Long,
        theoreticalCash: Long,
        omBalance: Long,
        moovBalance: Long,
        waveBalance: Long,
        notes: String,
        onCompleted: (CashClosureEntity) -> Unit
    ) {
        performDailyZClosure(physicalCash, theoreticalCash, omBalance, moovBalance, waveBalance, notes, onCompleted)
    }

    fun logout() {
        _currentUser.value = null
        _isLoggedIn.value = false
        _currentTab.value = AppTab.DASHBOARD
    }

    // Tab Navigation
    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // Cash Float
    private val _cashFloat = MutableStateFlow(CashFloat())
    val cashFloat: StateFlow<CashFloat> = _cashFloat.asStateFlow()

    fun updateCashFloat(cash: Long, om: Long, moov: Long, wave: Long) {
        _cashFloat.value = CashFloat(
            cashOnHand = cash,
            orangeMoneyBalance = om,
            moovMoneyBalance = moov,
            waveBalance = wave
        )
    }

    // Transactions Flow
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // CNI Records Flow
    val allCniRecords: StateFlow<List<CniEntity>> = repository.allCniRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products Flow
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Users Flow
    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveUser(user: UserEntity) {
        viewModelScope.launch {
            if (user.id == 0L) {
                repository.insertUser(user)
            } else {
                repository.updateUser(user)
            }
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            repository.deleteUser(user)
        }
    }

    // Subscriptions Flow
    val activeSubscription: StateFlow<SubscriptionEntity?> = repository.activeSubscription
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSubscriptions: StateFlow<List<SubscriptionEntity>> = repository.allSubscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun activateSubscriptionWithTransactionId(
        plan: SubscriptionPlan,
        transactionId: String,
        operator: PaymentProvider,
        amount: Long,
        durationDays: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.activateSubscriptionWithTxId(
                plan = plan,
                transactionId = transactionId,
                operator = operator,
                amountPaid = amount,
                durationDays = durationDays,
                activatedBy = _currentUser.value?.fullName ?: "Admin"
            )
            onSuccess()
        }
    }

    // Cash Closures (Rapport Z)
    val allClosures: StateFlow<List<CashClosureEntity>> = repository.allClosures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun performDailyZClosure(
        physicalCash: Long,
        theoreticalCash: Long,
        omBalance: Long,
        moovBalance: Long,
        waveBalance: Long,
        notes: String,
        onCompleted: (CashClosureEntity) -> Unit
    ) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val txList = allTransactions.value
            val totalCommissions = txList.sumOf { it.commission }
            val totalVol = txList.sumOf { it.amount }

            val closure = CashClosureEntity(
                dateString = dateStr,
                closedAt = System.currentTimeMillis(),
                closingAgent = _currentUser.value?.fullName ?: "Agent Caisse",
                physicalCash = physicalCash,
                theoreticalCash = theoreticalCash,
                cashDifference = physicalCash - theoreticalCash,
                omBalance = omBalance,
                moovBalance = moovBalance,
                waveBalance = waveBalance,
                totalTransactionsCount = txList.size,
                totalVolume = totalVol,
                totalCommissions = totalCommissions,
                notes = notes,
                isApproved = true
            )
            val id = repository.insertClosure(closure)
            onCompleted(closure.copy(id = id))
        }
    }

    // Float Adjustments
    val allFloatAdjustments: StateFlow<List<FloatAdjustmentEntity>> = repository.allFloatAdjustments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recordFloatAdjustment(type: String, provider: PaymentProvider, amount: Long, notes: String) {
        viewModelScope.launch {
            val entity = FloatAdjustmentEntity(
                type = type,
                provider = provider,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                agentName = _currentUser.value?.fullName ?: "Admin",
                notes = notes
            )
            repository.insertFloatAdjustment(entity)

            val currentFloat = _cashFloat.value
            val multiplier = if (type == "APPROVISIONNEMENT") 1 else -1
            when (provider) {
                PaymentProvider.ESPECES -> _cashFloat.value = currentFloat.copy(cashOnHand = (currentFloat.cashOnHand + (amount * multiplier)).coerceAtLeast(0))
                PaymentProvider.ORANGE_MONEY -> _cashFloat.value = currentFloat.copy(orangeMoneyBalance = (currentFloat.orangeMoneyBalance + (amount * multiplier)).coerceAtLeast(0))
                PaymentProvider.MOOV_MONEY -> _cashFloat.value = currentFloat.copy(moovMoneyBalance = (currentFloat.moovMoneyBalance + (amount * multiplier)).coerceAtLeast(0))
                PaymentProvider.WAVE -> _cashFloat.value = currentFloat.copy(waveBalance = (currentFloat.waveBalance + (amount * multiplier)).coerceAtLeast(0))
            }
        }
    }

    // Cart Management for POS
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    fun addToCart(product: ProductEntity) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val item = currentList[index]
            if (item.quantity < product.stockQuantity) {
                currentList[index] = item.copy(quantity = item.quantity + 1)
            }
        } else {
            if (product.stockQuantity > 0) {
                currentList.add(CartItem(product = product, quantity = 1))
            }
        }
        _cart.value = currentList
    }

    fun removeFromCart(product: ProductEntity) {
        removeFromCart(product.id)
    }

    fun removeFromCart(productId: Long) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = currentList[index]
            if (item.quantity > 1) {
                currentList[index] = item.copy(quantity = item.quantity - 1)
            } else {
                currentList.removeAt(index)
            }
            _cart.value = currentList
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    // Last completed transaction for receipt display
    private val _lastCompletedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val lastCompletedTransaction: StateFlow<TransactionEntity?> = _lastCompletedTransaction.asStateFlow()

    fun dismissReceiptDialog() {
        _lastCompletedTransaction.value = null
    }

    // Execute Mobile Money Transaction
    fun executeMobileMoneyTransaction(
        type: TransactionType,
        provider: PaymentProvider,
        amount: Long,
        clientPhone: String,
        clientName: String = "",
        clientCniNumber: String = "",
        note: String = ""
    ) {
        viewModelScope.launch {
            val fee = MobileMoneyService.calculateCustomerFee(amount, type, provider)
            val commission = MobileMoneyService.calculateAgentCommission(amount, type, provider)

            val refPrefix = when (provider) {
                PaymentProvider.ORANGE_MONEY -> "OM"
                PaymentProvider.MOOV_MONEY -> "MM"
                PaymentProvider.WAVE -> "WV"
                PaymentProvider.ESPECES -> "ESP"
            }
            val refCode = "$refPrefix" + SimpleDateFormat("yyMMdd.HHmm", Locale.getDefault()).format(Date()) + "." + (100..999).random()

            val tx = TransactionEntity(
                type = type,
                provider = provider,
                amount = amount,
                fee = fee,
                commission = commission,
                clientName = clientName.ifBlank { "Client $clientPhone" },
                clientPhone = clientPhone,
                clientCniNumber = clientCniNumber,
                status = TransactionStatus.SUCCESS,
                referenceCode = refCode,
                note = note,
                agentName = _currentUser.value?.fullName ?: "Agent Wend-Lamita"
            )
            val id = repository.insertTransaction(tx)
            _lastCompletedTransaction.value = tx.copy(id = id)

            // Adjust cash float balances
            val currentFloat = _cashFloat.value
            when (type) {
                TransactionType.DEPOT -> {
                    when (provider) {
                        PaymentProvider.ORANGE_MONEY -> _cashFloat.value = currentFloat.copy(
                            cashOnHand = currentFloat.cashOnHand + amount,
                            orangeMoneyBalance = currentFloat.orangeMoneyBalance - amount
                        )
                        PaymentProvider.MOOV_MONEY -> _cashFloat.value = currentFloat.copy(
                            cashOnHand = currentFloat.cashOnHand + amount,
                            moovMoneyBalance = currentFloat.moovMoneyBalance - amount
                        )
                        PaymentProvider.WAVE -> _cashFloat.value = currentFloat.copy(
                            cashOnHand = currentFloat.cashOnHand + amount,
                            waveBalance = currentFloat.waveBalance - amount
                        )
                        PaymentProvider.ESPECES -> {}
                    }
                }
                TransactionType.RETRAIT -> {
                    when (provider) {
                        PaymentProvider.ORANGE_MONEY -> _cashFloat.value = currentFloat.copy(
                            cashOnHand = currentFloat.cashOnHand - amount,
                            orangeMoneyBalance = currentFloat.orangeMoneyBalance + amount
                        )
                        PaymentProvider.MOOV_MONEY -> _cashFloat.value = currentFloat.copy(
                            cashOnHand = currentFloat.cashOnHand - amount,
                            moovMoneyBalance = currentFloat.moovMoneyBalance + amount
                        )
                        PaymentProvider.WAVE -> _cashFloat.value = currentFloat.copy(
                            cashOnHand = currentFloat.cashOnHand - amount,
                            waveBalance = currentFloat.waveBalance + amount
                        )
                        PaymentProvider.ESPECES -> {}
                    }
                }
                else -> {}
            }
        }
    }

    // Checkout POS Cart
    fun checkoutCart(clientName: String, clientPhone: String, paymentProvider: PaymentProvider) {
        val currentItems = _cart.value
        if (currentItems.isEmpty()) return
        viewModelScope.launch {
            val tx = repository.processSale(currentItems, clientName, clientPhone, paymentProvider)
            _lastCompletedTransaction.value = tx
            _cart.value = emptyList()
            if (paymentProvider == PaymentProvider.ESPECES) {
                val current = _cashFloat.value
                _cashFloat.value = current.copy(cashOnHand = current.cashOnHand + tx.amount)
            }
        }
    }

    // CNI Management
    fun saveCniRecord(cni: CniEntity) {
        viewModelScope.launch {
            if (cni.id == 0L) {
                repository.insertCniRecord(cni)
            } else {
                repository.updateCniRecord(cni)
            }
        }
    }

    fun deleteCniRecord(cni: CniEntity) {
        viewModelScope.launch {
            repository.deleteCniRecord(cni)
        }
    }

    // Product Management
    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            if (product.id == 0L) {
                repository.insertProduct(product)
            } else {
                repository.updateProduct(product)
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // Customer Debts / Credit Book
    val allDebts: StateFlow<List<CustomerDebtEntity>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingDebts: StateFlow<List<CustomerDebtEntity>> = repository.pendingDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveDebt(debt: CustomerDebtEntity) {
        viewModelScope.launch {
            if (debt.id == 0L) {
                repository.insertDebt(debt)
            } else {
                repository.updateDebt(debt)
            }
        }
    }

    fun makeDebtPayment(debt: CustomerDebtEntity, amountPaidNow: Long) {
        viewModelScope.launch {
            val newPaid = debt.paidAmount + amountPaidNow
            val isNowSettled = newPaid >= debt.totalAmount
            repository.updateDebt(
                debt.copy(
                    paidAmount = newPaid,
                    isSettled = isNowSettled
                )
            )
        }
    }

    fun deleteDebt(debt: CustomerDebtEntity) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }

    // ML Kit Live OCR Processing
    private val _isOcrProcessing = MutableStateFlow(false)
    val isOcrProcessing: StateFlow<Boolean> = _isOcrProcessing.asStateFlow()

    private val _lastOcrScannedCni = MutableStateFlow<CniEntity?>(null)
    val lastOcrScannedCni: StateFlow<CniEntity?> = _lastOcrScannedCni.asStateFlow()

    private val _ocrRawText = MutableStateFlow<String?>(null)
    val ocrRawText: StateFlow<String?> = _ocrRawText.asStateFlow()

    private val _ocrErrorMessage = MutableStateFlow<String?>(null)
    val ocrErrorMessage: StateFlow<String?> = _ocrErrorMessage.asStateFlow()

    fun processCniImageUri(uri: Uri) {
        _isOcrProcessing.value = true
        _ocrErrorMessage.value = null
        viewModelScope.launch {
            val result = CniOcrService.processUriWithMlKit(getApplication(), uri)
            result.onSuccess { (entity, raw) ->
                _lastOcrScannedCni.value = entity
                _ocrRawText.value = raw
                _isOcrProcessing.value = false
            }.onFailure { err ->
                _ocrErrorMessage.value = "Erreur OCR : ${err.localizedMessage ?: "Image illisible"}"
                _isOcrProcessing.value = false
            }
        }
    }

    fun processCniBitmap(bitmap: Bitmap) {
        _isOcrProcessing.value = true
        _ocrErrorMessage.value = null
        viewModelScope.launch {
            val result = CniOcrService.processBitmapWithMlKit(bitmap)
            result.onSuccess { (entity, raw) ->
                _lastOcrScannedCni.value = entity
                _ocrRawText.value = raw
                _isOcrProcessing.value = false
            }.onFailure { err ->
                _ocrErrorMessage.value = "Erreur OCR : ${err.localizedMessage ?: "Image illisible"}"
                _isOcrProcessing.value = false
            }
        }
    }

    fun clearOcrResult() {
        _lastOcrScannedCni.value = null
        _ocrRawText.value = null
        _ocrErrorMessage.value = null
    }

    // AI Assistant State
    private val _aiMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "ai",
                text = "Ne y windiga ! Bienvenue sur l'Assistant IA Wend-Lamita. Comment puis-je vous aider pour votre caisse, la vérification CNI ou la gestion des stocks ?"
            )
        )
    )
    val aiMessages: StateFlow<List<ChatMessage>> = _aiMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    fun sendAiPrompt(prompt: String) {
        if (prompt.isBlank()) return
        val current = _aiMessages.value.toMutableList()
        current.add(ChatMessage(sender = "user", text = prompt))
        _aiMessages.value = current
        _isAiThinking.value = true

        viewModelScope.launch {
            val responseText = GeminiAiService.askAssistant(
                userPrompt = prompt,
                recentTransactions = allTransactions.value,
                lowStockProducts = lowStockProducts.value,
                registeredCnisCount = allCniRecords.value.size
            )
            val updated = _aiMessages.value.toMutableList()
            updated.add(ChatMessage(sender = "ai", text = responseText))
            _aiMessages.value = updated
            _isAiThinking.value = false
        }
    }
}
