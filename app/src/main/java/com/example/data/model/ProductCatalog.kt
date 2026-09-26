package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Catálogo local de produtos vinculados a códigos de barras.
 * Quando um produto é bipado pelo código de barras, busca automaticamente
 * o nome do produto, o código interno do produto, o setor, a unidade e o preço.
 */
@Entity(tableName = "product_catalog")
data class ProductCatalog(
    @PrimaryKey
    val barcode: String,
    val name: String,
    val internalCode: String = "",
    val category: String = "Laticínios",
    val unit: String = "un",
    val regularPrice: Double = 0.0,
    val defaultLocation: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)
