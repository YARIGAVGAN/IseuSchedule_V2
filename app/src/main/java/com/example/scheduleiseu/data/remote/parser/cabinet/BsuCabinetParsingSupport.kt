package com.example.scheduleiseu.data.remote.parser.cabinet

import org.jsoup.nodes.Element

internal fun String.normalizeCabinetText(): String {
    return replace('\u00A0', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun String.normalizeAverageScore(): String {
    return normalizeCabinetText()
        .removePrefix("средний балл:")
        .removePrefix("Средний балл:")
        .trim()
}

internal fun extractEventTargetFromJsPostBack(href: String): String? {
    val match = BsuCabinetConfig.jsPostBackRegex.find(href) ?: return null
    return match.groupValues[1]
}

internal fun extractEventArgumentFromJsPostBack(href: String): String? {
    val match = BsuCabinetConfig.jsPostBackRegex.find(href) ?: return null
    return match.groupValues[2]
}

internal fun extractCellValue(td: Element?): String {
    if (td == null) return ""

    val title = td.attr("title").trim()
    val text = td.text()
        .replace('\u00A0', ' ')
        .trim()

    return when {
        text.isNotBlank() && text != " " -> text
        title.isNotBlank() -> title
        else -> ""
    }
}

internal fun extractGrade(value: String): String? {
    val cleaned = value.trim()
    val match = BsuCabinetConfig.gradeRegex.find(cleaned)
    return match?.groupValues?.get(1)
}

internal fun isPass(value: String): Boolean {
    val normalized = value.trim().lowercase()
    return normalized == "+" ||
        normalized.contains("зачтено") ||
        normalized == "зачет"
}

internal fun isExam(value: String): Boolean {
    val normalized = value.trim().lowercase()
    return normalized == "экзамен" ||
        normalized == "экз." ||
        normalized == "экз" ||
        normalized.contains("экзам")
}
