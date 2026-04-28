package com.example.scheduleiseu.data.mapper.contract

import com.example.scheduleiseu.data.remote.model.ProfileData
import com.example.scheduleiseu.domain.core.model.StudentProfile
import com.example.scheduleiseu.domain.core.model.TeacherProfile
import com.example.scheduleiseu.domain.core.model.UserPhoto

fun interface ProfileDataToStudentProfileMapper : DataToDomainMapper<ProfileData, StudentProfile>

fun interface ProfileDataToTeacherProfileMapper : DataToDomainMapper<ProfileData, TeacherProfile>

fun interface ProfileDataToUserPhotoMapper : DataToDomainMapper<ProfileData, UserPhoto?>
