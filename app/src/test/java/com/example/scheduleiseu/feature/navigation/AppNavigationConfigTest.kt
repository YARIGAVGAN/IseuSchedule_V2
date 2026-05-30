package com.example.scheduleiseu.feature.navigation

import com.example.scheduleiseu.domain.core.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppNavigationConfigTest {

    @Test
    fun `student settings contain cache subgroup and notification switches`() {
        val settings = settingsFrom(
            role = UserRole.STUDENT,
            cacheCurrentAndPreviousWeek = true,
            lessonNotificationsEnabled = false,
            showMismatchedSubgroupLessons = true,
        )

        assertEquals(
            listOf(
                CACHE_CURRENT_AND_PREVIOUS_WEEK_ID,
                SHOW_MISMATCHED_SUBGROUP_LESSONS_ID,
                LESSON_NOTIFICATIONS_ID,
            ),
            settings.map { it.id },
        )
        assertEquals(listOf(true, true, false), settings.map { it.checked })
    }

    @Test
    fun `teacher settings contain notifications only`() {
        assertEquals(
            listOf(LESSON_NOTIFICATIONS_ID),
            settingsFrom(UserRole.TEACHER, true, true, true).map { it.id },
        )
    }

    @Test
    fun `bootstrap account key trims and normalizes login`() {
        assertEquals("student@example.com", " Student@Example.COM ".toStudentBootstrapAccountKey())
        assertNull("   ".toStudentBootstrapAccountKey())
    }
}
