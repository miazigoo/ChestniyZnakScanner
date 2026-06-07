package ru.devandprod.chestniyznak.core.runtime

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
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
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.device.DeviceIdentity
import ru.devandprod.chestniyznak.core.i18n.AppStringProvider

@Singleton
class ChzConnectionMonitor @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val strings: AppStringProvider,
) {
    private companion object {
        const val TAG = "ChestniyZnakWS"
    }

    private val baseReconnectDelayMs = 5_000L
    private val maxReconnectDelayMs = 30_000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(
        ConnectionState(
            statusText = strings.get(R.string.connection_not_started),
        ),
    )
    val state: StateFlow<ConnectionState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<ChzRealtimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ChzRealtimeEvent> = _events.asSharedFlow()

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
        Log.i(TAG, "start runtime ws monitor deviceId=${DeviceIdentity.clientDeviceId}")
        _state.value = ConnectionState(
            isStarted = true,
            isConnected = false,
            isBlocking = true,
            statusText = strings.get(R.string.connection_connecting),
            reconnectDelaySec = 0,
        )
        openSocket()
        maintenanceJob = scope.launch {
            while (started) {
                delay(5_000)
                val now = System.currentTimeMillis()
                val inboundAge = now - lastInboundAt
                if (lastInboundAt > 0L && inboundAge > 45_000) {
                    Log.w(TAG, "heartbeat timeout inboundAgeMs=$inboundAge")
                    markDisconnected(strings.get(R.string.connection_no_heartbeat))
                    scheduleReconnect()
                } else if (socket != null) {
                    sendHeartbeat()
                }
            }
        }
    }

    fun stop() {
        Log.i(TAG, "stop runtime ws monitor")
        started = false
        maintenanceJob?.cancel()
        maintenanceJob = null
        reconnectJob?.cancel()
        reconnectJob = null
        socket?.close(1000, "stop")
        socket = null
        connecting = false
        reconnectDelayMs = baseReconnectDelayMs
        _state.value = ConnectionState(
            statusText = strings.get(R.string.connection_not_started),
        )
    }

    fun retry() {
        if (!started) return
        Log.i(TAG, "manual retry requested")
        reconnectDelayMs = baseReconnectDelayMs
        reconnectNow()
    }

    private fun reconnectNow() {
        Log.i(TAG, "reconnectNow")
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
            statusText = strings.get(R.string.connection_reconnect_countdown, (delayMs / 1000L).toInt()),
        )
        Log.w(TAG, "schedule reconnect in ${delayMs}ms")
        reconnectJob = scope.launch {
            delay(delayMs)
            reconnectJob = null
            if (started) {
                Log.i(TAG, "reconnect timer fired")
                openSocket()
            }
        }
        reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(maxReconnectDelayMs)
    }

    private fun openSocket() {
        if (!started || connecting) return
        val apiUrl = BuildConfig.API_BASE_URL.toHttpUrlOrNull() ?: return
        val websocketUrl = apiUrl.let {
            val scheme = if (it.isHttps) "wss" else "ws"
            val defaultPort = if (it.isHttps) 443 else 80
            buildString {
                append(scheme)
                append("://")
                append(it.host)
                if (it.port != defaultPort) {
                    append(':')
                    append(it.port)
                }
                append("/ws/chestniy-znak/client/?device_id=")
                append(DeviceIdentity.clientDeviceId)
            }
        }

        Log.i(TAG, "openSocket host=${apiUrl.host}")
        connecting = true
        reconnectJob?.cancel()
        reconnectJob = null
        _state.value = _state.value.copy(
            isStarted = true,
            isConnected = false,
            isBlocking = true,
            statusText = strings.get(R.string.connection_connecting),
            reconnectDelaySec = 0,
        )
        socket = okHttpClient.newWebSocket(
            Request.Builder().url(websocketUrl).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    connecting = false
                    reconnectDelayMs = baseReconnectDelayMs
                    Log.i(TAG, "onOpen code=${response.code}")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    lastInboundAt = System.currentTimeMillis()
                    val payload = runCatching { json.parseToJsonElement(text) }.getOrNull()
                    val type = payload?.jsonObject?.get("type")?.jsonPrimitive?.content.orEmpty()
                    payload?.jsonObject?.let { body ->
                        if (type.startsWith("package.")) {
                            _events.tryEmit(
                                ChzRealtimeEvent(
                                    type = type,
                                    orderId = body["order_id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                                    orderLineId = body["order_line_id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                                    packageId = body["package_id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                                    packageCode = body["package_code"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                                ),
                            )
                        }
                    }
                    Log.d(TAG, "onMessage type=${type.ifBlank { "unknown" }} bytes=${text.length}")
                    when (type) {
                        "connected" -> {
                            reconnectDelayMs = baseReconnectDelayMs
                            _state.value = ConnectionState(
                                isStarted = true,
                                isConnected = true,
                                isBlocking = false,
                                statusText = strings.get(R.string.connection_active),
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
                                statusText = strings.get(R.string.connection_active),
                                reconnectDelaySec = 0,
                            )
                        }
                        "pong" -> {
                            reconnectDelayMs = baseReconnectDelayMs
                            _state.value = _state.value.copy(
                                isStarted = true,
                                isConnected = true,
                                isBlocking = false,
                                statusText = strings.get(R.string.connection_active),
                                reconnectDelaySec = 0,
                            )
                        }
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.w(TAG, "onClosing code=$code reason=$reason")
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.w(TAG, "onClosed code=$code reason=$reason")
                    markDisconnected(strings.get(R.string.connection_closed))
                    socket = null
                    scheduleReconnect()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(
                        TAG,
                        "onFailure code=${response?.code} message=${t.message ?: "unknown"}",
                        t,
                    )
                    markDisconnected(t.message ?: strings.get(R.string.connection_websocket_error))
                    socket = null
                    connecting = false
                    scheduleReconnect()
                }
            },
        )
    }

    private fun sendHeartbeat() {
        Log.d(TAG, "send heartbeat")
        socket?.send(buildHeartbeatPayload())
    }

    private fun buildHeartbeatPayload(): String = buildJsonObject {
        put("type", "heartbeat")
    }.toString()

    private fun markDisconnected(reason: String) {
        Log.w(TAG, "markDisconnected reason=$reason")
        _state.value = ConnectionState(
            isStarted = started,
            isConnected = false,
            isBlocking = started,
            statusText = reason,
            reconnectDelaySec = _state.value.reconnectDelaySec,
        )
    }
}

data class ChzRealtimeEvent(
    val type: String,
    val orderId: String = "",
    val orderLineId: String = "",
    val packageId: String = "",
    val packageCode: String = "",
)
