package ru.devandprod.chestniyznak.core.scanner

import android.hardware.camera2.CaptureRequest
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay
import ru.devandprod.chestniyznak.R

private const val ROI_LEFT = 0.12f
private const val ROI_TOP = 0.16f
private const val ROI_RIGHT = 0.88f
private const val ROI_BOTTOM = 0.84f

@Composable
@OptIn(ExperimentalCamera2Interop::class)
fun BarcodeCameraPreview(
    isEnabled: Boolean,
    barcodeFormats: IntArray,
    onCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
    restartKey: Any? = Unit,
    rearmKey: Any? = Unit,
    showZoomControls: Boolean = true,
    showTorchControl: Boolean = true,
    autoZoomEnabled: Boolean = true,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onCodeScannedUpdated by rememberUpdatedState(onCodeScanned)
    val isEnabledUpdated by rememberUpdatedState(isEnabled)
    var camera by remember { mutableStateOf<Camera?>(null) }
    var minZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(1f) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var trackedDetection by remember { mutableStateOf<BarcodeFrameDetection?>(null) }
    var detectionSerial by remember { mutableLongStateOf(0L) }
    var lastAutoFocusAt by remember { mutableLongStateOf(0L) }
    var lastAutoZoomAt by remember { mutableLongStateOf(0L) }

    val previewView = remember(context, restartKey) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    fun focusAt(x: Float, y: Float) {
        val activeCamera = camera ?: return
        val meteringPoint = previewView.meteringPointFactory.createPoint(x, y, 0.25f)
        val action = FocusMeteringAction.Builder(
            meteringPoint,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
        )
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        activeCamera.cameraControl.startFocusAndMetering(action)
    }
    fun setZoom(target: Float) {
        val activeCamera = camera ?: return
        val normalized = target.coerceIn(minZoomRatio, maxZoomRatio)
        activeCamera.cameraControl.setZoomRatio(normalized)
    }
    fun mapDetectionToPreview(xRatio: Float, yRatio: Float, frameAspect: Float): Pair<Float, Float> {
        val width = previewView.width.toFloat().coerceAtLeast(1f)
        val height = previewView.height.toFloat().coerceAtLeast(1f)
        val viewAspect = width / height

        return if (frameAspect > viewAspect) {
            val visibleFraction = (viewAspect / frameAspect).coerceIn(0.01f, 1f)
            val xStart = (1f - visibleFraction) / 2f
            val x = ((xRatio - xStart) / visibleFraction).coerceIn(0f, 1f) * width
            val y = yRatio.coerceIn(0f, 1f) * height
            x to y
        } else {
            val visibleFraction = (frameAspect / viewAspect).coerceIn(0.01f, 1f)
            val yStart = (1f - visibleFraction) / 2f
            val x = xRatio.coerceIn(0f, 1f) * width
            val y = ((yRatio - yStart) / visibleFraction).coerceIn(0f, 1f) * height
            x to y
        }
    }

    val scanGate = remember(restartKey) { AtomicBoolean(isEnabled) }
    val analyzerExecutor = remember(restartKey) { Executors.newSingleThreadExecutor() }
    val analyzer = remember(barcodeFormats.contentHashCode(), restartKey) {
        BarcodeFrameAnalyzer(
            scanGate = scanGate,
            barcodeFormats = barcodeFormats,
            onBarcodeFrame = { detection ->
                trackedDetection = detection
                detectionSerial += 1
                if (detection != null && isEnabledUpdated) {
                    val now = System.currentTimeMillis()
                    if (autoZoomEnabled && now - lastAutoZoomAt >= 320L) {
                        val targetZoom = when {
                            detection.normalizedArea < 0.04f -> zoomRatio * 1.18f
                            detection.normalizedArea > 0.34f -> zoomRatio * 0.86f
                            else -> null
                        }
                        if (targetZoom != null) {
                            val normalizedTarget = targetZoom.coerceIn(minZoomRatio, maxZoomRatio)
                            if (kotlin.math.abs(normalizedTarget - zoomRatio) >= 0.08f) {
                                setZoom(normalizedTarget)
                                lastAutoZoomAt = now
                            }
                        }
                    }
                    if (now - lastAutoFocusAt >= 380L) {
                        val (focusX, focusY) = mapDetectionToPreview(
                            xRatio = detection.centerXRatio,
                            yRatio = detection.centerYRatio,
                            frameAspect = detection.frameAspectRatio,
                        )
                        focusAt(focusX, focusY)
                        lastAutoFocusAt = now
                    }
                }
            },
            onBarcodeDetected = { code, detection ->
                if (autoZoomEnabled && isEnabledUpdated) {
                    detection?.let {
                        val targetZoom = when {
                            it.normalizedArea < 0.04f -> zoomRatio * 1.12f
                            it.normalizedArea > 0.34f -> zoomRatio * 0.9f
                            else -> null
                        }
                        if (targetZoom != null) setZoom(targetZoom)
                    }
                }
                onCodeScannedUpdated(code)
            },
        )
    }
    val scaleGestureDetector = remember(context, restartKey) {
        ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val activeCamera = camera ?: return false
                    val updatedZoom = (zoomRatio * detector.scaleFactor).coerceIn(minZoomRatio, maxZoomRatio)
                    activeCamera.cameraControl.setZoomRatio(updatedZoom)
                    return true
                }
            },
        )
    }
    val tapGestureDetector = remember(context, restartKey) {
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(event: MotionEvent): Boolean {
                    focusAt(event.x, event.y)
                    return true
                }

                override fun onDoubleTap(event: MotionEvent): Boolean {
                    val activeCamera = camera ?: return false
                    val targetZoom = when {
                        maxZoomRatio <= 1.2f -> 1f
                        zoomRatio < 1.5f -> 2f.coerceAtMost(maxZoomRatio)
                        else -> 1f
                    }
                    activeCamera.cameraControl.setZoomRatio(targetZoom)
                    return true
                }
            },
        )
    }

    LaunchedEffect(isEnabled, rearmKey) {
        scanGate.set(isEnabled)
        trackedDetection = null
        detectionSerial += 1
        if (isEnabled) {
            previewView.post {
                focusAt(previewView.width / 2f, previewView.height / 2f)
            }
        } else {
            camera?.cameraControl?.enableTorch(false)
        }
    }

    LaunchedEffect(detectionSerial, isEnabled) {
        if (!isEnabled || trackedDetection == null) return@LaunchedEffect
        val serial = detectionSerial
        delay(900L)
        if (serial == detectionSerial) {
            trackedDetection = null
            detectionSerial += 1
        }
    }

    DisposableEffect(previewView, scaleGestureDetector, tapGestureDetector) {
        previewView.setOnTouchListener { _, event ->
            if (!isEnabledUpdated) {
                return@setOnTouchListener false
            }
            scaleGestureDetector.onTouchEvent(event)
            if (!scaleGestureDetector.isInProgress) {
                tapGestureDetector.onTouchEvent(event)
            }
            if (event.action == MotionEvent.ACTION_UP) {
                previewView.performClick()
            }
            true
        }
        onDispose {
            previewView.setOnTouchListener(null)
        }
    }

    DisposableEffect(lifecycleOwner, previewView, restartKey) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                val previewBuilder = Preview.Builder()
                Camera2Interop.Extender(previewBuilder)
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                    )
                val preview = previewBuilder.build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }
                val analysisBuilder = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                Camera2Interop.Extender(analysisBuilder)
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                    )
                val imageAnalysis = analysisBuilder
                    .build()
                    .apply {
                        setAnalyzer(analyzerExecutor, analyzer)
                    }

                runCatching {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
                    val cameraInfo = camera?.cameraInfo
                    hasFlashUnit = cameraInfo?.hasFlashUnit() == true
                    cameraInfo?.zoomState?.observe(lifecycleOwner) { zoomState ->
                        minZoomRatio = zoomState.minZoomRatio
                        maxZoomRatio = zoomState.maxZoomRatio
                        zoomRatio = zoomState.zoomRatio
                    }
                    cameraInfo?.torchState?.observe(lifecycleOwner) { state ->
                        torchEnabled = state == TorchState.ON
                    }
                    previewView.post {
                        focusAt(previewView.width / 2f, previewView.height / 2f)
                    }
                }
            },
            mainExecutor,
        )

        onDispose {
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
            camera = null
            hasFlashUnit = false
            torchEnabled = false
            analyzer.close()
            analyzerExecutor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        if (isEnabledUpdated) {
            DetectionOverlay(
                detection = trackedDetection,
                modifier = Modifier.fillMaxSize(),
            )
            val detection = trackedDetection
            val hintText = when {
                detection == null && !torchEnabled -> stringResource(R.string.camera_hint_aim_or_light)
                detection == null -> stringResource(R.string.camera_hint_aim_frame)
                detection.inRoi.not() -> stringResource(R.string.camera_hint_center_code)
                detection.normalizedArea < 0.04f -> stringResource(R.string.camera_hint_move_closer)
                detection.normalizedArea > 0.34f -> stringResource(R.string.camera_hint_move_farther)
                else -> stringResource(R.string.camera_hint_hold_steady)
            }
            Text(
                text = hintText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp),
            )
        }
        if (showZoomControls && maxZoomRatio > minZoomRatio + 0.1f) {
            ZoomControls(
                enabled = isEnabledUpdated,
                canZoomIn = zoomRatio < maxZoomRatio - 0.05f,
                canZoomOut = zoomRatio > minZoomRatio + 0.05f,
                zoomLabel = "${String.format(Locale.ROOT, "%.1f", zoomRatio)}x",
                onZoomIn = { setZoom(zoomRatio * 1.2f) },
                onZoomOut = { setZoom(zoomRatio / 1.2f) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
            )
        }
        if (showTorchControl && hasFlashUnit) {
            Button(
                onClick = { camera?.cameraControl?.enableTorch(!torchEnabled) },
                enabled = isEnabledUpdated,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            ) {
                Text(
                    if (torchEnabled) {
                        stringResource(R.string.camera_torch_on)
                    } else {
                        stringResource(R.string.camera_torch)
                    },
                )
            }
        }
    }
}

