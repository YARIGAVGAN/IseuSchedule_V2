package com.example.scheduleiseu.data.remote.model

data class ProfileData(
    val fullName: String?,
    val role: String?,
    val faculty: String?,
    val groupInfo: String?,
    val averageScore: String?,
    val cabinetMenuLinks: List<CabinetMenuLink> = emptyList(),
    val cabinetHtml: String,
    val progressHtml: String? = null,
    val photoBytes: ByteArray?
)

data class CabinetMenuLink(
    val title: String,
    val url: String?
)
