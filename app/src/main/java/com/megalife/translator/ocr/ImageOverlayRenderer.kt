package com.megalife.translator.ocr

import android.graphics.*
import android.text.TextPaint
import com.megalife.translator.data.model.LanguagePair
import kotlin.math.atan2
import kotlin.math.sqrt

class ImageOverlayRenderer {

    fun renderOverlay(
        originalBitmap: Bitmap,
        blocks: List<OcrBlock>,
        translations: List<String>,
        targetLangCode: String
    ): Bitmap {
        val result = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        for (i in blocks.indices) {
            if (i >= translations.size) break
            val block = blocks[i]
            val translation = translations[i]
            if (translation.isBlank()) continue

            val box = block.boundingBox
            if (box.isEmpty) continue

            // Sample background color
            val bgColor = sampleBackgroundColor(originalBitmap, box)

            // Calculate rotation from corner points
            val rotation = calculateRotation(block.cornerPoints)

            canvas.save()

            if (rotation != 0f) {
                canvas.rotate(rotation, box.centerX().toFloat(), box.centerY().toFloat())
            }

            // Draw filled background rectangle
            val bgPaint = Paint().apply {
                color = bgColor
                style = Paint.Style.FILL
            }
            val padding = 2
            canvas.drawRect(
                (box.left - padding).toFloat(),
                (box.top - padding).toFloat(),
                (box.right + padding).toFloat(),
                (box.bottom + padding).toFloat(),
                bgPaint
            )

            // Draw translated text
            val textPaint = TextPaint().apply {
                color = getContrastColor(bgColor)
                isAntiAlias = true
                textAlign = if (LanguagePair.isRtl(targetLangCode)) {
                    Paint.Align.RIGHT
                } else {
                    Paint.Align.LEFT
                }
            }

            // Auto-fit font size
            val fontSize = calculateFontSize(translation, box, textPaint)
            textPaint.textSize = fontSize

            // Draw the text
            val x = if (LanguagePair.isRtl(targetLangCode)) {
                box.right.toFloat()
            } else {
                box.left.toFloat()
            }

            // Vertically center text in bounding box
            val fontMetrics = textPaint.fontMetrics
            val textHeight = fontMetrics.descent - fontMetrics.ascent
            val y = box.top + (box.height() - textHeight) / 2 - fontMetrics.ascent

            canvas.drawText(translation, x, y, textPaint)

            canvas.restore()
        }

        return result
    }

    private fun sampleBackgroundColor(bitmap: Bitmap, box: Rect): Int {
        val samplePoints = mutableListOf<Int>()
        val margin = 5
        val width = bitmap.width
        val height = bitmap.height

        // Sample pixels around the bounding box edges
        val samplePositions = listOf(
            // Above the box
            Pair(box.centerX(), (box.top - margin).coerceIn(0, height - 1)),
            // Below the box
            Pair(box.centerX(), (box.bottom + margin).coerceIn(0, height - 1)),
            // Left of box
            Pair((box.left - margin).coerceIn(0, width - 1), box.centerY()),
            // Right of box
            Pair((box.right + margin).coerceIn(0, width - 1), box.centerY()),
            // Corners
            Pair((box.left - margin).coerceIn(0, width - 1), (box.top - margin).coerceIn(0, height - 1)),
            Pair((box.right + margin).coerceIn(0, width - 1), (box.top - margin).coerceIn(0, height - 1)),
            Pair((box.left - margin).coerceIn(0, width - 1), (box.bottom + margin).coerceIn(0, height - 1)),
            Pair((box.right + margin).coerceIn(0, width - 1), (box.bottom + margin).coerceIn(0, height - 1))
        )

        for ((x, y) in samplePositions) {
            val px = x.coerceIn(0, width - 1)
            val py = y.coerceIn(0, height - 1)
            samplePoints.add(bitmap.getPixel(px, py))
        }

        // Average the sampled colors
        if (samplePoints.isEmpty()) return Color.WHITE

        var r = 0; var g = 0; var b = 0
        for (pixel in samplePoints) {
            r += Color.red(pixel)
            g += Color.green(pixel)
            b += Color.blue(pixel)
        }
        val count = samplePoints.size
        return Color.rgb(r / count, g / count, b / count)
    }

    private fun getContrastColor(bgColor: Int): Int {
        val luminance = (0.299 * Color.red(bgColor) +
                0.587 * Color.green(bgColor) +
                0.114 * Color.blue(bgColor)) / 255.0
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
    }

    private fun calculateRotation(cornerPoints: Array<Point>?): Float {
        if (cornerPoints == null || cornerPoints.size < 2) return 0f
        val dx = (cornerPoints[1].x - cornerPoints[0].x).toDouble()
        val dy = (cornerPoints[1].y - cornerPoints[0].y).toDouble()
        val angle = Math.toDegrees(atan2(dy, dx)).toFloat()
        return if (kotlin.math.abs(angle) < 2f) 0f else angle
    }

    private fun calculateFontSize(text: String, box: Rect, paint: TextPaint): Float {
        var fontSize = box.height().toFloat() * 0.8f
        paint.textSize = fontSize

        val maxWidth = box.width().toFloat()
        var measuredWidth = paint.measureText(text)

        // Shrink font if text is wider than the box
        while (measuredWidth > maxWidth && fontSize > 6f) {
            fontSize -= 1f
            paint.textSize = fontSize
            measuredWidth = paint.measureText(text)
        }

        return fontSize.coerceAtLeast(6f)
    }
}