@Composable
private fun DetectionOverlay(
    detection: BarcodeFrameDetection?,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val frameAspect = detection?.frameAspectRatio?.coerceAtLeast(0.1f) ?: (size.width / size.height)
        val viewAspect = size.width / size.height

        fun mapX(xRatio: Float): Float {
            return if (frameAspect > viewAspect) {
                val visibleFraction = (viewAspect / frameAspect).coerceIn(0.01f, 1f)
                val xStart = (1f - visibleFraction) / 2f
                (((xRatio - xStart) / visibleFraction).coerceIn(0f, 1f) * size.width)
            } else {
                xRatio.coerceIn(0f, 1f) * size.width
            }
        }

        fun mapY(yRatio: Float): Float {
            return if (frameAspect > viewAspect) {
                yRatio.coerceIn(0f, 1f) * size.height
            } else {
                val visibleFraction = (frameAspect / viewAspect).coerceIn(0.01f, 1f)
                val yStart = (1f - visibleFraction) / 2f
                (((yRatio - yStart) / visibleFraction).coerceIn(0f, 1f) * size.height)
            }
        }

        val roiLeft = mapX(ROI_LEFT)
        val roiTop = mapY(ROI_TOP)
        val roiRight = mapX(ROI_RIGHT)
        val roiBottom = mapY(ROI_BOTTOM)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.45f),
            topLeft = Offset(roiLeft, roiTop),
            size = Size((roiRight - roiLeft).coerceAtLeast(2f), (roiBottom - roiTop).coerceAtLeast(2f)),
            cornerRadius = CornerRadius(20f, 20f),
            style = Stroke(width = 2f),
        )

        if (detection != null) {
            val left = mapX(detection.leftRatio)
            val top = mapY(detection.topRatio)
            val right = mapX(detection.rightRatio)
            val bottom = mapY(detection.bottomRatio)
            val width = (right - left).coerceAtLeast(2f)
            val height = (bottom - top).coerceAtLeast(2f)

            drawRoundRect(
                color = if (detection.inRoi) Color(0xFF00E5FF) else Color(0xFFFF9800),
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = CornerRadius(18f, 18f),
                style = Stroke(width = 4f),
            )
        }
    }
}

