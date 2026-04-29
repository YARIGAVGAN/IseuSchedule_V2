package com.example.scheduleiseu.data.remote.parser

import com.example.scheduleiseu.data.remote.model.WeeklyTimetableDayData
import com.example.scheduleiseu.data.remote.model.WeeklyTimetableLessonData
import com.example.scheduleiseu.data.remote.model.WeeklyTimetableTableData
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class WeeklyTimetableHtmlParser {

    private enum class TimetableKind {
        STUDENT,
        TEACHER
    }

    fun parse(tableHtml: String): WeeklyTimetableTableData {
        if (tableHtml.isBlank()) {
            return emptyTable()
        }

        val document = Jsoup.parse(tableHtml)
        val table = document.selectFirst("#TT") ?: return emptyTable()
        val timetableKind = detectTimetableKind(table)

        val captionElement = table.selectFirst("caption")
        val captionText = captionElement?.text()?.normalizeWhitespace().orEmpty()
        val captionWholeText = captionElement?.wholeText()?.normalizeLineBreaks().orEmpty()

        val (weekStartDate, contextDescription) = parseCaption(captionText, captionWholeText)
        val rows = table.select("tr")

        val days = mutableListOf<WeeklyTimetableDayData>()
        var currentDayName: String? = null
        var currentDate: String? = null
        var currentIsToday = false
        var currentLessons = mutableListOf<WeeklyTimetableLessonData>()

        fun flushCurrentDay() {
            val dayName = currentDayName
            val date = currentDate
            if (dayName != null && date != null) {
                days += WeeklyTimetableDayData(
                    dayName = dayName,
                    date = date,
                    isCurrentDay = currentIsToday,
                    lessons = currentLessons.toList()
                )
            }
        }

        rows.forEach { row ->
            val rowClasses = row.classNames()

            if ("row-separator" in rowClasses || "row-header" in rowClasses) {
                return@forEach
            }

            val dateCell = row.selectFirst("td.cell-date")
            if (dateCell != null) {
                flushCurrentDay()
                currentLessons = mutableListOf()
                currentDayName = dateCell.extractDayName()
                currentDate = dateCell.extractDate()
                currentIsToday = dateCell.hasClass("today-date")
            }

            if (currentDayName == null || currentDate == null) return@forEach
            if ("row-empty" in rowClasses) return@forEach

            val time = row.extractTimeRange()

            val subgroup = row.selectFirst("td.cell-subgroup")
                ?.text()
                ?.normalizeNullable()

            val disciplineCell = row.selectFirst("td.cell-discipline")

            val staff = row.selectFirst("td.cell-staff")
                ?.text()
                ?.normalizeNullable()

            val auditory = row.selectFirst("td.cell-auditory")
                ?.text()
                ?.normalizeNullable()

            if (
                time.isBlank() &&
                disciplineCell == null &&
                subgroup.isNullOrBlank() &&
                staff.isNullOrBlank() &&
                auditory.isNullOrBlank()
            ) {
                return@forEach
            }

            currentLessons += parseLesson(
                kind = timetableKind,
                timeRange = time,
                subgroup = subgroup,
                disciplineCell = disciplineCell,
                staff = staff,
                auditory = auditory
            )
        }

        flushCurrentDay()

        return WeeklyTimetableTableData(
            caption = captionText,
            weekStartDate = weekStartDate,
            contextDescription = contextDescription,
            days = days
        )
    }

    private fun emptyTable(): WeeklyTimetableTableData {
        return WeeklyTimetableTableData(
            caption = "",
            weekStartDate = null,
            contextDescription = null,
            days = emptyList()
        )
    }

    private fun parseCaption(captionText: String, captionWholeText: String): Pair<String?, String?> {
        if (captionText.isBlank() && captionWholeText.isBlank()) return null to null

        val source = if (captionWholeText.isNotBlank()) captionWholeText else captionText

        val weekStartDate = Regex("""на неделю с\s+(\d{2}\.\d{2}\.\d{4})""")
            .find(source)
            ?.groupValues
            ?.getOrNull(1)

        val lines = source
            .lines()
            .map { it.normalizeWhitespace() }
            .filter { it.isNotBlank() }

        val contextDescription = when {
            lines.size >= 2 -> lines.drop(1).joinToString(" ").normalizeNullable()
            weekStartDate != null -> source.substringAfter(weekStartDate, "")
                .trim(' ', ',', '\n')
                .normalizeNullable()
            else -> null
        }

        return weekStartDate to contextDescription
    }

    private fun parseLesson(
        kind: TimetableKind,
        timeRange: String,
        subgroup: String?,
        disciplineCell: Element?,
        staff: String?,
        auditory: String?
    ): WeeklyTimetableLessonData {
        val rawTopic = disciplineCell
            ?.selectFirst("span.topic")
            ?.text()
            ?.normalizeNullable()

        val topicSubgroup = extractExplicitSubgroup(rawTopic)

        val topicFromDiscipline = rawTopic
            ?.takeUnless { it.isExplicitSubgroupOnly() }
            ?.normalizeNullable()

        val disciplineRaw = disciplineCell
            ?.clone()
            ?.apply { select("span.topic").remove() }
            ?.text()
            ?.normalizeWhitespace()
            .orEmpty()

        val parsedStudentDiscipline = splitLessonTypeAndSubject(disciplineRaw)
        val studentLessonType = parsedStudentDiscipline.first
        val studentSubjectWithoutPrefix = parsedStudentDiscipline.second
        val studentSubjectSubgroup = extractExplicitSubgroup(studentSubjectWithoutPrefix)
        val studentSubject = studentSubjectWithoutPrefix.removeExplicitSubgroupMarkers()

        val teacherSubject = disciplineRaw

        val resolvedSubgroup = when (kind) {
            TimetableKind.TEACHER -> {

                subgroup
            }

            TimetableKind.STUDENT -> {
                val cellSubgroup = extractExplicitSubgroup(subgroup) ?: subgroup
                val staffSubgroup = extractExplicitSubgroup(staff)

                cellSubgroup ?: topicSubgroup ?: staffSubgroup ?: studentSubjectSubgroup
            }
        }

        val resolvedTopic = when (kind) {
            TimetableKind.TEACHER -> {

                staff ?: topicFromDiscipline
            }

            TimetableKind.STUDENT -> {
                topicFromDiscipline
            }
        }

        val resolvedStaff = when (kind) {
            TimetableKind.TEACHER -> null

            TimetableKind.STUDENT -> {

                staff
                    ?.takeUnless { it.isExplicitSubgroupOnly() }
                    ?.normalizeNullable()
            }
        }

        val resolvedLessonType = when (kind) {
            TimetableKind.TEACHER -> null
            TimetableKind.STUDENT -> studentLessonType
        }

        val resolvedSubject = when (kind) {
            TimetableKind.TEACHER -> teacherSubject
            TimetableKind.STUDENT -> studentSubject
        }

        return WeeklyTimetableLessonData(
            timeRange = timeRange,
            subgroup = resolvedSubgroup,
            disciplineRaw = disciplineRaw,
            subject = resolvedSubject,
            lessonType = resolvedLessonType,
            topic = resolvedTopic,
            staff = resolvedStaff,
            auditory = auditory
        )
    }

    private fun splitLessonTypeAndSubject(raw: String): Pair<String?, String> {
        val value = raw.normalizeWhitespace()
            .trim('"')
            .trim()

        if (value.isBlank()) return null to ""

        lessonTypePatterns.firstNotNullOfOrNull { pattern ->
            val match = pattern.find(value) ?: return@firstNotNullOfOrNull null
            val matchedPrefix = match.value.normalizeWhitespace().trim()
            val subject = value
                .removeRange(match.range)
                .trim()
                .trim('"')
                .trim()

            matchedPrefix to subject
        }?.let { return it }

        return null to value
    }

    private fun detectTimetableKind(table: Element): TimetableKind {
        val staffHeader = table
            .selectFirst("tr.row-header td.cell-staff")
            ?.text()
            ?.normalizeWhitespace()
            ?.lowercase()
            .orEmpty()

        return if ("тема" in staffHeader) {
            TimetableKind.TEACHER
        } else {
            TimetableKind.STUDENT
        }
    }

    private fun extractExplicitSubgroup(value: String?): String? {
        val normalized = value?.normalizeWhitespace().orEmpty()
        if (normalized.isBlank()) return null

        explicitSubgroupPatterns.forEach { pattern ->
            val match = pattern.find(normalized) ?: return@forEach
            return match.groupValues.getOrNull(1)
                ?.trim()
                ?.takeIf { it == "1" || it == "2" }
        }

        return null
    }

    private fun String.isExplicitSubgroupOnly(): Boolean {
        val normalized = normalizeWhitespace()
        if (normalized.isBlank()) return false

        return explicitSubgroupPatterns.any { pattern ->
            pattern.matchEntire(normalized) != null
        }
    }

    private fun String.removeExplicitSubgroupMarkers(): String {
        var value = this
        explicitSubgroupPatterns.forEach { pattern ->
            value = value.replace(pattern, " ")
        }

        return value
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim(',', ';', '-', '(', ')')
            .trim()
    }

    private fun Element.extractTimeRange(): String {
        selectFirst("td.cell-time")
            ?.text()
            ?.normalizeWhitespace()
            ?.takeIf { it.isTimeRangeLike() }
            ?.let { return it }

        val cells = select("td")

        val candidateCells = cells.filterNot { cell ->
            cell.hasClass("cell-date") ||
                    cell.hasClass("cell-subgroup") ||
                    cell.hasClass("cell-discipline") ||
                    cell.hasClass("cell-staff") ||
                    cell.hasClass("cell-auditory") ||
                    cell.hasClass("cell-empty") ||
                    cell.hasClass("cell-header")
        }

        candidateCells.firstOrNull { cell ->
            cell.text().normalizeWhitespace().isTimeRangeLike()
        }?.let {
            return it.text().normalizeWhitespace()
        }

        cells.firstOrNull { cell ->
            cell.attr("align").equals("center", ignoreCase = true) &&
                    cell.text().normalizeWhitespace().isTimeRangeLike()
        }?.let {
            return it.text().normalizeWhitespace()
        }

        return ""
    }

    private fun Element.extractDayName(): String =
        selectFirst("span.day")?.text()?.normalizeWhitespace().orEmpty()

    private fun Element.extractDate(): String =
        selectFirst("span.date")?.text()?.normalizeWhitespace().orEmpty()

    private fun String.isTimeRangeLike(): Boolean {
        val normalized = replace('\u00A0', ' ')
            .replace('–', '-')
            .replace('—', '-')
            .trim()

        return Regex("""^\d{1,2}[:.]\d{2}\s*-\s*\d{1,2}[:.]\d{2}$""")
            .matches(normalized)
    }

    private fun String.normalizeWhitespace(): String =
        replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun String.normalizeLineBreaks(): String =
        replace("\r\n", "\n")
            .replace('\r', '\n')

    private fun String.normalizeNullable(): String? =
        normalizeWhitespace().ifBlank { null }

    private companion object {
        val lessonTypePatterns = listOf(
            Regex("""(?i)^\s*практ\.?\s*зан\.?\s*"""),
            Regex("""(?i)^\s*лаб\.?\s*"""),
            Regex("""(?i)^\s*лек\.?\s*"""),
            Regex("""(?i)^\s*сем\.?\s*"""),
            Regex("""(?i)^\s*конс\.?\s*"""),
            Regex("""(?i)^\s*зач\.?\s*"""),
            Regex("""(?i)^\s*экз\.?\s*""")
        )
        val explicitSubgroupPatterns = listOf(
            Regex("""(?i)(?<![\p{L}\p{N}])([12])\s*(?:п\s*/\s*гр|п\.?\s*гр|подгр\.?|подгруппа)(?![\p{L}\p{N}])"""),
            Regex("""(?i)(?<![\p{L}\p{N}])(?:п\s*/\s*гр|п\.?\s*гр|подгр\.?|подгруппа)\s*([12])(?![\p{L}\p{N}])""")
        )
    }
}
