package com.example.scheduleiseu.domain.core.service

interface CaptchaRecognizer {
    suspend fun recognize(imageBytes: ByteArray): String?
}
