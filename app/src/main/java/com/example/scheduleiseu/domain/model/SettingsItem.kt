package com.example.scheduleiseu.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class SettingsItem(
    val id: String,
    val title: String,
    val checked: Boolean,
)
