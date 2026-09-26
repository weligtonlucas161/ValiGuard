package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ProductCatalog
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductCatalogDao {
    @Query("SELECT * FROM product_catalog WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductCatalog?

    @Query("SELECT * FROM product_catalog WHERE barcode = :barcode LIMIT 1")
    fun getByBarcodeFlow(barcode: String): Flow<ProductCatalog?>

    @Query("SELECT * FROM product_catalog ORDER BY lastUpdated DESC")
    fun getAllCatalogItems(): Flow<List<ProductCatalog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(catalogItem: ProductCatalog)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(catalogItems: List<ProductCatalog>)

    @Query("DELETE FROM product_catalog WHERE barcode = :barcode")
    suspend fun deleteByBarcode(barcode: String)
}
