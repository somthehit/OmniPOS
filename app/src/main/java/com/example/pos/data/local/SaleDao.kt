package com.example.pos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Sale DAO providing CRUD operations strictly scoped by org_id for multi-tenant data isolation.
 */
@Dao
interface SaleDao {

    // --- Create ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalePayments(payments: List<SalePaymentEntity>)

    // --- Read (strictly scoped by org_id) ---
    @Query("SELECT * FROM sales WHERE org_id = :orgId ORDER BY created_at DESC")
    fun getSales(orgId: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE org_id = :orgId AND id = :saleId LIMIT 1")
    suspend fun getSaleById(orgId: String, saleId: String): SaleEntity?

    @Query("SELECT * FROM sales WHERE org_id = :orgId AND register_id = :registerId ORDER BY created_at DESC")
    fun getSalesByRegister(orgId: String, registerId: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE org_id = :orgId AND created_at >= :startTime AND created_at <= :endTime ORDER BY created_at DESC")
    fun getSalesByDateRange(orgId: String, startTime: Long, endTime: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sale_items WHERE org_id = :orgId AND sale_id = :saleId")
    suspend fun getSaleItems(orgId: String, saleId: String): List<SaleItemEntity>

    @Query("SELECT * FROM sale_payments WHERE org_id = :orgId AND sale_id = :saleId")
    suspend fun getSalePayments(orgId: String, saleId: String): List<SalePaymentEntity>

    // --- Update (strictly scoped by org_id) ---
    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Query("UPDATE sales SET payment_status = :status WHERE org_id = :orgId AND id = :saleId")
    suspend fun updateSaleStatus(orgId: String, saleId: String, status: String)

    // --- Delete (strictly scoped by org_id) ---
    @Query("DELETE FROM sales WHERE org_id = :orgId AND id = :saleId")
    suspend fun deleteSale(orgId: String, saleId: String)

    @Query("DELETE FROM sale_items WHERE org_id = :orgId AND sale_id = :saleId")
    suspend fun deleteSaleItems(orgId: String, saleId: String)

    @Query("DELETE FROM sale_payments WHERE org_id = :orgId AND sale_id = :saleId")
    suspend fun deleteSalePayments(orgId: String, saleId: String)

    @Query("DELETE FROM sales WHERE org_id = :orgId")
    suspend fun deleteAllSales(orgId: String)
}
