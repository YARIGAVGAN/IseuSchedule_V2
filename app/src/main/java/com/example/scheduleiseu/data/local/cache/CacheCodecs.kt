package com.example.scheduleiseu.data.local.cache

import com.example.scheduleiseu.data.local.db.CachedPerformanceEntity
import com.example.scheduleiseu.data.local.db.CachedScheduleWeekEntity
import com.example.scheduleiseu.domain.core.model.Lesson
import com.example.scheduleiseu.domain.core.model.ScheduleDay
import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import com.example.scheduleiseu.domain.core.model.SemesterPerformance
import com.example.scheduleiseu.domain.core.model.SemesterReference
import com.example.scheduleiseu.domain.core.model.SubjectPerformance
import com.example.scheduleiseu.domain.core.model.WeekInfo
import org.json.JSONArray
import org.json.JSONObject

object ScheduleCacheCodec {
    fun toEntity(
        cacheKey: String,
        role: String,
        ownerId: String,
        week: ScheduleWeek,
        isCurrentWeek: Boolean,
        isNextWeek: Boolean,
        cachedAtMillis: Long = System.currentTimeMillis()
    ): CachedScheduleWeekEntity {
        return CachedScheduleWeekEntity(
            cacheKey = cacheKey,
            role = role,
            ownerId = ownerId,
            weekValue = week.week.value,
            weekTitle = week.week.title,
            isCurrentWeek = isCurrentWeek,
            isNextWeek = isNextWeek,
            cachedAtMillis = cachedAtMillis,
            payloadJson = week.toJson().toString()
        )
    }

    fun fromEntity(entity: CachedScheduleWeekEntity): ScheduleWeek {
        val parsed = parseWeek(JSONObject(entity.payloadJson))
        return parsed.copy(week = parsed.week.copy(isCached = true))
    }

    private fun ScheduleWeek.toJson(): JSONObject {
        return JSONObject()
            .put("week", week.toJson())
            .put("days", JSONArray().also { array -> days.forEach { array.put(it.toJson()) } })
            .putOpt("caption", caption)
            .putOpt("contextTitle", contextTitle)
            .putOpt("currentDayDate", currentDay?.date)
            .putOpt("selectedDayDate", selectedDay?.date)
    }

    private fun ScheduleDay.toJson(): JSONObject {
        return JSONObject()
            .put("title", title)
            .put("date", date)
            .put("isCurrentDay", isCurrentDay)
            .put("lessons", JSONArray().also { array -> lessons.forEach { array.put(it.toJson()) } })
    }

    private fun Lesson.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("title", title)
            .putOpt("type", type)
            .putOpt("teacherName", teacherName)
            .putOpt("classroom", classroom)
            .put("startTime", startTime)
            .putOpt("endTime", endTime)
            .putOpt("subgroup", subgroup)
            .putOpt("note", note)
            .putOpt("dayTitle", dayTitle)
            .putOpt("date", date)
            .putOpt("topic", topic)
            .putOpt("rawTimeRange", rawTimeRange)
    }

    private fun WeekInfo.toJson(): JSONObject {
        return JSONObject()
            .put("value", value)
            .put("title", title)
            .put("isCurrent", isCurrent)
            .put("isCached", isCached)
    }

    private fun parseWeek(json: JSONObject): ScheduleWeek {
        val week = parseWeekInfo(json.getJSONObject("week"))
        val days = json.getJSONArray("days").mapObjects { parseDay(it) }
        val currentDayDate = json.optStringOrNull("currentDayDate")
        val selectedDayDate = json.optStringOrNull("selectedDayDate")
        return ScheduleWeek(
            week = week,
            days = days,
            caption = json.optStringOrNull("caption"),
            contextTitle = json.optStringOrNull("contextTitle"),
            currentDay = days.firstOrNull { it.date == currentDayDate },
            selectedDay = days.firstOrNull { it.date == selectedDayDate } ?: days.firstOrNull()
        )
    }

    private fun parseDay(json: JSONObject): ScheduleDay {
        return ScheduleDay(
            title = json.optString("title"),
            date = json.optString("date"),
            lessons = json.getJSONArray("lessons").mapObjects { parseLesson(it) },
            isCurrentDay = json.optBoolean("isCurrentDay", false)
        )
    }

    private fun parseLesson(json: JSONObject): Lesson {
        return Lesson(
            id = json.optString("id"),
            title = json.optString("title"),
            type = json.optStringOrNull("type"),
            teacherName = json.optStringOrNull("teacherName"),
            classroom = json.optStringOrNull("classroom"),
            startTime = json.optString("startTime"),
            endTime = json.optStringOrNull("endTime"),
            subgroup = json.optStringOrNull("subgroup"),
            note = json.optStringOrNull("note"),
            dayTitle = json.optStringOrNull("dayTitle"),
            date = json.optStringOrNull("date"),
            topic = json.optStringOrNull("topic"),
            rawTimeRange = json.optStringOrNull("rawTimeRange")
        )
    }

    private fun parseWeekInfo(json: JSONObject): WeekInfo {
        return WeekInfo(
            value = json.optString("value"),
            title = json.optString("title"),
            isCurrent = json.optBoolean("isCurrent", false),
            isCached = json.optBoolean("isCached", false)
        )
    }
}

object PerformanceCacheCodec {
    fun toEntity(cacheKey: String, performance: SemesterPerformance): CachedPerformanceEntity {
        return CachedPerformanceEntity(
            cacheKey = cacheKey,
            semesterId = performance.semesterId,
            semesterTitle = performance.semesterTitle,
            averageScore = performance.averageScore,
            subjectsJson = JSONArray().also { array ->
                performance.subjects.forEach { subject ->
                    array.put(
                        JSONObject()
                            .put("subjectName", subject.subjectName)
                            .put("controlType", subject.controlType)
                            .put("result", subject.result)
                    )
                }
            }.toString(),
            semestersJson = JSONArray().also { array ->
                performance.availableSemesters.forEach { semester ->
                    array.put(JSONObject().put("id", semester.id).put("title", semester.title))
                }
            }.toString(),
            cachedAtMillis = System.currentTimeMillis()
        )
    }

    fun fromEntity(entity: CachedPerformanceEntity): SemesterPerformance {
        return SemesterPerformance(
            semesterId = entity.semesterId,
            semesterTitle = entity.semesterTitle,
            averageScore = entity.averageScore,
            subjects = JSONArray(entity.subjectsJson).mapObjects {
                SubjectPerformance(
                    subjectName = it.optString("subjectName"),
                    controlType = it.optString("controlType"),
                    result = it.optString("result")
                )
            },
            availableSemesters = JSONArray(entity.semestersJson).mapObjects {
                SemesterReference(
                    id = it.optString("id"),
                    title = it.optString("title")
                )
            }
        )
    }
}

private fun JSONObject.optStringOrNull(name: String): String? {
    if (!has(name) || isNull(name)) return null
    return optString(name).takeIf { it.isNotBlank() }
}

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
    return List(length()) { index -> transform(getJSONObject(index)) }
}
