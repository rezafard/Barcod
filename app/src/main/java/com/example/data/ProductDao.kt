package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM scanned_products ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<ScannedProduct>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ScannedProduct): Long

    @Update
    suspend fun updateProduct(product: ScannedProduct)

    @Query("DELETE FROM scanned_products WHERE id = :id")
    suspend fun deleteProductById(id: Int)

    @Query("DELETE FROM scanned_products")
    suspend fun clearHistory()
}
