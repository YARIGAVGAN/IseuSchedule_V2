package com.example.scheduleiseu.data.remote.parser

import com.example.scheduleiseu.data.remote.model.CurrentWeekInfo
import com.example.scheduleiseu.data.remote.model.SelectOption
import com.example.scheduleiseu.data.remote.model.TeacherTimeTableData
import com.example.scheduleiseu.data.remote.model.TimeTableData
import com.example.scheduleiseu.data.remote.model.TimeTableFormData
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.GZIPInputStream

class TimeTableParser(
    private val client: OkHttpClient = OkHttpClient()
) {

    companion object {
        private const val STUDENT_URL = "http://rsp.iseu.by/Raspisanie/TimeTable/umu.aspx"
        private const val TEACHER_URL = "http://rsp.iseu.by/Raspisanie/TimeTable/umuteachers.aspx"
        private val WEEK_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru"))
    }

    fun getInitialStudentPage(): TimeTableData? {
        return try {
            val html = sendGetRequest(STUDENT_URL)
            html?.let { parseStudentPage(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun getInitialTeacherPage(): TeacherTimeTableData? {
        return try {
            val html = sendGetRequest(TEACHER_URL)
            html?.let { parseTeacherPage(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun getCurrentStudentWeek(): CurrentWeekInfo? {
        return try {
            val html = sendGetRequest(STUDENT_URL) ?: return null
            val data = parseStudentPage(html)
            data.currentWeek ?: data.selectedWeek?.let { CurrentWeekInfo(it.value, it.text) }
        } catch (_: Exception) {
            null
        }
    }

    fun getCurrentTeacherWeek(): CurrentWeekInfo? {
        return try {
            val html = sendGetRequest(TEACHER_URL) ?: return null
            val data = parseTeacherPage(html)
            data.currentWeek ?: data.selectedWeek?.let { CurrentWeekInfo(it.value, it.text) }
        } catch (_: Exception) {
            null
        }
    }

    fun updateOnFacultySelect(facultyId: String, currentData: TimeTableData): TimeTableData? {
        return try {
            val html = sendPostRequest(
                url = STUDENT_URL,
                formData = currentData.formData,
                additionalParams = mapOf("ddlFac" to facultyId)
            )
            html?.let { parseStudentPage(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun updateOnDepartmentSelect(
        facultyId: String,
        departmentId: String,
        currentData: TimeTableData
    ): TimeTableData? {
        return try {
            val html = sendPostRequest(
                url = STUDENT_URL,
                formData = currentData.formData,
                additionalParams = mapOf(
                    "ddlFac" to facultyId,
                    "ddlDep" to departmentId
                )
            )
            html?.let { parseStudentPage(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun updateOnCourseSelect(
        facultyId: String,
        departmentId: String,
        courseId: String,
        currentData: TimeTableData
    ): TimeTableData? {
        return try {
            val html = sendPostRequest(
                url = STUDENT_URL,
                formData = currentData.formData,
                additionalParams = mapOf(
                    "ddlFac" to facultyId,
                    "ddlDep" to departmentId,
                    "ddlCourse" to courseId
                )
            )
            html?.let { parseStudentPage(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun updateOnGroupSelect(
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String,
        currentData: TimeTableData
    ): TimeTableData? {
        return try {
            val html = sendPostRequest(
                url = STUDENT_URL,
                formData = currentData.formData,
                additionalParams = mapOf(
                    "ddlFac" to facultyId,
                    "ddlDep" to departmentId,
                    "ddlCourse" to courseId,
                    "ddlGroup" to groupId
                )
            )
            html?.let { parseStudentPage(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun getStudentTimeTable(
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String,
        weekDate: String,
        currentData: TimeTableData
    ): TimeTableData? {
        return try {
            val html = sendPostRequest(
                url = STUDENT_URL,
                formData = currentData.formData,
                additionalParams = mapOf(
                    "ddlFac" to facultyId,
                    "ddlDep" to departmentId,
                    "ddlCourse" to courseId,
                    "ddlGroup" to groupId,
                    "ddlWeek" to weekDate,
                    "ShowTT" to "Показать"
                )
            )
            html?.let { parseStudentPage(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun getTeacherTimeTable(
        teacherId: String,
        weekDate: String,
        currentData: TeacherTimeTableData
    ): TeacherTimeTableData? {
        return try {
            val html = sendPostRequest(
                url = TEACHER_URL,
                formData = currentData.formData,
                additionalParams = mapOf(
                    "DropDownList1" to teacherId,
                    "ddlWeek" to weekDate,
                    "cbShowDraftForTeacher" to "on",
                    "Show" to "Показать"
                )
            )
            html?.let { parseTeacherPage(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseStudentPage(html: String): TimeTableData {
        val document = Jsoup.parse(html)

        val formData = parseFormData(document)
        val faculties = parseSelectOptions(document, "ddlFac")
        val departments = parseSelectOptions(document, "ddlDep")
        val courses = parseSelectOptions(document, "ddlCourse")
        val groups = parseSelectOptions(document, "ddlGroup")
        val weeks = parseSelectOptions(document, "ddlWeek")

        val selectedWeek = weeks.find { it.isSelected }
        val currentWeek = parseCurrentWeekFromOptions(weeks)
            ?: parseCurrentWeekFromCaption(document)
            ?: parseCurrentWeekFromChosen(document)

        val timeTableHtml = document.select("#TT").outerHtml()

        return TimeTableData(
            formData = formData,
            faculties = faculties,
            departments = departments,
            courses = courses,
            groups = groups,
            weeks = weeks,
            selectedWeek = selectedWeek,
            currentWeek = currentWeek,
            timeTableHtml = timeTableHtml
        )
    }

    private fun parseTeacherPage(html: String): TeacherTimeTableData {
        val document = Jsoup.parse(html)

        val formData = parseFormData(document)
        val teachers = parseSelectOptions(document, "DropDownList1")
        val weeks = parseSelectOptions(document, "ddlWeek")

        val selectedWeek = weeks.find { it.isSelected }
        val currentWeek = parseCurrentWeekFromOptions(weeks)
            ?: parseCurrentWeekFromCaption(document)
            ?: parseCurrentWeekFromChosen(document)

        val timeTableHtml = document.select("#TT").outerHtml()

        return TeacherTimeTableData(
            formData = formData,
            teachers = teachers,
            weeks = weeks,
            selectedWeek = selectedWeek,
            currentWeek = currentWeek,
            timeTableHtml = timeTableHtml
        )
    }

    private fun parseCurrentWeekFromOptions(weeks: List<SelectOption>): CurrentWeekInfo? {
        val today = LocalDate.now()
        val currentOption = weeks.firstOrNull { option ->
            val weekStart = option.value.toWeekStartDateOrNull() ?: option.text.toWeekStartDateOrNull()
            weekStart != null && !today.isBefore(weekStart) && !today.isAfter(weekStart.plusDays(6))
        } ?: weeks.firstOrNull { it.isSelected }

        return currentOption?.let { CurrentWeekInfo(it.value, it.text) }
    }

    private fun parseCurrentWeekFromChosen(document: Document): CurrentWeekInfo? {
        val text = document.selectFirst("#ddlWeek_chosen .chosen-single span")
            ?.text()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return CurrentWeekInfo(
            value = "$text 0:00:00",
            text = text
        )
    }

    private fun parseCurrentWeekFromCaption(document: Document): CurrentWeekInfo? {
        val caption = document.selectFirst("#TT caption")?.text().orEmpty()
        val match = Regex("""на неделю с (\d{2}\.\d{2}\.\d{4})""").find(caption) ?: return null
        val text = match.groupValues[1]
        return CurrentWeekInfo(
            value = "$text 0:00:00",
            text = text
        )
    }

    private fun parseFormData(document: Document): TimeTableFormData {
        return TimeTableFormData(
            viewState = document.select("#__VIEWSTATE").`val`(),
            eventValidation = document.select("#__EVENTVALIDATION").`val`(),
            viewStateGenerator = document.select("#__VIEWSTATEGENERATOR").`val`()
        )
    }

    private fun parseSelectOptions(document: Document, selectId: String): List<SelectOption> {
        val options = mutableListOf<SelectOption>()

        val selectElement = document.select("#$selectId")
        if (selectElement.isNotEmpty()) {
            selectElement.first()?.select("option")?.forEach { option ->
                options.add(
                    SelectOption(
                        value = option.attr("value"),
                        text = option.text().trim(),
                        isSelected = option.hasAttr("selected")
                    )
                )
            }
        }

        return options
    }

    private fun sendGetRequest(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
            .addHeader("Accept-Encoding", "gzip, deflate")
            .addHeader("Accept-Language", "ru,en;q=0.9")
            .addHeader("Cache-Control", "max-age=0")
            .addHeader("Connection", "keep-alive")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 YaBrowser/24.10.0.0 Safari/537.36")
            .build()

        return executeRequest(request)
    }

    private fun sendPostRequest(
        url: String,
        formData: TimeTableFormData,
        additionalParams: Map<String, String>
    ): String? {
        val formBody = buildFormBody(formData, additionalParams)

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
            .addHeader("Accept-Encoding", "gzip, deflate")
            .addHeader("Accept-Language", "ru,en;q=0.9")
            .addHeader("Cache-Control", "max-age=0")
            .addHeader("Connection", "keep-alive")
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Origin", url.substringBefore("/Raspisanie"))
            .addHeader("Referer", url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 YaBrowser/24.10.0.0 Safari/537.36")
            .build()

        return executeRequest(request)
    }

    private fun buildFormBody(formData: TimeTableFormData, additionalParams: Map<String, String>): RequestBody {
        val formBuilder = FormBody.Builder()

        formBuilder.add("__EVENTTARGET", "")
        formBuilder.add("__EVENTARGUMENT", "")
        formBuilder.add("__LASTFOCUS", "")

        formData.viewState?.let { formBuilder.add("__VIEWSTATE", it) }
        formData.eventValidation?.let { formBuilder.add("__EVENTVALIDATION", it) }
        formData.viewStateGenerator?.let { formBuilder.add("__VIEWSTATEGENERATOR", it) }

        additionalParams.forEach { (key, value) ->
            formBuilder.add(key, value)
        }

        formBuilder.add("iframeheight", "400")
        return formBuilder.build()
    }

    private fun executeRequest(request: Request): String? {
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null

                val body = response.body ?: return null
                val contentEncoding = response.header("Content-Encoding")

                if (contentEncoding != null && contentEncoding.equals("gzip", ignoreCase = true)) {
                    GZIPInputStream(body.byteStream()).bufferedReader().use { reader ->
                        reader.readText()
                    }
                } else {
                    body.string()
                }
            }
        } catch (_: IOException) {
            null
        }
    }

    private fun String.toWeekStartDateOrNull(): LocalDate? {
        val rawDate = Regex("\\d{2}\\.\\d{2}\\.\\d{4}").find(this)?.value ?: return null
        return runCatching { LocalDate.parse(rawDate, WEEK_DATE_FORMATTER) }.getOrNull()
    }

}
