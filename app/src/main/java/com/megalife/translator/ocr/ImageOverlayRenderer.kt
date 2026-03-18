package com.megalife.translator.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import com.megalife.translator.data.model.Language

class ImageOverlayRenderer {

    fun renderOverlay(
        original: Bitmap,
        blocks: List<OcrTextBlock>,
        translations: List<String>,
        targetLanguage: Language
    ): Bitmap {
        val result = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        for ((index, block) in blocks.withIndex()) {
            if (index >= translations.size) break

            val translatedText = translations[index]
            if (translatedText.isBlank()) continue

            val bbox = block.boundingBox

            // Sample background color around the bounding box
            val bgColor = sampleBackgroundColor(original, bbox)

            // Draw filled rectangle over original text
            val bgPaint = Paint().apply {
                color = bgColor
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            canvas.save()

            // Handle rotation if text is rotated
            if (Math.abs(block.angle) > 1f) {
                val cx = bbox.centerX().toFloat()
                val cy = bbox.centerY().toFloat()
                canvas.rotate(block.angle, cx, cy)
            }

            // Draw background rect (slightly larger for padding)
            val padding = 2
            canvas.drawRect(
                RectF(
                    (bbox.left - padding).toFloat(),
                    (bbox.top - padding).toFloat(),
                    (bbox.right + padding).toFloat(),
                    (bbox.bottom + padding).toFloat()
                ),
                bgPaint
            )

            // Prepare text paint
            val textPaint = TextPaint().apply {
                color = getContrastingTextColor(bgColor)
                isAntiAlias = true
                textAlign = if (targetLanguage.isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
            }

            // Auto-shrink font size to fit within bounding box
            val maxWidth = bbox.width().toFloat()
            val maxHeight = bbox.height().toFloat()
            var fontSize = maxHeight * 0.85f
            textPaint.textSize = fontSize

            // Shrink until text fits width
            while (textPaint.measureText(translatedText) > maxWidth && fontSize > 6f) {
                fontSize -= 1f
                textPaint.textSize = fontSize
            }

            // Draw translated text
            val textX = if (targetLanguage.isRtl) {
                bbox.right.toFloat() - padding
            } else {
                bbox.left.toFloat() + padding
            }
            val textY = bbox.top + (bbox.height() + fontSize) / 2f - 2f

            canvas.drawText(translatedText, textX, textY, textPaint)
            canvas.restore()
        }

        return result
    }

    private fun sampleBackgroundColor(bitmap: Bitmap, bbox: Rect): Int {
        val samplePoints = mutableListOf<Int>()
        val margin = 5
        val width = bitmap.width
        val height = bitmap.height

        // Sample pixels around the bounding box edges
        val sampleLocations = listOf(
            // Above
            Pair(bbox.centerX(), maxOf(0, bbox.top - margin)),
            // Below
            Pair(bbox.centerX(), minOf(height - 1, bbox.bottom + margin)),
            // Left
            Pair(maxOf(0, bbox.left - margin), bbox.centerY()),
            // Right
            Pair(minOf(width - 1, bbox.right + margin), bbox.centerY()),
            // Corners
            Pair(maxOf(0, bbox.left - margin), maxOf(0, bbox.top - margin)),
            Pair(minOf(width - 1, bbox.right + margin), maxOf(0, bbox.top - margin)),
            Pair(maxOf(0, bbox.left - margin), minOf(height - 1, bbox.bottom + margin)),
            Pair(minOf(width - 1, bbox.right + margin), minOf(height - 1, bbox.bottom + margin))
        )

        for ((x, y) in sampleLocations) {
            val clampedX = x.coerceIn(0, width - 1)
            val clampedY = y.coerceIn(0, height - 1)
            samplePoints.add(bitmap.getPixel(clampedX, clampedY))
        }

        // Average the sampled colors
        if (samplePoints.isEmpty()) return Color.WHITE

        var r = 0
        var g = 0
        var b = 0
        for (pixel in samplePoints) {
            r += Color.red(pixel)
            g += Color.green(pixel)
            b += Color.blue(pixel)
        }
        val count = samplePoints.size
        return Color.rgb(r / count, g / count, b / count)
    }

    private fun getContrastingTextColor(bgColor: Int): Int {
        val luminance = (0.299 * Color.red(bgColor) +
                0.587 * Color.green(bgColor) +
                0.114 * Color.blue(bgColor)) / 255.0
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
    }
}
