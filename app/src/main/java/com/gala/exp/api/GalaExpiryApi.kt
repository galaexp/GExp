package com.gala.exp.api

import retrofit2.Response
import retrofit2.http.*

data class ArticleDto(
    val article: String?,
    val barcode: String?,
    val description: String?,
    val department: String?
)

data class LastWeekRowDto(
    val Article: String?,
    val Barcode: String?,
    val Description: String?,
    val Department: String?,
    val Stock: String?,
    val ExpiryDate: String?
)

data class LastWeekResponse(
    val rows: List<LastWeekRowDto>?
)

data class ThisWeekRowDto(
    val Article: String?,
    val Barcode: String? = null,
    val Description: String?,
    val Department: String?,
    val Stock: String?,
    val ExpiryDate: String?,
    val RowIndex: Int?
)

data class ThisWeekResponse(
    val rows: List<ThisWeekRowDto>?
)

data class StoreDataRowDto(
    val Timestamp: String? = null,
    val Week: String? = null,
    val Article: String? = null,
    val Barcode: String? = null,
    val Description: String? = null,
    val Department: String? = null,
    val Stock: String? = null,
    val ExpiryDate: String? = null,
    val DaysLeft: String? = null,
    val RowIndex: Int? = null,
    val SubMonth: String? = null,
    val SubMonthDisp: String? = null
)

data class StoreDataResponse(
    val rows: List<StoreDataRowDto>?
)

data class VersionInfoResponse(
    val latestVersionCode: Int? = null,
    val latestVersionName: String? = null,
    val minRequiredVersionCode: Int? = null,
    val apkUrl: String? = null,
    val releaseNotes: String? = null,
    val forceUpdate: Boolean? = null
)

data class SuccessResponse(
    val success: Boolean?,
    val message: String?
)

data class FollowUpRequest(
    val storeCode: String,
    val storeName: String,
    val week: String,
    val article: String,
    val barcode: String,
    val description: String,
    val department: String,
    val stock: String,
    val expiry: String
)

data class AddExpiryRequest(
    val storeCode: String,
    val storeName: String,
    val week: String,
    val article: String,
    val barcode: String,
    val description: String,
    val department: String,
    val stock: String,
    val expiry: String
)

interface GalaExpiryApi {
    @GET("/")
    suspend fun storeAuth(
        @Query("action") action: String = "storeAuth",
        @Query("_t") timestamp: Long = System.currentTimeMillis()
    ): Response<List<Any>>

    @GET("/")
    suspend fun getArticles(
        @Query("action") action: String = "articles"
    ): Response<List<ArticleDto>>

    @GET("/")
    suspend fun lastWeekData(
        @Query("action") action: String = "lastWeekData",
        @Query("storeCode") storeCode: String,
        @Query("week") week: String,
        @Query("submissionMonth") submissionMonth: String
    ): Response<LastWeekResponse>

    @POST("/")
    suspend fun followUpUpdate(
        @Query("action") action: String = "followUpUpdate",
        @Body request: FollowUpRequest
    ): Response<SuccessResponse>

    @GET("/")
    suspend fun thisWeekReview(
        @Query("action") action: String = "thisWeekReview",
        @Query("storeCode") storeCode: String,
        @Query("week") week: String,
        @Query("submissionMonth") submissionMonth: String
    ): Response<ThisWeekResponse>

    @GET("/")
    suspend fun deleteRow(
        @Query("action") action: String = "deleteRow",
        @Query("rowIndex") rowIndex: Int
    ): Response<SuccessResponse>

    @GET("/")
    suspend fun editRow(
        @Query("action") action: String = "editRow",
        @Query("rowIndex") rowIndex: Int,
        @Query("stock") stock: String,
        @Query("expiry") expiry: String,
        @Query("_t") timestamp: Long = System.currentTimeMillis()
    ): Response<SuccessResponse>

    @GET("/")
    suspend fun sendRTCEmail(
        @Query("action") action: String = "sendRTCEmail",
        @Query("storeCode") storeCode: String,
        @Query("storeName") storeName: String,
        @Query("week") week: String,
        @Query("submissionMonth") submissionMonth: String
    ): Response<SuccessResponse>

    @POST("/")
    suspend fun addExpiry(
        @Query("action") action: String = "addExpiry",
        @Body request: AddExpiryRequest
    ): Response<SuccessResponse>

    @GET("/")
    suspend fun thisStoreData(
        @Query("action") action: String = "thisStoreData",
        @Query("storeCode") storeCode: String,
        @Query("_t") timestamp: Long = System.currentTimeMillis()
    ): Response<StoreDataResponse>

    @GET("/")
    suspend fun checkVersion(
        @Query("action") action: String = "version",
        @Query("_t") timestamp: Long = System.currentTimeMillis()
    ): Response<VersionInfoResponse>
}
