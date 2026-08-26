package com.example.data.repository

import com.example.data.local.CashClosureDao
import com.example.data.local.CniDao
import com.example.data.local.DebtDao
import com.example.data.local.FloatAdjustmentDao
import com.example.data.local.ProductDao
import com.example.data.local.SubscriptionDao
import com.example.data.local.TransactionDao
import com.example.data.local.UserDao
import com.example.data.model.CartItem
import com.example.data.model.CashClosureEntity
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
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WendLamitaRepository(
    private val transactionDao: TransactionDao,
    private val cniDao: CniDao,
    private val productDao: ProductDao,
    private val debtDao: DebtDao,
    private val userDao: UserDao,
    private val subscriptionDao: SubscriptionDao,
    private val cashClosureDao: CashClosureDao,
    private val floatAdjustmentDao: FloatAdjustmentDao
) {
    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
        transactionDao.searchTransactions(query)

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    // CNI / KYC Records
    val allCniRecords: Flow<List<CniEntity>> = cniDao.getAllCniRecords()

    fun searchCniRecords(query: String): Flow<List<CniEntity>> =
        cniDao.searchCniRecords(query)

    suspend fun findCniByNumber(cniNumber: String): CniEntity? =
        cniDao.findByCniNumber(cniNumber)

    suspend fun insertCniRecord(cni: CniEntity): Long =
        cniDao.insertCniRecord(cni)

    suspend fun updateCniRecord(cni: CniEntity) =
        cniDao.updateCniRecord(cni)

    suspend fun deleteCniRecord(cni: CniEntity) =
        cniDao.deleteCniRecord(cni)

    // Products / Stock
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<ProductEntity>> = productDao.getLowStockProducts()

    fun searchProducts(query: String): Flow<List<ProductEntity>> =
        productDao.searchProducts(query)

    suspend fun insertProduct(product: ProductEntity): Long =
        productDao.insertProduct(product)

    suspend fun updateProduct(product: ProductEntity) =
        productDao.updateProduct(product)

    suspend fun deleteProduct(product: ProductEntity) =
        productDao.deleteProduct(product)

    // Customer Debts & Credit Book
    val allDebts: Flow<List<CustomerDebtEntity>> = debtDao.getAllDebts()
    val pendingDebts: Flow<List<CustomerDebtEntity>> = debtDao.getPendingDebts()

    suspend fun insertDebt(debt: CustomerDebtEntity): Long =
        debtDao.insertDebt(debt)

    suspend fun updateDebt(debt: CustomerDebtEntity) =
        debtDao.updateDebt(debt)

    suspend fun deleteDebt(debt: CustomerDebtEntity) =
        debtDao.deleteDebt(debt)

    // Users & Roles
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()

    suspend fun getUserByPin(pin: String): UserEntity? =
        userDao.getUserByPin(pin)

    suspend fun getUserByUsername(username: String): UserEntity? =
        userDao.getUserByUsername(username)

    suspend fun insertUser(user: UserEntity): Long =
        userDao.insertUser(user)

    suspend fun updateUser(user: UserEntity) =
        userDao.updateUser(user)

    suspend fun deleteUser(user: UserEntity) =
        userDao.deleteUser(user)

    // Subscriptions
    val allSubscriptions: Flow<List<SubscriptionEntity>> = subscriptionDao.getAllSubscriptions()
    val activeSubscription: Flow<SubscriptionEntity?> = subscriptionDao.getActiveSubscription()

    suspend fun activateSubscriptionWithTxId(
        plan: SubscriptionPlan,
        transactionId: String,
        operator: PaymentProvider,
        amountPaid: Long,
        durationDays: Int = 30,
        activatedBy: String = "Admin"
    ): Long {
        val now = System.currentTimeMillis()
        val expiry = now + (durationDays.toLong() * 24 * 60 * 60 * 1000L)
        val entity = SubscriptionEntity(
            plan = plan,
            transactionId = transactionId.trim().uppercase(),
            operator = operator,
            amountPaid = amountPaid,
            activationDate = now,
            expiryDate = expiry,
            isActive = true,
            referenceCode = "WL-SUB-${SimpleDateFormat("yyMMdd-HHmm", Locale.getDefault()).format(Date())}",
            activatedBy = activatedBy
        )
        return subscriptionDao.insertSubscription(entity)
    }

    // Cash Closures (Rapport Z)
    val allClosures: Flow<List<CashClosureEntity>> = cashClosureDao.getAllClosures()

    suspend fun insertClosure(closure: CashClosureEntity): Long =
        cashClosureDao.insertClosure(closure)

    // Float Adjustments
    val allFloatAdjustments: Flow<List<FloatAdjustmentEntity>> = floatAdjustmentDao.getAllAdjustments()

    suspend fun insertFloatAdjustment(adjustment: FloatAdjustmentEntity): Long =
        floatAdjustmentDao.insertAdjustment(adjustment)

    // POS Checkout / Sale Transaction
    suspend fun processSale(
        cartItems: List<CartItem>,
        clientName: String = "Client Comptoir",
        clientPhone: String = "",
        paymentProvider: PaymentProvider = PaymentProvider.ESPECES
    ): TransactionEntity {
        val totalAmount = cartItems.sumOf { it.totalPrice }
        val totalProfit = cartItems.sumOf { it.totalProfit }
        val summaryNote = cartItems.joinToString(", ") { "${it.quantity}x ${it.product.name}" }

        // Deduct stocks
        cartItems.forEach { item ->
            productDao.deductStock(item.product.id, item.quantity)
        }

        val refCode = "VTE-" + SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault()).format(Date())
        val transaction = TransactionEntity(
            type = TransactionType.VENTE_PRODUIT,
            provider = paymentProvider,
            amount = totalAmount,
            fee = 0,
            commission = totalProfit,
            clientName = clientName.ifBlank { "Client Comptoir" },
            clientPhone = clientPhone,
            status = TransactionStatus.SUCCESS,
            referenceCode = refCode,
            note = summaryNote
        )

        val id = transactionDao.insertTransaction(transaction)
        return transaction.copy(id = id)
    }
}

