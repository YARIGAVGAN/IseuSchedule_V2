package com.example.scheduleiseu.feature.home

import com.example.scheduleiseu.domain.model.TeacherSearchItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeacherScheduleSearchRulesTest {

    private val teachers = listOf(
        TeacherSearchItem("1", "Иванов Иван Иванович", "кафедра информатики"),
        TeacherSearchItem("2", "Петров Петр Петрович", "кафедра физики"),
    )

    @Test
    fun `teacher filter searches name and subtitle ignoring case`() {
        assertEquals(listOf("1"), filterTeachers("ивАНов", teachers).map { it.id })
        assertEquals(listOf("2"), filterTeachers(" ФИЗИКИ ", teachers).map { it.id })
        assertEquals(teachers, filterTeachers("  ", teachers))
    }

    @Test
    fun `placeholder rule recognizes selector prompt only`() {
        assertTrue("Выберите фамилию преподавателя".isTeacherPlaceholder())
        assertTrue(" выберите группу ".isTeacherPlaceholder())
        assertFalse("Иванов Иван Иванович".isTeacherPlaceholder())
    }
}
