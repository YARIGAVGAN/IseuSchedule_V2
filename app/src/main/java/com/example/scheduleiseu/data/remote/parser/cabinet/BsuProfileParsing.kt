package com.example.scheduleiseu.data.remote.parser.cabinet

import com.example.scheduleiseu.data.remote.model.CabinetMenuLink
import com.example.scheduleiseu.data.remote.model.ProfileData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

internal fun parseProfileData(
    cabinetHtml: String,
    progressHtml: String? = null,
    photoBytes: ByteArray? = null
): ProfileData {
    val cabinetDoc = Jsoup.parse(cabinetHtml, BsuCabinetConfig.cabinetUrl)
    val progressDoc = progressHtml
        ?.takeIf { it.isNotBlank() }
        ?.let { Jsoup.parse(it, BsuCabinetConfig.studProgressUrl) }

    val fullName = cabinetDoc.selectFirst(BsuCabinetConfig.primaryDisplayNameSelector)
        ?.text()
        ?.normalizeCabinetText()
        ?.takeIf { it.isNotBlank() }
        ?: cabinetDoc.selectFirst(BsuCabinetConfig.secondaryDisplayNameSelector)
            ?.text()
            ?.normalizeCabinetText()
            ?.takeIf { it.isNotBlank() }
        ?: progressDoc?.selectFirst(BsuCabinetConfig.secondaryDisplayNameSelector)
            ?.text()
            ?.normalizeCabinetText()
            ?.takeIf { it.isNotBlank() }

    val faculty = progressDoc
        ?.selectFirst(BsuCabinetConfig.facultySelector)
        ?.text()
        ?.normalizeCabinetText()
        ?.takeIf { it.isNotBlank() }

    val groupInfo = progressDoc
        ?.selectFirst(BsuCabinetConfig.groupInfoSelector)
        ?.text()
        ?.normalizeCabinetText()
        ?.takeIf { it.isNotBlank() }

    val averageScore = progressDoc
        ?.selectFirst(BsuCabinetConfig.averageScoreSelector)
        ?.text()
        ?.normalizeAverageScore()
        ?.takeIf { it.isNotBlank() }

    return ProfileData(
        fullName = fullName,
        role = if (isCabinetPage(cabinetHtml)) "Студент" else null,
        faculty = faculty,
        groupInfo = groupInfo,
        averageScore = averageScore,
        cabinetMenuLinks = parseCabinetMenuLinks(cabinetDoc),
        cabinetHtml = cabinetHtml,
        progressHtml = progressHtml,
        photoBytes = photoBytes
    )
}

internal fun parseCabinetMenuLinks(doc: Document): List<CabinetMenuLink> {
    return doc.select(BsuCabinetConfig.cabinetMenuSelector)
        .mapNotNull { link ->
            val title = link.text().normalizeCabinetText()
            if (title.isBlank()) return@mapNotNull null

            CabinetMenuLink(
                title = title,
                url = link.absUrl("href").ifBlank { link.attr("href").takeIf { it.isNotBlank() } }
            )
        }
        .distinctBy { it.title to it.url }
}

internal fun extractDisplayName(html: String): String? {
    val document = Jsoup.parse(html, BsuCabinetConfig.cabinetUrl)

    return document.selectFirst(BsuCabinetConfig.primaryDisplayNameSelector)
        ?.text()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: document.selectFirst(BsuCabinetConfig.secondaryDisplayNameSelector)
            ?.text()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
}

internal fun extractLoginError(html: String): String? {
    val document = Jsoup.parse(html, BsuCabinetConfig.loginUrl)
    return document.selectFirst(BsuCabinetConfig.loginResultSelector)
        ?.text()
        ?.replace('\u00A0', ' ')
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}

internal fun isCabinetPage(html: String): Boolean {
    val normalized = html.lowercase()
    val hasCabinetLandingLink = normalized.contains(BsuCabinetConfig.cabinetLandingPath)
    val hasCabinetTitle = html.contains(BsuCabinetConfig.cabinetTitleMarker)
    val hasDisplayName = extractDisplayName(html) != null
    val hasCabinetMenu = html.contains("ctl00_ctl00_LoginView1_LoginFIO") ||
        html.contains("ctl00_ctl00_ContentPlaceHolder0_lbFIO1") ||
        html.contains(BsuCabinetConfig.progressPath) ||
        html.contains(BsuCabinetConfig.photoPath)

    return hasCabinetLandingLink && (hasCabinetTitle || hasDisplayName || hasCabinetMenu)
}
