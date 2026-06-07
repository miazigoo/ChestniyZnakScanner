package ru.devandprod.chestniyznak

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import ru.devandprod.chestniyznak.app.ChestniyZnakApp
import ru.devandprod.chestniyznak.core.scanner.HidScannerInputBus

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var debugScanReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
        )
        hideKeyboard()
        setContent {
            ChestniyZnakApp()
        }
        registerDebugScanReceiver()
    }

    override fun onResume() {
        super.onResume()
        hideKeyboard()
    }

    override fun onDestroy() {
        debugScanReceiver?.let(::unregisterReceiver)
        debugScanReceiver = null
        super.onDestroy()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (HidScannerInputBus.onKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun hideKeyboard() {
        WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
    }

    private fun registerDebugScanReceiver() {
        if (!BuildConfig.DEBUG || debugScanReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val value = intent.getStringExtra(EXTRA_DEBUG_SCAN_VALUE).orEmpty()
                HidScannerInputBus.onTextCommitted(value)
            }
        }
        debugScanReceiver = receiver
        val filter = IntentFilter(ACTION_DEBUG_SCAN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }
    }

    companion object {
        const val ACTION_DEBUG_SCAN = "ru.devandprod.chestniyznak.DEBUG_SCAN"
        const val EXTRA_DEBUG_SCAN_VALUE = "value"
    }
}
