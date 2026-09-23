package com.example.ui.screens.compose

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SalimApplication
import com.example.data.model.ContactItem
import com.example.data.preferences.SalimSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NewMessageUiState(
    val recipientInput: String = "",
    val suggestedContacts: List<ContactItem> = emptyList(),
    val selectedRecipients: List<String> = emptyList(),
    val composerText: String = "",
    val isSending: Boolean = false,
    val createdThreadId: Long? = null
)

class NewMessageViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SalimApplication
    private val contactsRepo = app.contactsRepository
    private val telephonyRepo = app.telephonyRepository
    private val preferencesRepo = app.preferencesRepository

    val settings: StateFlow<SalimSettings> = preferencesRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SalimSettings())

    private val _recipientInput = MutableStateFlow("")
    private val _suggestedContacts = MutableStateFlow<List<ContactItem>>(emptyList())
    private val _selectedRecipients = MutableStateFlow<List<String>>(emptyList())
    private val _composerText = MutableStateFlow("")
    private val _isSending = MutableStateFlow(false)
    private val _createdThreadId = MutableStateFlow<Long?>(null)

    private data class RecipientState(
        val input: String,
        val suggested: List<ContactItem>,
        val selected: List<String>
    )

    private val _recipientState = combine(_recipientInput, _suggestedContacts, _selectedRecipients) { input, contacts, selected ->
        RecipientState(input, contacts, selected)
    }

    private data class ComposerSendState(
        val text: String,
        val isSending: Boolean,
        val threadId: Long?
    )

    private val _composerSendState = combine(_composerText, _isSending, _createdThreadId) { text, sending, threadId ->
        ComposerSendState(text, sending, threadId)
    }

    val uiState: StateFlow<NewMessageUiState> = combine(
        _recipientState,
        _composerSendState
    ) { recipient, composer ->
        NewMessageUiState(
            recipientInput = recipient.input,
            suggestedContacts = recipient.suggested,
            selectedRecipients = recipient.selected,
            composerText = composer.text,
            isSending = composer.isSending,
            createdThreadId = composer.threadId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NewMessageUiState()
    )

    init {
        searchContacts("")
    }

    fun onRecipientInputChanged(input: String) {
        _recipientInput.value = input
        searchContacts(input)
    }

    fun onComposerTextChanged(text: String) {
        _composerText.value = text
    }

    private fun searchContacts(query: String) {
        viewModelScope.launch {
            _suggestedContacts.value = contactsRepo.getContacts(query)
        }
    }

    fun selectContact(contact: ContactItem) {
        val current = _selectedRecipients.value.toMutableList()
        if (!current.contains(contact.phoneNumber)) {
            current.add(contact.phoneNumber)
            _selectedRecipients.value = current
        }
        _recipientInput.value = ""
        searchContacts("")
    }

    fun addManualNumber(number: String) {
        val trimmed = number.trim()
        if (trimmed.isNotBlank()) {
            val current = _selectedRecipients.value.toMutableList()
            if (!current.contains(trimmed)) {
                current.add(trimmed)
                _selectedRecipients.value = current
            }
            _recipientInput.value = ""
            searchContacts("")
        }
    }

    fun removeRecipient(number: String) {
        val current = _selectedRecipients.value.toMutableList()
        current.remove(number)
        _selectedRecipients.value = current
    }

    fun sendFirstMessage(onSuccess: (threadId: Long, address: String) -> Unit) {
        val text = _composerText.value.trim()
        val recipients = _selectedRecipients.value.toMutableList()
        val manual = _recipientInput.value.trim()
        if (manual.isNotBlank() && !recipients.contains(manual)) {
            recipients.add(manual)
        }

        if (recipients.isEmpty() || text.isBlank()) return

        _isSending.value = true
        viewModelScope.launch {
            val primaryRecipient = recipients[0]
            val threadId = telephonyRepo.getOrCreateThreadId(primaryRecipient)

            recipients.forEach { recipient ->
                telephonyRepo.sendMessage(
                    threadId = threadId,
                    destinationAddress = recipient,
                    messageText = text
                )
            }

            _isSending.value = false
            onSuccess(threadId, primaryRecipient)
        }
    }
}
