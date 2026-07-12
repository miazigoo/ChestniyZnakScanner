package ru.devandprod.chestniyznak.core.scanner

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun HidScannerInputField(
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            EditText(context).apply {
                val inputMethodManager = context.getSystemService(InputMethodManager::class.java)
                fun hideKeyboard() {
                    inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
                }

                isSingleLine = true
                isFocusable = true
                isFocusableInTouchMode = true
                imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                showSoftInputOnFocus = false
                isCursorVisible = false
                background = null
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                setTextColor(android.graphics.Color.TRANSPARENT)
                setHintTextColor(android.graphics.Color.TRANSPARENT)
                setOnClickListener { hideKeyboard() }
                setOnLongClickListener {
                    hideKeyboard()
                    true
                }
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
                val scannerInput = this
                val emitRunnable = Runnable {
                    val text = this.text?.toString().orEmpty()
                    if (text.isNotBlank()) {
                        hideKeyboard()
                        HidScannerInputBus.onTextCommitted(text)
                        setText("")
                        post { hideKeyboard() }
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
                                hideKeyboard()
                                HidScannerInputBus.onTextCommitted(text)
                                setText("")
                                scannerInput.post { hideKeyboard() }
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
            val inputMethodManager = editText.context.getSystemService(InputMethodManager::class.java)
            inputMethodManager?.hideSoftInputFromWindow(editText.windowToken, 0)
        },
        modifier = modifier,
    )
}
