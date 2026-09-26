package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ProductHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductHistoryDao {
    @Query("SELECT * FROM product_history WHERE productId = :productId ORDER BY timestamp DESC")
    fun getHistoryForProduct(productId: Long): Flow<List<ProductHistory>>

    @Query("SELECT * FROM product_history ORDER BY timestamp DESC LIMIT :limit")
    fun getAllRecentHistory(limit: Int = 100): Flow<List<ProductHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ProductHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistories(histories: List<ProductHistory>): List<Long>
}
