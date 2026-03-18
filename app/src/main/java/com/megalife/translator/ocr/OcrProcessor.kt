package com.megalife.translator.ocr

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.hebrew.HebrewTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrTextBlock(
    val text: String,
    val boundingBox: Rect,
    val cornerPoints: Array<Point>?,
    val angle: Float = 0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OcrTextBlock) return false
        return text == other.text && boundingBox == other.boundingBox
    }

    override fun hashCode(): Int = 31 * text.hashCode() + boundingBox.hashCode()
}

class OcrProcessor {

    private val latinRecognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val hebrewRecognizer: TextRecognizer =
        TextRecognition.getClient(HebrewTextRecognizerOptions.Builder().build())

    suspend fun processImage(bitmap: Bitmap): List<OcrTextBlock> = withContext(Dispatchers.IO) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        // Run both recognizers and merge results
        val latinBlocks = runRecognizer(latinRecognizer, inputImage)
        val hebrewBlocks = runRecognizer(hebrewRecognizer, inputImage)

        mergeResults(latinBlocks, hebrewBlocks)
    }

    private suspend fun runRecognizer(
        recognizer: TextRecognizer,
        image: InputImage
    ): List<OcrTextBlock> = suspendCancellableCoroutine { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val blocks = mutableListOf<OcrTextBlock>()
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val bbox = line.boundingBox ?: continue
                        val corners = line.cornerPoints
                        val angle = if (corners != null && corners.size >= 2) {
                            calculateAngle(corners[0], corners[1])
                        } else {
                            0f
                        }
                        blocks.add(
                            OcrTextBlock(
                                text = line.text,
                                boundingBox = bbox,
                                cornerPoints = corners,
                                angle = angle
                            )
                        )
                    }
                }
                continuation.resume(blocks)
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }

    private fun calculateAngle(p1: Point, p2: Point): Float {
        val dx = (p2.x - p1.x).toFloat()
        val dy = (p2.y - p1.y).toFloat()
        return Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }

    private fun mergeResults(
        latinBlocks: List<OcrTextBlock>,
        hebrewBlocks: List<OcrTextBlock>
    ): List<OcrTextBlock> {
        val merged = mutableListOf<OcrTextBlock>()
        val used = mutableSetOf<Int>()

        // Add all Hebrew blocks (they typically capture Hebrew text better)
        for (hBlock in hebrewBlocks) {
            merged.add(hBlock)
            // Mark overlapping Latin blocks as used
            for ((index, lBlock) in latinBlocks.withIndex()) {
                if (overlaps(hBlock.boundingBox, lBlock.boundingBox)) {
                    used.add(index)
                }
            }
        }

        // Add non-overlapping Latin blocks
        for ((index, lBlock) in latinBlocks.withIndex()) {
            if (index !in used) {
                merged.add(lBlock)
            }
        }

        return merged.sortedBy { it.boundingBox.top }
    }

    private fun overlaps(a: Rect, b: Rect): Boolean {
        if (a.isEmpty || b.isEmpty) return false
        val overlapX = maxOf(0, minOf(a.right, b.right) - maxOf(a.left, b.left))
        val overlapY = maxOf(0, minOf(a.bottom, b.bottom) - maxOf(a.top, b.top))
        val overlapArea = overlapX * overlapY
        val smallerArea = minOf(a.width() * a.height(), b.width() * b.height())
        return smallerArea > 0 && overlapArea.toFloat() / smallerArea > 0.5f
    }

    fun close() {
        latinRecognizer.close()
        hebrewRecognizer.close()
    }
}
