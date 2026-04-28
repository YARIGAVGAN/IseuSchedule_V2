package com.example.scheduleiseu.data.mapper.impl

import com.example.scheduleiseu.data.mapper.contract.CurrentWeekInfoToWeekInfoMapper
import com.example.scheduleiseu.data.mapper.contract.StudentTimeTableDataToScheduleContextMapper
import com.example.scheduleiseu.data.mapper.contract.StudentTimeTableDataToScheduleWeekMapper
import com.example.scheduleiseu.data.mapper.contract.TeacherTimeTableDataToScheduleContextMapper
import com.example.scheduleiseu.data.mapper.contract.TeacherTimeTableDataToScheduleWeekMapper
import com.example.scheduleiseu.data.remote.model.CurrentWeekInfo
import com.example.scheduleiseu.data.remote.model.SelectOption
import com.example.scheduleiseu.data.remote.model.TeacherTimeTableData
import com.example.scheduleiseu.data.remote.model.TimeTableData
import com.example.scheduleiseu.data.remote.model.WeeklyTimetableDayData
import com.example.scheduleiseu.data.remote.model.WeeklyTimetableLessonData
import com.example.scheduleiseu.data.remote.model.WeeklyTimetableTableData
import com.example.scheduleiseu.data.remote.parser.WeeklyTimetableHtmlParser
import com.example.scheduleiseu.domain.core.model.Lesson
import com.example.scheduleiseu.domain.core.model.ScheduleContext
import com.example.scheduleiseu.domain.core.model.ScheduleDay
import com.example.scheduleiseu.domain.core.model.ScheduleOption
import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.domain.core.model.WeekInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class CurrentWeekInfoToWeekInfoMapperImpl : CurrentWeekInfoToWeekInfoMapper {
    override fun map(input: CurrentWeekInfo): WeekInfo {
        return WeekInfo(
            value = input.value,
            title = input.text,
            isCurrent = true
        )
    }
}

class StudentTimeTableDataToScheduleContextMapperImpl(
    private val currentWeekMapper: CurrentWeekInfoToWeekInfoMapper = CurrentWeekInfoToWeekInfoMapperImpl()
) : StudentTimeTableDataToScheduleContextMapper {
    override fun map(input: TimeTableData): ScheduleContext {
        val selectedWeekValue = input.selectedWeek?.value
        val currentWeekValue = input.currentWeek?.value
        val weeks = input.weeks.map { option ->
            WeekInfo(
                value = option.value,
                title = option.text,
                isCurrent = option.value == currentWeekValue
            )
        }

        return ScheduleContext(
            userRole = UserRole.STUDENT,
            faculties = input.faculties.map { it.toDomainOption() },
            departments = input.departments.map { it.toDomainOption() },
            courses = input.courses.map { it.toDomainOption() },
            groups = input.groups.map { it.toDomainOption() },
            weeks = weeks,
            selectedFacultyId = input.faculties.firstOrNull { it.isSelected }?.value,
            selectedDepartmentId = input.departments.firstOrNull { it.isSelected }?.value,
            selectedCourseId = input.courses.firstOrNull { it.isSelected }?.value,
            selectedGroupId = input.groups.firstOrNull { it.isSelected }?.value,
            selectedWeek = input.selectedWeek?.let {
                WeekInfo(
                    value = it.value,
                    title = it.text,
                    isCurrent = it.value == currentWeekValue
                )
            } ?: weeks.firstOrNull { it.value == selectedWeekValue },
            currentWeek = input.currentWeek?.let(currentWeekMapper::map)
        )
    }
}

class TeacherTimeTableDataToScheduleContextMapperImpl(
    private val currentWeekMapper: CurrentWeekInfoToWeekInfoMapper = CurrentWeekInfoToWeekInfoMapperImpl()
) : TeacherTimeTableDataToScheduleContextMapper {
    override fun map(input: TeacherTimeTableData): ScheduleContext {
        val selectedWeekValue = input.selectedWeek?.value
        val currentWeekValue = input.currentWeek?.value
        val weeks = input.weeks.map { option ->
            WeekInfo(
                value = option.value,
                title = option.text,
                isCurrent = option.value == currentWeekValue
            )
        }

        return ScheduleContext(
            userRole = UserRole.TEACHER,
            teachers = input.teachers.filter { it.isRealTeacherScheduleOption() }.map { it.toDomainOption() },
            weeks = weeks,
            selectedTeacherId = input.teachers.firstOrNull { it.isSelected }?.value,
            selectedWeek = input.selectedWeek?.let {
                WeekInfo(
                    value = it.value,
                    title = it.text,
                    isCurrent = it.value == currentWeekValue
                )
            } ?: weeks.firstOrNull { it.value == selectedWeekValue },
            currentWeek = input.currentWeek?.let(currentWeekMapper::map)
        )
    }
}

