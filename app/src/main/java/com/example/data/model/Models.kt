package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class IdDocumentType(val label: String, val shortCode: String, val badgeText: String) {
    CNIB("CNIB (Carte Nationale)", "CNIB", "CNIB"),
    PASSEPORT("Passeport", "PASSEPORT", "Passeport"),
    CARTE_CONSULAIRE("Carte consulaire", "CONSULAIRE", "C. Consulaire"),
    CARTE_MILITAIRE("Carte militaire", "MILITAIRE", "C. Militaire"),
    CARTE_REFUGIE("Carte de réfugié", "REFUGIE", "C. Réfugié"),
    AUTRE("Autre pièce d'identité", "AUTRE", "Autre")
}

enum class TransactionType(val label: String, val shortCode: String) {
    DEPOT("Dépôt", "D"),
    RETRAIT("Retrait", "R"),
    TRANSFERT("Transfert", "T"),
    ACHAT_CREDIT("Achat Crédit / Unité", "A"),
    VENTE_PRODUIT("Vente Boutique", "V")
}

enum class PaymentProvider(val label: String, val colorHex: Long) {
    ORANGE_MONEY("Orange Money", 0xFFFF6600),
    MOOV_MONEY("Moov Money", 0xFF00875A),
    WAVE("Wave", 0xFF0066FF),
    ESPECES("Espèces / Caisse", 0xFF6B7280)
}

enum class TransactionStatus {
    SUCCESS,
    PENDING,
    FAILED
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val provider: PaymentProvider,
    val amount: Long, // in FCFA
    val fee: Long = 0, // Client fee in FCFA
    val commission: Long = 0, // Agent commission in FCFA
    val clientName: String = "",
    val clientPhone: String = "",
    val clientCniNumber: String = "",
    val idDocumentType: IdDocumentType = IdDocumentType.CNIB,
    val senderPhone: String = "",
    val receiverPhone: String = "",
    val initialOrangeBalance: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val status: TransactionStatus = TransactionStatus.SUCCESS,
    val referenceCode: String = "",
    val note: String = "",
    val agentName: String = "Agent Wend-Lamita"
)

@Entity(tableName = "cni_records")
data class CniEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idDocumentType: IdDocumentType = IdDocumentType.CNIB,
    val cniNumber: String, // e.g. B12894732, P0129482, CC-4921, etc.
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String = "",
    val placeOfBirth: String = "",
    val deliveryDate: String = "",
    val expiryDate: String = "",
    val phone: String = "",
    val nationality: String = "Burkinabè",
    val profession: String = "",
    val photoUrl: String = "",
    val scanDate: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    val fullName: String get() = "$lastName $firstName".trim()
}

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Alimentation",
    val barcode: String = "",
    val purchasePrice: Long = 0, // Prix d'achat en FCFA
    val salePrice: Long = 0,     // Prix de vente en FCFA
    val stockQuantity: Int = 0,
    val minStockAlert: Int = 5,
    val unit: String = "pièce"
)

@Entity(tableName = "customer_debts")
data class CustomerDebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val customerPhone: String = "",
    val totalAmount: Long, // in FCFA
    val paidAmount: Long = 0, // in FCFA
    val description: String = "",
    val dateCreated: Long = System.currentTimeMillis(),
    val dueDate: String = "",
    val isSettled: Boolean = false
) {
    val remainingAmount: Long get() = (totalAmount - paidAmount).coerceAtLeast(0)
}

data class CartItem(
    val product: ProductEntity,
    var quantity: Int
) {
    val totalPrice: Long get() = product.salePrice * quantity
    val totalProfit: Long get() = (product.salePrice - product.purchasePrice) * quantity
}

data class CashFloat(
    val cashOnHand: Long = 150000,
    val orangeMoneyBalance: Long = 350000,
    val moovMoneyBalance: Long = 200000,
    val waveBalance: Long = 100000
) {
    val totalAssets: Long get() = cashOnHand + orangeMoneyBalance + moovMoneyBalance + waveBalance
}

enum class UserRole(val label: String, val badgeColor: Long) {
    ADMIN("Administrateur", 0xFFE11D48),
    GERANT("Gérant de Caisse", 0xFFF59E0B),
    CAISSIER("Caissier", 0xFF10B981)
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val fullName: String,
    val pinCode: String, // 4-digit PIN e.g. "1234"
    val password: String = "1234",
    val role: UserRole = UserRole.CAISSIER,
    val isActive: Boolean = true,
    val phone: String = "",
    val lastLogin: Long = System.currentTimeMillis()
)

enum class SubscriptionPlan(val title: String, val priceMonthly: Long, val priceQuarterly: Long, val priceYearly: Long) {
    FREE("Gratuit (Essai)", 0, 0, 0),
    PRO_KIOSQUE("Pro Kiosque", 3500, 9000, 30000),
    ENTERPRISE("Entreprise Multi-Caisses", 7500, 20000, 70000)
}

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plan: SubscriptionPlan = SubscriptionPlan.PRO_KIOSQUE,
    val transactionId: String, // e.g. "OM260826.0945.A18" or "MM84920"
    val operator: PaymentProvider = PaymentProvider.ORANGE_MONEY,
    val amountPaid: Long = 3500,
    val activationDate: Long = System.currentTimeMillis(),
    val expiryDate: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L),
    val isActive: Boolean = true,
    val referenceCode: String = "",
    val activatedBy: String = "Admin Wend-Lamita"
) {
    val isExpired: Boolean get() = System.currentTimeMillis() > expiryDate
    val daysRemaining: Long get() = ((expiryDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
}

@Entity(tableName = "cash_closures")
data class CashClosureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // e.g. "26/08/2026"
    val closedAt: Long = System.currentTimeMillis(),
    val closingAgent: String = "Agent Caisse",
    val physicalCash: Long = 0,
    val theoreticalCash: Long = 0,
    val cashDifference: Long = 0, // physical - theoretical (positive = excédent, negative = manquant)
    val omBalance: Long = 0,
    val moovBalance: Long = 0,
    val waveBalance: Long = 0,
    val totalTransactionsCount: Int = 0,
    val totalVolume: Long = 0,
    val totalCommissions: Long = 0,
    val notes: String = "",
    val isApproved: Boolean = true
)

@Entity(tableName = "float_adjustments")
data class FloatAdjustmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String = "APPROVISIONNEMENT", // APPROVISIONNEMENT (Float In) or DEGUSTATION/RETRAIT (Float Out)
    val provider: PaymentProvider = PaymentProvider.ORANGE_MONEY,
    val amount: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val agentName: String = "Admin",
    val notes: String = ""
)

