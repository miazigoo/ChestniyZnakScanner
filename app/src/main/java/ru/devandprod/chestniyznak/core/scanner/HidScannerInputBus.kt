package ru.devandprod.chestniyznak.core.scanner

import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object HidScannerInputBus {
    private const val INPUT_TIMEOUT_MS = 1_000L
    private const val DEDUPE_WINDOW_MS = 750L

    private val scannedCodesFlow = MutableSharedFlow<String>(extraBufferCapacity = 32)
    private val buffer = StringBuilder()
    private var lastEventTimestamp = 0L
    private var lastEmittedCode = ""
    private var lastEmittedAt = 0L

    fun scannedCodes(): SharedFlow<String> = scannedCodesFlow.asSharedFlow()

    fun onTextCommitted(text: String) {
        val code = text.trim().trim('\r', '\n', '\t')
        if (code.isBlank()) return
        emitCode(code)
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            return false
        }

        val now = event.eventTime
        if (buffer.isNotEmpty() && now - lastEventTimestamp > INPUT_TIMEOUT_MS) {
            buffer.clear()
        }
        lastEventTimestamp = now

        return when (event.keyCode) {
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_TAB -> emitBufferedCode()
            KeyEvent.KEYCODE_DEL -> {
                if (buffer.isNotEmpty()) {
                    buffer.deleteCharAt(buffer.lastIndex)
                    true
                } else {
                    false
                }
            }
            else -> {
                val unicodeChar = event.unicodeChar
                if (unicodeChar <= 0 || Character.isISOControl(unicodeChar)) {
                    false
                } else {
                    buffer.append(unicodeChar.toChar())
                    true
                }
            }
        }
    }

    private fun emitBufferedCode(): Boolean {
        val code = buffer.toString().trim()
        buffer.clear()
        if (code.isBlank()) {
            return false
        }
        emitCode(code)
        return true
    }

    private fun emitCode(code: String) {
        val now = System.currentTimeMillis()
        if (code == lastEmittedCode && now - lastEmittedAt < DEDUPE_WINDOW_MS) {
            return
        }
        lastEmittedCode = code
        lastEmittedAt = now
        scannedCodesFlow.tryEmit(code)
    }
}
