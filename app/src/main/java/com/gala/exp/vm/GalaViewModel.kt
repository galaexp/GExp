package com.gala.exp.vm

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gala.exp.api.AddExpiryRequest
import com.gala.exp.api.FollowUpRequest
import com.gala.exp.api.LastWeekRowDto
import com.gala.exp.api.ThisWeekRowDto
import com.gala.exp.api.StoreDataRowDto
import com.gala.exp.db.ArticleEntity
import com.gala.exp.notification.NotificationHelper
import com.gala.exp.repo.GalaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GalaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GalaRepository(application)
    private val notificationHelper = NotificationHelper(application)
    private val prefs: android.content.SharedPreferences = SafeSharedPreferences(application, "gala_prefs")

    // Auth flows
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _storeCode = MutableStateFlow("")
    val storeCode: StateFlow<String> = _storeCode.asStateFlow()

    private val _storeName = MutableStateFlow("")
    val storeName: StateFlow<String> = _storeName.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _rememberMe = MutableStateFlow(true)
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Staff Management state
    private val _staffList = MutableStateFlow<List<String>>(emptyList())
    val staffList: StateFlow<List<String>> = _staffList.asStateFlow()

    private val _selectedStaff = MutableStateFlow<String>("")
    val selectedStaff: StateFlow<String> = _selectedStaff.asStateFlow()

    private val _isStaffLoading = MutableStateFlow(false)
    val isStaffLoading: StateFlow<Boolean> = _isStaffLoading.asStateFlow()

    private val _staffError = MutableStateFlow<String?>(null)
    val staffError: StateFlow<String?> = _staffError.asStateFlow()

    // Operational session state
    private val _selectedWeek = MutableStateFlow("")
    val selectedWeek: StateFlow<String> = _selectedWeek.asStateFlow()

    private val _currentMonth = MutableStateFlow("")
    val currentMonth: StateFlow<String> = _currentMonth.asStateFlow()

    // Articles search cache
    private val _articles = MutableStateFlow<List<ArticleEntity>>(emptyList())
    val articles: StateFlow<List<ArticleEntity>> = _articles.asStateFlow()

    // Add list items state
    private val _barcode = MutableStateFlow("")
    val barcode: StateFlow<String> = _barcode.asStateFlow()

    private val _articleCode = MutableStateFlow("")
    val articleCode: StateFlow<String> = _articleCode.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _department = MutableStateFlow("")
    val department: StateFlow<String> = _department.asStateFlow()

    private val _stock = MutableStateFlow("")
    val stock: StateFlow<String> = _stock.asStateFlow()

    private val _expiry = MutableStateFlow("")
    val expiry: StateFlow<String> = _expiry.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()
    private var isCurrentlySubmitting = false

    // Barcode Match Event Flow
    private val _barcodeMatchEvent = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 64)
    val barcodeMatchEvent: SharedFlow<Unit> = _barcodeMatchEvent.asSharedFlow()

    // Follow up state
    private val _lastWeekRows = MutableStateFlow<List<LastWeekRowDto>>(emptyList())
    val lastWeekRows: StateFlow<List<LastWeekRowDto>> = _lastWeekRows.asStateFlow()

    private val _lastWeekLoading = MutableStateFlow(false)
    val lastWeekLoading: StateFlow<Boolean> = _lastWeekLoading.asStateFlow()

    private val _lastWeekError = MutableStateFlow<String?>(null)
    val lastWeekError: StateFlow<String?> = _lastWeekError.asStateFlow()

    // Review state
    private val _reviewRows = MutableStateFlow<List<ThisWeekRowDto>>(emptyList())
    val reviewRows: StateFlow<List<ThisWeekRowDto>> = _reviewRows.asStateFlow()

    private val _reviewLoading = MutableStateFlow(false)
    val reviewLoading: StateFlow<Boolean> = _reviewLoading.asStateFlow()

    private val _reviewError = MutableStateFlow<String?>(null)
    val reviewError: StateFlow<String?> = _reviewError.asStateFlow()

    private val _reviewTonalMessage = MutableStateFlow<String?>(null)
    val reviewTonalMessage: StateFlow<String?> = _reviewTonalMessage.asStateFlow()

    private val _emailSending = MutableStateFlow(false)
    val emailSending: StateFlow<Boolean> = _emailSending.asStateFlow()

    private val _lockedWeeks = MutableStateFlow<Set<String>>(emptySet())
    val lockedWeeks: StateFlow<Set<String>> = _lockedWeeks.asStateFlow()

    // Store Dashboard state
    private val _storeDashboardRows = MutableStateFlow<List<StoreDataRowDto>>(emptyList())
    val storeDashboardRows: StateFlow<List<StoreDataRowDto>> = _storeDashboardRows.asStateFlow()

    private val _storeDashboardLoading = MutableStateFlow(false)
    val storeDashboardLoading: StateFlow<Boolean> = _storeDashboardLoading.asStateFlow()

    private val _storeDashboardError = MutableStateFlow<String?>(null)
    val storeDashboardError: StateFlow<String?> = _storeDashboardError.asStateFlow()

    // App update state
    private val _appUpdateState = MutableStateFlow<AppUpdateState?>(null)
    val appUpdateState: StateFlow<AppUpdateState?> = _appUpdateState.asStateFlow()

    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    fun loadLockedWeeks() {
        val month = _currentMonth.value
        if (month.isBlank()) return
        val allKeys = prefs.all.keys
        val currentMonthPrefix = "lock_${month}_"
        
        // Clean up old month locks to free up space
        val editor = prefs.edit()
        var changed = false
        for (key in allKeys) {
            if (key.startsWith("lock_") && !key.startsWith(currentMonthPrefix)) {
                editor.remove(key)
                changed = true
            }
        }
        if (changed) {
            editor.apply()
        }

        val keys = prefs.all.keys.filter { it.startsWith(currentMonthPrefix) }
        val lockedSet = keys.map { it.removePrefix(currentMonthPrefix) }.toSet()
        _lockedWeeks.value = lockedSet
    }

    init {
        // Automatically default month in yyyy-MM format
        _currentMonth.value = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        loadLockedWeeks()

        // Load prefilled cached auth
        val savedRemember = prefs.getBoolean("rememberMe", true)
        val savedCode = prefs.getString("storeCode", "") ?: ""
        val savedPass = prefs.getString("password", "") ?: ""
        val savedName = prefs.getString("storeName", "") ?: ""
        val wasLoggedIn = prefs.getBoolean("wasLoggedIn", false)

        _storeCode.value = savedCode
        _password.value = savedPass
        _storeName.value = savedName
        _rememberMe.value = savedRemember

        // Auto-login if rememberMe is enabled and saved credentials exist
        if (savedCode.isNotBlank()) {
            val cachedStaffsStr = prefs.getString("staffs_$savedCode", "") ?: ""
            val parsedStaffs = cachedStaffsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
            _staffList.value = parsedStaffs
            _selectedStaff.value = ""
        }

        if (savedRemember && savedCode.isNotBlank() && savedPass.isNotBlank()) {
            if (wasLoggedIn || savedName.isNotBlank()) {
                if (savedName.isNotBlank()) {
                    _storeName.value = savedName
                }
                _isLoggedIn.value = true
            }
            login()
        }

        loadLocalArticles()
        refreshArticlesFromServer()
        checkAppVersion(isManualCheck = false)
    }

    private fun loadLocalArticles() {
        viewModelScope.launch {
            _articles.value = repository.getCachedArticles()
        }
    }

    private fun refreshArticlesFromServer() {
        viewModelScope.launch {
            repository.fetchAndCacheArticles()
            _articles.value = repository.getCachedArticles()
        }
    }

    fun setRememberMe(value: Boolean) {
        _rememberMe.value = value
        prefs.edit().putBoolean("rememberMe", value).apply()
        if (!value) {
            prefs.edit()
                .remove("storeCode")
                .remove("password")
                .remove("storeName")
                .remove("wasLoggedIn")
                .apply()
        } else if (_storeCode.value.isNotBlank() && _password.value.isNotBlank()) {
            prefs.edit()
                .putString("storeCode", _storeCode.value)
                .putString("password", _password.value)
                .putString("storeName", _storeName.value)
                .apply()
        }
    }

    fun setStoreCode(value: String) {
        _storeCode.value = value
        _authError.value = null
        if (_rememberMe.value) {
            prefs.edit().putString("storeCode", value).apply()
        }
    }

    fun setPassword(value: String) {
        _password.value = value
        _authError.value = null
        if (_rememberMe.value) {
            prefs.edit().putString("password", value).apply()
        }
    }

    fun setSelectedWeek(value: String) {
        _selectedWeek.value = value
    }

    fun login() {
        if (_storeCode.value.isBlank() || _password.value.isBlank()) {
            _authError.value = "Please enter Store Code and password."
            return
        }

        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            try {
                val store = repository.authenticateStore(_storeCode.value, _password.value)
                _storeName.value = store.storeName
                _isLoggedIn.value = true

                val parsedStaffs = store.staffs.split(",").map { it.trim() }.filter { it.isNotBlank() }
                _staffList.value = parsedStaffs
                _selectedStaff.value = ""
                prefs.edit().putString("staffs_${store.storeCode}", store.staffs).apply()

                // Save preferences when remember me is enabled
                if (_rememberMe.value) {
                    prefs.edit()
                        .putString("storeCode", _storeCode.value)
                        .putString("password", _password.value)
                        .putString("storeName", store.storeName)
                        .putBoolean("rememberMe", true)
                        .putBoolean("wasLoggedIn", true)
                        .apply()
                } else {
                    prefs.edit()
                        .remove("storeCode")
                        .remove("password")
                        .remove("storeName")
                        .putBoolean("rememberMe", false)
                        .putBoolean("wasLoggedIn", false)
                        .apply()
                }
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Connection error. Please try again."
                if (e is java.net.UnknownHostException || msg.contains("unable to resolve host", ignoreCase = true)) {
                    _authError.value = "Check Internet Connection"
                } else {
                    _authError.value = msg
                }
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        // Reset in-session state but keep cached inputs if rememberMe is enabled
        _selectedWeek.value = ""
        clearInputs()
        prefs.edit().putBoolean("wasLoggedIn", false).apply()
        if (!_rememberMe.value) {
            _storeCode.value = ""
            _password.value = ""
            _storeName.value = ""
            prefs.edit()
                .remove("storeCode")
                .remove("password")
                .remove("storeName")
                .apply()
        }
    }

    // Barcode scanning / Form filling
    fun setBarcode(value: String) {
        _barcode.value = value
        // Check for match
        viewModelScope.launch {
            val article = repository.getArticleByBarcodeLocal(value)
            if (article != null) {
                _articleCode.value = article.article
                _description.value = article.description
                _department.value = article.department
                _barcodeMatchEvent.emit(Unit)
            }
        }
    }

    fun setArticleCode(value: String) {
        _articleCode.value = value
        // Check for match
        viewModelScope.launch {
            val article = repository.getArticleByCodeLocal(value)
            if (article != null) {
                _barcode.value = article.barcode
                _description.value = article.description
                _department.value = article.department
                _barcodeMatchEvent.emit(Unit)
            }
        }
    }

    fun setStock(value: String) {
        _stock.value = value
    }

    fun setExpiry(value: String) {
        _expiry.value = value
    }

    fun clearInputs() {
        _barcode.value = ""
        _articleCode.value = ""
        _description.value = ""
        _department.value = ""
        _stock.value = ""
        _expiry.value = ""
        _isSubmitting.value = false
    }

    fun setSelectedStaff(name: String) {
        _selectedStaff.value = name
    }

    fun addStaffMember(name: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val current = _staffList.value.toMutableList()
        if (current.any { it.equals(cleanName, ignoreCase = true) }) {
            val err = "Staff '$cleanName' already exists."
            _staffError.value = err
            onError(err)
            return
        }
        current.add(cleanName)
        val newStaffsStr = current.joinToString(",")

        viewModelScope.launch {
            _isStaffLoading.value = true
            _staffError.value = null
            try {
                val resp = repository.updateStoreStaffs(_storeCode.value, newStaffsStr)
                if (resp.success == true) {
                    _staffList.value = current
                    if (_selectedStaff.value.isBlank()) {
                        setSelectedStaff(cleanName)
                    }
                    prefs.edit().putString("staffs_${_storeCode.value}", newStaffsStr).apply()
                    onSuccess()
                } else {
                    val err = resp.message ?: "Failed to update staff list."
                    _staffError.value = err
                    onError(err)
                }
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Failed to update staff list."
                _staffError.value = err
                onError(err)
            } finally {
                _isStaffLoading.value = false
            }
        }
    }

    fun removeStaffMember(name: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val current = _staffList.value.toMutableList()
        current.removeAll { it.equals(name, ignoreCase = true) }
        val newStaffsStr = current.joinToString(",")

        viewModelScope.launch {
            _isStaffLoading.value = true
            _staffError.value = null
            try {
                val resp = repository.updateStoreStaffs(_storeCode.value, newStaffsStr)
                if (resp.success == true) {
                    _staffList.value = current
                    if (_selectedStaff.value.equals(name, ignoreCase = true)) {
                        setSelectedStaff(current.firstOrNull() ?: "")
                    }
                    prefs.edit().putString("staffs_${_storeCode.value}", newStaffsStr).apply()
                    onSuccess()
                } else {
                    val err = resp.message ?: "Failed to remove staff member."
                    _staffError.value = err
                    onError(err)
                }
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Failed to remove staff member."
                _staffError.value = err
                onError(err)
            } finally {
                _isStaffLoading.value = false
            }
        }
    }

    // Add near-expiry item
    fun submitExpiryItem(onSuccess: () -> Unit) {
        if (isCurrentlySubmitting) {
            return
        }
        if (_selectedWeek.value.isBlank() || _description.value.isBlank() || _stock.value.isBlank() || _expiry.value.isBlank() || _selectedStaff.value.isBlank()) {
            return
        }

        isCurrentlySubmitting = true
        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                val req = AddExpiryRequest(
                    storeCode = _storeCode.value,
                    storeName = _storeName.value,
                    week = _selectedWeek.value,
                    article = _articleCode.value,
                    barcode = _barcode.value,
                    description = _description.value,
                    department = _department.value,
                    stock = _stock.value,
                    expiry = _expiry.value,
                    staffName = _selectedStaff.value
                )
                repository.submitItemExpiry(req)
                // Trigger real-time post notification alert
                notificationHelper.showItemSubmittedNotification(_articleCode.value, _description.value, _stock.value)
                
                // Immediately update local reviewRows state flow in memory to prevent duplicates and keep tracking synchronized without calling network API
                val newLocalRow = ThisWeekRowDto(
                    RowIndex = -1,
                    Article = _articleCode.value,
                    Barcode = _barcode.value,
                    Description = _description.value,
                    Department = _department.value,
                    Stock = _stock.value,
                    ExpiryDate = _expiry.value,
                    StaffName = _selectedStaff.value,
                    rawStaffName = _selectedStaff.value
                )
                val updatedList = _reviewRows.value.toMutableList()
                val existingIndex = updatedList.indexOfFirst {
                    it.Article?.trim()?.lowercase() == newLocalRow.Article?.trim()?.lowercase() &&
                    it.ExpiryDate?.trim()?.lowercase() == newLocalRow.ExpiryDate?.trim()?.lowercase()
                }
                if (existingIndex >= 0) {
                    updatedList[existingIndex] = newLocalRow
                } else {
                    updatedList.add(0, newLocalRow)
                }
                _reviewRows.value = updatedList
                
                clearInputs()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isCurrentlySubmitting = false
                _isSubmitting.value = false
            }
        }
    }

    private fun getMonthNameOnly(ym: String): String {
        val parts = ym.split("-")
        if (parts.size == 2 && parts[0].length == 4) {
            val m = parts[1].toIntOrNull()
            if (m != null && m in 1..12) {
                val dfs = java.text.DateFormatSymbols.getInstance(java.util.Locale.US)
                return dfs.months[m - 1]
            }
        }
        return ym.replace(Regex("\\b20\\d{2}\\b"), "").replace("(", "").replace(")", "").trim()
    }

    // Follow up last week items
    fun startFollowUp() {
        val prev = getPrevWeek(_selectedWeek.value, _currentMonth.value)
        _lastWeekRows.value = emptyList()
        _lastWeekError.value = null

        if (prev == null) {
            _lastWeekLabel.value = "No previous week"
            return
        }

        _lastWeekLabel.value = "Follow Up for ${prev.week} ${getMonthNameOnly(prev.month)}"

        viewModelScope.launch {
            _lastWeekLoading.value = true
            try {
                val rows = repository.getLastWeekData(
                    storeCode = _storeCode.value,
                    week = prev.week,
                    month = prev.month
                )
                // Filter out items that were marked as 0 stock (sold out) in the previous week
                _lastWeekRows.value = rows.filter { row ->
                    val stockStr = row.Stock?.trim() ?: ""
                    val stockVal = stockStr.toDoubleOrNull()
                    if (stockVal != null) {
                        stockVal > 0
                    } else {
                        stockStr != "0" && stockStr != "0.0" && stockStr != "0.00"
                    }
                }
            } catch (e: Exception) {
                _lastWeekError.value = e.localizedMessage ?: "Failed to load last week follow up data."
            } finally {
                _lastWeekLoading.value = false
            }
        }
    }

    private val _lastWeekLabel = MutableStateFlow("")
    val lastWeekLabel: StateFlow<String> = _lastWeekLabel.asStateFlow()

    fun submitFollowUpStock(stockRow: LastWeekRowDto, newStock: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (isCurrentlySubmitting) {
            return
        }
        if (newStock.isBlank()) return
        if (_selectedStaff.value.isBlank()) {
            onError("Staff member selection is required. Please select a staff member on the Homepage first.")
            return
        }

        isCurrentlySubmitting = true
        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                val req = FollowUpRequest(
                    storeCode = _storeCode.value,
                    storeName = _storeName.value,
                    week = _selectedWeek.value,
                    article = stockRow.Article ?: "",
                    barcode = stockRow.Barcode ?: "",
                    description = stockRow.Description ?: "",
                    department = stockRow.Department ?: "",
                    stock = newStock,
                    expiry = stockRow.ExpiryDate ?: "",
                    staffName = _selectedStaff.value
                )
                val resp = repository.submitFollowUp(req)
                if (resp.success == true) {
                    // Immediately update local reviewRows in memory to prevent duplicates and keep tracking synchronized without calling network API
                    val newLocalRow = ThisWeekRowDto(
                        RowIndex = -1,
                        Article = stockRow.Article,
                        Barcode = stockRow.Barcode,
                        Description = stockRow.Description,
                        Department = stockRow.Department,
                        Stock = newStock,
                        ExpiryDate = stockRow.ExpiryDate,
                        StaffName = _selectedStaff.value,
                        rawStaffName = _selectedStaff.value
                    )
                    val updatedList = _reviewRows.value.toMutableList()
                    val existingIndex = updatedList.indexOfFirst {
                        it.Article?.trim()?.lowercase() == newLocalRow.Article?.trim()?.lowercase() &&
                        it.ExpiryDate?.trim()?.lowercase() == newLocalRow.ExpiryDate?.trim()?.lowercase()
                    }
                    if (existingIndex >= 0) {
                        updatedList[existingIndex] = newLocalRow
                    } else {
                        updatedList.add(0, newLocalRow)
                    }
                    _reviewRows.value = updatedList
                    onSuccess()
                } else {
                    onError(resp.message ?: "Failed.")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "A connection error occurred.")
            } finally {
                isCurrentlySubmitting = false
                _isSubmitting.value = false
            }
        }
    }

    private var reviewMessageJob: kotlinx.coroutines.Job? = null

    fun setReviewTonalMessage(msg: String?) {
        _reviewTonalMessage.value = msg
        reviewMessageJob?.cancel()
        if (msg != null) {
            reviewMessageJob = viewModelScope.launch {
                kotlinx.coroutines.delay(3500)
                if (_reviewTonalMessage.value == msg) {
                    _reviewTonalMessage.value = null
                }
            }
        }
    }

    fun clearReviewTonalMessage() {
        reviewMessageJob?.cancel()
        _reviewTonalMessage.value = null
    }

    // Review lists
    fun loadThisWeekReview() {
        _reviewRows.value = emptyList()
        _reviewError.value = null
        clearReviewTonalMessage()

        viewModelScope.launch {
            _reviewLoading.value = true
            try {
                val rows = repository.getThisWeekReview(
                    storeCode = _storeCode.value,
                    week = _selectedWeek.value,
                    month = _currentMonth.value
                )
                _reviewRows.value = rows
            } catch (e: Exception) {
                _reviewError.value = e.localizedMessage ?: "Failed to fetch review data."
            } finally {
                _reviewLoading.value = false
            }
        }
    }

    fun deleteReviewRow(rowIndex: Int, arrayIndex: Int, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val resp = repository.deleteRow(rowIndex)
                if (resp.success == true) {
                    val currentList = _reviewRows.value.toMutableList()
                    if (arrayIndex in currentList.indices) {
                        currentList.removeAt(arrayIndex)
                        _reviewRows.value = currentList
                    }
                    onComplete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun editReviewRow(rowIndex: Int, arrayIndex: Int, stock: String, expiry: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (isCurrentlySubmitting) {
            return
        }
        isCurrentlySubmitting = true
        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                val resp = repository.editRow(rowIndex, stock, expiry)
                if (resp.success == true) {
                    val currentList = _reviewRows.value.toMutableList()
                    if (arrayIndex in currentList.indices) {
                        val row = currentList[arrayIndex]
                        currentList[arrayIndex] = row.copy(Stock = stock, ExpiryDate = expiry)
                        _reviewRows.value = currentList
                    } else {
                        // Fallback: if we didn't have an exact index, refresh all rows
                        loadThisWeekReview()
                    }
                    onSuccess()
                } else {
                    onError(resp.message ?: "Failed to update item.")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "A connection error occurred.")
            } finally {
                isCurrentlySubmitting = false
                _isSubmitting.value = false
            }
        }
    }

    fun loadStoreDashboardData() {
        _storeDashboardRows.value = emptyList()
        _storeDashboardError.value = null
        _storeDashboardLoading.value = true

        viewModelScope.launch {
            try {
                val rows = repository.getThisStoreData(_storeCode.value)
                _storeDashboardRows.value = rows
            } catch (e: Exception) {
                _storeDashboardError.value = e.localizedMessage ?: "Failed to load store dashboard data."
            } finally {
                _storeDashboardLoading.value = false
            }
        }
    }

    fun sendSubmissionEmail(onSuccess: () -> Unit) {
        if (_reviewRows.value.isEmpty()) return

        viewModelScope.launch {
            _emailSending.value = true
            clearReviewTonalMessage()
            try {
                val resp = repository.sendRTCEmail(
                    storeCode = _storeCode.value,
                    storeName = _storeName.value,
                    week = _selectedWeek.value,
                    month = _currentMonth.value
                )
                if (resp.success == true) {
                    setReviewTonalMessage("✅ Email sent successfully!")
                    notificationHelper.showEmailSentNotification(_storeName.value, _selectedWeek.value)
                    
                    // Lock current week upon successful submission
                    val month = _currentMonth.value
                    val week = _selectedWeek.value
                    if (month.isNotBlank() && week.isNotBlank()) {
                        prefs.edit().putBoolean("lock_${month}_$week", true).apply()
                        loadLockedWeeks()
                    }

                    onSuccess()
                } else {
                    setReviewTonalMessage("❌ ${resp.message ?: "Error sending submission."}")
                }
            } catch (e: Exception) {
                setReviewTonalMessage("❌ Network error: ${e.localizedMessage}")
            } finally {
                _emailSending.value = false
            }
        }
    }

    fun triggerReminderNotification() {
        notificationHelper.showExpiryReminderAlert()
    }

    fun checkAppVersion(isManualCheck: Boolean = false) {
        viewModelScope.launch {
            if (isManualCheck) {
                _toastEvent.emit("Checking for updates...")
            }
            try {
                val versionInfo = repository.checkVersion()
                val currentVersionCode = com.gala.exp.BuildConfig.VERSION_CODE
                val minRequired = versionInfo.minRequiredVersionCode ?: 1
                val latestCode = versionInfo.latestVersionCode ?: 1
                val forceUpdateFlag = versionInfo.forceUpdate == true

                val isForce = (currentVersionCode < minRequired) || (forceUpdateFlag && currentVersionCode < latestCode)
                val isAvailable = currentVersionCode < latestCode

                if (isAvailable || isForce) {
                    _appUpdateState.value = AppUpdateState(
                        isUpdateAvailable = true,
                        isForceUpdate = isForce,
                        latestVersionCode = latestCode,
                        latestVersionName = versionInfo.latestVersionName ?: "1.0.1",
                        minRequiredVersionCode = minRequired,
                        releaseNotes = versionInfo.releaseNotes ?: "Critical bug fixes and new features added.",
                        apkUrl = versionInfo.apkUrl ?: ""
                    )
                } else {
                    _appUpdateState.value = null
                    if (isManualCheck) {
                        _toastEvent.emit("App is up to date! (v${com.gala.exp.BuildConfig.VERSION_NAME})")
                    }
                }
            } catch (e: Exception) {
                if (isManualCheck) {
                    _toastEvent.emit("Update check failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun dismissOptionalUpdate() {
        _appUpdateState.value = null
    }

    fun downloadAndInstallApk(context: Context, apkUrl: String) {
        if (apkUrl.isBlank()) {
            _downloadStatus.value = DownloadStatus.Error("Download URL is empty.")
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _downloadStatus.value = DownloadStatus.Downloading(0)
                val client = okhttp3.OkHttpClient.Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = okhttp3.Request.Builder().url(apkUrl).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    _downloadStatus.value = DownloadStatus.Error("Download failed with HTTP ${response.code}")
                    return@launch
                }

                val body = response.body ?: throw Exception("Response body is null")
                val contentLength = body.contentLength()

                val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                val apkFile = java.io.File(downloadsDir, "GalaExp-update.apk")
                if (apkFile.exists()) apkFile.delete()

                body.byteStream().use { input ->
                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (contentLength > 0) {
                                val progress = ((totalBytesRead * 100) / contentLength).toInt()
                                _downloadStatus.value = DownloadStatus.Downloading(progress.coerceIn(0, 100))
                            } else {
                                _downloadStatus.value = DownloadStatus.Downloading(-1)
                            }
                        }
                    }
                }
                _downloadStatus.value = DownloadStatus.Success(apkFile)
            } catch (e: Exception) {
                e.printStackTrace()
                _downloadStatus.value = DownloadStatus.Error(e.localizedMessage ?: "Failed to download update")
            }
        }
    }

    fun resetDownloadStatus() {
        _downloadStatus.value = DownloadStatus.Idle
    }

    // Helper data structures matching JS
    data class PrevWeekResult(val week: String, val month: String)

    private fun getPrevWeek(currentWeek: String, currentYM: String): PrevWeekResult? {
        val wMap = mapOf(
            "Week 1 (1–7)" to Pair("Week 4 (22–31)", false),
            "Week 2 (8–14)" to Pair("Week 1 (1–7)", true),
            "Week 3 (15–21)" to Pair("Week 2 (8–14)", true),
            "Week 4 (22–31)" to Pair("Week 3 (15–21)", true)
        )
        val entry = wMap[currentWeek] ?: return null
        var ym = currentYM
        if (!entry.second) {
            try {
                val parts = currentYM.split("-")
                val y = parts[0].toInt()
                val m = parts[1].toInt()
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m - 1) // 0-based
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, -1)
                }
                val prevY = calendar.get(Calendar.YEAR)
                val prevM = calendar.get(Calendar.MONTH) + 1
                ym = String.format(Locale.getDefault(), "%04d-%02d", prevY, prevM)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return PrevWeekResult(entry.first, ym)
    }
}

class SafeSharedPreferences(
    context: Context,
    name: String
) : android.content.SharedPreferences by context.getSharedPreferences(name, Context.MODE_PRIVATE)

data class AppUpdateState(
    val isUpdateAvailable: Boolean = false,
    val isForceUpdate: Boolean = false,
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0",
    val minRequiredVersionCode: Int = 1,
    val releaseNotes: String = "",
    val apkUrl: String = ""
)

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Downloading(val progressPercent: Int) : DownloadStatus()
    data class Success(val apkFile: java.io.File) : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}
