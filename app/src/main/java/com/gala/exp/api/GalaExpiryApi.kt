package com.gala.exp.api

import com.squareup.moshi.Json
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

data class ArticleDto(
    val article: String?,
    val barcode: String?,
    val description: String?,
    val department: String?
)

data class LastWeekRowDto(
    @Json(name = "Article")
    val Article: String? = null,
    @Json(name = "Barcode")
    val Barcode: String? = null,
    @Json(name = "Description")
    val Description: String? = null,
    @Json(name = "Department")
    val Department: String? = null,
    @Json(name = "Stock")
    val Stock: String? = null,
    @Json(name = "ExpiryDate")
    val ExpiryDate: String? = null,
    @Json(name = "StaffName")
    val StaffName: String? = null,
    @Json(name = "staffName")
    val rawStaffName: String? = null,
    @Json(name = "Staff")
    val Staff: String? = null,
    @Json(name = "staff")
    val lowerStaff: String? = null,
    @Json(name = "Staff_Name")
    val staffNameSnake: String? = null,
    @Json(name = "staff_name")
    val staffNameLowerSnake: String? = null,
    @Json(name = "Staff Name")
    val staffNameSpace: String? = null,
    @Json(name = "STAFF")
    val staffUpper: String? = null,
    @Json(name = "STAFF_NAME")
    val staffNameUpperSnake: String? = null,
    @Json(name = "StaffMember")
    val StaffMember: String? = null,
    @Json(name = "staffMember")
    val rawStaffMember: String? = null,
    @Json(name = "Staff_Member")
    val staffMemberSnake: String? = null,
    @Json(name = "staff_member")
    val staffMemberLowerSnake: String? = null,
    @Json(name = "Staff Member")
    val staffMemberSpace: String? = null,
    @Json(name = "User")
    val User: String? = null,
    @Json(name = "user")
    val lowerUser: String? = null,
    @Json(name = "LoggedBy")
    val LoggedBy: String? = null,
    @Json(name = "loggedBy")
    val lowerLoggedBy: String? = null,
    @Json(name = "SubmittedBy")
    val SubmittedBy: String? = null,
    @Json(name = "submittedBy")
    val lowerSubmittedBy: String? = null,
    @Json(name = "CreatedBy")
    val CreatedBy: String? = null,
    @Json(name = "createdBy")
    val lowerCreatedBy: String? = null
) {
    val staffDisplayName: String
        get() = listOfNotNull(
            StaffName,
            rawStaffName,
            Staff,
            lowerStaff,
            staffNameSnake,
            staffNameLowerSnake,
            staffNameSpace,
            staffUpper,
            staffNameUpperSnake,
            StaffMember,
            rawStaffMember,
            staffMemberSnake,
            staffMemberLowerSnake,
            staffMemberSpace,
            User,
            lowerUser,
            LoggedBy,
            lowerLoggedBy,
            SubmittedBy,
            lowerSubmittedBy,
            CreatedBy,
            lowerCreatedBy
        ).firstOrNull { it.isNotBlank() && !it.equals("null", ignoreCase = true) }?.trim() ?: ""
}

data class LastWeekResponse(
    val rows: List<LastWeekRowDto>?
)

data class ThisWeekRowDto(
    @Json(name = "RowIndex")
    val RowIndex: Int? = null,
    @Json(name = "Article")
    val Article: String? = null,
    @Json(name = "Barcode")
    val Barcode: String? = null,
    @Json(name = "Description")
    val Description: String? = null,
    @Json(name = "Department")
    val Department: String? = null,
    @Json(name = "Stock")
    val Stock: String? = null,
    @Json(name = "ExpiryDate")
    val ExpiryDate: String? = null,
    @Json(name = "DaysLeft")
    val DaysLeft: Int? = null,
    @Json(name = "StaffName")
    val StaffName: String? = null,
    @Json(name = "staffName")
    val rawStaffName: String? = null,
    @Json(name = "Staff")
    val Staff: String? = null,
    @Json(name = "staff")
    val lowerStaff: String? = null,
    @Json(name = "Staff_Name")
    val staffNameSnake: String? = null,
    @Json(name = "staff_name")
    val staffNameLowerSnake: String? = null,
    @Json(name = "Staff Name")
    val staffNameSpace: String? = null,
    @Json(name = "STAFF")
    val staffUpper: String? = null,
    @Json(name = "STAFF_NAME")
    val staffNameUpperSnake: String? = null
) {
    val staffDisplayName: String
        get() = listOfNotNull(
            StaffName,
            rawStaffName,
            Staff,
            lowerStaff,
            staffNameSnake,
            staffNameLowerSnake,
            staffNameSpace,
            staffUpper,
            staffNameUpperSnake
        ).firstOrNull { it.isNotBlank() && !it.equals("null", ignoreCase = true) }?.trim() ?: ""
}

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
    val SubMonthDisp: String? = null,
    val Staff: String? = null,
    @Json(name = "staff")
    val lowerStaff: String? = null,
    val StaffName: String? = null,
    @Json(name = "staffName")
    val rawStaffName: String? = null,
    @Json(name = "Staff_Name")
    val staffNameSnake: String? = null,
    @Json(name = "staff_name")
    val staffNameLowerSnake: String? = null,
    @Json(name = "Staff Name")
    val staffNameSpace: String? = null,
    @Json(name = "STAFF")
    val staffUpper: String? = null,
    @Json(name = "STAFF_NAME")
    val staffNameUpperSnake: String? = null
) {
    val staffDisplayName: String
        get() = listOfNotNull(
            StaffName,
            rawStaffName,
            Staff,
            lowerStaff,
            staffNameSnake,
            staffNameLowerSnake,
            staffNameSpace,
            staffUpper,
            staffNameUpperSnake
        ).firstOrNull { it.isNotBlank() && !it.equals("null", ignoreCase = true) }?.trim() ?: ""
}

data class StoreDataResponse(
    val rows: List<StoreDataRowDto>?
)

data class VersionInfoResponse(
    val latestVersionCode: Int? = null,
    val latestVersionName: String? = null,
    val minRequiredVersionCode: Int? = null,
    val apkUrl: String? = null,
    val releaseNotes: String? = null,
    val forceUpdate: Boolean? = null,
    val artETag: Any? = null
) {
    val artETagString: String?
        get() = artETag?.toString()?.trim()
}

data class ArticlesResponse(
    val rows: List<ArticleDto>? = null,
    val artETag: Any? = null
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
    val expiry: String,
    val staffName: String = ""
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
    val expiry: String,
    val staffName: String = ""
)

data class UpdateStaffsRequest(
    val storeCode: String,
    val staffs: String
)

data class StoreAuthRequest(
    val storeCode: String,
    val password: String
)

data class StoreAuthResponse(
    val success: Boolean? = null,
    val storeCode: String? = null,
    val storeName: String? = null,
    val staffs: String? = null,
    val message: String? = null
)

interface GalaExpiryApi {
    @POST("/")
    suspend fun storeAuth(
        @Query("action") action: String = "storeAuth",
        @Body request: StoreAuthRequest
    ): Response<ResponseBody>

    @GET("/")
    suspend fun storeAuthGet(
        @Query("action") action: String = "storeAuth",
        @Query("_t") timestamp: Long = System.currentTimeMillis()
    ): Response<ResponseBody>

    @GET("/")
    suspend fun getArticles(
        @Query("action") action: String = "articles"
    ): Response<ResponseBody>

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

    @POST("/")
    suspend fun updateStoreStaffs(
        @Query("action") action: String = "updateStoreStaffs",
        @Body request: UpdateStaffsRequest
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
