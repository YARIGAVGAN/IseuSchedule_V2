package com.example.scheduleiseu.feature.auth.teacherregistration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.scheduleiseu.data.repository.core.TeacherRegistrationRepositoryFactory
import com.example.scheduleiseu.domain.core.repository.TeacherRegistrationRepository
import com.example.scheduleiseu.domain.core.usecase.LoadTeacherRegistrationContextUseCase
import com.example.scheduleiseu.domain.core.usecase.SaveTeacherProfileUseCase

class TeacherRegistrationViewModelFactory(
    repository: TeacherRegistrationRepository = TeacherRegistrationRepositoryFactory.create()
) : ViewModelProvider.Factory {

    private val loadContext = LoadTeacherRegistrationContextUseCase(repository)
    private val saveProfile = SaveTeacherProfileUseCase(repository)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TeacherRegistrationViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        return TeacherRegistrationViewModel(
            loadContext = loadContext,
            saveProfile = saveProfile
        ) as T
    }
}
