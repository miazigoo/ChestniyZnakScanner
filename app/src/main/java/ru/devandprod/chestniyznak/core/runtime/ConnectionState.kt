package ru.devandprod.chestniyznak.core.runtime

data class ConnectionState(
    val isStarted: Boolean = false,
    val isConnected: Boolean = false,
    val isBlocking: Boolean = false,
    val statusText: String = "Соединение не запущено",
)
