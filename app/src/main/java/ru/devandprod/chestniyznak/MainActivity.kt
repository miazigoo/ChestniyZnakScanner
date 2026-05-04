package ru.devandprod.chestniyznak

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import ru.devandprod.chestniyznak.app.ChestniyZnakApp
import ru.devandprod.chestniyznak.core.scanner.HidScannerInputBus

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setContent {
            ChestniyZnakApp()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (HidScannerInputBus.onKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
