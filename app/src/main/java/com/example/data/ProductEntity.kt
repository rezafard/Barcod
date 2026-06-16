package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_products")
data class ScannedProduct(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String,
    val name: String,
    val category: String = "Others",
    val brand: String = "",
    val price: String = "",
    val details: String = "",
    val detailsPersian: String = "",
    val rating: String = "N/A",
    val healthGrade: String = "", // e.g. A, B, C, D, E or "N/A"
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
