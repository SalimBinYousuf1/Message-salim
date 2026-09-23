package com.example.ui.screens.conversation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SalimApplication
import com.example.data.model.Message
import com.example.data.model.SimCardInfo
import com.example.data.preferences.SalimSettings
import com.example.telephony.SmsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConversationDetailUiState(
    val threadId: Long = -1L,
    val address: String = "",
    val displayName: String = "",
    val photoUri: String? = null,
    val messages: List<Message> = emptyList(),
    val composerText: String = "",
    val attachedMediaUri: Uri? = null,
    val availableSims: List<SimCardInfo> = emptyList(),
    val selectedSim: SimCardInfo? = null,
    val isSending: Boolean = false,
    val selectedMessageIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false
)

class ConversationDetailViewModel(
    application: Application,
    private val initialThreadId: Long,
    private val initialAddress: String
) : AndroidViewModel(application) {

    private val app = application as SalimApplication
    private val telephonyRepo = app.telephonyRepository
    private val preferencesRepo = app.preferencesRepository

    val settings: StateFlow<SalimSettings> = preferencesRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SalimSettings())

    private val _threadIdState = MutableStateFlow(initialThreadId)
    private val _addressState = MutableStateFlow(initialAddress)
    private val _composerText = MutableStateFlow("")
    private val _attachedMedia = MutableStateFlow<Uri?>(null)
    private val _availableSims = MutableStateFlow<List<SimCardInfo>>(emptyList())
    private val _selectedSim = MutableStateFlow<SimCardInfo?>(null)
    private val _isSending = MutableStateFlow(false)
    private val _selectedMessageIds = MutableStateFlow<Set<Long>>(emptySet())

    private data class ComposerData(
        val text: String,
        val media: Uri?,
        val isSending: Boolean
    )

    private val _composerData = combine(_composerText, _attachedMedia, _isSending) { text, media, sending ->
        ComposerData(text, media, sending)
    }

    private data class SimData(
        val sims: List<SimCardInfo>,
        val selected: SimCardInfo?
    )

    private val _simData = combine(_availableSims, _selectedSim) { sims, sel ->
        SimData(sims, sel)
    }

    val uiState: StateFlow<ConversationDetailUiState> = combine(
        _composerData,
        _simData,
        _selectedMessageIds
    ) { composer, sim, selectedMsgIds ->
        val (name, photo) = SmsHelper.resolveContact(app, initialAddress)
        ConversationDetailUiState(
            threadId = initialThreadId,
            address = initialAddress,
            displayName = name,
            photoUri = photo,
            messages = emptyList(),
            composerText = composer.text,
            attachedMediaUri = composer.media,
            availableSims = sim.sims,
            selectedSim = sim.selected,
            isSending = composer.isSending,
            selectedMessageIds = selectedMsgIds,
            isSelectionMode = selectedMsgIds.isNotEmpty()
        )
    }.combine(
        telephonyRepo.getMessagesFlow(initialThreadId)
    ) { baseState, messages ->
        baseState.copy(messages = messages)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConversationDetailUiState(
            threadId = initialThreadId,
            address = initialAddress,
            displayName = initialAddress
        )
    )

    init {
        loadSims()
        loadDraftAndMarkRead()
    }

    private fun loadSims() {
        val sims = telephonyRepo.getAvailableSims()
        _availableSims.value = sims
        if (sims.isNotEmpty()) {
            _selectedSim.value = sims[0]
        }
    }

    private fun loadDraftAndMarkRead() {
        viewModelScope.launch {
            if (_threadIdState.value > 0) {
                telephonyRepo.markThreadAsRead(_threadIdState.value)
                val draft = telephonyRepo.getDraft(_threadIdState.value)
                if (!draft.isNullOrBlank()) {
                    _composerText.value = draft
                }
            } else if (_addressState.value.isNotBlank()) {
                val resolvedThreadId = telephonyRepo.getOrCreateThreadId(_addressState.value)
                _threadIdState.value = resolvedThreadId
                telephonyRepo.markThreadAsRead(resolvedThreadId)
            }
        }
    }

    fun onComposerTextChanged(newText: String) {
        _composerText.value = newText
        viewModelScope.launch {
            if (_threadIdState.value > 0) {
                telephonyRepo.saveDraft(_threadIdState.value, if (newText.isBlank()) null else newText)
            }
        }
    }

    fun setAttachedMedia(uri: Uri?) {
        _attachedMedia.value = uri
    }

    fun toggleSim() {
        val list = _availableSims.value
        if (list.size <= 1) return
        val current = _selectedSim.value
        val currentIndex = list.indexOf(current)
        val nextIndex = (currentIndex + 1) % list.size
        _selectedSim.value = list[nextIndex]
    }

    fun sendMessage() {
        val text = _composerText.value.trim()
        val media = _attachedMedia.value
        val address = _addressState.value.ifBlank { uiState.value.address }
        if (text.isBlank() && media == null) return
        if (address.isBlank()) return

        _isSending.value = true
        val threadId = _threadIdState.value
        val subId = _selectedSim.value?.subscriptionId ?: -1

        viewModelScope.launch {
            val success = telephonyRepo.sendMessage(
                threadId = threadId,
                destinationAddress = address,
                messageText = text,
                subId = subId
            )
            if (success) {
                _composerText.value = ""
                _attachedMedia.value = null
            }
            _isSending.value = false
        }
    }

    fun retrySendMessage(message: Message) {
        viewModelScope.launch {
            telephonyRepo.sendMessage(
                threadId = message.threadId,
                destinationAddress = message.address,
                messageText = message.body,
                subId = message.subId
            )
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            telephonyRepo.deleteMessage(messageId)
        }
    }

    fun toggleSelectMessage(messageId: Long) {
        val current = _selectedMessageIds.value.toMutableSet()
        if (current.contains(messageId)) {
            current.remove(messageId)
        } else {
            current.add(messageId)
        }
        _selectedMessageIds.value = current
    }

    fun clearSelection() {
        _selectedMessageIds.value = emptySet()
    }

    fun deleteSelectedMessages() {
        val toDelete = _selectedMessageIds.value
        clearSelection()
        viewModelScope.launch {
            toDelete.forEach { msgId ->
                telephonyRepo.deleteMessage(msgId)
            }
        }
    }
}
