package com.example.scheduleiseu.domain.core.usecase

object ScheduleLessonFilterRules {
    val explicitSubgroupPatterns = listOf(
        Regex("""(?i)([12])\s*(?:п\s*/\s*гр|п\.?\s*гр|подгр\.?|подгруппа)"""),
        Regex("""(?i)(?:п\s*/\s*гр|п\.?\s*гр|подгр\.?|подгруппа)\s*([12])""")
    )

    fun normalizeSubgroup(rawValue: String?): String? {
        val normalized = rawValue
            ?.replace('\u00A0', ' ')
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()

        if (normalized.isBlank()) return null
        if (normalized == "1" || normalized == "2") return normalized

        explicitSubgroupPatterns.forEach { pattern ->
            val match = pattern.matchEntire(normalized) ?: return@forEach
            return match.groupValues.getOrNull(1)?.takeIf { it == "1" || it == "2" }
        }

        return null
    }
}
