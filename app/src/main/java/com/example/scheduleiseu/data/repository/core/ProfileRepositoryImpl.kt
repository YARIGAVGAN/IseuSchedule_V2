package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.data.local.cache.ProfilePhotoCacheDataSource
import com.example.scheduleiseu.data.local.preferences.AppPreferencesDataSource
import com.example.scheduleiseu.data.mapper.contract.ProfileDataToStudentProfileMapper
import com.example.scheduleiseu.data.mapper.contract.ProfileDataToTeacherProfileMapper
import com.example.scheduleiseu.data.mapper.contract.ProfileDataToUserPhotoMapper
import com.example.scheduleiseu.data.remote.datasource.ProfileRemoteDataSource
import com.example.scheduleiseu.domain.core.model.AuthSession
import com.example.scheduleiseu.domain.core.model.StudentProfile
import com.example.scheduleiseu.domain.core.model.TeacherProfile
import com.example.scheduleiseu.domain.core.model.UserPhoto
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.domain.core.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ProfileRepositoryImpl(
    private val remoteDataSource: ProfileRemoteDataSource,
    private val studentProfileMapper: ProfileDataToStudentProfileMapper,
    private val teacherProfileMapper: ProfileDataToTeacherProfileMapper,
    private val photoMapper: ProfileDataToUserPhotoMapper,
    private val preferencesDataSource: AppPreferencesDataSource? = null,
    private val photoCacheDataSource: ProfilePhotoCacheDataSource? = null
) : ProfileRepository {

    private val profileRefreshMutex = Mutex()
    private val photoRefreshMutex = Mutex()

    private val photoCacheEvents = MutableSharedFlow<UserRole>(
        extraBufferCapacity = 1
    )

    override fun observeCachedStudentProfile(): Flow<StudentProfile?> {
        return preferencesDataSource?.observeStudentProfile() ?: flowOf(null)
    }

    override fun observeCachedTeacherProfile(): Flow<TeacherProfile?> {
        return preferencesDataSource?.observeTeacherProfile() ?: flowOf(null)
    }

    override fun observeCachedUserPhoto(role: UserRole): Flow<UserPhoto?> {
        val photoCache = photoCacheDataSource ?: return flowOf(null)

        return flow {
            emit(photoCache.read(role))

            emitAll(
                photoCacheEvents
                    .filter { changedRole -> changedRole == role }
                    .map { photoCache.read(role) }
            )
        }
    }

    override suspend fun refreshStudentProfile(session: AuthSession): StudentProfile {
        return profileRefreshMutex.withLock {
            ensureAuthenticated(session, UserRole.STUDENT)

            val remoteProfile = studentProfileMapper.map(remoteDataSource.getProfile())
            val savedProfile = preferencesDataSource?.getStudentProfile()
            val mergedProfile = remoteProfile.mergeWithSavedRegistrationProfile(savedProfile)
            val profileWithoutPhoto = mergedProfile.copy(photo = null)

            preferencesDataSource?.saveStudentProfile(profileWithoutPhoto)

            profileWithoutPhoto
        }
    }

    override suspend fun refreshTeacherProfile(session: AuthSession): TeacherProfile {
        return profileRefreshMutex.withLock {
            ensureAuthenticated(session, UserRole.TEACHER)

            val remoteProfile = teacherProfileMapper.map(remoteDataSource.getProfile())
            val savedProfile = preferencesDataSource?.getTeacherProfile()
            val mergedProfile = remoteProfile.mergeWithSavedRegistrationProfile(savedProfile)
            val profileWithoutPhoto = mergedProfile.copy(photo = null)

            preferencesDataSource?.saveTeacherProfile(profileWithoutPhoto)

            profileWithoutPhoto
        }
    }

    override suspend fun refreshUserPhoto(session: AuthSession): UserPhoto? {
        return photoRefreshMutex.withLock {
            ensureAuthenticated(session, session.userRole)

            val photo = runCatching {
                photoMapper.map(remoteDataSource.getProfile())
            }.getOrNull()

            if (photo == null) {
                return@withLock photoCacheDataSource?.read(session.userRole)
            }

            photoCacheDataSource?.write(session.userRole, photo)
            photoCacheEvents.tryEmit(session.userRole)

            photo
        }
    }

    private fun StudentProfile.mergeWithSavedRegistrationProfile(
        savedProfile: StudentProfile?
    ): StudentProfile {
        if (savedProfile == null) return this

        return copy(
            login = login.takeUnless(String?::isNullOrBlank) ?: savedProfile.login,
            facultyId = savedProfile.facultyId,
            departmentId = savedProfile.departmentId,
            courseId = savedProfile.courseId,
            groupId = savedProfile.groupId,
            faculty = faculty.takeUnless(String?::isNullOrBlank) ?: savedProfile.faculty,
            department = department.takeUnless(String?::isNullOrBlank) ?: savedProfile.department,
            course = course.takeUnless(String?::isNullOrBlank) ?: savedProfile.course,
            group = group.takeUnless(String?::isNullOrBlank) ?: savedProfile.group,
            averageScore = averageScore.takeUnless(String?::isNullOrBlank) ?: savedProfile.averageScore
        )
    }

    private fun TeacherProfile.mergeWithSavedRegistrationProfile(
        savedProfile: TeacherProfile?
    ): TeacherProfile {
        if (savedProfile == null) return this

        return copy(
            teacherId = savedProfile.teacherId,
            login = login.takeUnless(String?::isNullOrBlank) ?: savedProfile.login,
            faculty = faculty.takeUnless(String?::isNullOrBlank) ?: savedProfile.faculty,
            department = department.takeUnless(String?::isNullOrBlank) ?: savedProfile.department
        )
    }

    private fun ensureAuthenticated(session: AuthSession, expectedRole: UserRole) {
        if (!session.isAuthenticated) {
            throw IllegalStateException("Пользователь не авторизован")
        }

        if (session.userRole != expectedRole) {
            throw IllegalStateException("Профиль недоступен для текущей роли")
        }
    }
}
