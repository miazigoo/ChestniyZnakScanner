package ru.devandprod.chestniyznak.core.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.ExperimentalGetImage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.hypot

data class BarcodeFrameDetection(
    val leftRatio: Float,
    val topRatio: Float,
    val rightRatio: Float,
    val bottomRatio: Float,
    val centerXRatio: Float,
    val centerYRatio: Float,
    val frameAspectRatio: Float,
    val inRoi: Boolean,
    val normalizedArea: Float,
)

class BarcodeFrameAnalyzer(
    private val scanGate: AtomicBoolean,
    barcodeFormats: IntArray = intArrayOf(Barcode.FORMAT_DATA_MATRIX),
    private val onBarcodeFrame: (BarcodeFrameDetection?) -> Unit,
    private val onBarcodeDetected: (String, BarcodeFrameDetection?) -> Unit,
) : ImageAnalysis.Analyzer, Closeable {
    private companion object {
        private const val ROI_LEFT = 0.12f
        private const val ROI_TOP = 0.16f
        private const val ROI_RIGHT = 0.88f
        private const val ROI_BOTTOM = 0.84f
    }

    private val frameInFlight = AtomicBoolean(false)
    private var lastValue: String? = null
    private var stableHits: Int = 0

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

    @OptIn(ExperimentalGetImage::class)
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
                val frameWidth = imageProxy.width.toFloat()
                val frameHeight = imageProxy.height.toFloat()

                val allCandidates = barcodes
                    .asSequence()
                    .filter { it.rawValue != null }
                    .mapNotNull { barcode ->
                        val box = barcode.boundingBox ?: return@mapNotNull null
                        val cx = box.centerX() / frameWidth
                        val cy = box.centerY() / frameHeight
                        val inRoi = cx in ROI_LEFT..ROI_RIGHT && cy in ROI_TOP..ROI_BOTTOM
                        Triple(barcode, box, inRoi)
                    }
                    .toList()

                val bestBarcode = allCandidates
                    .asSequence()
                    .filter { it.third }
                    .maxByOrNull { (_, box, _) ->
                        val areaNorm = (box.width() * box.height()) / (frameWidth * frameHeight)
                        val cx = box.centerX() / frameWidth
                        val cy = box.centerY() / frameHeight
                        val distance = hypot(cx - 0.5f, cy - 0.5f)
                        areaNorm - distance * 0.2f
                    }
                    ?: allCandidates.maxByOrNull { (_, box, _) ->
                        val areaNorm = (box.width() * box.height()) / (frameWidth * frameHeight)
                        val cx = box.centerX() / frameWidth
                        val cy = box.centerY() / frameHeight
                        val distance = hypot(cx - 0.5f, cy - 0.5f)
                        areaNorm - distance * 0.2f
                    }

                if (bestBarcode == null) {
                    lastValue = null
                    stableHits = 0
                    onBarcodeFrame(null)
                    return@addOnSuccessListener
                }

                val detection = if (frameWidth > 0f && frameHeight > 0f) {
                    val (_, box, inRoi) = bestBarcode
                    val left = (box.left.toFloat() / frameWidth).coerceIn(0f, 1f)
                    val top = (box.top.toFloat() / frameHeight).coerceIn(0f, 1f)
                    val right = (box.right.toFloat() / frameWidth).coerceIn(0f, 1f)
                    val bottom = (box.bottom.toFloat() / frameHeight).coerceIn(0f, 1f)
                    val area = ((right - left) * (bottom - top)).coerceIn(0f, 1f)
                    BarcodeFrameDetection(
                        leftRatio = left,
                        topRatio = top,
                        rightRatio = right,
                        bottomRatio = bottom,
                        centerXRatio = ((left + right) / 2f).coerceIn(0f, 1f),
                        centerYRatio = ((top + bottom) / 2f).coerceIn(0f, 1f),
                        frameAspectRatio = (frameWidth / frameHeight).coerceAtLeast(0.1f),
                        inRoi = inRoi,
                        normalizedArea = area,
                    )
                } else {
                    null
                }

                onBarcodeFrame(detection)

                if (detection?.inRoi != true) {
                    stableHits = 0
                    lastValue = null
                    return@addOnSuccessListener
                }

                val value = bestBarcode.first.rawValue ?: return@addOnSuccessListener
                stableHits = if (value == lastValue) stableHits + 1 else 1
                lastValue = value

                if (stableHits >= 2 && scanGate.compareAndSet(true, false)) {
                    onBarcodeDetected(value, detection)
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
