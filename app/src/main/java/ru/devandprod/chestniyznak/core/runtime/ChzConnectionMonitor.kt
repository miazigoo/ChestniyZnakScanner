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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(ConnectionState())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var socket: WebSocket? = null
    private var maintenanceJob: Job? = null
    private var lastInboundAt: Long = 0L
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
        )
        openSocket()
        maintenanceJob = scope.launch {
            while (started) {
                delay(5_000)
                val now = System.currentTimeMillis()
                val inboundAge = now - lastInboundAt
                if (socket == null && !connecting) {
                    openSocket()
                } else if (lastInboundAt > 0L && inboundAge > 45_000) {
                    markDisconnected("Нет heartbeat от сервера")
                    reconnect()
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
        socket?.close(1000, "stop")
        socket = null
        connecting = false
        _state.value = ConnectionState()
    }

    fun retry() {
        if (!started) return
        reconnect()
    }

    private fun reconnect() {
        socket?.cancel()
        socket = null
        connecting = false
        if (started) openSocket()
    }

    private fun openSocket() {
        if (!started || connecting) return
        val websocketUrl = BuildConfig.API_BASE_URL.toHttpUrlOrNull()?.let { apiUrl ->
            apiUrl.newBuilder()
                .scheme(if (apiUrl.isHttps) "wss" else "ws")
                .encodedPath("/ws/chestniy-znak/client/")
                .setQueryParameter("device_id", DeviceIdentity.clientDeviceId)
                .build()
        } ?: return

        connecting = true
        _state.value = _state.value.copy(
            isStarted = true,
            isConnected = false,
            isBlocking = true,
            statusText = "Подключаемся к серверу...",
        )
        socket = okHttpClient.newWebSocket(
            Request.Builder().url(websocketUrl).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    connecting = false
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    lastInboundAt = System.currentTimeMillis()
                    val payload = runCatching { json.parseToJsonElement(text) }.getOrNull()
                    val type = payload?.jsonObject?.get("type")?.jsonPrimitive?.content.orEmpty()
                    when (type) {
                        "connected" -> {
                            _state.value = ConnectionState(
                                isStarted = true,
                                isConnected = true,
                                isBlocking = false,
                                statusText = "Соединение с сервером активно",
                            )
                        }
                        "heartbeat" -> {
                            webSocket.send(buildHeartbeatPayload())
                            _state.value = _state.value.copy(
                                isStarted = true,
                                isConnected = true,
                                isBlocking = false,
                                statusText = "Соединение с сервером активно",
                            )
                        }
                        "pong" -> {
                            _state.value = _state.value.copy(
                                isStarted = true,
                                isConnected = true,
                                isBlocking = false,
                                statusText = "Соединение с сервером активно",
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
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    markDisconnected(t.message ?: "Ошибка WebSocket")
                    socket = null
                    connecting = false
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
        )
    }
}
