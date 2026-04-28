package com.example.scheduleiseu.data.remote.parser

import com.example.scheduleiseu.data.remote.cookie.MemoryCookieJar
import com.example.scheduleiseu.data.remote.model.CabinetMenuLink
import com.example.scheduleiseu.data.remote.model.LoginPageData
import com.example.scheduleiseu.data.remote.model.LoginResult
import com.example.scheduleiseu.data.remote.model.ProfileData
import com.example.scheduleiseu.data.remote.model.ProgressItem
import com.example.scheduleiseu.data.remote.model.ProgressTableResult
import com.example.scheduleiseu.data.remote.model.SemesterLink
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class BsuParser(
    private val cookieJar: MemoryCookieJar = MemoryCookieJar(),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    companion object {
        const val LOGIN_URL = "https://student.bsu.by/login?ReturnUrl=%2fPersonalCabinet%2fNews"
        const val STUD_PROGRESS_URL = "https://student.bsu.by/PersonalCabinet/StudProgress"
        const val CABINET_URL = "https://student.bsu.by/PersonalCabinet/News"
        const val PHOTO_URL = "https://student.bsu.by/Photo/Photo.aspx"
    }

    fun loadLoginPage(): LoginPageData {
        val request = Request.Builder()
            .url(LOGIN_URL)
            .get()
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Не удалось загрузить страницу логина: HTTP ${response.code}")
            }

            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html, LOGIN_URL)

            val viewState = doc.selectFirst("#__VIEWSTATE")?.attr("value").orEmpty()
            val eventValidation = doc.selectFirst("#__EVENTVALIDATION")?.attr("value").orEmpty()
            val viewStateGenerator = doc.selectFirst("#__VIEWSTATEGENERATOR")?.attr("value").orEmpty()
            val formAction = doc.selectFirst("form#aspnetForm")?.absUrl("action").orEmpty()
            val captchaUrl = doc.selectFirst("img[src*=CaptchaImage.aspx]")?.absUrl("src").orEmpty()

            if (viewState.isBlank()) error("Не найден __VIEWSTATE")
            if (eventValidation.isBlank()) error("Не найден __EVENTVALIDATION")
            if (viewStateGenerator.isBlank()) error("Не найден __VIEWSTATEGENERATOR")
            if (captchaUrl.isBlank()) error("Не найдена captcha")
            if (formAction.isBlank()) error("Не найден action формы")

            return LoginPageData(
                viewState = viewState,
                eventValidation = eventValidation,
                viewStateGenerator = viewStateGenerator,
                captchaUrl = captchaUrl,
                loginUrl = formAction
            )
        }
    }

    fun loadCaptchaImage(loginPageData: LoginPageData): ByteArray {
        val request = Request.Builder()
            .url(loginPageData.captchaUrl)
            .get()
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Не удалось загрузить captcha: HTTP ${response.code}")
            }

            return response.body?.bytes() ?: error("Пустой ответ captcha")
        }
    }

    fun login(
        loginPageData: LoginPageData,
        username: String,
        password: String,
        captcha: String
    ): LoginResult {
        val body = FormBody.Builder()
            .add("__EVENTTARGET", "")
            .add("__EVENTARGUMENT", "")
            .add("__VIEWSTATE", loginPageData.viewState)
            .add("__VIEWSTATEGENERATOR", loginPageData.viewStateGenerator)
            .add("__EVENTVALIDATION", loginPageData.eventValidation)
            .add("ctl00\$ContentPlaceHolder0\$txtUserLogin", username)
            .add("ctl00\$ContentPlaceHolder0\$txtUserPassword", password)
            .add("ctl00\$ContentPlaceHolder0\$txtCapture", captcha)
            .add("ctl00\$ContentPlaceHolder0\$btnLogon", "Войти")
            .build()

        val request = Request.Builder()
            .url(loginPageData.loginUrl)
            .post(body)
            .header("User-Agent", "Mozilla/5.0")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        client.newCall(request).execute().use { response ->
            val html = response.body?.string().orEmpty()

            val looksLikeLoginPage =
                html.contains("Вход в личный кабинет студента", ignoreCase = true) &&
                    (
                        html.contains("ctl00\$ContentPlaceHolder0\$txtUserLogin") ||
                            html.contains("ctl00_ContentPlaceHolder0_txtUserLogin") ||
                            html.contains("CaptchaImage.aspx")
                        )

            val looksLikeCabinet = isCabinetPage(html)

            val success = !looksLikeLoginPage || looksLikeCabinet

            return LoginResult(
                success = success,
                html = html
            )
        }
    }

    fun isCabinetPage(html: String): Boolean {
        val normalized = html.lowercase()
        val hasCabinetLandingLink = normalized.contains("/personalcabinet/news")
        val hasCabinetTitle = html.contains("Личный кабинет студента БГУ")
        val hasDisplayName = extractDisplayName(html) != null
        val hasCabinetMenu = html.contains("ctl00_ctl00_LoginView1_LoginFIO") ||
            html.contains("ctl00_ctl00_ContentPlaceHolder0_lbFIO1") ||
            html.contains("/PersonalCabinet/StudProgress") ||
            html.contains("/PersonalCabinet/stbd")

        return hasCabinetLandingLink && (hasCabinetTitle || hasDisplayName || hasCabinetMenu)
    }

    fun extractDisplayName(html: String): String? {
        val document = Jsoup.parse(html, CABINET_URL)

        return document.selectFirst("#ctl00_ctl00_LoginView1_LoginFIO")
            ?.text()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: document.selectFirst("#ctl00_ctl00_ContentPlaceHolder0_lbFIO1")
                ?.text()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
    }

    fun extractLoginError(html: String): String? {
        val document = Jsoup.parse(html, LOGIN_URL)
        return document.selectFirst("#ctl00_ContentPlaceHolder0_lbLoginResult")
            ?.text()
            ?.replace('\u00A0', ' ')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun getPage(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("GET failed: HTTP ${response.code}")
            }
            return response.body?.string().orEmpty()
        }
    }

    fun doPostBack(
        pageUrl: String,
        pageHtml: String,
        eventTarget: String,
        eventArgument: String = ""
    ): String {
        val doc = Jsoup.parse(pageHtml, pageUrl)

        val viewState = doc.selectFirst("#__VIEWSTATE")?.attr("value").orEmpty()
        val eventValidation = doc.selectFirst("#__EVENTVALIDATION")?.attr("value").orEmpty()
        val viewStateGenerator = doc.selectFirst("#__VIEWSTATEGENERATOR")?.attr("value").orEmpty()

        if (viewState.isBlank()) error("Не найден __VIEWSTATE")
        if (eventValidation.isBlank()) error("Не найден __EVENTVALIDATION")
        if (viewStateGenerator.isBlank()) error("Не найден __VIEWSTATEGENERATOR")

        val bodyBuilder = FormBody.Builder()
            .add("__EVENTTARGET", eventTarget)
            .add("__EVENTARGUMENT", eventArgument)
            .add("__VIEWSTATE", viewState)
            .add("__EVENTVALIDATION", eventValidation)
            .add("__VIEWSTATEGENERATOR", viewStateGenerator)

        doc.select("input[name]").forEach { input ->
            val name = input.attr("name")
            val type = input.attr("type").lowercase()
            val value = input.attr("value")

            if (name.isBlank()) return@forEach
            if (type == "submit" || type == "button" || type == "image") return@forEach

            if (
                name != "__EVENTTARGET" &&
                name != "__EVENTARGUMENT" &&
                name != "__VIEWSTATE" &&
                name != "__EVENTVALIDATION" &&
                name != "__VIEWSTATEGENERATOR"
            ) {
                bodyBuilder.add(name, value)
            }
        }

        val request = Request.Builder()
            .url(pageUrl)
            .post(bodyBuilder.build())
            .header("User-Agent", "Mozilla/5.0")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("POSTBACK failed: HTTP ${response.code}")
            }
            return response.body?.string().orEmpty()
        }
    }

    fun logoutFromCabinet(): Boolean {
        val pageHtml = runCatching { getPage(CABINET_URL) }.getOrElse { return false }
        val doc = Jsoup.parse(pageHtml, CABINET_URL)
        val logoutLink = doc.select("a[href^=javascript:__doPostBack]")
            .firstOrNull { link ->
                val text = link.text().trim().lowercase()
                val id = link.id().lowercase()
                text == "выход" || id.contains("loginstatus") || id.endsWith("ggg")
            } ?: return false

        val href = logoutLink.attr("href")
        val eventTarget = extractEventTargetFromJsPostBack(href) ?: return false
        val eventArgument = extractEventArgumentFromJsPostBack(href) ?: ""

        val resultHtml = doPostBack(
            pageUrl = CABINET_URL,
            pageHtml = pageHtml,
            eventTarget = eventTarget,
            eventArgument = eventArgument
        )

        return !isCabinetPage(resultHtml)
    }

    fun getAvailableSemesters(): List<SemesterLink> {
        val html = getPage(STUD_PROGRESS_URL)
        return findAvailableSemesters(html, STUD_PROGRESS_URL)
    }
    fun findAvailableSemesters(
        pageHtml: String,
        pageUrl: String = STUD_PROGRESS_URL
    ): List<SemesterLink> {
        val doc = Jsoup.parse(pageHtml, pageUrl)

        val table = doc.selectFirst("#ctl00_ctl00_ContentPlaceHolder0_ContentPlaceHolder1_ctlStudProgress1_tblSemester")
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

    private fun extractEventTargetFromJsPostBack(href: String): String? {
        val regex = Regex("""__doPostBack\('([^']*)','([^']*)'\)""")
        val match = regex.find(href) ?: return null
        return match.groupValues[1]
    }

    private fun extractEventArgumentFromJsPostBack(href: String): String? {
        val regex = Regex("""__doPostBack\('([^']*)','([^']*)'\)""")
        val match = regex.find(href) ?: return null
        return match.groupValues[2]
    }

    fun openSemester(semester: SemesterLink): String {
        val studProgressHtml = getPage(STUD_PROGRESS_URL)
        return doPostBack(
            pageUrl = STUD_PROGRESS_URL,
            pageHtml = studProgressHtml,
            eventTarget = semester.eventTarget,
            eventArgument = semester.eventArgument
        )
    }

    fun openLatestSemester(): Pair<SemesterLink, String> {
        val studProgressHtml = getPage(STUD_PROGRESS_URL)
        val semesters = findAvailableSemesters(studProgressHtml, STUD_PROGRESS_URL)

        val latest = semesters.firstOrNull { it.isSelected }
            ?: semesters.lastOrNull()
            ?: error("Семестры не найдены на странице успеваемости")

        val html = doPostBack(
            pageUrl = STUD_PROGRESS_URL,
            pageHtml = studProgressHtml,
            eventTarget = latest.eventTarget,
            eventArgument = latest.eventArgument
        )

        return latest to html
    }

    fun parseProfileData(
        cabinetHtml: String,
        progressHtml: String? = null,
        photoBytes: ByteArray? = null
    ): ProfileData {
        val cabinetDoc = Jsoup.parse(cabinetHtml, CABINET_URL)
        val progressDoc = progressHtml
            ?.takeIf { it.isNotBlank() }
            ?.let { Jsoup.parse(it, STUD_PROGRESS_URL) }

        val fullName = cabinetDoc.selectFirst("#ctl00_ctl00_LoginView1_LoginFIO")
            ?.text()
            ?.normalizeCabinetText()
            ?.takeIf { it.isNotBlank() }
            ?: cabinetDoc.selectFirst("#ctl00_ctl00_ContentPlaceHolder0_lbFIO1")
                ?.text()
                ?.normalizeCabinetText()
                ?.takeIf { it.isNotBlank() }
            ?: progressDoc?.selectFirst("#ctl00_ctl00_ContentPlaceHolder0_lbFIO1")
                ?.text()
                ?.normalizeCabinetText()
                ?.takeIf { it.isNotBlank() }

        val faculty = progressDoc
            ?.selectFirst("#ctl00_ctl00_ContentPlaceHolder0_ContentPlaceHolder1_ctlStudProgress1_lbStudFacultet")
            ?.text()
            ?.normalizeCabinetText()
            ?.takeIf { it.isNotBlank() }

        val groupInfo = progressDoc
            ?.selectFirst("#ctl00_ctl00_ContentPlaceHolder0_ContentPlaceHolder1_ctlStudProgress1_lbStudKurs")
            ?.text()
            ?.normalizeCabinetText()
            ?.takeIf { it.isNotBlank() }

        val averageScore = progressDoc
            ?.selectFirst("#ctl00_ctl00_ContentPlaceHolder0_ContentPlaceHolder1_ctlStudProgress1_lbStudBall")
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

    private fun parseCabinetMenuLinks(doc: org.jsoup.nodes.Document): List<CabinetMenuLink> {
        return doc.select(".Sub1 a[href], .Sub2 a[href]")
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

    private fun String.normalizeAverageScore(): String {
        return normalizeCabinetText()
            .removePrefix("средний балл:")
            .removePrefix("Средний балл:")
            .trim()
    }

    private fun String.normalizeCabinetText(): String {
        return replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun loadPhotoImage(): ByteArray {
        val request = Request.Builder()
            .url(PHOTO_URL)
            .get()
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Не удалось загрузить фото: HTTP ${response.code}")
            }

            return response.body?.bytes() ?: error("Пустой ответ фото")
        }
    }

    fun parseProgressTable(html: String, pageUrl: String = STUD_PROGRESS_URL): ProgressTableResult {
        val doc = Jsoup.parse(html, pageUrl)

        val table = doc.selectFirst("#ctl00_ctl00_ContentPlaceHolder0_ContentPlaceHolder1_ctlStudProgress1_tblProgress")
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

    private fun classifyResult(zachValue: String, examValue: String): Pair<String, String>? {
        val zachGrade = extractGrade(zachValue)
        val examGrade = extractGrade(examValue)

        val zachIsPass = isPass(zachValue)
        val examHasGrade = examGrade != null

        return when {
            zachGrade != null -> "Диф. зачет" to zachGrade
            zachIsPass && examHasGrade -> "Диф. зачет" to examGrade
            zachIsPass && !examHasGrade -> "Зачет" to "+"
            !zachIsPass && examHasGrade -> "Экзамен" to examGrade
            else -> null
        }
    }

    private fun extractCellValue(td: Element?): String {
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

    private fun extractGrade(value: String): String? {
        val cleaned = value.trim()
        val match = Regex("""\b([0-9]|10)\b""").find(cleaned)
        return match?.groupValues?.get(1)
    }

    private fun isPass(value: String): Boolean {
        val normalized = value.trim().lowercase()
        return normalized == "+" ||
            normalized.contains("зачтено") ||
            normalized == "зачет"
    }

}
