package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.data.local.db.CachedScheduleWeekEntity
import com.example.scheduleiseu.domain.core.model.ScheduleContext
import com.example.scheduleiseu.domain.core.model.WeekInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object ScheduleCachePolicy {
    private val weekDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru"))

    fun resolveAllowedWeeks(
        context: ScheduleContext,
        selectedWeek: WeekInfo
    ): List<WeekInfo> {
        val current = resolveCurrentWeek(context = context, selectedWeek = selectedWeek).copy(isCurrent = true)
        val next = resolveNextWeek(context = context, selectedWeek = current)
        return listOfNotNull(current, next).distinctBy { it.value }
    }

    fun normalizeRequestedWeek(requestedWeek: WeekInfo, parsedWeek: WeekInfo): WeekInfo {
        return requestedWeek.copy(
            title = requestedWeek.title.ifBlank { parsedWeek.title },
            isCurrent = requestedWeek.isCurrent || parsedWeek.isCurrent,
            isCached = parsedWeek.isCached
        )
    }

    fun toCachedWeekInfos(entities: List<CachedScheduleWeekEntity>): List<WeekInfo> {
        return entities.map { entity ->
            WeekInfo(
                value = entity.weekValue,
                title = entity.weekTitle,
                isCurrent = entity.isCurrentWeek,
                isCached = true
            )
        }.distinctBy { it.value }
    }

    private fun resolveCurrentWeek(context: ScheduleContext, selectedWeek: WeekInfo): WeekInfo {
        return context.currentWeek
            ?: context.weeks.firstOrNull { it.isCurrent }
            ?: context.weeks.firstOrNull { it.value == selectedWeek.value }?.copy(isCurrent = selectedWeek.isCurrent)
            ?: selectedWeek
    }

    private fun resolveNextWeek(context: ScheduleContext, selectedWeek: WeekInfo): WeekInfo? {
        return resolveCalendarNextWeek(
            weeks = context.weeks,
            baseWeek = selectedWeek
        )
    }

    private fun resolveCalendarNextWeek(
        weeks: List<WeekInfo>,
        baseWeek: WeekInfo
    ): WeekInfo? {
        val baseDate = baseWeek.toStartDateOrNull()
        val datedWeeks = weeks.mapNotNull { week ->
            week.toStartDateOrNull()?.let { date -> week to date }
        }

        if (baseDate != null && datedWeeks.isNotEmpty()) {
            val exactDate = baseDate.plusDays(7)
            datedWeeks.firstOrNull { it.second == exactDate }?.let { return it.first }

            return datedWeeks
                .filter { it.second.isAfter(baseDate) }
                .minByOrNull { it.second }
                ?.first
        }

        val rawIndex = weeks.indexOfFirst { it.value == baseWeek.value }
        if (rawIndex < 0) return null
        return weeks.getOrNull(rawIndex - 1)
    }

    private fun WeekInfo.toStartDateOrNull(): LocalDate? {
        val rawDate = Regex("\\d{2}\\.\\d{2}\\.\\d{4}").find(value)?.value
            ?: Regex("\\d{2}\\.\\d{2}\\.\\d{4}").find(title)?.value
            ?: return null
        return runCatching { LocalDate.parse(rawDate, weekDateFormatter) }.getOrNull()
    }
}
