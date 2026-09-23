package com.example.ui.screens.conversation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SalimApplication
import com.example.data.local.BlockedContact
import com.example.data.local.ScheduledMessage
import com.example.data.model.Message
import com.example.data.model.SimCardInfo
import com.example.data.preferences.SalimSettings
import com.example.telephony.AudioPlayerHelper
import com.example.telephony.AudioRecorderHelper
import com.example.telephony.ScheduledSmsManager
import com.example.telephony.SmsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val isSelectionMode: Boolean = false,
    val isBlocked: Boolean = false,
    val pendingScheduled: List<ScheduledMessage> = emptyList(),
    val isRecordingVoice: Boolean = false,
    val recordingDurationSec: Int = 0,
    val recordingAmplitude: Float = 0f,
    val replyingToMessage: Message? = null
)

class ConversationDetailViewModel(
    application: Application,
    private val initialThreadId: Long,
    private val initialAddress: String
) : AndroidViewModel(application) {

    private val app = application as SalimApplication
    private val telephonyRepo = app.telephonyRepository
    private val preferencesRepo = app.preferencesRepository
    private val scheduledDao = app.database.scheduledMessageDao()
    private val blockedDao = app.database.blockedContactDao()
    private val conversationDao = app.database.conversationDao()

    val audioRecorder = AudioRecorderHelper(app)
    val audioPlayer = AudioPlayerHelper()

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
    private val _isBlockedState = MutableStateFlow(false)
    private val _isRecordingVoice = MutableStateFlow(false)
    private val _replyingToMessage = MutableStateFlow<Message?>(null)

    private data class ComposerData(
        val text: String,
        val media: Uri?,
        val isSending: Boolean,
        val isRecording: Boolean,
        val replyingTo: Message?
    )

    private val _composerData = combine(
        _composerText,
        _attachedMedia,
        _isSending,
        _isRecordingVoice,
        _replyingToMessage
    ) { text, media, sending, recording, replyingTo ->
        ComposerData(text, media, sending, recording, replyingTo)
    }

    private data class SimData(
        val sims: List<SimCardInfo>,
        val selected: SimCardInfo?
    )

    private val _simData = combine(_availableSims, _selectedSim) { sims, sel ->
        SimData(sims, sel)
    }

    private data class StatusData(
        val selectedIds: Set<Long>,
        val isBlocked: Boolean,
        val scheduledList: List<ScheduledMessage>
    )

    private val _statusData = combine(
        _selectedMessageIds,
        _isBlockedState,
        scheduledDao.getPendingByThreadFlow(initialThreadId)
    ) { selectedIds, blocked, scheduled ->
        StatusData(selectedIds, blocked, scheduled)
    }

    val uiState: StateFlow<ConversationDetailUiState> = combine(
        _composerData,
        _simData,
        _statusData
    ) { composer, sim, status ->
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
            selectedMessageIds = status.selectedIds,
            isSelectionMode = status.selectedIds.isNotEmpty(),
            isBlocked = status.isBlocked,
            pendingScheduled = status.scheduledList,
            isRecordingVoice = composer.isRecording,
            recordingDurationSec = audioRecorder.recordingDurationSeconds.value,
            recordingAmplitude = audioRecorder.currentAmplitude.value,
            replyingToMessage = composer.replyingTo
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
        loadSimCardsAndMetadata()
        checkBlockedStatus()
    }

    private fun loadSimCardsAndMetadata() {
        viewModelScope.launch {
            val sims = SmsHelper.getAvailableSims(app)
            _availableSims.value = sims

            // Check if thread has a preferred SIM
            val metadata = conversationDao.getMetadata(initialThreadId)
            val preferredSubId = metadata?.preferredSubId

            val matchedSim = if (preferredSubId != null && preferredSubId != -1) {
                sims.find { it.subscriptionId == preferredSubId }
            } else {
                null
            }

            _selectedSim.value = matchedSim ?: sims.firstOrNull()

            // Load saved draft if any
            if (metadata?.draft != null) {
                _composerText.value = metadata.draft
            }
        }
    }

    private fun checkBlockedStatus() {
        viewModelScope.launch {
            val blocked = blockedDao.isBlocked(initialAddress)
            _isBlockedState.value = blocked
        }
    }

    fun onComposerTextChanged(text: String) {
        _composerText.value = text
        // Persist draft
        viewModelScope.launch {
            conversationDao.saveDraft(initialThreadId, text.ifBlank { null })
        }
    }

    fun onMediaAttached(uri: Uri?) {
        _attachedMedia.value = uri
    }

    fun setReplyingTo(message: Message?) {
        _replyingToMessage.value = message
    }

    fun toggleSim() {
        val list = _availableSims.value
        if (list.size <= 1) return
        val current = _selectedSim.value
        val currentIndex = list.indexOf(current)
        val nextIndex = (currentIndex + 1) % list.size
        val newSim = list[nextIndex]
        _selectedSim.value = newSim

        // Bind preference to this conversation
        viewModelScope.launch {
            conversationDao.setPreferredSubId(initialThreadId, newSim.subscriptionId)
        }
    }

    fun startVoiceRecording() {
        if (audioRecorder.startRecording()) {
            _isRecordingVoice.value = true
        }
    }

    fun stopVoiceRecordingAndSend() {
        val recordedFile = audioRecorder.stopRecording()
        _isRecordingVoice.value = false
        if (recordedFile != null && recordedFile.exists()) {
            val uri = Uri.fromFile(recordedFile)
            _attachedMedia.value = uri
            sendMessage()
        }
    }

    fun cancelVoiceRecording() {
        audioRecorder.cancelRecording()
        _isRecordingVoice.value = false
    }

    fun sendMessage() {
        val rawText = _composerText.value.trim()
        val media = _attachedMedia.value
        val address = _addressState.value.ifBlank { uiState.value.address }
        if (rawText.isBlank() && media == null) return
        if (address.isBlank()) return

        val reply = _replyingToMessage.value
        val textToSend = if (reply != null && rawText.isNotBlank()) {
            "Re: \"${reply.body.take(24)}...\": $rawText"
        } else {
            rawText
        }

        _isSending.value = true
        val threadId = _threadIdState.value
        val subId = _selectedSim.value?.subscriptionId ?: -1

        viewModelScope.launch {
            val success = telephonyRepo.sendMessage(
                threadId = threadId,
                destinationAddress = address,
                messageText = textToSend,
                subId = subId
            )
            if (success) {
                _composerText.value = ""
                _attachedMedia.value = null
                _replyingToMessage.value = null
                conversationDao.saveDraft(threadId, null)
            }
            _isSending.value = false
        }
    }

    fun scheduleMessage(scheduledTimestamp: Long) {
        val text = _composerText.value.trim()
        if (text.isBlank()) return
        val subId = _selectedSim.value?.subscriptionId ?: -1

        viewModelScope.launch {
            val scheduled = ScheduledMessage(
                threadId = initialThreadId,
                address = initialAddress,
                body = text,
                subId = subId,
                scheduledTimestamp = scheduledTimestamp
            )
            val newId = scheduledDao.insert(scheduled)
            val inserted = scheduled.copy(id = newId)
            ScheduledSmsManager.scheduleMessage(app, inserted)
            _composerText.value = ""
            conversationDao.saveDraft(initialThreadId, null)
        }
    }

    fun cancelScheduledMessage(id: Long) {
        viewModelScope.launch {
            ScheduledSmsManager.cancelSchedule(app, id)
            scheduledDao.delete(id)
        }
    }

    fun toggleBlockCurrentContact() {
        viewModelScope.launch {
            val currentBlocked = _isBlockedState.value
            if (currentBlocked) {
                blockedDao.unblock(initialAddress)
                _isBlockedState.value = false
            } else {
                blockedDao.block(
                    BlockedContact(
                        address = initialAddress,
                        displayName = uiState.value.displayName
                    )
                )
                _isBlockedState.value = true
            }
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

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
        audioRecorder.cancelRecording()
    }
}
