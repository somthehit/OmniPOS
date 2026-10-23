package com.example.pos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Product DAO providing CRUD operations strictly scoped by org_id for multi-tenant data isolation.
 */
@Dao
interface ProductDao {

    // --- Create ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    // --- Read (strictly scoped by org_id) ---
    @Query("SELECT * FROM products WHERE org_id = :orgId ORDER BY name ASC")
    fun getProducts(orgId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE org_id = :orgId AND id = :productId LIMIT 1")
    suspend fun getProductById(orgId: String, productId: String): ProductEntity?

    @Query("SELECT * FROM products WHERE org_id = :orgId AND category_id = :categoryId ORDER BY name ASC")
    fun getProductsByCategory(orgId: String, categoryId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE org_id = :orgId AND (barcode = :query OR sku = :query OR name LIKE '%' || :query || '%')")
    fun searchProducts(orgId: String, query: String): Flow<List<ProductEntity>>

    @Query("SELECT COALESCE(SUM(quantity_delta), 0) FROM stock_movements WHERE org_id = :orgId AND product_id = :productId AND branch_id = :branchId")
    fun observeStockQuantity(orgId: String, productId: String, branchId: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(quantity_delta), 0) FROM stock_movements WHERE org_id = :orgId AND product_id = :productId AND branch_id = :branchId")
    suspend fun getStockQuantity(orgId: String, productId: String, branchId: String): Int

    // --- Update (strictly scoped by org_id) ---
    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET name = :name, selling_price_in_cents = :sellingPriceInCents, cost_price_in_cents = :costPriceInCents WHERE org_id = :orgId AND id = :productId")
    suspend fun updateProductPrice(
        orgId: String,
        productId: String,
        name: String,
        sellingPriceInCents: Long,
        costPriceInCents: Long
    )

    @Query("UPDATE products SET min_stock_alert = :minStockAlert WHERE org_id = :orgId AND id = :productId")
    suspend fun updateProductStockAlert(orgId: String, productId: String, minStockAlert: Int)

    // --- Delete (strictly scoped by org_id) ---
    @Query("DELETE FROM products WHERE org_id = :orgId AND id = :productId")
    suspend fun deleteProduct(orgId: String, productId: String)

    @Query("DELETE FROM products WHERE org_id = :orgId")
    suspend fun deleteAllProducts(orgId: String)
}
