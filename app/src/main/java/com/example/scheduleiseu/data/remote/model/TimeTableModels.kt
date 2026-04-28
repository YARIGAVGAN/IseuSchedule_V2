package com.example.scheduleiseu.data.remote.model

data class TimeTableFormData(
    val viewState: String?,
    val eventValidation: String?,
    val viewStateGenerator: String?
)

data class SelectOption(
    val value: String,
    val text: String,
    val isSelected: Boolean = false
) {
    override fun toString(): String = text
}

data class CurrentWeekInfo(
    val value: String,
    val text: String
)

data class TimeTableData(
    val formData: TimeTableFormData,
    val faculties: List<SelectOption>,
    val departments: List<SelectOption>,
    val courses: List<SelectOption>,
    val groups: List<SelectOption>,
    val weeks: List<SelectOption>,
    val selectedWeek: SelectOption? = null,
    val currentWeek: CurrentWeekInfo? = null,
    val timeTableHtml: String = ""
)

data class TeacherTimeTableData(
    val formData: TimeTableFormData,
    val teachers: List<SelectOption>,
    val weeks: List<SelectOption>,
    val selectedWeek: SelectOption? = null,
    val currentWeek: CurrentWeekInfo? = null,
    val timeTableHtml: String = ""
)
