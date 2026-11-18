package com.example.digitscan.data

import android.content.Context
import android.net.Uri
import com.example.digitscan.domain.TextProcessing
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/** Офлайн-распознавание текста (ML Kit, модель вшита в APK). */
class OcrEngine(private val context: Context) {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /** По ТЗ на фото один блок текста; если блоков несколько — берём самый крупный. */
    suspend fun recognize(file: File): String {
        val image = withContext(Dispatchers.IO) {
            InputImage.fromFilePath(context, Uri.fromFile(file))
        }
        val result = recognizer.process(image).await()
        val block = result.textBlocks.maxByOrNull { b ->
            b.boundingBox?.let { it.width() * it.height() } ?: 0
        }
        return TextProcessing.normalize(block?.text ?: result.text)
    }
}
