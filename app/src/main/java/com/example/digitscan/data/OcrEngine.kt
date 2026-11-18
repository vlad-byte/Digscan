package com.example.digitscan.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import com.example.digitscan.domain.TextProcessing
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/** Офлайн-распознавание текста (ML Kit, модель вшита в APK). */
class OcrEngine {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /** По ТЗ на фото один блок текста; если блоков несколько — берём самый крупный. */
    suspend fun recognize(file: File): String {
        val (bitmap, rotation) = withContext(Dispatchers.IO) { decodeScaled(file) }
        try {
            val result = recognizer.process(InputImage.fromBitmap(bitmap, rotation)).await()
            val block = result.textBlocks.maxByOrNull { b ->
                b.boundingBox?.let { it.width() * it.height() } ?: 0
            }
            return TextProcessing.normalize(block?.text ?: result.text)
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Полноразмерный снимок (12+ Мп) в памяти занимает десятки МБ, из-за чего приложение падало
     * при нескольких фото. Для распознавания достаточно ~2000 px по длинной стороне.
     */
    private fun decodeScaled(file: File): Pair<Bitmap, Int> {
        val path = file.absolutePath
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val longest = max(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (longest / sample > MAX_SIDE) sample *= 2

        val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IllegalStateException("Не удалось прочитать фото")
        val rotation = when (
            ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        return bitmap to rotation
    }

    private companion object {
        const val MAX_SIDE = 2000
    }
}
