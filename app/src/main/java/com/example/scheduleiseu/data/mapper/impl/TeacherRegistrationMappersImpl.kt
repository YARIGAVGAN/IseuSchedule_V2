package com.example.scheduleiseu.data.mapper.impl

import com.example.scheduleiseu.data.mapper.contract.TeacherRegistrationContextToTeacherProfileMapper
import com.example.scheduleiseu.data.mapper.contract.TeacherTimeTableDataToTeacherRegistrationContextMapper
import com.example.scheduleiseu.data.remote.model.SelectOption
import com.example.scheduleiseu.data.remote.model.TeacherTimeTableData
import com.example.scheduleiseu.domain.core.model.TeacherProfile
import com.example.scheduleiseu.domain.core.model.TeacherRegistrationContext
import com.example.scheduleiseu.domain.core.model.TeacherRegistrationOption

class TeacherTimeTableDataToTeacherRegistrationContextMapperImpl : TeacherTimeTableDataToTeacherRegistrationContextMapper {
    override fun map(input: TeacherTimeTableData): TeacherRegistrationContext {
        val realTeachers = input.teachers.filter { it.isRealTeacherRegistrationOption() }

        return TeacherRegistrationContext(
            teachers = realTeachers.map { it.toTeacherRegistrationOption() },
            selectedTeacherId = realTeachers.firstOrNull { it.isSelected }?.value
        )
    }
}

class TeacherRegistrationContextToTeacherProfileMapperImpl : TeacherRegistrationContextToTeacherProfileMapper {
    override fun map(teacherId: String, context: TeacherRegistrationContext): TeacherProfile {
        val selectedTeacher = context.teachers.firstOrNull { it.id == teacherId }
            ?: throw IllegalStateException("Выбранный преподаватель не найден")

        return TeacherProfile(
            teacherId = selectedTeacher.id,
            fullName = selectedTeacher.title
        )
    }
}

private fun SelectOption.toTeacherRegistrationOption(): TeacherRegistrationOption {
    return TeacherRegistrationOption(
        id = value,
        title = text.trim(),
        selected = isSelected
    )
}

private fun SelectOption.isRealTeacherRegistrationOption(): Boolean {
    val normalizedText = text.trim().lowercase()
    return value.isNotBlank() &&
        normalizedText.isNotBlank() &&
        normalizedText != "выберите фамилию преподавателя" &&
        !normalizedText.startsWith("выберите ")
}
