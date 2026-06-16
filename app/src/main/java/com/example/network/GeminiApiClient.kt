package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.ScannedProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini REST API Data Classes ---

data class Part(
    val text: String? = null
)

data class Content(
    val parts: List<Part>
)

data class ResponseFormatText(
    val mimeType: String
)

data class ResponseFormat(
    val text: ResponseFormatText? = null
)

data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null
)

data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

data class Candidate(
    val content: Content
)

data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

// --- Retrofit Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Retrofit Client & Service implementation ---

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    /**
     * Ask Gemini to identify or generate detailed information about a product with a given barcode.
     */
    suspend fun identifyProduct(barcode: String): ScannedProduct = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is unconfigured or placeholder!")
            return@withContext getFallbackProduct(barcode, "API key is missing or not configured in Secrets panel.")
        }

        val prompt = """
            You are an expert product database. Analyze the scanned barcode number: "$barcode".
            Identify the product, brand, category, standard pricing, health grade, and detailed descriptions.
            If the barcode is realistic/known, return the actual product details. If it's a random test barcode like "12345", "1111", etc., return a simulated example product of your choice (e.g. a popular Iranian soft drink or a universal food item) so the user gets a working demonstration.
            
            Return ONE single JSON object with EXACTLY the fields below (and no Markdown format, no backticks, no wrapping):
            - "name": String (Product name. Example: "Zamzam Cola 250ml" or "Shampoo")
            - "category": String (Must be one of: "Food", "Beverage", "Electronics", "Cosmetics", "Books", "Clothing", "Others")
            - "brand": String (Brand name. Example: "Zamzam" or "Samsung")
            - "price": String (Approximate or typical price. Try to express in Iranian Rial or Toman with English equivalent in parentheses. Example: "15,000 تومان ($0.30)")
            - "details": String (English detailed description, 1-2 smooth sentences)
            - "detailsPersian": String (Beautiful specifications or information in Persian/Farsi, 2-3 detailed sentences. Example: "نوشابه گازدار زمرم با طعم کولا فاقد چربی‌های ترانس بوده...")
            - "rating": String (Rating out of 5. Example: "4.5")
            - "healthGrade": String (Standard nutrition score: "A", "B", "C", "D", "E" or "N/A" if not food)
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(text = ResponseFormatText(mimeType = "application/json")),
                temperature = 0.2f
            ),
            systemInstruction = Content(parts = listOf(Part(text = "You are a professional product scanning server. Output ONLY syntactically valid JSON. Do not include markdown codeblocks or comments.")))
        )

        try {
            val response = service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                Log.d(TAG, "Gemini RAW Response: $jsonText")
                val cleanJson = cleanJsonResponse(jsonText)
                val json = JSONObject(cleanJson)
                
                ScannedProduct(
                    barcode = barcode,
                    name = json.optString("name", "Product $barcode"),
                    category = json.optString("category", "Others"),
                    brand = json.optString("brand", "Unknown"),
                    price = json.optString("price", "N/A"),
                    details = json.optString("details", ""),
                    detailsPersian = json.optString("detailsPersian", ""),
                    rating = json.optString("rating", "N/A"),
                    healthGrade = json.optString("healthGrade", "N/A")
                )
            } else {
                Log.e(TAG, "Empty Gemini response candidates")
                getFallbackProduct(barcode, "No details received from AI service.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking Gemini:", e)
            getFallbackProduct(barcode, "Error querying AI: ${e.localizedMessage}")
        }
    }

    /**
     * Talk to the AI Assistant regarding scanned products or in general.
     */
    suspend fun chatWithAi(historyContext: String, userMessage: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "به نظر می‌رسد کلید API برای Gemini تنظیم نشده است. لطفاً آن را در بخش Secrets پنل گوگل آی استودیو ثبت کنید."
        }

        val prompt = """
            You are "ScanAI", an intelligent barcode scanner assistant.
            The user is speaking in Persian (Farsi). Always respond in clear, helpful, and friendly Persian.
            Here is the user's scan history and context:
            $historyContext
            
            The user says: "$userMessage"
            Provide a helpful response. If they ask about scanned products, or their healthy status, or comparisons, use the history.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "پاسخی دریافت نشد. دوباره تلاش کنید."
        } catch (e: Exception) {
            "خطا در ارتباط با سرور هوش مصنوعی: ${e.localizedMessage}"
        }
    }

    private fun cleanJsonResponse(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    private fun getFallbackProduct(barcode: String, errorMsg: String): ScannedProduct {
        // Create a helpful demonstration product so the app is fully functional even and indicates how to set up things
        return ScannedProduct(
            barcode = barcode,
            name = "کالای آزمایشی " + barcode.takeLast(4),
            category = "Food",
            brand = "برند آزمایشی",
            price = "۲۵,۰۰۰ تومان",
            details = "Barcode identified locally. $errorMsg",
            detailsPersian = "کالای شناسایی شده به صورت آفلاین. علت: $errorMsg. برای فعالسازی جستجوی پیشرفته، کلید Gemini API را در پنل Secrets اضافه کنید.",
            rating = "۴.۰",
            healthGrade = "B"
        )
    }
}
