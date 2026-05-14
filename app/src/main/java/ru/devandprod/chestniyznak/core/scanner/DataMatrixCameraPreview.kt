package ru.devandprod.chestniyznak.core.scanner

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun BarcodeCameraPreview(
    isEnabled: Boolean,
    barcodeFormats: IntArray,
    onCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val scanGate = remember { AtomicBoolean(isEnabled) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember {
        BarcodeFrameAnalyzer(
            scanGate = scanGate,
            barcodeFormats = barcodeFormats,
            onBarcodeDetected = onCodeScanned,
        )
    }

    LaunchedEffect(isEnabled) {
        scanGate.set(isEnabled)
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(analyzerExecutor, analyzer)
                    }

                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
                }
            },
            mainExecutor,
        )

        onDispose {
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
            analyzer.close()
            analyzerExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}

@Composable
fun DataMatrixCameraPreview(
    isEnabled: Boolean,
    onCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BarcodeCameraPreview(
        isEnabled = isEnabled,
        barcodeFormats = intArrayOf(Barcode.FORMAT_DATA_MATRIX),
        onCodeScanned = onCodeScanned,
        modifier = modifier,
    )
}

@Composable
fun QrCodeCameraPreview(
    isEnabled: Boolean,
    onCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BarcodeCameraPreview(
        isEnabled = isEnabled,
        barcodeFormats = intArrayOf(Barcode.FORMAT_QR_CODE),
        onCodeScanned = onCodeScanned,
        modifier = modifier,
    )
}
