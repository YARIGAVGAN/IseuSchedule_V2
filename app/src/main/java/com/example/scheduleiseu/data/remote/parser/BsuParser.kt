package com.example.scheduleiseu.data.remote.parser

import com.example.scheduleiseu.data.remote.cookie.MemoryCookieJar
import com.example.scheduleiseu.data.remote.model.LoginPageData
import com.example.scheduleiseu.data.remote.model.LoginResult
import com.example.scheduleiseu.data.remote.model.ProfileData
import com.example.scheduleiseu.data.remote.model.ProgressTableResult
import com.example.scheduleiseu.data.remote.model.SemesterLink
import com.example.scheduleiseu.data.remote.parser.cabinet.BsuCabinetConfig
import com.example.scheduleiseu.data.remote.parser.cabinet.classifyResult
import com.example.scheduleiseu.data.remote.parser.cabinet.extractCellValue
import com.example.scheduleiseu.data.remote.parser.cabinet.extractDisplayName
import com.example.scheduleiseu.data.remote.parser.cabinet.extractEventArgumentFromJsPostBack
import com.example.scheduleiseu.data.remote.parser.cabinet.extractEventTargetFromJsPostBack
import com.example.scheduleiseu.data.remote.parser.cabinet.extractLoginError
import com.example.scheduleiseu.data.remote.parser.cabinet.findAvailableSemesters
import com.example.scheduleiseu.data.remote.parser.cabinet.isCabinetPage
import com.example.scheduleiseu.data.remote.parser.cabinet.parseProfileData
import com.example.scheduleiseu.data.remote.parser.cabinet.parseProgressTable
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class BsuParser(
    private val cookieJar: MemoryCookieJar = MemoryCookieJar(),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    companion object {
        const val LOGIN_URL = BsuCabinetConfig.loginUrl
        const val STUD_PROGRESS_URL = BsuCabinetConfig.studProgressUrl
        const val CABINET_URL = BsuCabinetConfig.cabinetUrl
        const val PHOTO_URL = BsuCabinetConfig.photoUrl
    }

    fun loadLoginPage(): LoginPageData {
        val request = Request.Builder()
            .url(BsuCabinetConfig.loginUrl)
            .get()
            .header("User-Agent", BsuCabinetConfig.userAgent)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Не удалось загрузить страницу логина: HTTP ${response.code}")
            }

            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html, BsuCabinetConfig.loginUrl)

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
            .header("User-Agent", BsuCabinetConfig.userAgent)
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
            .header("User-Agent", BsuCabinetConfig.userAgent)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        client.newCall(request).execute().use { response ->
            val html = response.body?.string().orEmpty()

            val looksLikeLoginPage =
                html.contains(BsuCabinetConfig.loginTitleMarker, ignoreCase = true) &&
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
        return com.example.scheduleiseu.data.remote.parser.cabinet.isCabinetPage(html)
    }

    fun extractDisplayName(html: String): String? {
        return com.example.scheduleiseu.data.remote.parser.cabinet.extractDisplayName(html)
    }

    fun extractLoginError(html: String): String? {
        return com.example.scheduleiseu.data.remote.parser.cabinet.extractLoginError(html)
    }

    fun getPage(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", BsuCabinetConfig.userAgent)
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
            .header("User-Agent", BsuCabinetConfig.userAgent)
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
        val pageHtml = runCatching { getPage(BsuCabinetConfig.cabinetUrl) }.getOrElse { return false }
        val doc = Jsoup.parse(pageHtml, BsuCabinetConfig.cabinetUrl)
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
            pageUrl = BsuCabinetConfig.cabinetUrl,
            pageHtml = pageHtml,
            eventTarget = eventTarget,
            eventArgument = eventArgument
        )

        return !isCabinetPage(resultHtml)
    }

    fun getAvailableSemesters(): List<SemesterLink> {
        val html = getPage(BsuCabinetConfig.studProgressUrl)
        return findAvailableSemesters(html, BsuCabinetConfig.studProgressUrl)
    }
    fun findAvailableSemesters(
        pageHtml: String,
        pageUrl: String = BsuCabinetConfig.studProgressUrl
    ): List<SemesterLink> {
        return com.example.scheduleiseu.data.remote.parser.cabinet.findAvailableSemesters(pageHtml, pageUrl)
    }

    fun openSemester(semester: SemesterLink): String {
        val studProgressHtml = getPage(BsuCabinetConfig.studProgressUrl)
        return doPostBack(
            pageUrl = BsuCabinetConfig.studProgressUrl,
            pageHtml = studProgressHtml,
            eventTarget = semester.eventTarget,
            eventArgument = semester.eventArgument
        )
    }

    fun openLatestSemester(): Pair<SemesterLink, String> {
        val studProgressHtml = getPage(BsuCabinetConfig.studProgressUrl)
        val semesters = findAvailableSemesters(studProgressHtml, BsuCabinetConfig.studProgressUrl)

        val latest = semesters.firstOrNull { it.isSelected }
            ?: semesters.lastOrNull()
            ?: error("Семестры не найдены на странице успеваемости")

        val html = doPostBack(
            pageUrl = BsuCabinetConfig.studProgressUrl,
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
        return com.example.scheduleiseu.data.remote.parser.cabinet.parseProfileData(
            cabinetHtml = cabinetHtml,
            progressHtml = progressHtml,
            photoBytes = photoBytes
        )
    }

    fun loadPhotoImage(): ByteArray {
        val request = Request.Builder()
            .url(BsuCabinetConfig.photoUrl)
            .get()
            .header("User-Agent", BsuCabinetConfig.userAgent)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Не удалось загрузить фото: HTTP ${response.code}")
            }

            return response.body?.bytes() ?: error("Пустой ответ фото")
        }
    }

    fun parseProgressTable(html: String, pageUrl: String = BsuCabinetConfig.studProgressUrl): ProgressTableResult {
        return com.example.scheduleiseu.data.remote.parser.cabinet.parseProgressTable(html, pageUrl)
    }

}
