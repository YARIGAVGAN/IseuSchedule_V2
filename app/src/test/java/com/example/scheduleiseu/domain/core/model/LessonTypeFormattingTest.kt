package com.example.scheduleiseu.domain.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonTypeFormattingTest {

    @Test
    fun `known lesson types are converted to presentation labels`() {
        assertEquals("лекция", "Лек.".toFormattedLessonTypeLabel())
        assertEquals("практическое занятие", "практ. зан.".toFormattedLessonTypeLabel())
        assertEquals("лабораторная работа", "Лабораторная".toFormattedLessonTypeLabel())
        assertEquals("диф. зачет", "Дифференцированный зачёт".toFormattedLessonTypeLabel())
    }

    @Test
    fun `unknown type is preserved and blank type is omitted`() {
        assertEquals("Семинар", "  Семинар  ".toFormattedLessonTypeLabel())
        assertNull("  ".toFormattedLessonTypeLabel())
        assertNull(null.toFormattedLessonTypeLabel())
    }

    @Test
    fun `badge color rule is enabled only for control lesson types`() {
        assertTrue("экзамен".shouldUseMidGreenLessonTypeBadge())
        assertTrue("зачёт".shouldUseMidGreenLessonTypeBadge())
        assertFalse("лекция".shouldUseMidGreenLessonTypeBadge())
    }
}
