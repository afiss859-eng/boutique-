package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CniEntity::class,
        ProductEntity::class,
        CustomerDebtEntity::class,
        UserEntity::class,
        SubscriptionEntity::class,
        CashClosureEntity::class,
        FloatAdjustmentEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun cniDao(): CniDao
    abstract fun productDao(): ProductDao
    abstract fun debtDao(): DebtDao
    abstract fun userDao(): UserDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun cashClosureDao(): CashClosureDao
    abstract fun floatAdjustmentDao(): FloatAdjustmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wend_lamita_db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val productDao = database.productDao()
            val cniDao = database.cniDao()
            val transactionDao = database.transactionDao()

            // Initial Products
            val sampleProducts = listOf(
                ProductEntity(name = "Carte Recharge Orange 1000F", category = "Télécom", barcode = "6041001", purchasePrice = 960, salePrice = 1000, stockQuantity = 45, minStockAlert = 10, unit = "carte"),
                ProductEntity(name = "Carte Recharge Moov 1000F", category = "Télécom", barcode = "6041002", purchasePrice = 965, salePrice = 1000, stockQuantity = 30, minStockAlert = 10, unit = "carte"),
                ProductEntity(name = "Carte Recharge Orange 500F", category = "Télécom", barcode = "6041003", purchasePrice = 480, salePrice = 500, stockQuantity = 50, minStockAlert = 15, unit = "carte"),
                ProductEntity(name = "Pack Eau Minérale Lafi (6x1.5L)", category = "Boissons", barcode = "6042001", purchasePrice = 1800, salePrice = 2200, stockQuantity = 18, minStockAlert = 5, unit = "pack"),
                ProductEntity(name = "Canette Coca-Cola 33cl", category = "Boissons", barcode = "6042002", purchasePrice = 375, salePrice = 500, stockQuantity = 32, minStockAlert = 8, unit = "canette"),
                ProductEntity(name = "Canette Youki Pomme 33cl", category = "Boissons", barcode = "6042003", purchasePrice = 350, salePrice = 500, stockQuantity = 24, minStockAlert = 6, unit = "canette"),
                ProductEntity(name = "Riz Parfumé 5kg (Dinan)", category = "Alimentation", barcode = "6043001", purchasePrice = 3200, salePrice = 3800, stockQuantity = 12, minStockAlert = 4, unit = "sac"),
                ProductEntity(name = "Huile Végétale Mayol (1L)", category = "Alimentation", barcode = "6043002", purchasePrice = 1050, salePrice = 1300, stockQuantity = 15, minStockAlert = 5, unit = "bouteille"),
                ProductEntity(name = "Sucre Morceaux SN SOSUCO 1kg", category = "Alimentation", barcode = "6043003", purchasePrice = 750, salePrice = 900, stockQuantity = 25, minStockAlert = 8, unit = "boîte"),
                ProductEntity(name = "Savon BF 180g", category = "Hygiène", barcode = "6044001", purchasePrice = 220, salePrice = 300, stockQuantity = 40, minStockAlert = 10, unit = "morceau"),
                ProductEntity(name = "Piles R6 Energizer (Lot 4)", category = "Accessoires", barcode = "6045001", purchasePrice = 1200, salePrice = 1600, stockQuantity = 8, minStockAlert = 3, unit = "lot")
            )
            sampleProducts.forEach { productDao.insertProduct(it) }

            // Initial CNI Records
            val sampleCnis = listOf(
                CniEntity(
                    cniNumber = "B11849204",
                    firstName = "Aminata",
                    lastName = "OUEDRAOGO",
                    dateOfBirth = "14/08/1993",
                    placeOfBirth = "Ouagadougou",
                    deliveryDate = "10/02/2021",
                    expiryDate = "10/02/2031",
                    phone = "70123456",
                    nationality = "Burkinabè",
                    profession = "Commerçante"
                ),
                CniEntity(
                    cniNumber = "B09341285",
                    firstName = "Moussa",
                    lastName = "SAWADOGO",
                    dateOfBirth = "05/11/1988",
                    placeOfBirth = "Koudougou",
                    deliveryDate = "18/06/2019",
                    expiryDate = "18/06/2029",
                    phone = "76543210",
                    nationality = "Burkinabè",
                    profession = "Enseignant"
                ),
                CniEntity(
                    cniNumber = "B14920193",
                    firstName = "Fatoumata",
                    lastName = "KABORE",
                    dateOfBirth = "22/03/1997",
                    placeOfBirth = "Bobo-Dioulasso",
                    deliveryDate = "04/09/2022",
                    expiryDate = "04/09/2032",
                    phone = "64112233",
                    nationality = "Burkinabè",
                    profession = "Étudiante"
                )
            )
            sampleCnis.forEach { cniDao.insertCniRecord(it) }

            // Initial Sample Transactions
            val sampleTransactions = listOf(
                TransactionEntity(
                    type = TransactionType.DEPOT,
                    provider = PaymentProvider.ORANGE_MONEY,
                    amount = 25000,
                    fee = 0,
                    commission = 250,
                    clientName = "Aminata OUEDRAOGO",
                    clientPhone = "70123456",
                    clientCniNumber = "B11849204",
                    status = TransactionStatus.SUCCESS,
                    referenceCode = "OM260825.1042.A49",
                    note = "Dépôt d'argent client"
                ),
                TransactionEntity(
                    type = TransactionType.RETRAIT,
                    provider = PaymentProvider.ORANGE_MONEY,
                    amount = 15000,
                    fee = 350,
                    commission = 175,
                    clientName = "Moussa SAWADOGO",
                    clientPhone = "76543210",
                    clientCniNumber = "B09341285",
                    status = TransactionStatus.SUCCESS,
                    referenceCode = "OM260825.1118.B12",
                    note = "Retrait espèces OM"
                ),
                TransactionEntity(
                    type = TransactionType.DEPOT,
                    provider = PaymentProvider.MOOV_MONEY,
                    amount = 10000,
                    fee = 0,
                    commission = 120,
                    clientName = "Fatoumata KABORE",
                    clientPhone = "64112233",
                    clientCniNumber = "B14920193",
                    status = TransactionStatus.SUCCESS,
                    referenceCode = "MM260825.1205.C88",
                    note = "Dépôt Moov Money"
                )
            )
            sampleTransactions.forEach { transactionDao.insertTransaction(it) }

            // Initial Sample Debts
            val debtDao = database.debtDao()
            val sampleDebts = listOf(
                CustomerDebtEntity(
                    customerName = "Issa COMPAORE",
                    customerPhone = "78129034",
                    totalAmount = 7500,
                    paidAmount = 2500,
                    description = "2 packs eau Lafi + 1 carte OM 1000",
                    dueDate = "Fin de mois",
                    isSettled = false
                ),
                CustomerDebtEntity(
                    customerName = "Salif DIALLO",
                    customerPhone = "70554433",
                    totalAmount = 3000,
                    paidAmount = 0,
                    description = "Recharge crédit Moov 3000F",
                    dueDate = "Samedi",
                    isSettled = false
                )
            )
            sampleDebts.forEach { debtDao.insertDebt(it) }

            // Initial Users
            val userDao = database.userDao()
            val sampleUsers = listOf(
                UserEntity(
                    username = "admin",
                    fullName = "Sawadogo Afis (Administrateur)",
                    pinCode = "1234",
                    password = "admin",
                    role = UserRole.ADMIN,
                    phone = "70000001"
                ),
                UserEntity(
                    username = "gerant",
                    fullName = "Ouedraogo Rachid (Gérant)",
                    pinCode = "2222",
                    password = "gerant",
                    role = UserRole.GERANT,
                    phone = "76000002"
                ),
                UserEntity(
                    username = "caissier1",
                    fullName = "Aminata KABORE (Caissière)",
                    pinCode = "0000",
                    password = "0000",
                    role = UserRole.CAISSIER,
                    phone = "78000003"
                )
            )
            sampleUsers.forEach { userDao.insertUser(it) }

            // Initial Pro Subscription
            val subscriptionDao = database.subscriptionDao()
            val initialSub = SubscriptionEntity(
                plan = SubscriptionPlan.PRO_KIOSQUE,
                transactionId = "OM260826.0945.A18",
                operator = PaymentProvider.ORANGE_MONEY,
                amountPaid = 3500,
                activationDate = System.currentTimeMillis() - (2L * 24 * 60 * 60 * 1000L),
                expiryDate = System.currentTimeMillis() + (28L * 24 * 60 * 60 * 1000L),
                isActive = true,
                referenceCode = "WL-PRO-2026-849",
                activatedBy = "Sawadogo Afis"
            )
            subscriptionDao.insertSubscription(initialSub)

            // Initial Cash Closure
            val closureDao = database.cashClosureDao()
            val sampleClosure = CashClosureEntity(
                dateString = "25/08/2026",
                closedAt = System.currentTimeMillis() - (24 * 60 * 60 * 1000L),
                closingAgent = "Aminata KABORE",
                physicalCash = 150000,
                theoreticalCash = 150000,
                cashDifference = 0,
                omBalance = 350000,
                moovBalance = 200000,
                waveBalance = 100000,
                totalTransactionsCount = 3,
                totalVolume = 50000,
                totalCommissions = 545,
                notes = "Clôture conforme, aucun écart de caisse constaté."
            )
            closureDao.insertClosure(sampleClosure)
        }
    }
}
