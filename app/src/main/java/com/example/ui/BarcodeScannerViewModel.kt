package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ProductRepository
import com.example.data.ScannedProduct
import com.example.network.GeminiApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "user" or "bot"
    val text: String,
    val time: Long = System.currentTimeMillis()
)

class BarcodeScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProductRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProductRepository(database.productDao())
    }

    // Tab Selection state (0: Scanner, 1: History, 2: AI assistant Chat)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Active scanned product for display in bottom sheet/details card
    private val _activeProduct = MutableStateFlow<ScannedProduct?>(null)
    val activeProduct: StateFlow<ScannedProduct?> = _activeProduct.asStateFlow()

    // Loading indicator when API is running
    private val _isLoadingProduct = MutableStateFlow(false)
    val isLoadingProduct: StateFlow<Boolean> = _isLoadingProduct.asStateFlow()

    // Torch / Flash Toggle State
    private val _torchEnabled = MutableStateFlow(false)
    val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()

    // Global scanning active (frozen while displaying results)
    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Search query for scan history
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Chat assistance history list
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "bot",
                text = "سلام! من دستیار هوشمند بارکدخوان شما هستم. هر سوالی در مورد برچسب مواد غذایی، مشخصات محصولات اسکن شده، یا ویژگی‌های سلامت کالاها داری از من بپرس! 📱🔍"
            )
        )
    )
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // History Flow from Room Repository
    val rawHistory: StateFlow<List<ScannedProduct>> = repository.history
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered History Flow (Combined with SearchQuery)
    val filteredHistory: StateFlow<List<ScannedProduct>> = combine(rawHistory, searchQuery) { historyList, query ->
        if (query.isBlank()) {
            historyList
        } else {
            historyList.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.barcode.contains(query) ||
                it.brand.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                it.details.contains(query, ignoreCase = true) ||
                it.detailsPersian.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setTorchEnabled(enabled: Boolean) {
        _torchEnabled.value = enabled
    }

    fun setScanningState(scanning: Boolean) {
        _isScanning.value = scanning
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun dismissActiveProduct() {
        _activeProduct.value = null
        _isScanning.value = true // Re-enable scan camera
    }

    /**
     * Entry point when a barcode is scanned or entered manually
     */
    fun processBarcode(barcode: String) {
        // Freeze scanner to avoid double triggers
        _isScanning.value = false
        _isLoadingProduct.value = true
        _selectedTab.value = 0 // Auto focus on Scanner Tab if scanner processed it

        viewModelScope.launch {
            try {
                Log.d("ViewModel", "Analyzing barcode: $barcode")
                val product = GeminiApiClient.identifyProduct(barcode)
                
                // Set as active product displaying on detail sheet
                _activeProduct.value = product
                
                // Add to Room scanned database history
                repository.insertProduct(product)
                
                Log.d("ViewModel", "Saved identified product: ${product.name}")
            } catch (e: Exception) {
                Log.e("ViewModel", "Error processing barcode $barcode", e)
                val fallbackPr = ScannedProduct(
                    barcode = barcode,
                    name = "کالای آزمایشی " + barcode.takeLast(4),
                    detailsPersian = "خطا در برقراری ارتباط با سرویس ابری. لطفاً اتصال اینترنت خود را مجدد بررسی کنید.",
                    category = "Others"
                )
                _activeProduct.value = fallbackPr
                repository.insertProduct(fallbackPr)
            } finally {
                _isLoadingProduct.value = false
            }
        }
    }

    /**
     * Toggle favorite status
     */
    fun toggleProductFavorite(product: ScannedProduct) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(isFavorite = !product.isFavorite))
        }
    }

    /**
     * Delete an item from history
     */
    fun deleteProduct(product: ScannedProduct) {
        viewModelScope.launch {
            repository.deleteProductById(product.id)
            if (_activeProduct.value?.id == product.id) {
                _activeProduct.value = null
            }
        }
    }

    /**
     * Deep wipe scan database
     */
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _activeProduct.value = null
        }
    }

    /**
     * Send chat prompt to the Gemini assistant
     */
    fun sendChatMessage(messageText: String) {
        if (messageText.isBlank()) return
        
        val userMsg = ChatMessage(sender = "user", text = messageText)
        _chatHistory.value = _chatHistory.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            try {
                // Build a textual context from scanned products list to inform Gemini of past searches
                val historyCtx = rawHistory.value.take(4).joinToString("\n") { prod ->
                    "- بارکد: ${prod.barcode}، نام کالا: ${prod.name}، برند: ${prod.brand}، رده: ${prod.category}، توضیحات: ${prod.detailsPersian}"
                }
                
                val aiResponse = GeminiApiClient.chatWithAi(
                    historyContext = historyCtx,
                    userMessage = messageText
                )

                _chatHistory.value = _chatHistory.value + ChatMessage(sender = "bot", text = aiResponse)
            } catch (e: Exception) {
                _chatHistory.value = _chatHistory.value + ChatMessage(sender = "bot", text = "متاسفانه خطایی رخ داد: ${e.localizedMessage}")
            } finally {
                _isChatLoading.value = false
            }
        }
    }
}
