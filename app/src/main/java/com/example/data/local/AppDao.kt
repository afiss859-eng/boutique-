package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CniEntity
import com.example.data.model.ProductEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getTransactionsSince(startTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE clientPhone LIKE '%' || :query || '%' OR clientName LIKE '%' || :query || '%' OR referenceCode LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface CniDao {
    @Query("SELECT * FROM cni_records ORDER BY scanDate DESC")
    fun getAllCniRecords(): Flow<List<CniEntity>>

    @Query("SELECT * FROM cni_records WHERE cniNumber LIKE '%' || :query || '%' OR firstName LIKE '%' || :query || '%' OR lastName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY scanDate DESC")
    fun searchCniRecords(query: String): Flow<List<CniEntity>>

    @Query("SELECT * FROM cni_records WHERE cniNumber = :cniNumber LIMIT 1")
    suspend fun findByCniNumber(cniNumber: String): CniEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCniRecord(cni: CniEntity): Long

    @Update
    suspend fun updateCniRecord(cni: CniEntity)

    @Delete
    suspend fun deleteCniRecord(cni: CniEntity)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stockQuantity <= minStockAlert ORDER BY stockQuantity ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR barcode = :query OR category LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET stockQuantity = stockQuantity - :quantity WHERE id = :productId AND stockQuantity >= :quantity")
    suspend fun deductStock(productId: Long, quantity: Int): Int
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM customer_debts ORDER BY isSettled ASC, dateCreated DESC")
    fun getAllDebts(): Flow<List<com.example.data.model.CustomerDebtEntity>>

    @Query("SELECT * FROM customer_debts WHERE isSettled = 0 ORDER BY dateCreated DESC")
    fun getPendingDebts(): Flow<List<com.example.data.model.CustomerDebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: com.example.data.model.CustomerDebtEntity): Long

    @Update
    suspend fun updateDebt(debt: com.example.data.model.CustomerDebtEntity)

    @Delete
    suspend fun deleteDebt(debt: com.example.data.model.CustomerDebtEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<com.example.data.model.UserEntity>>

    @Query("SELECT * FROM users WHERE pinCode = :pin AND isActive = 1 LIMIT 1")
    suspend fun getUserByPin(pin: String): com.example.data.model.UserEntity?

    @Query("SELECT * FROM users WHERE username = :username AND isActive = 1 LIMIT 1")
    suspend fun getUserByUsername(username: String): com.example.data.model.UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: com.example.data.model.UserEntity): Long

    @Update
    suspend fun updateUser(user: com.example.data.model.UserEntity)

    @Delete
    suspend fun deleteUser(user: com.example.data.model.UserEntity)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY activationDate DESC")
    fun getAllSubscriptions(): Flow<List<com.example.data.model.SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE isActive = 1 ORDER BY expiryDate DESC LIMIT 1")
    fun getActiveSubscription(): Flow<com.example.data.model.SubscriptionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: com.example.data.model.SubscriptionEntity): Long

    @Update
    suspend fun updateSubscription(subscription: com.example.data.model.SubscriptionEntity)
}

@Dao
interface CashClosureDao {
    @Query("SELECT * FROM cash_closures ORDER BY closedAt DESC")
    fun getAllClosures(): Flow<List<com.example.data.model.CashClosureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClosure(closure: com.example.data.model.CashClosureEntity): Long
}

@Dao
interface FloatAdjustmentDao {
    @Query("SELECT * FROM float_adjustments ORDER BY timestamp DESC")
    fun getAllAdjustments(): Flow<List<com.example.data.model.FloatAdjustmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adjustment: com.example.data.model.FloatAdjustmentEntity): Long
}

