package com.example.scheduleiseu.data.session

import com.example.scheduleiseu.data.remote.model.TeacherTimeTableData
import com.example.scheduleiseu.data.remote.model.TimeTableData
import java.util.concurrent.atomic.AtomicReference

class ScheduleSessionStore {
    private val studentStateRef = AtomicReference<TimeTableData?>(null)
    private val teacherStateRef = AtomicReference<TeacherTimeTableData?>(null)

    fun getStudentRawState(): TimeTableData? = studentStateRef.get()

    fun saveStudentRawState(state: TimeTableData) {
        studentStateRef.set(state)
    }

    fun getTeacherRawState(): TeacherTimeTableData? = teacherStateRef.get()

    fun saveTeacherRawState(state: TeacherTimeTableData) {
        teacherStateRef.set(state)
    }

    fun clearTeacherRawState() {
        teacherStateRef.set(null)
    }
}
