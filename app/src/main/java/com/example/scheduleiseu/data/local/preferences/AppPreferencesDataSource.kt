package com.example.scheduleiseu.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.scheduleiseu.domain.core.model.StudentProfile
import com.example.scheduleiseu.domain.core.model.TeacherProfile
import com.example.scheduleiseu.domain.core.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.appDataStore by preferencesDataStore(name = "scheduleiseu_preferences")

class AppPreferencesDataSource(
    context: Context
) {
    private val dataStore = context.applicationContext.appDataStore

    private val studentBootstrapCompletedAccountKeys =
        stringSetPreferencesKey("student_bootstrap_completed_account_keys")

    fun observeCacheCurrentAndPreviousWeekEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[Keys.CACHE_CURRENT_AND_PREVIOUS_WEEK] ?: false
        }
    }

    suspend fun isCacheCurrentAndPreviousWeekEnabled(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[Keys.CACHE_CURRENT_AND_PREVIOUS_WEEK] ?: false
        }.first()
    }

    suspend fun setCacheCurrentAndPreviousWeekEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.CACHE_CURRENT_AND_PREVIOUS_WEEK] = value
        }
    }

    fun observeLessonNotificationsEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[Keys.LESSON_NOTIFICATIONS_ENABLED] ?: false
        }
    }

    suspend fun isLessonNotificationsEnabled(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[Keys.LESSON_NOTIFICATIONS_ENABLED] ?: false
        }.first()
    }

    suspend fun setLessonNotificationsEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.LESSON_NOTIFICATIONS_ENABLED] = value
        }
    }

    fun observeShowMismatchedSubgroupLessonsEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[Keys.SHOW_MISMATCHED_SUBGROUP_LESSONS] ?: true
        }
    }

    suspend fun isShowMismatchedSubgroupLessonsEnabled(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[Keys.SHOW_MISMATCHED_SUBGROUP_LESSONS] ?: true
        }.first()
    }

    suspend fun setShowMismatchedSubgroupLessonsEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SHOW_MISMATCHED_SUBGROUP_LESSONS] = value
        }
    }

    suspend fun isStudentScheduleOnlyModeEnabled(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[Keys.STUDENT_SCHEDULE_ONLY_MODE] ?: false
        }.first()
    }

    fun isStudentScheduleOnlyModeEnabledBlocking(): Boolean = runBlocking { isStudentScheduleOnlyModeEnabled() }

    suspend fun setStudentScheduleOnlyModeEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.STUDENT_SCHEDULE_ONLY_MODE] = value
        }
    }

    suspend fun clearStudentScheduleOnlyMode() {
        setStudentScheduleOnlyModeEnabled(false)
    }

    suspend fun isStudentBootstrapCompleted(accountKey: String): Boolean {
        val normalizedAccountKey = accountKey.trim()
        if (normalizedAccountKey.isBlank()) return false

        val completedAccountKeys = dataStore.data.first()[studentBootstrapCompletedAccountKeys]
            ?: emptySet()
        return normalizedAccountKey in completedAccountKeys
    }

    suspend fun markStudentBootstrapCompleted(accountKey: String) {
        val normalizedAccountKey = accountKey.trim()
        if (normalizedAccountKey.isBlank()) return

        dataStore.edit { preferences ->
            val currentCompletedAccountKeys = preferences[studentBootstrapCompletedAccountKeys]
                ?: emptySet()
            preferences[studentBootstrapCompletedAccountKeys] =
                currentCompletedAccountKeys + normalizedAccountKey
        }
    }

    suspend fun resetStudentBootstrap() {
        dataStore.edit { preferences ->
            preferences.remove(studentBootstrapCompletedAccountKeys)
        }
    }

    suspend fun setUserRole(role: UserRole) {
        dataStore.edit { preferences ->
            preferences[Keys.USER_ROLE] = role.name
        }
    }

    suspend fun getUserRole(): UserRole? {
        val raw = dataStore.data.map { it[Keys.USER_ROLE] }.first()
        return raw?.let { value -> runCatching { UserRole.valueOf(value) }.getOrNull() }
    }

    fun getUserRoleBlocking(): UserRole? = runBlocking { getUserRole() }

    suspend fun setAuthFlags(
        isAuthenticated: Boolean,
        loginPagePrepared: Boolean,
        captchaRequired: Boolean
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.IS_AUTHENTICATED] = isAuthenticated
            preferences[Keys.LOGIN_PAGE_PREPARED] = loginPagePrepared
            preferences[Keys.CAPTCHA_REQUIRED] = captchaRequired
        }
    }

    suspend fun clearAuthFlags() {
        dataStore.edit { preferences ->
            preferences[Keys.IS_AUTHENTICATED] = false
            preferences[Keys.LOGIN_PAGE_PREPARED] = false
            preferences[Keys.CAPTCHA_REQUIRED] = true
        }
    }

    suspend fun isAuthenticated(): Boolean {
        return dataStore.data.map { it[Keys.IS_AUTHENTICATED] ?: false }.first()
    }

    suspend fun saveStudentCredentials(login: String, password: String) {
        dataStore.edit { preferences ->
            putOrRemove(preferences, Keys.SAVED_STUDENT_LOGIN, login)
            putOrRemove(preferences, Keys.SAVED_STUDENT_PASSWORD, password)
        }
    }

    suspend fun clearStudentCredentials() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.SAVED_STUDENT_LOGIN)
            preferences.remove(Keys.SAVED_STUDENT_PASSWORD)
        }
    }

    suspend fun getSavedStudentLogin(): String? {
        return dataStore.data.map { it[Keys.SAVED_STUDENT_LOGIN]?.takeIf(String::isNotBlank) }.first()
    }

    suspend fun getSavedStudentPassword(): String? {
        return dataStore.data.map { it[Keys.SAVED_STUDENT_PASSWORD]?.takeIf(String::isNotBlank) }.first()
    }

    suspend fun hasSavedStudentCredentials(): Boolean {
        val preferences = dataStore.data.first()
        return !preferences[Keys.SAVED_STUDENT_LOGIN].isNullOrBlank() &&
            !preferences[Keys.SAVED_STUDENT_PASSWORD].isNullOrBlank()
    }

    suspend fun isStudentRegistrationCompleted(): Boolean {
        return dataStore.data.map { it[Keys.STUDENT_REGISTRATION_COMPLETED] ?: false }.first()
    }

    suspend fun setStudentRegistrationCompleted(value: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.STUDENT_REGISTRATION_COMPLETED] = value }
    }

    fun observeStudentProfile(): Flow<StudentProfile?> {
        return dataStore.data.map { preferences -> preferences.toStudentProfile() }
    }

    suspend fun saveStudentProfile(profile: StudentProfile) {
        dataStore.edit { preferences ->
            preferences[Keys.STUDENT_FULL_NAME] = profile.fullName
            putOrRemove(preferences, Keys.STUDENT_LOGIN, profile.login)
            putOrRemove(preferences, Keys.STUDENT_FACULTY_ID, profile.facultyId)
            putOrRemove(preferences, Keys.STUDENT_FACULTY, profile.faculty)
            putOrRemove(preferences, Keys.STUDENT_DEPARTMENT_ID, profile.departmentId)
            putOrRemove(preferences, Keys.STUDENT_DEPARTMENT, profile.department)
            putOrRemove(preferences, Keys.STUDENT_COURSE_ID, profile.courseId)
            putOrRemove(preferences, Keys.STUDENT_COURSE, profile.course)
            putOrRemove(preferences, Keys.STUDENT_GROUP_ID, profile.groupId)
            putOrRemove(preferences, Keys.STUDENT_GROUP, profile.group)
            putOrRemove(preferences, Keys.STUDENT_SUBGROUP, profile.subgroup)
            putOrRemove(preferences, Keys.STUDENT_AVERAGE_SCORE, profile.averageScore)
            preferences[Keys.STUDENT_REGISTRATION_COMPLETED] = true
        }
    }

    suspend fun getStudentProfile(): StudentProfile? {
        return dataStore.data.first().toStudentProfile()
    }

    fun getStudentProfileBlocking(): StudentProfile? = runBlocking { getStudentProfile() }

    fun observeTeacherProfile(): Flow<TeacherProfile?> {
        return dataStore.data.map { preferences -> preferences.toTeacherProfile() }
    }

    suspend fun saveTeacherProfile(profile: TeacherProfile) {
        dataStore.edit { preferences ->
            preferences[Keys.TEACHER_FULL_NAME] = profile.fullName
            putOrRemove(preferences, Keys.TEACHER_ID, profile.teacherId)
            putOrRemove(preferences, Keys.TEACHER_LOGIN, profile.login)
            putOrRemove(preferences, Keys.TEACHER_FACULTY, profile.faculty)
            putOrRemove(preferences, Keys.TEACHER_DEPARTMENT, profile.department)
        }
    }

    suspend fun getTeacherProfile(): TeacherProfile? {
        return dataStore.data.first().toTeacherProfile()
    }

    fun getTeacherProfileBlocking(): TeacherProfile? = runBlocking { getTeacherProfile() }
    suspend fun clearTeacherProfileForLogout() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.TEACHER_FULL_NAME)
            preferences.remove(Keys.TEACHER_ID)
            preferences.remove(Keys.TEACHER_LOGIN)
            preferences.remove(Keys.TEACHER_FACULTY)
            preferences.remove(Keys.TEACHER_DEPARTMENT)
            preferences[Keys.IS_AUTHENTICATED] = false
            preferences[Keys.LOGIN_PAGE_PREPARED] = false
            preferences[Keys.CAPTCHA_REQUIRED] = true
        }
    }

    suspend fun clearSessionFlagsForLogout() {
        dataStore.edit { preferences ->
            preferences[Keys.IS_AUTHENTICATED] = false
            preferences[Keys.LOGIN_PAGE_PREPARED] = false
            preferences[Keys.CAPTCHA_REQUIRED] = true
            preferences[Keys.STUDENT_SCHEDULE_ONLY_MODE] = false
        }
    }

    private fun Preferences.toStudentProfile(): StudentProfile? {
        val fullName = this[Keys.STUDENT_FULL_NAME]?.takeIf { it.isNotBlank() } ?: return null
        return StudentProfile(
            fullName = fullName,
            login = this[Keys.STUDENT_LOGIN],
            facultyId = this[Keys.STUDENT_FACULTY_ID],
            faculty = this[Keys.STUDENT_FACULTY],
            departmentId = this[Keys.STUDENT_DEPARTMENT_ID],
            department = this[Keys.STUDENT_DEPARTMENT],
            courseId = this[Keys.STUDENT_COURSE_ID],
            course = this[Keys.STUDENT_COURSE],
            groupId = this[Keys.STUDENT_GROUP_ID],
            group = this[Keys.STUDENT_GROUP],
            subgroup = this[Keys.STUDENT_SUBGROUP],
            averageScore = this[Keys.STUDENT_AVERAGE_SCORE]
        )
    }

    private fun Preferences.toTeacherProfile(): TeacherProfile? {
        val fullName = this[Keys.TEACHER_FULL_NAME]?.takeIf { it.isNotBlank() } ?: return null
        return TeacherProfile(
            fullName = fullName,
            teacherId = this[Keys.TEACHER_ID],
            login = this[Keys.TEACHER_LOGIN],
            faculty = this[Keys.TEACHER_FACULTY],
            department = this[Keys.TEACHER_DEPARTMENT]
        )
    }

    private fun putOrRemove(
        preferences: MutablePreferences,
        key: Preferences.Key<String>,
        value: String?
    ) {
        if (value.isNullOrBlank()) {
            preferences.remove(key)
        } else {
            preferences[key] = value
        }
    }

    private object Keys {
        val USER_ROLE = stringPreferencesKey("user_role")
        val IS_AUTHENTICATED = booleanPreferencesKey("is_authenticated")
        val LOGIN_PAGE_PREPARED = booleanPreferencesKey("login_page_prepared")
        val CAPTCHA_REQUIRED = booleanPreferencesKey("captcha_required")
        val SAVED_STUDENT_LOGIN = stringPreferencesKey("saved_student_login")
        val SAVED_STUDENT_PASSWORD = stringPreferencesKey("saved_student_password")
        val STUDENT_REGISTRATION_COMPLETED = booleanPreferencesKey("student_registration_completed")
        val CACHE_CURRENT_AND_PREVIOUS_WEEK = booleanPreferencesKey("cache_current_and_previous_week")
        val LESSON_NOTIFICATIONS_ENABLED = booleanPreferencesKey("lesson_notifications_enabled")
        val SHOW_MISMATCHED_SUBGROUP_LESSONS = booleanPreferencesKey("show_mismatched_subgroup_lessons")
        val STUDENT_SCHEDULE_ONLY_MODE = booleanPreferencesKey("student_schedule_only_mode")

        val STUDENT_FULL_NAME = stringPreferencesKey("student_full_name")
        val STUDENT_LOGIN = stringPreferencesKey("student_login")
        val STUDENT_FACULTY_ID = stringPreferencesKey("student_faculty_id")
        val STUDENT_FACULTY = stringPreferencesKey("student_faculty")
        val STUDENT_DEPARTMENT_ID = stringPreferencesKey("student_department_id")
        val STUDENT_DEPARTMENT = stringPreferencesKey("student_department")
        val STUDENT_COURSE_ID = stringPreferencesKey("student_course_id")
        val STUDENT_COURSE = stringPreferencesKey("student_course")
        val STUDENT_GROUP_ID = stringPreferencesKey("student_group_id")
        val STUDENT_GROUP = stringPreferencesKey("student_group")
        val STUDENT_SUBGROUP = stringPreferencesKey("student_subgroup")
        val STUDENT_AVERAGE_SCORE = stringPreferencesKey("student_average_score")

        val TEACHER_FULL_NAME = stringPreferencesKey("teacher_full_name")
        val TEACHER_ID = stringPreferencesKey("teacher_id")
        val TEACHER_LOGIN = stringPreferencesKey("teacher_login")
        val TEACHER_FACULTY = stringPreferencesKey("teacher_faculty")
        val TEACHER_DEPARTMENT = stringPreferencesKey("teacher_department")
    }
}
