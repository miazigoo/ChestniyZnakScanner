package ru.devandprod.chestniyznak.core.scanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

class BarcodeFrameAnalyzer(
    private val scanGate: AtomicBoolean,
    barcodeFormats: IntArray = intArrayOf(Barcode.FORMAT_DATA_MATRIX),
    private val onBarcodeDetected: (String) -> Unit,
) : ImageAnalysis.Analyzer, Closeable {

    private val frameInFlight = AtomicBoolean(false)
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .apply {
                setBarcodeFormats(
                    barcodeFormats.firstOrNull() ?: Barcode.FORMAT_DATA_MATRIX,
                    *barcodeFormats.drop(1).toIntArray(),
                )
            }
            .build(),
    )

    override fun analyze(imageProxy: ImageProxy) {
        if (!scanGate.get() || !frameInFlight.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            frameInFlight.set(false)
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstOrNull { it.rawValue != null }?.rawValue ?: return@addOnSuccessListener
                if (scanGate.compareAndSet(true, false)) {
                    onBarcodeDetected(value)
                }
            }
            .addOnCompleteListener {
                frameInFlight.set(false)
                imageProxy.close()
            }
    }

    override fun close() {
        scanner.close()
    }
}
