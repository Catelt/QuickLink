package com.catelt.quicklink.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.catelt.quicklink.data.QuickLinkDataStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class QuickLinkViewModel(
    private val dataStore: QuickLinkDataStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuickLinkState())
    val uiState: StateFlow<QuickLinkState> = _uiState.asStateFlow()

    private val _quickLinkEvent =
        MutableSharedFlow<QuickLinkEvent>(replay = 1, extraBufferCapacity = 1)
    val quickLinkEvent = _quickLinkEvent.asSharedFlow()

    var deeplinkInput by mutableStateOf("")
        private set


    init {
        loadLinksLocal()
    }

    fun updateInput(value: String) {
        deeplinkInput = value
    }

    fun copyToClipboard(url: String) {
        if (url.isBlank()) {
            _quickLinkEvent.tryEmit(QuickLinkEvent.ShowError("URL cannot be empty"))
        } else {
            _quickLinkEvent.tryEmit(QuickLinkEvent.CopyToClipboard(url))
        }
    }

    fun saveLink(value: String) {
        val newLinks = _uiState.value.links.toMutableList()
        newLinks.remove(value)
        newLinks.add(value)
        _uiState.value = _uiState.value.copy(links = newLinks)
        saveLinkLocal(value)
    }

    fun removeLink(value: String) {
        // Remove from UI state
        val newLinks = _uiState.value.links.toMutableList()
        newLinks.remove(value)
        _uiState.value = _uiState.value.copy(links = newLinks)

        // Remove from local storage
        removeLinkLocal(value)
    }

    fun openLink() {
        val url = deeplinkInput
        openSpecificLink(url)
    }

    fun openSpecificLink(url: String) {
        if (url.isBlank()) {
            _quickLinkEvent.tryEmit(QuickLinkEvent.ShowError("URL cannot be empty"))
        } else {
            _quickLinkEvent.tryEmit(QuickLinkEvent.OpenLink(url))
        }
    }

    private fun saveLinkLocal(link: String) {
        viewModelScope.launch {
            dataStore.saveLink(link)
        }
    }

    private fun removeLinkLocal(link: String) {
        viewModelScope.launch {
            dataStore.removeLink(link)
        }
    }

    private fun loadLinksLocal() {
        viewModelScope.launch {
            dataStore.links.collectLatest { savedLinks ->
                _uiState.value = QuickLinkState(links = savedLinks.toList())
            }
        }
    }


    companion object {
        fun createFactory(dataStore: QuickLinkDataStore): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    QuickLinkViewModel(dataStore)
                }
            }
        }

//        val Factory: ViewModelProvider.Factory = viewModelFactory {
//            initializer {
////                val savedStateHandle = createSavedStateHandle()
////                val myRepository = (this[APPLICATION_KEY] as MyApplication).myRepository
//                QuickLinkViewModel()
//            }
//        }
    }
}