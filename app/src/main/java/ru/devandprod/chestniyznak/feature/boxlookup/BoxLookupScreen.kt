package ru.devandprod.chestniyznak.feature.boxlookup

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import ru.devandprod.chestniyznak.core.designsystem.theme.CurrentAppThemeSpec
import ru.devandprod.chestniyznak.core.designsystem.theme.ThemedAppBackground
import ru.devandprod.chestniyznak.core.scanner.HidScannerInputBus
import ru.devandprod.chestniyznak.feature.scanner.ScanResultCardUi
import ru.devandprod.chestniyznak.feature.scanner.ScanResultTone
import ru.devandprod.chestniyznak.feature.scanner.StatusCard

@Composable
fun BoxLookupRoute(
    onBack: () -> Unit,
    onOpenBox: (Long) -> Unit,
    viewModel: BoxLookupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        HidScannerInputBus.scannedCodes().collect(viewModel::onCodeScanned)
    }

    LaunchedEffect(viewModel) {
        viewModel.openBoxEvents.collect(onOpenBox)
    }

    BoxLookupScreen(
        state = state,
        onBack = onBack,
        onResetStatus = viewModel::onResetStatus,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxLookupScreen(
    state: BoxLookupUiState,
    onBack: () -> Unit,
    onResetStatus: () -> Unit,
) {
    val themeSpec = CurrentAppThemeSpec
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Просмотреть коробку") },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HidScannerInputField(modifier = Modifier.size(1.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    color = themeSpec.decorColors.panelSurface.copy(alpha = 0.92f),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(164.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(30.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(20.dp),
                        ) {
                            Text(
                                text = if (state.isBusy) "Поиск коробки..." else "Сканируйте штрихкод коробки",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "ТСД-сканер должен считать SSCC коробки или ее ID.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                    }
                }

                (state.errorText?.let {
                    ScanResultCardUi(
                        headline = "NO",
                        message = it,
                        tone = ScanResultTone.Error,
                    )
                } ?: state.lastScannedCode.takeIf(String::isNotBlank)?.let {
                    ScanResultCardUi(
                        headline = "OK",
                        message = state.statusText,
                        tone = if (state.isBusy) ScanResultTone.Warning else ScanResultTone.Success,
                    )
                })?.let { StatusCard(result = it) }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    color = themeSpec.decorColors.panelSurface.copy(alpha = 0.92f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = state.statusText,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (state.lastScannedCode.isNotBlank()) {
                            Text(
                                text = state.lastScannedCode,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Button(
                            onClick = onResetStatus,
                            enabled = !state.isBusy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Сбросить статус")
                        }
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
