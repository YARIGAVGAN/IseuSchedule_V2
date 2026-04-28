package com.example.scheduleiseu.data.remote.model

data class LoginPageData(
    val viewState: String,
    val eventValidation: String,
    val viewStateGenerator: String,
    val captchaUrl: String,
    val loginUrl: String
)

data class LoginResult(
    val success: Boolean,
    val html: String
)

data class SemesterLink(
    val title: String,
    val eventTarget: String,
    val eventArgument: String = "",
    val isSelected: Boolean = false
) {
    override fun toString(): String = title
}

data class ProgressItem(
    val subject: String,
    val type: String,
    val result: String
)

data class ProgressTableResult(
    val semesterTitle: String,
    val items: List<ProgressItem>
)
