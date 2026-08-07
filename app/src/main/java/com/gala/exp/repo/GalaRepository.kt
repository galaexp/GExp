package com.gala.exp.repo

import android.content.Context
import com.gala.exp.api.*
import com.gala.exp.db.ArticleEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class StoreMatched(
    val storeCode: String,
    val storeName: String,
    val passwordPass: String
)

class GalaRepository(context: Context) {

    private var inMemoryArticles: List<ArticleEntity> = emptyList()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (com.gala.exp.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://expiry-api.gala-it1.workers.dev/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(okHttpClient)
        .build()
        .create(GalaExpiryApi::class.java)

    suspend fun authenticateStore(code: String, pass: String): StoreMatched = withContext(Dispatchers.IO) {
        val uppercaseCode = code.trim().uppercase()
        val response = api.storeAuth()
        if (!response.isSuccessful) {
            throw Exception("Network error: Code ${response.code()}")
        }

        val stores = response.body() ?: throw Exception("Failed to retrieve store directory.")
        var matchedStore: StoreMatched? = null

        for (item in stores) {
            matchedStore = parseStoreItem(item, uppercaseCode)
            if (matchedStore != null) {
                break
            }
        }

        if (matchedStore == null) {
            throw Exception("Store Code '$uppercaseCode' not found.")
        }

        if (matchedStore.passwordPass != pass.trim()) {
            throw Exception("Incorrect password.")
        }

        matchedStore
    }

    private fun parseStoreItem(item: Any, targetCode: String): StoreMatched? {
        // Option A: If parsed as Map (key-value)
        if (item is Map<*, *>) {
            val code = (item["storeCode"] as? String ?: item["0"] as? String ?: "").trim().uppercase()
            if (code == targetCode) {
                val name = (item["storeName"] as? String ?: item["1"] as? String ?: "").trim()
                val password = (item["password"] as? String ?: item["2"] as? String ?: "").trim()
                return StoreMatched(code, name, password)
            }
        }
        // Option B: If parsed as List/Array
        if (item is List<*>) {
            val code = (item.getOrNull(0) as? String ?: "").trim().uppercase()
            if (code == targetCode) {
                val name = (item.getOrNull(1) as? String ?: "").trim()
                val password = (item.getOrNull(2) as? String ?: "").trim()
                return StoreMatched(code, name, password)
            }
        }
        return null
    }

    // Returns locally cached articles, triggers silent background network refresh
    suspend fun getCachedArticles(): List<ArticleEntity> = withContext(Dispatchers.IO) {
        inMemoryArticles
    }

    suspend fun fetchAndCacheArticles() = withContext(Dispatchers.IO) {
        try {
            val response = api.getArticles()
            if (response.isSuccessful) {
                val articlesDto = response.body() ?: emptyList()
                if (articlesDto.isNotEmpty()) {
                    inMemoryArticles = articlesDto.map {
                        ArticleEntity(
                            article = (it.article ?: "").trim(),
                            barcode = (it.barcode ?: "").trim(),
                            description = cleanDescription((it.description ?: "").trim()) ?: "",
                            department = (it.department ?: "").trim()
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getArticleByCodeLocal(articleCode: String): ArticleEntity? = withContext(Dispatchers.IO) {
        inMemoryArticles.firstOrNull { it.article.trim() == articleCode.trim() }
    }

    suspend fun getArticleByBarcodeLocal(barcode: String): ArticleEntity? = withContext(Dispatchers.IO) {
        inMemoryArticles.firstOrNull { it.barcode.trim() == barcode.trim() }
    }

    suspend fun getLastWeekData(storeCode: String, week: String, month: String): List<LastWeekRowDto> = withContext(Dispatchers.IO) {
        val response = api.lastWeekData(storeCode = storeCode, week = week, submissionMonth = month)
        if (response.isSuccessful) {
            response.body()?.rows?.map { row ->
                row.copy(Description = cleanDescription(row.Description))
            } ?: emptyList()
        } else {
            throw Exception("Failed to load last week data.")
        }
    }

    suspend fun submitFollowUp(request: FollowUpRequest): SuccessResponse = withContext(Dispatchers.IO) {
        val response = api.followUpUpdate(request = request)
        if (response.isSuccessful) {
            response.body() ?: SuccessResponse(true, "Saved successfully.")
        } else {
            throw Exception("Server failed to update follow up stock: Code ${response.code()}")
        }
    }

    suspend fun getThisWeekReview(storeCode: String, week: String, month: String): List<ThisWeekRowDto> = withContext(Dispatchers.IO) {
        val response = api.thisWeekReview(storeCode = storeCode, week = week, submissionMonth = month)
        if (response.isSuccessful) {
            response.body()?.rows?.map { row ->
                row.copy(Description = cleanDescription(row.Description))
            } ?: emptyList()
        } else {
            throw Exception("Failed to fetch review rows.")
        }
    }

    suspend fun deleteRow(rowIndex: Int): SuccessResponse = withContext(Dispatchers.IO) {
        val response = api.deleteRow(rowIndex = rowIndex)
        if (response.isSuccessful) {
            response.body() ?: SuccessResponse(true, "Row deleted.")
        } else {
            throw Exception("Server failed to delete Row: Code ${response.code()}")
        }
    }

    suspend fun editRow(rowIndex: Int, stock: String, expiry: String): SuccessResponse = withContext(Dispatchers.IO) {
        val response = api.editRow(rowIndex = rowIndex, stock = stock, expiry = expiry)
        if (response.isSuccessful) {
            response.body() ?: SuccessResponse(true, "Row edited.")
        } else {
            throw Exception("Server failed to edit Row: Code ${response.code()}")
        }
    }

    suspend fun submitItemExpiry(request: AddExpiryRequest): SuccessResponse = withContext(Dispatchers.IO) {
        val response = api.addExpiry(request = request)
        if (response.isSuccessful) {
            response.body() ?: SuccessResponse(true, "Add successful.")
        } else {
            throw Exception("Server failed to add near-expiry item: Code ${response.code()}")
        }
    }

    suspend fun sendRTCEmail(storeCode: String, storeName: String, week: String, month: String): SuccessResponse = withContext(Dispatchers.IO) {
        val response = api.sendRTCEmail(storeCode = storeCode, storeName = storeName, week = week, submissionMonth = month)
        if (response.isSuccessful) {
            response.body() ?: SuccessResponse(true, "Email sent successfully.")
        } else {
            throw Exception("Server failed to send RTC Email: Code ${response.code()}")
        }
    }

    suspend fun getThisStoreData(storeCode: String): List<StoreDataRowDto> = withContext(Dispatchers.IO) {
        val response = api.thisStoreData(storeCode = storeCode)
        if (response.isSuccessful) {
            response.body()?.rows?.map { row ->
                row.copy(Description = cleanDescription(row.Description))
            } ?: emptyList()
        } else {
            throw Exception("Failed to load store dashboard data.")
        }
    }

    suspend fun checkVersion(): VersionInfoResponse = withContext(Dispatchers.IO) {
        val response = api.checkVersion()
        if (response.isSuccessful) {
            response.body() ?: VersionInfoResponse()
        } else {
            throw Exception("Failed to check app version.")
        }
    }

    private fun cleanDescription(desc: String?): String? {
        if (desc == null) return null
        return desc
            .replace("Bahc?van", "Bahçıvan")
            .replace("Bahç?van", "Bahçıvan")
            .replace("Bahc\uFFFDvan", "Bahçıvan")
            .replace("Bahç\uFFFDvan", "Bahçıvan")
            .replace("Bahcıvan", "Bahçıvan")
            .replace("BAHC?VAN", "BAHÇIVAN")
            .replace("BAHCVAN", "BAHÇIVAN")
            .replace("BAHCIvAN", "BAHÇIVAN")
            .replace("BAHCIVAN", "BAHÇIVAN")
    }
}
