package com.example.scheduleiseu.feature.menu

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class MenuProfileUiState(
    val isLoading: Boolean = true,
    val fullName: String = "Профиль",
    val groupOrPosition: String = "Данные профиля недоступны",
    val details: String = "Откройте меню после входа в личный кабинет",
    val role: String = "Пользователь",
    val isTeacherMode: Boolean = false,
    val isScheduleOnlyMode: Boolean = false,
    val averageScore: String? = null,
    val errorMessage: String? = null,
    val photoBitmap: Bitmap? = null,
    val isPhotoLoading: Boolean = true,
    val showPhoto: Boolean = true
)
