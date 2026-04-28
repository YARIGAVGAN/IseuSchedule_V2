package com.example.scheduleiseu.data.mapper.contract

import com.example.scheduleiseu.data.remote.model.LoginPageData
import com.example.scheduleiseu.domain.core.model.AuthSession

fun interface LoginPageDataToAuthSessionMapper : DataToDomainMapper<LoginPageData, AuthSession>
