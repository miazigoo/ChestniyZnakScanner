package ru.devandprod.chestniyznak.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import ru.devandprod.chestniyznak.feature.auth.AuthRoute
import ru.devandprod.chestniyznak.feature.auth.AuthViewModel
import ru.devandprod.chestniyznak.feature.scanner.ScanRoute

@Composable
fun AppNavHost() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    when {
        authState.session.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        authState.session.isAuthenticated -> {
            ScanRoute(
                currentUserName = authState.session.displayName.ifBlank { authState.session.username },
                onLogoutRequest = authViewModel::onLogoutRequested,
            )
        }
        else -> {
            AuthRoute(
                state = authState,
                onLoginClicked = authViewModel::onLoginClicked,
            )
        }
    }
}
