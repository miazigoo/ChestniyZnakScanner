package ru.devandprod.chestniyznak.core.runtime

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import ru.devandprod.chestniyznak.BuildConfig
import ru.devandprod.chestniyznak.core.device.DeviceIdentity

@Singleton
class ChzConnectionMonitor @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    private val baseReconnectDelayMs = 5_000L
    private val maxReconnectDelayMs = 30_000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(ConnectionState())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var socket: WebSocket? = null
    private var maintenanceJob: Job? = null
    private var lastInboundAt: Long = 0L
    private var reconnectJob: Job? = null
    private var reconnectDelayMs: Long = baseReconnectDelayMs
    @Volatile private var started = false
    @Volatile private var connecting = false

    fun start() {
        if (started) return
        started = true
        _state.value = ConnectionState(
            isStarted = true,
            isConnected = false,
            isBlocking = true,
            statusText = "Подключаемся к серверу...",
            reconnectDelaySec = 0,
        )
        openSocket()
        maintenanceJob = scope.launch {
            while (started) {
                delay(5_000)
                val now = System.currentTimeMillis()
                val inboundAge = now - lastInboundAt
                if (lastInboundAt > 0L && inboundAge > 45_000) {
                    markDisconnected("Нет heartbeat от сервера")
                    scheduleReconnect()
                } else if (socket != null) {
                    sendHeartbeat()
                }
            }
        }
    }

    fun stop() {
        started = false
        maintenanceJob?.cancel()
        maintenanceJob = null
        reconnectJob?.cancel()
        reconnectJob = null
        socket?.close(1000, "stop")
        socket = null
        connecting = false
        reconnectDelayMs = baseReconnectDelayMs
        _state.value = ConnectionState()
    }

    fun retry() {
        if (!started) return
        reconnectDelayMs = baseReconnectDelayMs
        reconnectNow()
    }

    private fun reconnectNow() {
        reconnectJob?.cancel()
        reconnectJob = null
        socket?.cancel()
        socket = null
        connecting = false
        if (started) openSocket()
    }

    private fun scheduleReconnect() {
        if (!started || reconnectJob?.isActive == true) return
        socket?.cancel()
        socket = null
        connecting = false
        val delayMs = reconnectDelayMs
        _state.value = _state.value.copy(
            isStarted = true,
            isConnected = false,
            isBlocking = true,
            reconnectDelaySec = (delayMs / 1000L).toInt(),
            statusText = "Связь потеряна. Автоподключение через ${(delayMs / 1000L).toInt()} сек.",
        )
        reconnectJob = scope.launch {
            delay(delayMs)
            reconnectJob = null
            if (started) {
                openSocket()
            }
        }
        reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(maxReconnectDelayMs)
    }

    private fun openSocket() {
        if (!started || connecting) return
        val websocketUrl = BuildConfig.API_BASE_URL.toHttpUrlOrNull()?.let { apiUrl ->
            val scheme = if (apiUrl.isHttps) "wss" else "ws"
            val defaultPort = if (apiUrl.isHttps) 443 else 80
            buildString {
                append(scheme)
                append("://")
                append(apiUrl.host)
                if (apiUrl.port != defaultPort) {
                    append(':')
                    append(apiUrl.port)
                }
                append("/ws/chestniy-znak/client/?device_id=")
                append(DeviceIdentity.clientDeviceId)
            }
        } ?: return

        connecting = true
        reconnectJob?.cancel()
        reconnectJob = null
        _state.value = _state.value.copy(
            isStarted = true,
            isConnected = false,
            isBlocking = true,
            statusText = "Подключаемся к серверу...",
            reconnectDelaySec = 0,
        )
        socket = okHttpClient.newWebSocket(
            Request.Builder().url(websocketUrl).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    connecting = false
                    reconnectDelayMs = baseReconnectDelayMs
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    lastInboundAt = System.currentTimeMillis()
                    val payload = runCatching { json.parseToJsonElement(text) }.getOrNull()
                    val type = payload?.jsonObject?.get("type")?.jsonPrimitive?.content.orEmpty()
                    when (type) {
                        "connected" -> {
                            reconnectDelayMs = baseReconnectDelayMs
                            _state.value = ConnectionState(
                                isStarted = true,
                                isConnected = true,
                                isBlocking = false,
                                statusText = "Соединение с сервером активно",
                                reconnectDelaySec = 0,
                            )
                        }
                        "heartbeat" -> {
                            webSocket.send(buildHeartbeatPayload())
                            reconnectDelayMs = baseReconnectDelayMs
                            _state.value = _state.value.copy(
                                isStarted = true,
                                isConnected = true,
                                isBlocking = false,
                                statusText = "Соединение с сервером активно",
                                reconnectDelaySec = 0,
                            )
                        }
                        "pong" -> {
                            reconnectDelayMs = baseReconnectDelayMs
                            _state.value = _state.value.copy(
                                isStarted = true,
                                isConnected = true,
                                isBlocking = false,
                                statusText = "Соединение с сервером активно",
                                reconnectDelaySec = 0,
                            )
                        }
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    markDisconnected("Соединение с сервером разорвано")
                    socket = null
                    scheduleReconnect()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    markDisconnected(t.message ?: "Ошибка WebSocket")
                    socket = null
                    connecting = false
                    scheduleReconnect()
                }
            },
        )
    }

    private fun sendHeartbeat() {
        socket?.send(buildHeartbeatPayload())
    }

    private fun buildHeartbeatPayload(): String = buildJsonObject {
        put("type", "heartbeat")
    }.toString()

    private fun markDisconnected(reason: String) {
        _state.value = ConnectionState(
            isStarted = started,
            isConnected = false,
            isBlocking = started,
            statusText = reason,
            reconnectDelaySec = _state.value.reconnectDelaySec,
        )
    }
}
