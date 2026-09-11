package com.gala.exp.repo

import android.content.Context
import com.gala.exp.api.*
import com.gala.exp.db.ArticleEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

data class StoreMatched(
    val storeCode: String,
    val storeName: String,
    val passwordPass: String,
    val staffs: String = ""
)

class GalaRepository(context: Context) {

    private val prefs = context.getSharedPreferences("GalaExpPrefs", Context.MODE_PRIVATE)

    @Volatile
    private var currentStoreCode: String = prefs.getString("storeCode", "") ?: ""

    @Volatile
    private var currentPassword: String = prefs.getString("password", "") ?: ""

    fun setCredentials(storeCode: String, password: String) {
        currentStoreCode = storeCode.trim().uppercase()
        currentPassword = password.trim()
    }

    fun clearCredentials() {
        currentStoreCode = ""
        currentPassword = ""
    }

    private var inMemoryArticles: List<ArticleEntity> = emptyList()
    private val articlesFile = File(context.filesDir, "cached_articles.json")

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val articlesListType = Types.newParameterizedType(List::class.java, ArticleEntity::class.java)
    private val articlesAdapter = moshi.adapter<List<ArticleEntity>>(articlesListType)
    private val articlesDtoListType = Types.newParameterizedType(List::class.java, ArticleDto::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val originalUrl = original.url

            val code = currentStoreCode.trim().uppercase()
            val pass = currentPassword.trim()

            val action = originalUrl.queryParameter("action")
            val newUrlBuilder = originalUrl.newBuilder()

            if (action != "version" && action != "status" && action != "storeAuth") {
                if (code.isNotBlank() && originalUrl.queryParameter("storeCode") == null) {
                    newUrlBuilder.addQueryParameter("storeCode", code)
                }
                if (pass.isNotBlank() && originalUrl.queryParameter("password") == null) {
                    newUrlBuilder.addQueryParameter("password", pass)
                }
            }

            val requestBuilder = original.newBuilder().url(newUrlBuilder.build())
            chain.proceed(requestBuilder.build())
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (com.gala.exp.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://exp.galamarkets.workers.dev/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(okHttpClient)
        .build()
        .create(GalaExpiryApi::class.java)

    suspend fun authenticateStore(code: String, pass: String): StoreMatched = withContext(Dispatchers.IO) {
        val uppercaseCode = code.trim().uppercase()
        val trimmedPass = pass.trim()

        // Try POST first
        var response = try {
            api.storeAuth(request = StoreAuthRequest(storeCode = uppercaseCode, password = trimmedPass))
        } catch (e: Exception) {
            null
        }

        // If POST fails with 404 or 405 (older worker version), fallback to GET
        if (response == null || (!response.isSuccessful && (response.code() == 404 || response.code() == 405))) {
            try {
                val getResp = api.storeAuthGet()
                if (getResp.isSuccessful) {
                    response = getResp
                }
            } catch (e: Exception) {
                // Ignore fallback error
            }
        }

        if (response == null) {
            throw Exception("Failed to connect to authentication server.")
        }

        val rawText = if (response.isSuccessful) {
            response.body()?.string()?.trim() ?: ""
        } else {
            response.errorBody()?.string()?.trim() ?: ""
        }

        if (rawText.isBlank()) {
            throw Exception("Empty response from server (Code ${response.code()}).")
        }

        // Case 1: Response is a JSON Object { success: true/false, ... }
        if (rawText.startsWith("{")) {
            val parsed = try {
                moshi.adapter(StoreAuthResponse::class.java).fromJson(rawText)
            } catch (e: Exception) {
                null
            }

            if (parsed == null) {
                throw Exception("Failed to parse store authentication response.")
            }

            if (parsed.success != true) {
                throw Exception(parsed.message ?: "Invalid Store Code or password.")
            }

            val matchedStoreCode = (parsed.storeCode ?: uppercaseCode).trim().uppercase()
            val matchedStoreName = (parsed.storeName ?: uppercaseCode).trim()
            val matchedStaffs = (parsed.staffs ?: "").trim()

            setCredentials(matchedStoreCode, trimmedPass)

            return@withContext StoreMatched(
                storeCode = matchedStoreCode,
                storeName = matchedStoreName,
                passwordPass = trimmedPass,
                staffs = matchedStaffs
            )
        }

        // Case 2: Response is a JSON Array [ { storeCode: ... }, ... ]
        if (rawText.startsWith("[")) {
            val listType = Types.newParameterizedType(List::class.java, Any::class.java)
            val listAdapter = moshi.adapter<List<Any>>(listType)
            val storesList = try {
                listAdapter.fromJson(rawText) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            var matchedStore: StoreMatched? = null
            for (item in storesList) {
                val match = parseStoreItem(item, uppercaseCode)
                if (match != null) {
                    matchedStore = match
                    break
                }
            }

            if (matchedStore == null) {
                throw Exception("Store Code '$uppercaseCode' not found.")
            }

            if (matchedStore.passwordPass != trimmedPass) {
                throw Exception("Incorrect password.")
            }

            setCredentials(matchedStore.storeCode, trimmedPass)
            return@withContext matchedStore
        }

        throw Exception("Invalid response format from server.")
    }

    private fun parseStoreItem(item: Any, targetCode: String): StoreMatched? {
        if (item is Map<*, *>) {
            val code = (item["storeCode"] as? String ?: item["StoreCode"] as? String ?: item["0"] as? String ?: "").trim().uppercase()
            if (code == targetCode) {
                val name = (item["storeName"] as? String ?: item["StoreName"] as? String ?: item["1"] as? String ?: "").trim()
                val password = (item["password"] as? String ?: item["Password"] as? String ?: item["2"] as? String ?: "").trim()
                val staffs = (item["staffs"] as? String ?: item["Staffs"] as? String ?: item["3"] as? String ?: "").trim()
                return StoreMatched(code, name, password, staffs)
            }
        }
        if (item is List<*>) {
            val code = (item.getOrNull(0) as? String ?: "").trim().uppercase()
            if (code == targetCode) {
                val name = (item.getOrNull(1) as? String ?: "").trim()
                val password = (item.getOrNull(2) as? String ?: "").trim()
                val staffs = (item.getOrNull(3) as? String ?: "").trim()
                return StoreMatched(code, name, password, staffs)
            }
        }
        return null
    }

    // Returns locally cached articles from memory or local disk file
    suspend fun getCachedArticles(): List<ArticleEntity> = withContext(Dispatchers.IO) {
        if (inMemoryArticles.isNotEmpty()) {
            return@withContext inMemoryArticles
        }
        if (articlesFile.exists()) {
            try {
                val jsonStr = articlesFile.readText()
                val loaded = articlesAdapter.fromJson(jsonStr) ?: emptyList()
                if (loaded.isNotEmpty()) {
                    inMemoryArticles = loaded
                    return@withContext loaded
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        emptyList()
    }

    // Smart fetch: compares server artETag with local stored artETag
    // If equal (server == local) and cache exists -> completely skips 12,000 row download
    // If different (server != local) or cache empty -> downloads, replaces cache, and saves artETag
    suspend fun fetchAndCacheArticles(serverETag: String? = null, force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val localETag = prefs.getString("artETag", "") ?: ""
        val hasCachedData = inMemoryArticles.isNotEmpty() || (articlesFile.exists() && articlesFile.length() > 50)

        var effectiveServerETag = serverETag
        if (effectiveServerETag == null && !force && hasCachedData) {
            try {
                val versionInfo = checkVersion()
                effectiveServerETag = versionInfo.artETagString
            } catch (e: Exception) {
                // If version check fails, fall back to local cached articles
            }
        }

        // Compare server ETag with local ETag
        if (!force && hasCachedData && !effectiveServerETag.isNullOrBlank() && effectiveServerETag == localETag) {
            // Already up to date! Zero rows downloaded
            if (inMemoryArticles.isEmpty()) {
                getCachedArticles()
            }
            return@withContext false
        }

        try {
            val response = api.getArticles()
            if (response.isSuccessful) {
                val rawText = response.body()?.string()?.trim() ?: ""
                if (rawText.isBlank()) return@withContext false

                var articlesDto: List<ArticleDto> = emptyList()
                var responseETag: String? = null

                if (rawText.startsWith("{")) {
                    val parsed = try {
                        moshi.adapter(ArticlesResponse::class.java).fromJson(rawText)
                    } catch (e: Exception) {
                        null
                    }
                    articlesDto = parsed?.rows ?: emptyList()
                    responseETag = parsed?.artETag?.toString()?.trim()
                } else if (rawText.startsWith("[")) {
                    val listAdapter = moshi.adapter<List<ArticleDto>>(articlesDtoListType)
                    articlesDto = try {
                        listAdapter.fromJson(rawText) ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                if (articlesDto.isNotEmpty()) {
                    val mapped = articlesDto.map {
                        ArticleEntity(
                            article = (it.article ?: "").trim(),
                            barcode = (it.barcode ?: "").trim(),
                            description = cleanDescription((it.description ?: "").trim()) ?: "",
                            department = (it.department ?: "").trim()
                        )
                    }
                    inMemoryArticles = mapped
                    try {
                        val json = articlesAdapter.toJson(mapped)
                        articlesFile.writeText(json)
                        val finalETag = responseETag ?: effectiveServerETag ?: localETag
                        val now = System.currentTimeMillis()
                        prefs.edit()
                            .putString("artETag", finalETag)
                            .putLong("last_articles_sync_time", now)
                            .apply()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    }

    suspend fun getArticleByCodeLocal(articleCode: String): ArticleEntity? = withContext(Dispatchers.IO) {
        getCachedArticles().firstOrNull { it.article.trim() == articleCode.trim() }
    }

    suspend fun getArticleByBarcodeLocal(barcode: String): ArticleEntity? = withContext(Dispatchers.IO) {
        getCachedArticles().firstOrNull { it.barcode.trim() == barcode.trim() }
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

    suspend fun updateStoreStaffs(storeCode: String, staffs: String): SuccessResponse = withContext(Dispatchers.IO) {
        val response = api.updateStoreStaffs(request = UpdateStaffsRequest(storeCode = storeCode, staffs = staffs))
        if (response.isSuccessful) {
            response.body() ?: SuccessResponse(true, "Staff list updated successfully.")
        } else {
            throw Exception("Server failed to update staff list: Code ${response.code()}")
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
