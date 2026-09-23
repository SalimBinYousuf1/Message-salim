package com.example.ui.screens.conversationlist

import android.app.Application
import android.content.ContentValues
import android.provider.Telephony
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SalimApplication
import com.example.data.model.Conversation
import com.example.data.preferences.SalimSettings
import com.example.telephony.OtpHelper
import com.example.telephony.SmsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ConversationCategoryFilter(val label: String) {
    ALL("All"),
    PERSONAL("Personal"),
    TRANSACTIONS("Transactions"),
    UNKNOWN("Unknown"),
    ARCHIVED("Archived")
}

data class ConversationListUiState(
    val conversations: List<Conversation> = emptyList(),
    val pinnedConversations: List<Conversation> = emptyList(),
    val unpinnedConversations: List<Conversation> = emptyList(),
    val searchQuery: String = "",
    val categoryFilter: ConversationCategoryFilter = ConversationCategoryFilter.ALL,
    val isDefaultSmsApp: Boolean = true,
    val hasSmsPermission: Boolean = true,
    val selectedThreadIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isBiometricUnlocked: Boolean = true
)

class ConversationListViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SalimApplication
    private val telephonyRepo = app.telephonyRepository
    private val preferencesRepo = app.preferencesRepository
    private val blockedDao = app.database.blockedContactDao()

    val settings: StateFlow<SalimSettings> = preferencesRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SalimSettings())

    private val _searchQuery = MutableStateFlow("")
    private val _categoryFilter = MutableStateFlow(ConversationCategoryFilter.ALL)
    private val _selectedThreadIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _permissionState = MutableStateFlow(SmsHelper.hasRequiredPermissions(application))
    private val _isDefaultSmsState = MutableStateFlow(SmsHelper.isDefaultSmsApp(application))
    private val _isBiometricUnlocked = MutableStateFlow(true)

    private val _filterState = combine(_searchQuery, _categoryFilter, _selectedThreadIds) { query, category, selected ->
        Triple(query, category, selected)
    }

    private val _systemStatus = combine(_permissionState, _isDefaultSmsState, _isBiometricUnlocked) { perm, defaultSms, unlocked ->
        Triple(perm, defaultSms, unlocked)
    }

    val uiState: StateFlow<ConversationListUiState> = combine(
        telephonyRepo.conversationsFlow,
        blockedDao.getAllBlockedFlow(),
        _filterState,
        _systemStatus
    ) { conversations, blockedList, filter, status ->
        val (query, category, selectedIds) = filter
        val (hasPerm, isDefault, unlocked) = status

        val blockedAddresses = blockedList.map { it.address }.toSet()

        val activeList = conversations.filter { !blockedAddresses.contains(it.address) }

        val filtered = activeList.filter { conv ->
            // Category filter
            val matchesCategory = when (category) {
                ConversationCategoryFilter.ALL -> !conv.isArchived
                ConversationCategoryFilter.ARCHIVED -> conv.isArchived
                ConversationCategoryFilter.PERSONAL -> {
                    !conv.isArchived && !OtpHelper.isTransactionOrOtp(conv.address, conv.snippet) && conv.displayName != conv.address
                }
                ConversationCategoryFilter.TRANSACTIONS -> {
                    !conv.isArchived && OtpHelper.isTransactionOrOtp(conv.address, conv.snippet)
                }
                ConversationCategoryFilter.UNKNOWN -> {
                    !conv.isArchived && !OtpHelper.isTransactionOrOtp(conv.address, conv.snippet) && conv.displayName == conv.address
                }
            }

            // Search query filter
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                conv.displayName.contains(query, ignoreCase = true) ||
                        conv.address.contains(query, ignoreCase = true) ||
                        conv.snippet.contains(query, ignoreCase = true)
            }

            matchesCategory && matchesQuery
        }

        val pinned = filtered.filter { it.isPinned }
        val unpinned = filtered.filter { !it.isPinned }

        ConversationListUiState(
            conversations = filtered,
            pinnedConversations = pinned,
            unpinnedConversations = unpinned,
            searchQuery = query,
            categoryFilter = category,
            isDefaultSmsApp = isDefault,
            hasSmsPermission = hasPerm,
            selectedThreadIds = selectedIds,
            isSelectionMode = selectedIds.isNotEmpty(),
            isBiometricUnlocked = unlocked
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConversationListUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(filter: ConversationCategoryFilter) {
        _categoryFilter.value = filter
    }

    fun refreshStatus() {
        _permissionState.value = SmsHelper.hasRequiredPermissions(app)
        _isDefaultSmsState.value = SmsHelper.isDefaultSmsApp(app)
    }

    fun setBiometricUnlocked(unlocked: Boolean) {
        _isBiometricUnlocked.value = unlocked
    }

    fun togglePin(threadId: Long, currentPinned: Boolean) {
        viewModelScope.launch {
            telephonyRepo.setPinned(threadId, !currentPinned)
        }
    }

    fun toggleArchive(threadId: Long, currentArchived: Boolean) {
        viewModelScope.launch {
            telephonyRepo.setArchived(threadId, !currentArchived)
        }
    }

    fun markThreadRead(threadId: Long, isRead: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cv = ContentValues().apply {
                    put(Telephony.Sms.READ, if (isRead) 1 else 0)
                }
                app.contentResolver.update(
                    Telephony.Sms.CONTENT_URI,
                    cv,
                    "${Telephony.Sms.THREAD_ID} = ?",
                    arrayOf(threadId.toString())
                )
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun deleteThread(threadId: Long) {
        viewModelScope.launch {
            telephonyRepo.deleteThread(threadId)
        }
    }

    fun toggleSelectThread(threadId: Long) {
        val current = _selectedThreadIds.value.toMutableSet()
        if (current.contains(threadId)) {
            current.remove(threadId)
        } else {
            current.add(threadId)
        }
        _selectedThreadIds.value = current
    }

    fun clearSelection() {
        _selectedThreadIds.value = emptySet()
    }

    fun deleteSelectedThreads() {
        val toDelete = _selectedThreadIds.value
        clearSelection()
        viewModelScope.launch {
            for (id in toDelete) {
                telephonyRepo.deleteThread(id)
            }
        }
    }

    fun markSelectedAsRead() {
        val selected = _selectedThreadIds.value
        clearSelection()
        viewModelScope.launch(Dispatchers.IO) {
            for (id in selected) {
                try {
                    val cv = ContentValues().apply { put(Telephony.Sms.READ, 1) }
                    app.contentResolver.update(
                        Telephony.Sms.CONTENT_URI,
                        cv,
                        "${Telephony.Sms.THREAD_ID} = ?",
                        arrayOf(id.toString())
                    )
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }
}
