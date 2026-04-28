package com.example.scheduleiseu.data.mapper.impl

import com.example.scheduleiseu.data.mapper.contract.ProfileDataToStudentProfileMapper
import com.example.scheduleiseu.data.mapper.contract.ProfileDataToTeacherProfileMapper
import com.example.scheduleiseu.data.mapper.contract.ProfileDataToUserPhotoMapper
import com.example.scheduleiseu.data.remote.model.ProfileData
import com.example.scheduleiseu.data.remote.parser.BsuParser
import com.example.scheduleiseu.domain.core.model.StudentProfile
import com.example.scheduleiseu.domain.core.model.TeacherProfile
import com.example.scheduleiseu.domain.core.model.UserPhoto

class ProfileDataToUserPhotoMapperImpl : ProfileDataToUserPhotoMapper {
    override fun map(input: ProfileData): UserPhoto? {
        val bytes = input.photoBytes?.takeIf { it.isNotEmpty() } ?: return null
        return UserPhoto(
            bytes = bytes,
            sourceUrl = BsuParser.PHOTO_URL,
            mimeType = "image/jpeg"
        )
    }
}

class ProfileDataToStudentProfileMapperImpl(
    private val photoMapper: ProfileDataToUserPhotoMapper = ProfileDataToUserPhotoMapperImpl()
) : ProfileDataToStudentProfileMapper {
    override fun map(input: ProfileData): StudentProfile {
        val groupInfo = input.groupInfo.orEmpty()

        return StudentProfile(
            fullName = input.fullName?.takeIf { it.isNotBlank() } ?: "Студент",
            faculty = input.faculty,
            course = extractCourse(groupInfo),
            group = extractGroup(groupInfo) ?: groupInfo.takeIf { it.isNotBlank() },
            averageScore = input.averageScore,
            photo = photoMapper.map(input)
        )
    }

    private fun extractCourse(groupInfo: String): String? {
        return Regex("""\b(\d+\s*курс)\b""", RegexOption.IGNORE_CASE)
            .find(groupInfo)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
    }

    private fun extractGroup(groupInfo: String): String? {
        return Regex("""группа\s+([^,]+)""", RegexOption.IGNORE_CASE)
            .find(groupInfo)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
    }
}

class ProfileDataToTeacherProfileMapperImpl(
    private val photoMapper: ProfileDataToUserPhotoMapper = ProfileDataToUserPhotoMapperImpl()
) : ProfileDataToTeacherProfileMapper {
    override fun map(input: ProfileData): TeacherProfile {
        return TeacherProfile(
            fullName = input.fullName?.takeIf { it.isNotBlank() } ?: "Преподаватель",
            faculty = input.faculty,
            photo = photoMapper.map(input)
        )
    }
}
