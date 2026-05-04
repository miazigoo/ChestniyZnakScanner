package ru.devandprod.chestniyznak.feature.boxedit

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import ru.devandprod.chestniyznak.core.designsystem.theme.CurrentAppDecorColors
import ru.devandprod.chestniyznak.core.designsystem.theme.ThemedAppBackground
import ru.devandprod.chestniyznak.core.scanner.HidScannerInputBus

@Composable
fun BoxEditRoute(
    onBack: () -> Unit,
    viewModel: BoxEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        HidScannerInputBus.scannedCodes().collect(viewModel::onCodeScanned)
    }

    LaunchedEffect(viewModel) {
        viewModel.boxDeleted.collect { onBack() }
    }

    BoxEditScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onAddRequested = viewModel::onAddRequested,
        onClearActionRequested = viewModel::onClearActionRequested,
        onConfirmClearAction = viewModel::onConfirmClearAction,
        onDismissClearDialog = viewModel::onDismissClearDialog,
        onItemLongPressed = viewModel::onItemLongPressed,
        onDismissItemMenu = viewModel::onDismissItemMenu,
        onRemoveItemRequested = viewModel::onRemoveItemRequested,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BoxEditScreen(
    state: BoxEditUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAddRequested: () -> Unit,
    onClearActionRequested: () -> Unit,
    onConfirmClearAction: () -> Unit,
    onDismissClearDialog: () -> Unit,
    onItemLongPressed: (Long) -> Unit,
    onDismissItemMenu: () -> Unit,
    onRemoveItemRequested: (Long) -> Unit,
) {
    val decor = CurrentAppDecorColors
    state.box?.let { box ->
        if (state.confirmClearDialog) {
            AlertDialog(
                onDismissRequest = onDismissClearDialog,
                title = {
                    Text(if (box.items.isEmpty()) "Удалить коробку?" else "Удалить все коды?")
                },
                text = {
                    Text(
                        if (box.items.isEmpty()) {
                            "Пустая коробка будет удалена без возможности восстановления."
                        } else {
                            "Все коды будут удалены из коробки. Это действие нельзя отменить."
                        },
                    )
                },
                confirmButton = {
                    TextButton(onClick = onConfirmClearAction) {
                        Text("Подтвердить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissClearDialog) {
                        Text("Отмена")
                    }
                },
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.title)
                        state.statusText.takeIf(String::isNotBlank)?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Назад")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        ThemedAppBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@ThemedAppBackground
            }

            val box = state.box ?: return@ThemedAppBackground

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    HidScannerInputField(modifier = Modifier.size(1.dp))
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onAddRequested,
                            enabled = !state.isBusy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (state.isAwaitingScan) "Сканируйте..." else "Добавить")
                        }
                        Button(
                            onClick = onClearActionRequested,
                            enabled = !state.isBusy,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) {
                            Text(if (box.items.isEmpty()) "Удалить коробку" else "Удалить все")
                        }
                    }
                }
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(26.dp)),
                        shape = RoundedCornerShape(26.dp),
                        color = decor.panelSurface.copy(alpha = 0.92f),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("ID: ${box.boxId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            box.orderName?.takeIf(String::isNotBlank)?.let { Text("Заказ: $it", style = MaterialTheme.typography.bodyMedium) }
                            box.sscc?.takeIf(String::isNotBlank)?.let { Text("SSCC: $it", style = MaterialTheme.typography.bodyMedium) }
                            Text("Наполнение: ${box.filled}/${box.capacity}", style = MaterialTheme.typography.bodyMedium)
                            if (state.lastScannedCode.isNotBlank()) {
                                Text(
                                    text = state.lastScannedCode,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            state.errorText?.takeIf(String::isNotBlank)?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (state.isAwaitingScan) {
                                "Ожидание сканирования встроенным сканером"
                            } else {
                                "Нажмите \"Добавить\", чтобы разрешить сканирование"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                item {
                    Text(
                        text = "Коды в коробке",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(box.items, key = { it.id }) { item ->
                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { onItemLongPressed(item.id) },
                                )
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(22.dp)),
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = item.visibleCode,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "GTIN: ${item.gtin}  •  SN: ${item.serial}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = state.itemMenuItemId == item.id,
                            onDismissRequest = onDismissItemMenu,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Удалить") },
                                onClick = { onRemoveItemRequested(item.id) },
                            )
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onRefresh,
                        enabled = !state.isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Обновить")
                    }
                }
            }
        }
    }
}

@Composable
private fun HidScannerInputField(
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            EditText(context).apply {
                val imm = context.getSystemService(InputMethodManager::class.java)
                fun hideKeyboard() {
                    imm?.hideSoftInputFromWindow(windowToken, 0)
                }
                isSingleLine = true
                isFocusable = true
                isFocusableInTouchMode = true
                imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                showSoftInputOnFocus = false
                isCursorVisible = false
                background = null
                setTextColor(android.graphics.Color.TRANSPARENT)
                setHintTextColor(android.graphics.Color.TRANSPARENT)
                requestFocus()
                post { hideKeyboard() }
                setOnFocusChangeListener { view, hasFocus ->
                    if (!hasFocus) {
                        view.post {
                            view.requestFocus()
                            hideKeyboard()
                        }
                    } else {
                        hideKeyboard()
                    }
                }
                val emitRunnable = Runnable {
                    val text = this.text?.toString().orEmpty()
                    if (text.isNotBlank()) {
                        HidScannerInputBus.onTextCommitted(text)
                        setText("")
                    }
                }
                addTextChangedListener(
                    object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                        override fun afterTextChanged(editable: Editable?) {
                            val text = editable?.toString().orEmpty()
                            removeCallbacks(emitRunnable)
                            if (text.contains('\n') || text.contains('\r') || text.contains('\t')) {
                                HidScannerInputBus.onTextCommitted(text)
                                setText("")
                            } else if (text.isNotBlank()) {
                                postDelayed(emitRunnable, 180L)
                            }
                        }
                    },
                )
            }
        },
        update = { editText ->
            if (!editText.hasFocus()) {
                editText.post { editText.requestFocus() }
            }
            val imm = editText.context.getSystemService(InputMethodManager::class.java)
            imm?.hideSoftInputFromWindow(editText.windowToken, 0)
        },
        modifier = modifier,
    )
}
