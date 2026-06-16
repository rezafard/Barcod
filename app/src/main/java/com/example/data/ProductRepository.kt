package com.example.data

import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {
    val history: Flow<List<ScannedProduct>> = productDao.getHistory()

    suspend fun insertProduct(product: ScannedProduct): Long {
        return productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ScannedProduct) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProductById(id: Int) {
        productDao.deleteProductById(id)
    }

    suspend fun clearHistory() {
        productDao.clearHistory()
    }
}
