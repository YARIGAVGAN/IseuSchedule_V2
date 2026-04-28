package com.example.scheduleiseu.feature.auth.studentregistration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.scheduleiseu.data.repository.core.StudentRegistrationRepositoryFactory
import com.example.scheduleiseu.domain.core.repository.StudentRegistrationRepository

class StudentRegistrationViewModelFactory(
    private val repository: StudentRegistrationRepository = StudentRegistrationRepositoryFactory.create()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(StudentRegistrationViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        return StudentRegistrationViewModel(repository) as T
    }
}
