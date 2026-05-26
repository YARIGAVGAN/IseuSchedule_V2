package com.example.scheduleiseu.data.remote.parser.cabinet

import com.example.scheduleiseu.data.remote.model.ProgressItem
import com.example.scheduleiseu.data.remote.model.ProgressTableResult
import com.example.scheduleiseu.data.remote.model.SemesterLink
import org.jsoup.Jsoup

internal fun findAvailableSemesters(
    pageHtml: String,
    pageUrl: String = BsuCabinetConfig.studProgressUrl
): List<SemesterLink> {
    val doc = Jsoup.parse(pageHtml, pageUrl)

    val table = doc.selectFirst(BsuCabinetConfig.progressSemestersSelector)
        ?: return emptyList()

    val result = mutableListOf<SemesterLink>()
    val rows = table.select("tr")
    if (rows.isEmpty()) return emptyList()

    val courseHeaders = rows.first()
        ?.select("th")
        ?.map { it.text().trim() }

    rows.drop(1).forEach { row ->
        val cells = row.select("td")
        cells.forEachIndexed { index, cell ->
            val link = cell.selectFirst("a[href^=javascript:__doPostBack]") ?: return@forEachIndexed
            val href = link.attr("href")
            val eventTarget = extractEventTargetFromJsPostBack(href) ?: return@forEachIndexed
            val eventArgument = extractEventArgumentFromJsPostBack(href) ?: ""

            val course = courseHeaders?.getOrNull(index).orEmpty()
            val sessionName = link.text().trim()

            val title = listOf(course, sessionName)
                .filter { it.isNotBlank() }
                .joinToString(" — ")

            result += SemesterLink(
                title = title,
                eventTarget = eventTarget,
                eventArgument = eventArgument,
                isSelected = link.attr("style").contains("font-weight:bold", ignoreCase = true)
            )
        }
    }

    return result
}

internal fun parseProgressTable(
    html: String,
    pageUrl: String = BsuCabinetConfig.studProgressUrl
): ProgressTableResult {
    val doc = Jsoup.parse(html, pageUrl)

    val table = doc.selectFirst(BsuCabinetConfig.progressTableSelector)
        ?: error("Таблица успеваемости не найдена")

    val rows = table.select("tr")
    if (rows.isEmpty()) error("Таблица успеваемости пуста")

    val semesterTitle = rows.first()
        ?.selectFirst("td[colspan]")
        ?.text()
        ?.trim()
        .orEmpty()
        .ifBlank { "Неизвестный семестр" }

    val items = mutableListOf<ProgressItem>()

    rows.drop(3).forEach { row ->
        val lessonTd = row.selectFirst("td.styleLessonBody") ?: return@forEach
        val zachTd = row.selectFirst("td.styleZachBody")
        val examTd = row.selectFirst("td.styleExamBody")

        val subject = lessonTd.text().trim()
        if (subject.isBlank()) return@forEach

        val zachValue = extractCellValue(zachTd)
        val examValue = extractCellValue(examTd)

        val parsed = classifyResult(zachValue, examValue) ?: return@forEach

        items += ProgressItem(
            subject = subject,
            type = parsed.first,
            result = parsed.second
        )
    }

    return ProgressTableResult(
        semesterTitle = semesterTitle,
        items = items
    )
}

internal fun classifyResult(zachValue: String, examValue: String): Pair<String, String>? {
    val zachGrade = extractGrade(zachValue)
    val examGrade = extractGrade(examValue)

    val zachIsPass = isPass(zachValue)
    val examHasGrade = examGrade != null
    val examIsDeclared = isExam(examValue)

    return when {
        zachGrade != null -> "Диф. зачет" to zachGrade
        zachIsPass && examHasGrade -> "Диф. зачет" to examGrade
        zachIsPass && !examHasGrade -> "Зачет" to "+"
        !zachIsPass && examHasGrade -> "Экзамен" to examGrade
        !zachIsPass && examIsDeclared -> "Экзамен" to ""
        else -> null
    }
}
