package com.example.scheduleiseu.domain.core.model

data class UserPhoto(
    val bytes: ByteArray,
    val sourceUrl: String? = null,
    val mimeType: String? = null
)