class StudentTimeTableDataToScheduleWeekMapperImpl(
    private val htmlParser: WeeklyTimetableHtmlParser = WeeklyTimetableHtmlParser()
) : StudentTimeTableDataToScheduleWeekMapper {
    override fun map(input: TimeTableData): ScheduleWeek {
        val current = input.currentWeek?.let { WeekInfo(it.value, it.text, true) }
        val selected = input.selectedWeek?.let {
            WeekInfo(
                value = it.value,
                title = it.text,
                isCurrent = it.value == current?.value
            )
        }
        return buildScheduleWeek(
            tableData = htmlParser.parse(input.timeTableHtml),
            selectedWeek = selected,
            currentWeek = current
        )
    }
}

class TeacherTimeTableDataToScheduleWeekMapperImpl(
    private val htmlParser: WeeklyTimetableHtmlParser = WeeklyTimetableHtmlParser()
) : TeacherTimeTableDataToScheduleWeekMapper {
    override fun map(input: TeacherTimeTableData): ScheduleWeek {
        val current = input.currentWeek?.let { WeekInfo(it.value, it.text, true) }
        val selected = input.selectedWeek?.let {
            WeekInfo(
                value = it.value,
                title = it.text,
                isCurrent = it.value == current?.value
            )
        }
        return buildScheduleWeek(
            tableData = htmlParser.parse(input.timeTableHtml),
            selectedWeek = selected,
            currentWeek = current
        )
    }
}

private fun buildScheduleWeek(
    tableData: WeeklyTimetableTableData,
    selectedWeek: WeekInfo?,
    currentWeek: WeekInfo?
): ScheduleWeek {
    val resolvedWeek = selectedWeek
        ?: currentWeek
        ?: WeekInfo(
            value = tableData.weekStartDate.orEmpty(),
            title = tableData.weekStartDate ?: "Неделя",
            isCurrent = tableData.weekStartDate?.isInsideCurrentCalendarWeek() == true
        )

    val days = tableData.days.map { it.toDomainDay() }
    val resolvedCurrentDay = days.firstOrNull { it.isCurrentDay }
    val resolvedSelectedDay = if (resolvedWeek.isCurrent) {
        resolvedCurrentDay ?: days.firstOrNull()
    } else {
        days.firstOrNull()
    }

    return ScheduleWeek(
        week = resolvedWeek,
        days = days,
        caption = tableData.caption,
        contextTitle = tableData.contextDescription,
        currentDay = if (resolvedWeek.isCurrent) resolvedCurrentDay else null,
        selectedDay = resolvedSelectedDay
    )
}

private fun WeeklyTimetableDayData.toDomainDay(): ScheduleDay {
    return ScheduleDay(
        title = dayName,
        date = date,
        lessons = lessons.mapIndexed { index, lesson -> lesson.toDomainLesson(dayName, date, index) },
        isCurrentDay = isCurrentDay || date.toLocalDateOrNull() == LocalDate.now()
    )
}

private fun WeeklyTimetableLessonData.toDomainLesson(dayName: String, date: String, index: Int): Lesson {
    val (startTime, endTime) = splitTimeRange(timeRange)

    return Lesson(
        id = buildString {
            append(date)
            append('_')
            append(startTime ?: timeRange.ifBlank { "row" })
            append('_')
            append(index)
        },
        title = subject.ifBlank { disciplineRaw },
        type = lessonType,
        teacherName = staff,
        classroom = auditory,
        startTime = startTime ?: "",
        endTime = endTime,
        subgroup = subgroup,
        note = null,
        dayTitle = dayName,
        date = date,
        topic = topic,
        rawTimeRange = timeRange
    )
}

private fun splitTimeRange(value: String): Pair<String?, String?> {
    val normalized = value
        .replace('–', '-')
        .replace('—', '-')
        .replace('\u00A0', ' ')
        .trim()

    if (normalized.isBlank()) return null to null

    val parts = normalized
        .split('-', limit = 2)
        .map { it.trim() }

    if (parts.size != 2) return normalized to null

    return parts[0].ifBlank { null } to parts[1].ifBlank { null }
}

private fun SelectOption.isRealTeacherScheduleOption(): Boolean {
    val normalizedText = text.trim().lowercase()
    return value.isNotBlank() &&
        normalizedText.isNotBlank() &&
        normalizedText != "выберите фамилию преподавателя" &&
        !normalizedText.startsWith("выберите ")
}

private fun SelectOption.toDomainOption(): ScheduleOption {
    return ScheduleOption(
        id = value,
        title = text,
        selected = isSelected
    )
}

private fun String.toLocalDateOrNull(): LocalDate? {
    val normalized = trim().substringBefore(' ')
    return runCatching { LocalDate.parse(normalized, DATE_FORMATTER) }.getOrNull()
}

private fun String.isInsideCurrentCalendarWeek(): Boolean {
    val start = toLocalDateOrNull() ?: return false
    val today = LocalDate.now()
    return !today.isBefore(start) && !today.isAfter(start.plusDays(6))
}

private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru"))
