package ru.devandprod.chestniyznak.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.devandprod.chestniyznak.BuildConfig
import ru.devandprod.chestniyznak.domain.usecase.LoginUseCase
import ru.devandprod.chestniyznak.domain.usecase.LogoutUseCase
import ru.devandprod.chestniyznak.domain.usecase.ObserveAuthSessionUseCase
import ru.devandprod.chestniyznak.domain.usecase.RestoreSessionUseCase

@HiltViewModel
class AuthViewModel @Inject constructor(
    observeAuthSessionUseCase: ObserveAuthSessionUseCase,
    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeAuthSessionUseCase().collect { session ->
                _uiState.update { state ->
                    state.copy(
                        session = session,
                        isSubmitting = false,
                        errorMessage = if (session.isAuthenticated) null else state.errorMessage,
                    )
                }
            }
        }

        viewModelScope.launch {
            restoreSessionUseCase()
        }
    }

    fun onLoginClicked() {
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                loginUseCase(BuildConfig.AUTH_TOKEN)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "Не удалось войти",
                    )
                }
            }
        }
    }

    fun onLogoutRequested() {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }
}
