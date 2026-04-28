package com.example.scheduleiseu.data.mapper.impl

import com.example.scheduleiseu.data.mapper.contract.ProgressTableResultToSemesterPerformanceMapper
import com.example.scheduleiseu.data.mapper.contract.SemesterLinkToSemesterReferenceMapper
import com.example.scheduleiseu.data.remote.model.ProgressTableResult
import com.example.scheduleiseu.data.remote.model.SemesterLink
import com.example.scheduleiseu.domain.core.model.SemesterPerformance
import com.example.scheduleiseu.domain.core.model.SemesterReference
import com.example.scheduleiseu.domain.core.model.SubjectPerformance
import java.util.Locale

class SemesterLinkToSemesterReferenceMapperImpl : SemesterLinkToSemesterReferenceMapper {
    override fun map(input: SemesterLink): SemesterReference {
        return SemesterReference(
            id = input.eventTarget,
            title = input.title.trim()
        )
    }
}

class ProgressTableResultToSemesterPerformanceMapperImpl : ProgressTableResultToSemesterPerformanceMapper {
    override fun map(input: ProgressTableResult): SemesterPerformance {
        val subjects = input.items.map { item ->
            SubjectPerformance(
                subjectName = item.subject.trim(),
                controlType = item.type.trim(),
                result = item.result.trim()
            )
        }

        return SemesterPerformance(
            semesterId = null,
            semesterTitle = input.semesterTitle.trim(),
            averageScore = calculateAverageScore(subjects),
            subjects = subjects
        )
    }

    private fun calculateAverageScore(subjects: List<SubjectPerformance>): String? {
        val grades = subjects.mapNotNull { it.result.toIntOrNull() }
        if (grades.isEmpty()) return null
        return String.format(Locale.US, "%.2f", grades.average())
    }
}
