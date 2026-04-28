package com.example.scheduleiseu.domain.core.repository

import com.example.scheduleiseu.domain.core.model.AuthSession
import com.example.scheduleiseu.domain.core.model.StudentProfile
import com.example.scheduleiseu.domain.core.model.TeacherProfile
import com.example.scheduleiseu.domain.core.model.UserPhoto
import com.example.scheduleiseu.domain.core.model.UserRole
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeCachedStudentProfile(): Flow<StudentProfile?>
    fun observeCachedTeacherProfile(): Flow<TeacherProfile?>
    fun observeCachedUserPhoto(role: UserRole): Flow<UserPhoto?>

    suspend fun refreshStudentProfile(session: AuthSession): StudentProfile
    suspend fun refreshTeacherProfile(session: AuthSession): TeacherProfile
    suspend fun refreshUserPhoto(session: AuthSession): UserPhoto?
}
