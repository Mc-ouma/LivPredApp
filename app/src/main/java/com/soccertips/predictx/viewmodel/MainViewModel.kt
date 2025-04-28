package com.soccertips.predictx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predictx.repository.PreloadNetworkState
import com.soccertips.predictx.repository.PreloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preloadRepository: PreloadRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preloadRepository.networkState.collect { state ->
                when (state) {
                    PreloadNetworkState.Loading -> {
                        _uiState.value = UiState.Loading
                    }

                    PreloadNetworkState.Available -> {
                        _uiState.value = UiState.Success
                    }

                    PreloadNetworkState.Unavailable -> {
                        _uiState.value = UiState.NetworkError
                    }

                    PreloadNetworkState.Done -> {
                        _uiState.value = UiState.Success
                    }
                }
            }
        }

    }

    fun retryNetworkOperation() {
        viewModelScope.launch {
            preloadRepository.preloadCategoryData()
        }
    }

    sealed class UiState {
        object Initial : UiState()
        object Loading : UiState()
        object NetworkError : UiState()
        object Success : UiState()
    }

}