package com.example.scheduleiseu.data.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.example.scheduleiseu.domain.core.service.CaptchaRecognizer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class MlKitCaptchaRecognizer(
    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
) : CaptchaRecognizer {

    override suspend fun recognize(imageBytes: ByteArray): String? = withContext(Dispatchers.Default) {
        val original = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return@withContext null

        val rawResult = recognizeBitmap(original)
        if (rawResult != null) return@withContext rawResult

        val prepared = original.preprocessForCaptcha()
        if (prepared === original) {
            null
        } else {
            recognizeBitmap(prepared)
        }
    }

    private suspend fun recognizeBitmap(bitmap: Bitmap): String? {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizedText = suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    if (continuation.isActive) continuation.resume(result.text)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume("")
                }
                .addOnCanceledListener {
                    if (continuation.isActive) continuation.resume("")
                }
        }

        return recognizedText.toCaptchaCodeOrNull()
    }

    private fun Bitmap.preprocessForCaptcha(): Bitmap {
        if (width <= 0 || height <= 0) return this

        val output = createBitmap(width, height)
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)

        var luminanceSum = 0L
        pixels.forEach { color ->
            luminanceSum += color.luminance()
        }
        val threshold = (luminanceSum / pixels.size).coerceIn(96L, 196L).toInt()

        val prepared = IntArray(pixels.size) { index ->
            val luminance = pixels[index].luminance()
            if (luminance < threshold) Color.BLACK else Color.WHITE
        }
        output.setPixels(prepared, 0, width, 0, 0, width, height)
        return output
    }

    private fun Int.luminance(): Int {
        val red = Color.red(this)
        val green = Color.green(this)
        val blue = Color.blue(this)
        return ((red * 30) + (green * 59) + (blue * 11)) / 100
    }

    private fun String.toCaptchaCodeOrNull(): String? {
        val candidates = lineSequence()
            .flatMap { line -> line.splitToSequence(Regex("\\s+")) }
            .map { token -> token.normalizeCaptchaToken() }
            .filter { token -> token.length in MIN_CAPTCHA_LENGTH..MAX_CAPTCHA_LENGTH }
            .toList()

        return candidates.firstOrNull()
    }

    private fun String.normalizeCaptchaToken(): String {
        return trim()
            .uppercase()
            .replace('О', 'O')
            .replace('І', 'I')
            .replace('С', 'C')
            .replace(Regex("[^A-Z0-9]"), "")
            .replace('O', '0')
            .replace('I', '1')
            .replace('L', '1')
            .replace('S', '5')
    }

    private companion object {
        const val MIN_CAPTCHA_LENGTH = 3
        const val MAX_CAPTCHA_LENGTH = 8
    }
}