@Composable
fun DataMatrixCameraPreview(
    isEnabled: Boolean,
    onCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
    restartKey: Any? = Unit,
    rearmKey: Any? = Unit,
) {
    BarcodeCameraPreview(
        isEnabled = isEnabled,
        barcodeFormats = intArrayOf(Barcode.FORMAT_DATA_MATRIX),
        onCodeScanned = onCodeScanned,
        modifier = modifier,
        restartKey = restartKey,
        rearmKey = rearmKey,
    )
}

@Composable
fun SsccCameraPreview(
    isEnabled: Boolean,
    onCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
    restartKey: Any? = Unit,
    rearmKey: Any? = Unit,
) {
    BarcodeCameraPreview(
        isEnabled = isEnabled,
        barcodeFormats = intArrayOf(
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_DATA_MATRIX,
        ),
        onCodeScanned = onCodeScanned,
        modifier = modifier,
        restartKey = restartKey,
        rearmKey = rearmKey,
    )
}

@Composable
fun QrCodeCameraPreview(
    isEnabled: Boolean,
    onCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
    restartKey: Any? = Unit,
) {
    BarcodeCameraPreview(
        isEnabled = isEnabled,
        barcodeFormats = intArrayOf(Barcode.FORMAT_QR_CODE),
        onCodeScanned = onCodeScanned,
        showZoomControls = false,
        autoZoomEnabled = false,
        modifier = modifier,
        restartKey = restartKey,
    )
}

@Composable
private fun ZoomControls(
    enabled: Boolean,
    canZoomIn: Boolean,
    canZoomOut: Boolean,
    zoomLabel: String,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = zoomLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 4.dp),
        )
        Button(
            onClick = onZoomIn,
            enabled = enabled && canZoomIn,
            modifier = Modifier.size(48.dp),
        ) {
            Text("+")
        }
        Button(
            onClick = onZoomOut,
            enabled = enabled && canZoomOut,
            modifier = Modifier.size(48.dp),
        ) {
            Text("-")
        }
    }
}
