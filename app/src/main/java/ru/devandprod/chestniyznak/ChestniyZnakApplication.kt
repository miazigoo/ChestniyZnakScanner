package ru.devandprod.chestniyznak

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ru.devandprod.chestniyznak.core.device.DeviceIdentity

@HiltAndroidApp
class ChestniyZnakApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DeviceIdentity.initialize(this)
    }
}
