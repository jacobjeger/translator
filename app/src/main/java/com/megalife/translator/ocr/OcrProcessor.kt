package com.megalife.translator.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.hebrew.HebrewTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrBlock(
    val text: String,
    val boundingBox: Rect,
    val cornerPoints: Array<android.graphics.Point>?,
    val recognizedLanguage: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OcrBlock) return false
        return text == other.text && boundingBox == other.boundingBox
    }

    override fun hashCode(): Int = text.hashCode() * 31 + boundingBox.hashCode()
}

class OcrProcessor {

    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val hebrewRecognizer = TextRecognition.getClient(HebrewTextRecognizerOptions.Builder().build())

    suspend fun processImage(bitmap: Bitmap): List<OcrBlock> {
        val image = InputImage.fromBitmap(bitmap, 0)

        val latinBlocks = recognizeText(image, latinRecognizer)
        val hebrewBlocks = recognizeText(image, hebrewRecognizer)

        return mergeBlocks(latinBlocks, hebrewBlocks)
    }

    private suspend fun recognizeText(
        image: InputImage,
        recognizer: com.google.mlkit.vision.text.TextRecognizer
    ): List<OcrBlock> = suspendCancellableCoroutine { cont ->
        recognizer.process(image)
            .addOnSuccessListener { text ->
                val blocks = text.textBlocks.flatMap { block ->
                    block.lines.map { line ->
                        OcrBlock(
                            text = line.text,
                            boundingBox = line.boundingBox ?: Rect(),
                            cornerPoints = line.cornerPoints?.map {
                                android.graphics.Point(it.x, it.y)
                            }?.toTypedArray(),
                            recognizedLanguage = line.recognizedLanguage ?: "en"
                        )
                    }
                }
                cont.resume(blocks)
            }
            .addOnFailureListener { e ->
                cont.resume(emptyList())
            }
    }

    private fun mergeBlocks(latin: List<OcrBlock>, hebrew: List<OcrBlock>): List<OcrBlock> {
        val merged = mutableListOf<OcrBlock>()
        val used = mutableSetOf<Int>()

        // Add all Hebrew blocks first (higher priority for Hebrew/Yiddish text)
        for (hebrewBlock in hebrew) {
            merged.add(hebrewBlock)
            // Mark any overlapping latin blocks
            for ((i, latinBlock) in latin.withIndex()) {
                if (Rect.intersects(hebrewBlock.boundingBox, latinBlock.boundingBox)) {
                    val overlap = calculateOverlap(hebrewBlock.boundingBox, latinBlock.boundingBox)
                    if (overlap > 0.5f) {
                        used.add(i)
                    }
                }
            }
        }

        // Add non-overlapping Latin blocks
        for ((i, latinBlock) in latin.withIndex()) {
            if (i !in used) {
                merged.add(latinBlock)
            }
        }

        return merged
    }

    private fun calculateOverlap(a: Rect, b: Rect): Float {
        val intersect = Rect()
        if (!intersect.setIntersect(a, b)) return 0f
        val intersectArea = intersect.width().toLong() * intersect.height().toLong()
        val smallerArea = minOf(
            a.width().toLong() * a.height().toLong(),
            b.width().toLong() * b.height().toLong()
        )
        if (smallerArea == 0L) return 0f
        return intersectArea.toFloat() / smallerArea.toFloat()
    }

    fun close() {
        latinRecognizer.close()
        hebrewRecognizer.close()
    }
}
