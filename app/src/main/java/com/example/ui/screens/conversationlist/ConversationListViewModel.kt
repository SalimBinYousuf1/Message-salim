package com.example.ui.screens.conversationlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SalimApplication
import com.example.data.model.Conversation
import com.example.data.preferences.SalimSettings
import com.example.telephony.SmsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConversationListUiState(
    val conversations: List<Conversation> = emptyList(),
    val pinnedConversations: List<Conversation> = emptyList(),
    val unpinnedConversations: List<Conversation> = emptyList(),
    val searchQuery: String = "",
    val filterArchived: Boolean = false,
    val isDefaultSmsApp: Boolean = true,
    val hasSmsPermission: Boolean = true,
    val selectedThreadIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false
)

class ConversationListViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SalimApplication
    private val telephonyRepo = app.telephonyRepository
    private val preferencesRepo = app.preferencesRepository

    val settings: StateFlow<SalimSettings> = preferencesRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SalimSettings())

    private val _searchQuery = MutableStateFlow("")
    private val _filterArchived = MutableStateFlow(false)
    private val _selectedThreadIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _permissionState = MutableStateFlow(SmsHelper.hasRequiredPermissions(application))
    private val _isDefaultSmsState = MutableStateFlow(SmsHelper.isDefaultSmsApp(application))

    private val _filterState = combine(_searchQuery, _filterArchived, _selectedThreadIds) { query, archived, selected ->
        Triple(query, archived, selected)
    }

    private val _systemStatus = combine(_permissionState, _isDefaultSmsState) { perm, defaultSms ->
        Pair(perm, defaultSms)
    }

    val uiState: StateFlow<ConversationListUiState> = combine(
        telephonyRepo.conversationsFlow,
        _filterState,
        _systemStatus
    ) { conversations: List<Conversation>, filter, status ->
        val (query, filterArchived, selectedIds) = filter
        val (hasPerm, isDefault) = status

        val filtered = conversations.filter { conv ->
            val matchesArchive = if (filterArchived) conv.isArchived else !conv.isArchived
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                conv.displayName.contains(query, ignoreCase = true) ||
                        conv.address.contains(query, ignoreCase = true) ||
                        conv.snippet.contains(query, ignoreCase = true)
            }
            matchesArchive && matchesQuery
        }

        val pinned = filtered.filter { it.isPinned }
        val unpinned = filtered.filter { !it.isPinned }

        ConversationListUiState(
            conversations = filtered,
            pinnedConversations = pinned,
            unpinnedConversations = unpinned,
            searchQuery = query,
            filterArchived = filterArchived,
            isDefaultSmsApp = isDefault,
            hasSmsPermission = hasPerm,
            selectedThreadIds = selectedIds,
            isSelectionMode = selectedIds.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConversationListUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleFilterArchived() {
        _filterArchived.value = !_filterArchived.value
    }

    fun refreshStatus() {
        _permissionState.value = SmsHelper.hasRequiredPermissions(app)
        _isDefaultSmsState.value = SmsHelper.isDefaultSmsApp(app)
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
            toDelete.forEach { threadId ->
                telephonyRepo.deleteThread(threadId)
            }
        }
    }
}
