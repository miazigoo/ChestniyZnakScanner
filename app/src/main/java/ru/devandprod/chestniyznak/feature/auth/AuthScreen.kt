package ru.devandprod.chestniyznak.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import ru.devandprod.chestniyznak.core.designsystem.theme.Accent
import ru.devandprod.chestniyznak.core.designsystem.theme.Error
import ru.devandprod.chestniyznak.core.designsystem.theme.Ink
import ru.devandprod.chestniyznak.core.designsystem.theme.Sand
import ru.devandprod.chestniyznak.core.designsystem.theme.Slate

@Composable
fun AuthRoute(
    state: AuthUiState,
    onLoginClicked: () -> Unit,
) {
    AuthScreen(
        state = state,
        onLoginClicked = onLoginClicked,
    )
}

@Composable
fun AuthScreen(
    state: AuthUiState,
    onLoginClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Sand, Sand.copy(alpha = 0.92f)),
                ),
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Вход в Честный знак",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Ink,
                )
                Text(
                    text = "Сервер: srv-dnp.argos.loc /api/v2/",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Slate,
                )
                Text(
                    text = "Тестовый токен прописан в приложении. Авторизация выполняется автоматически.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink,
                )
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Error,
                    )
                }
                Button(
                    onClick = onLoginClicked,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            color = Accent,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    } else {
                        Text("Повторить авторизацию")
                    }
                }
            }
        }
    }
}
