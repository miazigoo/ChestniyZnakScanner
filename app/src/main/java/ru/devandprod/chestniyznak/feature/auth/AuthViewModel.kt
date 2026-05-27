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
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.i18n.AppStringProvider
import ru.devandprod.chestniyznak.domain.auth.AuthTokenExtractor
import ru.devandprod.chestniyznak.domain.usecase.LoginUseCase
import ru.devandprod.chestniyznak.domain.usecase.LogoutUseCase
import ru.devandprod.chestniyznak.domain.usecase.ObserveAuthSessionUseCase
import ru.devandprod.chestniyznak.domain.usecase.RestoreSessionUseCase

@HiltViewModel
class AuthViewModel @Inject constructor(
    observeAuthSessionUseCase: ObserveAuthSessionUseCase,
    private val strings: AppStringProvider,
    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            statusMessage = strings.get(R.string.auth_status_waiting_token),
        ),
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private var lastToken: String? = null

    init {
        viewModelScope.launch {
            observeAuthSessionUseCase().collect { session ->
                _uiState.update { state ->
                    state.copy(
                        session = session,
                        isSubmitting = false,
                        errorMessage = if (session.isAuthenticated) null else state.errorMessage,
                        statusMessage = if (session.isAuthenticated) {
                            strings.get(R.string.auth_status_login_success)
                        } else {
                            state.statusMessage
                        },
                    )
                }
            }
        }

        viewModelScope.launch {
            restoreSessionUseCase()
        }
    }

    fun onTokenScanned(rawValue: String) {
        if (_uiState.value.isSubmitting) return

        val token = AuthTokenExtractor.extract(rawValue)
        if (token.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = strings.get(R.string.auth_error_token_missing),
                    statusMessage = strings.get(R.string.auth_status_scan_another_token),
                    isCameraScannerEnabled = false,
                )
            }
            return
        }

        lastToken = token
        submitLogin(token)
    }

    fun onLoginClicked() {
        val token = lastToken ?: run {
            _uiState.update {
                it.copy(
                    errorMessage = strings.get(R.string.auth_error_scan_token_first),
                    statusMessage = strings.get(R.string.auth_status_waiting_token),
                )
            }
            return
        }
        submitLogin(token)
    }

    fun onCameraScannerRearmRequested() {
        _uiState.update {
            it.copy(
                isCameraScannerEnabled = !it.isSubmitting,
            )
        }
    }

    fun onLogoutRequested() {
        viewModelScope.launch {
            logoutUseCase()
            lastToken = null
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    errorMessage = null,
                    tokenPreview = null,
                    statusMessage = strings.get(R.string.auth_status_logged_out),
                    isCameraScannerEnabled = true,
                )
            }
        }
    }

    private fun submitLogin(token: String) {
        _uiState.update {
            it.copy(
                isSubmitting = true,
                errorMessage = null,
                tokenPreview = token.maskToken(),
                statusMessage = strings.get(R.string.auth_status_signing_in),
                isCameraScannerEnabled = false,
            )
        }
        viewModelScope.launch {
            runCatching {
                loginUseCase(token)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: strings.get(R.string.auth_error_login_failed),
                        statusMessage = strings.get(R.string.auth_status_auth_failed),
                        isCameraScannerEnabled = false,
                    )
                }
            }
        }
    }

    private fun String.maskToken(): String {
        if (length <= 8) return strings.get(R.string.auth_token_accepted)
        return "${take(4)}••••${takeLast(4)}"
    }
}
