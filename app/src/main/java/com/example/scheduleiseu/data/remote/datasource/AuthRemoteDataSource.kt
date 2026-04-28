package com.example.scheduleiseu.data.remote.datasource

import com.example.scheduleiseu.data.remote.model.LoginPageData
import com.example.scheduleiseu.data.remote.model.LoginResult

interface AuthRemoteDataSource {
    suspend fun loadLoginPage(): LoginPageData
    suspend fun loadCaptcha(loginPageData: LoginPageData): ByteArray
    suspend fun login(
        loginPageData: LoginPageData,
        username: String,
        password: String,
        captcha: String
    ): LoginResult

    suspend fun logout(): Boolean
}
