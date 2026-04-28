package com.example.scheduleiseu.data.session

import com.example.scheduleiseu.data.remote.model.TeacherTimeTableData
import com.example.scheduleiseu.domain.core.model.TeacherProfile
import java.util.concurrent.atomic.AtomicReference

class TeacherRegistrationSessionStore {
    private val rawStateRef = AtomicReference<TeacherTimeTableData?>(null)
    private val profileRef = AtomicReference<TeacherProfile?>(null)

    fun getRawState(): TeacherTimeTableData? = rawStateRef.get()

    fun saveRawState(state: TeacherTimeTableData) {
        rawStateRef.set(state)
    }

    fun clearRawState() {
        rawStateRef.set(null)
    }

    fun getSavedProfile(): TeacherProfile? = profileRef.get()

    fun saveProfile(profile: TeacherProfile) {
        profileRef.set(profile)
    }

    fun clearProfile() {
        profileRef.set(null)
    }

    fun clearAll() {
        clearRawState()
        clearProfile()
    }
}
