package com.example.scheduleiseu.data.remote.datasource.impl

import com.example.scheduleiseu.data.remote.datasource.AuthRemoteDataSource
import com.example.scheduleiseu.data.remote.model.LoginPageData
import com.example.scheduleiseu.data.remote.model.LoginResult
import com.example.scheduleiseu.data.remote.parser.BsuParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRemoteDataSourceImpl(
    private val parser: BsuParser
) : AuthRemoteDataSource {

    override suspend fun loadLoginPage(): LoginPageData = withContext(Dispatchers.IO) {
        parser.loadLoginPage()
    }

    override suspend fun loadCaptcha(loginPageData: LoginPageData): ByteArray = withContext(Dispatchers.IO) {
        parser.loadCaptchaImage(loginPageData)
    }

    override suspend fun login(
        loginPageData: LoginPageData,
        username: String,
        password: String,
        captcha: String
    ): LoginResult = withContext(Dispatchers.IO) {
        parser.login(
            loginPageData = loginPageData,
            username = username,
            password = password,
            captcha = captcha
        )
    }

    override suspend fun logout(): Boolean = withContext(Dispatchers.IO) {
        parser.logoutFromCabinet()
    }
}
