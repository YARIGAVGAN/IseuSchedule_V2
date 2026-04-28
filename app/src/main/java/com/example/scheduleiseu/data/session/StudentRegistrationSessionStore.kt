package com.example.scheduleiseu.data.session

import com.example.scheduleiseu.data.remote.model.TimeTableData
import com.example.scheduleiseu.domain.core.model.StudentProfile
import java.util.concurrent.atomic.AtomicReference

class StudentRegistrationSessionStore {
    private val rawStateRef = AtomicReference<TimeTableData?>(null)
    private val profileRef = AtomicReference<StudentProfile?>(null)

    fun getRawState(): TimeTableData? = rawStateRef.get()

    fun saveRawState(state: TimeTableData) {
        rawStateRef.set(state)
    }

    fun clearRawState() {
        rawStateRef.set(null)
    }

    fun getSavedProfile(): StudentProfile? = profileRef.get()

    fun saveProfile(profile: StudentProfile) {
        profileRef.set(profile)
    }
}
